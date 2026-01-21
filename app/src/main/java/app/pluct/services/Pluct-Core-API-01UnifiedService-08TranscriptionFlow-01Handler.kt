package app.pluct.services

import android.util.Log
import app.pluct.core.debug.PluctCoreDebug01LogManager
import app.pluct.data.entity.ProcessingStatus
import kotlinx.coroutines.*
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
        
        // UX IMPROVEMENT #4 + #5: Check for already-completed videos with cache validity
        val existingCompletedVideo = videoRepository?.getVideoByUrl(sanitizedUrl)
        if (existingCompletedVideo != null &&
            existingCompletedVideo.status == ProcessingStatus.COMPLETED &&
            existingCompletedVideo.transcript != null) {

            // UX FIX #5: Check cache age (invalidate after 24 hours)
            val cacheAgeMs = existingCompletedVideo.transcriptCachedAt?.let {
                System.currentTimeMillis() - it
            } ?: 0L
            val cacheValidMs = 24 * 60 * 60 * 1000L // 24 hours
            val isCacheValid = cacheAgeMs < cacheValidMs

            if (isCacheValid) {
                Log.d(TAG, "URL $sanitizedUrl already completed with valid cache (age=${cacheAgeMs}ms), returning existing result")
                debugLogManager.logInfo(
                    category = "TRANSCRIPTION",
                    operation = "cache_hit",
                    message = "Video already transcribed, returning cached transcript",
                    details = "URL=$sanitizedUrl; TranscriptLength=${existingCompletedVideo.transcript.length}; CacheAge=${cacheAgeMs}ms"
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
            } else {
                Log.d(TAG, "URL $sanitizedUrl cache expired (age=${cacheAgeMs}ms > ${cacheValidMs}ms), re-transcribing")
                debugLogManager.logInfo(
                    category = "TRANSCRIPTION",
                    operation = "cache_expired",
                    message = "Cached transcript expired, re-transcribing",
                    details = "URL=$sanitizedUrl; CacheAge=${cacheAgeMs}ms"
                )
                // UX FIX #2: Update video status to indicate refresh in progress
                val refreshingVideo = existingCompletedVideo.copy(
                    status = ProcessingStatus.PROCESSING,
                    progress = 0,
                    failureReason = "Refreshing transcript (cache expired)"
                )
                videoRepository?.insertVideo(refreshingVideo)
            }
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
        suspend fun cleanupAndReturn(result: Result<TranscriptionStatusResponse>): Result<TranscriptionStatusResponse> {
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

        // Helper functions moved to PluctCoreAPI01UnifiedService08TranscriptionFlow05Helpers

        // Canonicalize short TikTok links using extracted handler
        val (canonicalizedUrl, _) = PluctCoreAPI01UnifiedService08TranscriptionFlow05Canonicalization01Handler.canonicalizeUrl(
            sanitizedUrl = sanitizedUrl,
            validator = validator,
            flowRequestId = flowRequestId,
            updateDebug = { step, jobId, pollingAttempt, maxPolling, entry -> updateDebug(step, jobId, pollingAttempt, maxPolling, entry) }
        )
        sanitizedUrl = canonicalizedUrl

        // Parallel metadata fetch and token vending
        val parallelStart = System.currentTimeMillis()
        updateDebug(OperationStep.METADATA, null, null, null, null)
        
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
                null, null, null,
                OperationTimelineEntry(
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
                null, null, null,
                OperationTimelineEntry(
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
        val vendToken = tokenResult.getOrNull()!!

        // Submit transcription using extracted submission handler
        val submissionHandler = PluctCoreAPI01UnifiedService08TranscriptionFlow06Submission02Handler(
            debugLogManager = debugLogManager,
            tokenCache = tokenCache,
            baseUrl = baseUrl,
            userIdentification = userIdentification
        )
        
        val submissionResult = submissionHandler.submitWithRetry(
            url = sanitizedUrl,
            vendToken = vendToken,
            clientRequestId = clientRequestId,
            flowRequestId = flowRequestId,
            sanitizedUrl = sanitizedUrl,
            submitTranscriptionJob = submitTranscriptionJob,
            createTokenVendingHandler = { createTokenVendingHandler(it) },
            updateDebug = { step, jobIdParam, entry -> updateDebug(step, jobIdParam, null, null, entry) }
        )
        
        if (submissionResult.isFailure) {
            return cleanupAndReturn(submissionResult.map { throw Exception("unreachable") })
        }
        
        val (jobId, hasRefreshedAuth) = submissionResult.getOrNull()!!

        // Poll for completion using extracted polling handler
        val pollingHandler = PluctCoreAPI01UnifiedService08TranscriptionFlow07Polling02Handler(
            debugLogManager = debugLogManager,
            circuitBreaker = circuitBreaker,
            jwtGenerator = jwtGenerator,
            userIdentification = userIdentification,
            baseUrl = baseUrl
        )
        
        val pollAuthToken = jwtGenerator.generateUserJWT(userIdentification.userId)
        
        return pollingHandler.pollUntilComplete(
            jobId = jobId,
            pollAuthToken = pollAuthToken,
            sanitizedUrl = sanitizedUrl,
            flowRequestId = flowRequestId,
            clientRequestId = clientRequestId,
            isBackground = isBackground,
            pollIntervalMsFast = pollIntervalMsFast,
            pollIntervalMsSlow = pollIntervalMsSlow,
            fastPollAttempts = fastPollAttempts,
            maxPollAttempts = maxPollAttempts,
            hasRefreshedAuth = hasRefreshedAuth,
            pollTranscriptionStatus = pollTranscriptionStatus,
            updateDebug = { step, jobId, pollingAttempt, maxPolling, entry -> updateDebug(step, jobId, pollingAttempt, maxPolling, entry) },
            cleanupAndReturn = { result -> cleanupAndReturn(result) }
        )
    }
}
