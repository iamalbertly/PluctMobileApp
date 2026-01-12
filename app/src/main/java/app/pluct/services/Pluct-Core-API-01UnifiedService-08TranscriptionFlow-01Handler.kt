package app.pluct.services

import android.util.Log
import app.pluct.core.debug.PluctCoreDebug01LogManager
import app.pluct.services.api.PluctCoreAPITranscriptionResult01Extractor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import kotlin.coroutines.coroutineContext

/**
 * Pluct-Core-API-01UnifiedService-08TranscriptionFlow-01Handler
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[TranscriptionFlow]-[Handler]
 * 6 scope layers: Project, Core, API, UnifiedService, TranscriptionFlow, Handler
 * Handles the complete TikTok video transcription flow
 */
class PluctCoreAPI01UnifiedService08TranscriptionFlow01Handler(
    private val validator: PluctCoreValidationInputSanitizer,
    private val deduplicationCoordinator: PluctCoreAPI01UnifiedService03Deduplication01Coordinator,
    private val videoRepository: app.pluct.data.repository.PluctVideoRepository?,
    private val debugLogManager: PluctCoreDebug01LogManager,
    private val tokenCache: PluctCoreAPIUnifiedServiceTokenCache,
    private val rateLimitTracker: PluctCoreRateLimitTracker,
    private val userIdentification: PluctCoreUserIdentification,
    private val jwtGenerator: PluctCoreAPIJWTGenerator,
    private val circuitBreaker: PluctCoreAPIUnifiedServiceCircuitBreaker,
    private val baseUrl: String,
    private val pollIntervalMsFast: Long,
    private val pollIntervalMsSlow: Long,
    private val fastPollAttempts: Int,
    private val maxPollAttempts: Int,
    private val onDebugFlowUpdate: (TranscriptionDebugInfo) -> Unit,
    private val getVendTokenBlockedUntil: () -> Long,
    private val setVendTokenBlockedUntil: (Long) -> Unit,
    private val vendToken: suspend (String) -> Result<VendTokenResponse>,
    private val getMetadata: suspend (String, Long?) -> Result<MetadataResponse>,
    private val submitTranscriptionJob: suspend (String, String, String) -> Result<TranscriptionResponse>,
    private val pollTranscriptionStatus: suspend (String, String) -> Result<TranscriptionStatusResponse>
) {
    // Token vending handler - single source of truth for token vending logic
    // Note: onDebugUpdate is a lambda that will be set per-flow to use the correct flowRequestId
    private fun createTokenVendingHandler(flowRequestId: String): PluctCoreAPI01UnifiedService08TranscriptionFlow02TokenVending01Handler {
        return PluctCoreAPI01UnifiedService08TranscriptionFlow02TokenVending01Handler(
            tokenCache = tokenCache,
            rateLimitTracker = rateLimitTracker,
            userIdentification = userIdentification,
            debugLogManager = debugLogManager,
            baseUrl = baseUrl,
            getVendTokenBlockedUntil = getVendTokenBlockedUntil,
            setVendTokenBlockedUntil = setVendTokenBlockedUntil,
            vendToken = vendToken,
            onDebugUpdate = { step, entry -> 
                // Convert to TranscriptionDebugInfo format using current timeline
                val timeline = mutableListOf<OperationTimelineEntry>()
                entry?.let { timeline.add(it) }
                onDebugFlowUpdate(
                    TranscriptionDebugInfo(
                        url = "",
                        flowRequestId = flowRequestId,
                        clientRequestId = "",
                        currentStep = step,
                        timeline = timeline,
                        jobId = null,
                        pollingAttempt = null,
                        maxPollingAttempts = null,
                        flowStartTime = entry?.startTime ?: System.currentTimeMillis(),
                        totalDuration = entry?.duration?.toLong()
                    )
                )
            }
        )
    }
    companion object {
        private const val TAG = "TranscriptionFlowHandler"
    }

    suspend fun processTikTokVideo(
        url: String,
        isBackground: Boolean,
        healthStatus: Map<String, HealthStatus>
    ): Result<TranscriptionStatusResponse> {
        val validation = validator.validateUrl(url)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid URL"))
        }
        var sanitizedUrl = validation.sanitizedValue
        val flowRequestId = "flow_${System.currentTimeMillis()}"

        val tttHealth = healthStatus["ttt"]
        if (tttHealth == HealthStatus.UNHEALTHY) {
            debugLogManager.logWarning(
                category = "TRANSCRIPTION",
                operation = "ttt_unhealthy",
                message = "TTTranscribe health check reports unhealthy; delaying submission",
                details = "URL=$sanitizedUrl; Health=$tttHealth"
            )
            return Result.failure(Exception("TTTranscribe service is temporarily unavailable. Please retry in a few moments."))
        }
        
        // Use unified deduplication coordinator to check and register processing
        val processingJob = coroutineContext[Job] ?: SupervisorJob()
        val deduplicationResult = deduplicationCoordinator.checkAndRegisterProcessing(
            url = sanitizedUrl,
            job = processingJob,
            videoRepository = videoRepository,
            tier = app.pluct.data.entity.ProcessingTier.EXTRACT_SCRIPT
        )
        
        // UX IMPROVEMENT #4: Check for already-completed videos before processing
        val existingCompletedVideo = videoRepository?.getVideoByUrl(sanitizedUrl)
        if (existingCompletedVideo != null && 
            existingCompletedVideo.status == ProcessingStatus.COMPLETED && 
            existingCompletedVideo.transcript != null) {
            Log.d(TAG, "URL $sanitizedUrl already completed with transcript, returning existing result")
            debugLogManager.logInfo(
                category = "TRANSCRIPTION",
                operation = "already_completed",
                message = "Video already transcribed, returning existing transcript",
                details = "URL=$sanitizedUrl; TranscriptLength=${existingCompletedVideo.transcript.length}"
            )
            // Return success with existing transcript - user gets immediate result
            val existingStatus = app.pluct.services.TranscriptionStatusResponse(
                jobId = existingCompletedVideo.jobId ?: "",
                status = "completed",
                progress = 100,
                transcript = existingCompletedVideo.transcript,
                text = existingCompletedVideo.transcript,
                _cacheHit = true, // Mark as cache hit since we're returning existing result
                url = sanitizedUrl
            )
            return Result.success(existingStatus)
        }
        
        val clientRequestId: String
        when (deduplicationResult) {
            is PluctCoreAPI01UnifiedService03Deduplication01Coordinator.DeduplicationResult.AlreadyProcessing -> {
                Log.w(TAG, "URL $sanitizedUrl is already being processed, rejecting duplicate request: ${deduplicationResult.reason}")
                debugLogManager.logWarning(
                    category = "TRANSCRIPTION",
                    operation = "duplicate_prevention",
                    message = "Duplicate processing request rejected",
                    details = "URL=$sanitizedUrl; Reason=${deduplicationResult.reason}"
                )
                return Result.failure(Exception("This video is already being processed. Please wait for it to complete."))
            }
            is PluctCoreAPI01UnifiedService03Deduplication01Coordinator.DeduplicationResult.Registered -> {
                clientRequestId = deduplicationResult.requestId
                Log.d(TAG, "Processing registered for URL: $sanitizedUrl with requestId: $clientRequestId")
            }
            is PluctCoreAPI01UnifiedService03Deduplication01Coordinator.DeduplicationResult.Failure -> {
                Log.e(TAG, "Failed to register processing for URL: $sanitizedUrl - ${deduplicationResult.reason}")
                return Result.failure(Exception("Failed to register processing: ${deduplicationResult.reason}"))
            }
        }

        // Helper to ensure cleanup on all exit paths
        fun cleanupAndReturn(result: Result<TranscriptionStatusResponse>): Result<TranscriptionStatusResponse> {
            deduplicationCoordinator.unregisterProcessing(sanitizedUrl, clientRequestId)
            return result
        }

        val timeline = mutableListOf<OperationTimelineEntry>()
        fun updateDebug(
            step: OperationStep,
            jobId: String? = null,
            pollingAttempt: Int? = null,
            maxPolling: Int? = null,
            newEntry: OperationTimelineEntry? = null
        ) {
            newEntry?.let { timeline.add(it) }
            onDebugFlowUpdate(
                TranscriptionDebugInfo(
                    url = sanitizedUrl,
                    flowRequestId = flowRequestId,
                    clientRequestId = clientRequestId,
                    currentStep = step,
                    timeline = timeline.toList(),
                    jobId = jobId,
                    pollingAttempt = pollingAttempt,
                    maxPollingAttempts = maxPolling,
                    flowStartTime = timeline.firstOrNull()?.startTime ?: System.currentTimeMillis(),
                    totalDuration = null
                )
            )
        }

        fun isSubmitTimeout(error: Throwable?): Boolean {
            if (error == null) return false
            val msg = error.message ?: ""
            if (error is PluctCoreAPIDetailedError) {
                val status = error.technicalDetails.responseStatusCode
                if (status == 408 || status == 504) return true
                if (error.technicalDetails.errorCode.contains("timeout", ignoreCase = true)) return true
                if (error.userMessage.contains("timed out", ignoreCase = true)) return true
            }
            return msg.contains("timeout", ignoreCase = true) || msg.contains("timed out", ignoreCase = true)
        }

        fun extractJobIdFromError(error: Throwable?): String? {
            if (error !is PluctCoreAPIDetailedError) return null
            val body = error.technicalDetails.responseBody.takeIf { it.isNotBlank() } ?: return null
            return try {
                val json = Json { ignoreUnknownKeys = true }
                val obj = json.parseToJsonElement(body).jsonObject
                obj["jobId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                Regex("""jobId"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
            }
        }


        // Canonicalize short TikTok links
        val canonicalizeStart = System.currentTimeMillis()
        updateDebug(
            OperationStep.CANONICALIZE,
            newEntry = OperationTimelineEntry(
                step = OperationStep.CANONICALIZE,
                startTime = canonicalizeStart,
                endTime = null,
                duration = null,
                request = RequestDebugDetails("RESOLVE", sanitizedUrl, "resolve", "None", """{"originalUrl":"$sanitizedUrl"}""", canonicalizeStart),
                response = null,
                error = null,
                expected = "Resolve vm.tiktok.com shortlink to canonical @user/video/{id}",
                received = null,
                nextAction = "Use resolved URL for metadata/submit"
            )
        )
        val resolutionResult = validator.resolveTikTokRedirect(sanitizedUrl)
        val resolvedUrl = resolutionResult.resolvedUrl
        val resolutionEnd = System.currentTimeMillis()
        if (!resolvedUrl.isNullOrBlank()) {
            sanitizedUrl = resolvedUrl
            updateDebug(
                OperationStep.CANONICALIZE,
                newEntry = OperationTimelineEntry(
                    step = OperationStep.CANONICALIZE,
                    startTime = canonicalizeStart,
                    endTime = resolutionEnd,
                    duration = resolutionEnd - canonicalizeStart,
                    request = RequestDebugDetails("RESOLVE", resolutionResult.originalUrl, "resolve", "None", """{"redirectChain":${resolutionResult.redirectChain}}""", canonicalizeStart),
                    response = ResponseDebugDetails(200, "Canonicalized", "Resolved to $sanitizedUrl", resolutionEnd, resolutionEnd - canonicalizeStart, flowRequestId),
                    error = null,
                    expected = "Canonical TikTok URL ready",
                    received = sanitizedUrl,
                    nextAction = "Proceed to metadata",
                    correlationId = flowRequestId
                )
            )
        } else {
            updateDebug(
                OperationStep.CANONICALIZE,
                newEntry = OperationTimelineEntry(
                    step = OperationStep.CANONICALIZE,
                    startTime = canonicalizeStart,
                    endTime = resolutionEnd,
                    duration = resolutionEnd - canonicalizeStart,
                    request = null,
                    response = null,
                    error = resolutionResult.error,
                    expected = "Attempt redirect resolution",
                    received = resolutionResult.redirectChain.joinToString(" -> ").ifBlank { "No redirects" },
                    nextAction = "Proceed with original URL; resolution_missed=true",
                    correlationId = flowRequestId
                )
            )
        }

        // Parallel metadata fetch and token vending
        val parallelStart = System.currentTimeMillis()
        updateDebug(OperationStep.METADATA)
        
        val tokenVendingHandler = createTokenVendingHandler(flowRequestId)
        val (metadataResult, tokenResult) = coroutineScope {
            val metadataDeferred = async { getMetadata(sanitizedUrl, 8000L) }
            val tokenDeferred = async { 
                tokenVendingHandler.vendAndLog(
                    label = "Token Issued (Parallel)",
                    clientRequestId = clientRequestId,
                    flowRequestId = flowRequestId,
                    jobId = null,
                    force = false
                )
            }
            Pair(metadataDeferred.await(), tokenDeferred.await())
        }
        
        if (metadataResult.isSuccess) {
            val metadata = metadataResult.getOrNull()!!
            updateDebug(
                OperationStep.METADATA,
                newEntry = OperationTimelineEntry(
                    step = OperationStep.METADATA,
                    startTime = parallelStart,
                    endTime = System.currentTimeMillis(),
                    duration = System.currentTimeMillis() - parallelStart,
                    request = RequestDebugDetails("GET", "$baseUrl/meta", "/meta", "None", null, parallelStart),
                    response = ResponseDebugDetails(200, "OK", "Title=${metadata.title}; Author=${metadata.author}", System.currentTimeMillis(), System.currentTimeMillis() - parallelStart),
                    error = null,
                    expected = "200 OK with metadata (title, author, duration)",
                    received = "title=${metadata.title}; author=${metadata.author}; duration=${metadata.duration}",
                    nextAction = "Proceed to submit (token already vended in parallel)",
                    correlationId = flowRequestId
                )
            )
        } else {
            val metaError = metadataResult.exceptionOrNull()
            val message = metaError?.message ?: "Metadata unavailable (timeout/fallback)"
            updateDebug(
                OperationStep.METADATA,
                newEntry = OperationTimelineEntry(
                    step = OperationStep.METADATA,
                    startTime = parallelStart,
                    endTime = System.currentTimeMillis(),
                    duration = System.currentTimeMillis() - parallelStart,
                    request = RequestDebugDetails("GET", "$baseUrl/meta", "/meta", "None", null, parallelStart),
                    response = null,
                    error = message,
                    expected = "200 OK metadata response",
                    received = "Metadata failed; proceeding without metadata",
                    nextAction = "Proceed to submit (token already vended in parallel)",
                    correlationId = flowRequestId
                )
            )
        }

        if (tokenResult.isFailure) {
            return cleanupAndReturn(tokenResult.map { throw Exception("unreachable") })
        }
        var vendToken = tokenResult.getOrNull()!!
        var hasRefreshedAuth = false
        var recoveredJobId: String? = null

        // Submit transcription
        val submitStart = System.currentTimeMillis()
        updateDebug(OperationStep.SUBMIT)
        var submitResult = submitTranscriptionJob(sanitizedUrl, vendToken, clientRequestId)
        if (submitResult.isFailure) {
            val submitError = submitResult.exceptionOrNull()
            val timeoutSubmit = isSubmitTimeout(submitError)

            if (timeoutSubmit) {
                debugLogManager.logWarning(
                    category = "TRANSCRIPTION",
                    operation = "submit_timeout_retry",
                    message = submitError?.message ?: "Submit timed out; retrying with same clientRequestId",
                    details = "URL=$sanitizedUrl; requestId=$clientRequestId"
                )
                delay(1000)
                val retry = submitTranscriptionJob(sanitizedUrl, vendToken, clientRequestId)
                if (retry.isSuccess) {
                    submitResult = retry
                } else {
                    recoveredJobId = extractJobIdFromError(retry.exceptionOrNull())
                        ?: extractJobIdFromError(submitError)
                }
            }
            val authSubmit = submitError?.message?.contains("401", true) == true ||
                submitError?.message?.contains("auth", true) == true
            val tokenExpired = submitError?.message?.contains("token_expired", true) == true ||
                submitError?.message?.contains("session has expired", true) == true

            if (tokenExpired) {
                tokenCache.clearToken()
                updateDebug(
                    OperationStep.FAILED,
                    newEntry = OperationTimelineEntry(
                        OperationStep.SUBMIT,
                        submitStart,
                        System.currentTimeMillis(),
                        null, null, null,
                        "Business Engine returned token_expired; please retry after backend refresh"
                    )
                )
                return cleanupAndReturn(Result.failure(
                    Exception("Business Engine session expired. Please retry in a moment while we refresh access.")
                ))
            }
            if (authSubmit) {
                tokenCache.clearToken()
                val retryTokenVendingHandler = createTokenVendingHandler(flowRequestId)
                val retryToken = retryTokenVendingHandler.vendAndLog(
                    label = "Submit Auth Retry",
                    clientRequestId = clientRequestId,
                    flowRequestId = flowRequestId,
                    jobId = null,
                    force = true
                )
                if (retryToken.isSuccess) {
                    vendToken = retryToken.getOrNull()!!
                    hasRefreshedAuth = true
                    submitResult = submitTranscriptionJob(sanitizedUrl, vendToken, clientRequestId)
                }
                if (submitResult.isFailure) {
                    val secondError = submitResult.exceptionOrNull()
                    debugLogManager.logWarning(
                        category = "TRANSCRIPTION",
                        operation = "submit_auth_failure",
                        message = secondError?.message ?: "Submit 401 after retry",
                        details = "URL=$sanitizedUrl; user=${userIdentification.userId}; Note: Only one auth retry attempted"
                    )
                }
            }
        }
        if (submitResult.isFailure && recoveredJobId.isNullOrBlank()) {
            updateDebug(
                OperationStep.FAILED,
                newEntry = OperationTimelineEntry(
                    OperationStep.SUBMIT,
                    submitStart,
                    System.currentTimeMillis(),
                    null, null, null,
                    submitResult.exceptionOrNull()?.message,
                    expected = "202 Accepted with jobId",
                    received = submitResult.exceptionOrNull()?.message ?: "No response",
                    nextAction = "Surface submit failure; suggest retry after session refresh",
                    correlationId = flowRequestId
                )
            )
            return cleanupAndReturn(Result.failure(submitResult.exceptionOrNull() ?: Exception("Submit failed")))
        }
        val jobId = recoveredJobId ?: submitResult.getOrNull()!!.jobId
        updateDebug(
            OperationStep.SUBMIT,
            jobId = jobId,
            newEntry = OperationTimelineEntry(
                step = OperationStep.SUBMIT,
                startTime = submitStart,
                endTime = System.currentTimeMillis(),
                duration = System.currentTimeMillis() - submitStart,
                request = RequestDebugDetails("POST", "$baseUrl/ttt/transcribe", "/ttt/transcribe", "Authorization: Bearer ...", """{"url":"$sanitizedUrl"}""", submitStart),
                response = ResponseDebugDetails(202, if (recoveredJobId != null) "Accepted (Recovered)" else "Accepted", "jobId=$jobId", System.currentTimeMillis(), System.currentTimeMillis() - submitStart),
                error = null,
                expected = "202 Accepted + jobId",
                received = "jobId=$jobId",
                nextAction = "Begin polling",
                correlationId = flowRequestId
            )
        )
        val submitDuration = System.currentTimeMillis() - submitStart
        if (submitDuration > 20000) {
            debugLogManager.logWarning(
                category = "TRANSCRIPTION",
                operation = "submit_duration_warning",
                message = "Submit took ${submitDuration}ms",
                details = "JobId=$jobId; URL=$sanitizedUrl"
            )
        }

        // Use /ttt/poll/:id endpoint with user JWT
        var pollAuthToken = jwtGenerator.generateUserJWT(userIdentification.userId)

        val getPollInterval = { attempt: Int ->
            if (isBackground) {
                pollIntervalMsSlow * 2
            } else {
                if (attempt <= fastPollAttempts) pollIntervalMsFast else pollIntervalMsSlow
            }
        }

        // Poll for completion
        repeat(maxPollAttempts) { attempt ->
            val pollingAttempt = attempt + 1
            val currentPollInterval = getPollInterval(pollingAttempt)

            if (circuitBreaker.isOpen()) {
                debugLogManager.logWarning(
                    category = "TRANSCRIPTION",
                    operation = "polling_circuit_open",
                    message = "Circuit breaker open during polling; waiting to retry",
                    details = "Attempt $pollingAttempt of $maxPollAttempts"
                )
                delay(currentPollInterval)
                return@repeat
            }
            updateDebug(OperationStep.POLLING, jobId = jobId, pollingAttempt = pollingAttempt, maxPolling = maxPollAttempts)
            delay(currentPollInterval)
            val statusResult = pollTranscriptionStatus(jobId, pollAuthToken)
            if (statusResult.isFailure) {
                val cause = statusResult.exceptionOrNull()
                val authError = cause?.message?.contains("401", ignoreCase = true) == true ||
                    cause?.message?.contains("authentication failed", ignoreCase = true) == true ||
                    (cause?.message?.contains("500", ignoreCase = true) == true && cause.message?.contains("authentication", ignoreCase = true) == true)

                if (authError) {
                    if (!hasRefreshedAuth) {
                        debugLogManager.logWarning(
                            category = "TRANSCRIPTION",
                            operation = "polling_auth_refresh",
                            message = "Polling auth expired; regenerating user JWT",
                            details = "JobId=$jobId; Attempt=$pollingAttempt"
                        )
                        pollAuthToken = jwtGenerator.generateUserJWT(userIdentification.userId)
                        hasRefreshedAuth = true
                        return@repeat
                    }
                    return@repeat
                } else {
                    if (pollingAttempt < maxPollAttempts) {
                        debugLogManager.logWarning(
                            category = "TRANSCRIPTION",
                            operation = "polling_transient_failure",
                            message = cause?.message ?: "Polling failed; retrying",
                            details = "Attempt $pollingAttempt of $maxPollAttempts"
                        )
                        return@repeat
                    }
                }

                updateDebug(
                    OperationStep.FAILED,
                    jobId = jobId,
                    newEntry = OperationTimelineEntry(
                        OperationStep.POLLING,
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        null, null, null,
                        cause?.message,
                        expected = "2xx status with transcript or in-progress",
                        received = cause?.message ?: "Polling failure",
                        nextAction = "Stop polling; surface error",
                        correlationId = flowRequestId,
                        retryCount = pollingAttempt
                    )
                )
                return cleanupAndReturn(Result.failure(cause ?: Exception("Status check failed")))
            }
            val status = statusResult.getOrNull()
            if (status == null) {
                debugLogManager.logError(
                    category = "TRANSCRIPTION",
                    operation = "polling_null_status",
                    message = "Status result was null despite successful HTTP response - JobId=$jobId; Attempt=$pollingAttempt"
                )
                // Continue polling instead of failing immediately - may be transient
                if (pollingAttempt >= maxPollAttempts) {
                    return cleanupAndReturn(Result.failure(Exception("Status check returned null after $maxPollAttempts attempts")))
                }
                return@repeat
            }

            debugLogManager.logInfo(
                category = "TRANSCRIPTION",
                operation = "polling_status_received",
                message = "Received status from server",
                details = "JobId=$jobId; Status=${status.status}; Progress=${status.progress}; " +
                    "HasTranscript=${status.transcript != null}; " +
                    "HasResult=${status.result != null}; " +
                    "HasResultTranscription=${status.result?.transcription != null}"
            )

            val extraction = PluctCoreAPITranscriptionResult01Extractor.extract(status)
            val transcript = extraction.transcript

            if (transcript == null && (status.status.equals("completed", ignoreCase = true) || status.status.equals("done", ignoreCase = true))) {
        debugLogManager.logError(
            category = "TRANSCRIPTION",
            operation = "transcript_extraction_failed",
            message = "Job marked completed but transcript not found in any expected field",
            responseBody = buildString {
                        appendLine("JobId=$jobId; Attempt=$pollingAttempt")
                        appendLine("Status=${status.status}")
                        appendLine("HasTranscript=${status.transcript != null}")
                        appendLine("HasResult=${status.result != null}")
                        appendLine("HasResultTranscription=${status.result?.transcription != null}")
                        appendLine("HasText=${status.text != null}")
                        appendLine("TranscriptSource=${extraction.source}")
                    },
                    requestUrl = "$baseUrl/ttt/poll/$jobId"
                )
            }

            if (transcript != null) {
                debugLogManager.logInfo(
                    category = "TRANSCRIPTION",
                    operation = "transcript_found",
                    message = "Transcript extracted successfully",
                    details = "JobId=$jobId; Length=${transcript.length}; Source=${extraction.source}"
                )
            }

            val isCompleted = status.status.equals("completed", ignoreCase = true)
                || status.status.equals("done", ignoreCase = true)
                || transcript != null
                || status._cacheHit == true

            if (isCompleted) {
                if (status._cacheHit == true) {
                    debugLogManager.logInfo(
                        category = "TRANSCRIPTION",
                        operation = "cache_hit_detected",
                        message = "Job already completed and cached (instant result)",
                        details = "JobId=$jobId; Attempt=$pollingAttempt; TranscriptLength=${transcript?.length ?: 0}"
                    )
                }

                val normalizedStatus = status.copy(
                    transcript = transcript ?: status.transcript,
                    status = "completed"
                )
                updateDebug(
                    OperationStep.COMPLETED,
                    jobId = jobId,
                    newEntry = OperationTimelineEntry(
                        OperationStep.POLLING,
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        null, null, null, null,
                        expected = "status=completed with transcript",
                        received = "status=${status.status}; hasTranscript=${transcript != null}; cacheHit=${status._cacheHit}",
                        nextAction = "Render transcript",
                        correlationId = flowRequestId,
                        retryCount = pollingAttempt
                    )
                )
                return cleanupAndReturn(Result.success(normalizedStatus))
            }
            if (status.status.equals("failed", ignoreCase = true)) {
                updateDebug(
                    OperationStep.FAILED,
                    jobId = jobId,
                    newEntry = OperationTimelineEntry(
                        OperationStep.POLLING,
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        null, null, null,
                        "Remote status failed",
                        expected = "status=completed or transcript",
                        received = "status=${status.status}",
                        nextAction = "Stop polling; surface remote failure",
                        correlationId = flowRequestId,
                        retryCount = pollingAttempt
                    )
                )
                return cleanupAndReturn(Result.failure(Exception("Transcription failed remotely")))
            }
        }

        updateDebug(
            OperationStep.FAILED,
            jobId = jobId,
            newEntry = OperationTimelineEntry(
                OperationStep.POLLING,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                null, null, null,
                "Polling timeout",
                expected = "status=completed within ${maxPollAttempts * pollIntervalMsSlow}ms",
                received = "Timed out at attempt $maxPollAttempts",
                nextAction = "Stop; ask user to retry",
                correlationId = flowRequestId,
                retryCount = maxPollAttempts
            )
        )
        return cleanupAndReturn(Result.failure(Exception("Transcription timed out. Please retry in a moment.")))
    }
}
