package app.pluct.services

import android.content.Context
import android.util.Log
import app.pluct.architecture.PluctComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import app.pluct.core.debug.PluctCoreDebug01LogManager
import app.pluct.core.retry.PluctCoreRetryUnifiedHandler
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Pluct-Core-API-01UnifiedService-01Main - Unified API service orchestrator.
 * Delegates HTTP, retry, circuit, metrics, and debug tracking to scoped collaborators.
 */
@Singleton
class PluctCoreAPIUnifiedService @Inject constructor(
    private val logger: PluctCoreLoggingStructuredLogger,
    private val validator: PluctCoreValidationInputSanitizer,
    private val userIdentification: PluctCoreUserIdentification,
    private val rateLimitTracker: PluctCoreRateLimitTracker,
    private val debugLogManager: PluctCoreDebug01LogManager,
    @ApplicationContext private val context: Context
) : PluctComponent {

    companion object {
        private const val TAG = "PluctCoreAPIUnified"
        private const val BASE_URL = "https://pluct-business-engine.romeo-lya2.workers.dev"
        private const val POLL_INTERVAL_MS = 2000L
        private const val MAX_POLL_ATTEMPTS = 20
        private const val PREWARM_THROTTLE_MS = 60000L
    }

    private val httpClient = PluctCoreAPIHTTPClientImpl(logger, validator, userIdentification)
    private val retryHandler = PluctCoreRetryUnifiedHandler()
    private val circuitBreaker = PluctCoreAPIUnifiedServiceCircuitBreaker()
    private val metrics = PluctCoreAPIUnifiedServiceMetrics()
    private val jwtGenerator = PluctCoreAPIJWTGenerator()
    private val tokenCache = PluctCoreAPIUnifiedServiceTokenCache(context)

    private val _healthStatus = MutableStateFlow<Map<String, HealthStatus>>(emptyMap())
    val healthStatus: StateFlow<Map<String, HealthStatus>> = _healthStatus.asStateFlow()

    private val _transcriptionDebugFlow = MutableStateFlow<TranscriptionDebugInfo?>(null)
    val transcriptionDebugFlow: StateFlow<TranscriptionDebugInfo?> = _transcriptionDebugFlow.asStateFlow()

    val apiMetrics: StateFlow<APIMetrics> = metrics.apiMetrics

    private val transcriptionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prewarmTimestamps = ConcurrentHashMap<String, Long>()

    override val componentId: String = "pluct-core-api-unified-service"
    override val dependencies: List<String> = listOf(
        "pluct-core-logging-structured-logger",
        "pluct-core-validation-input-sanitizer",
        "pluct-core-user-identification",
        "pluct-core-api-rate-limit-tracker"
    )

    override fun initialize() {
        startHealthMonitoring()
    }

    override fun cleanup() {}

    suspend fun checkUserBalance(): Result<CreditBalanceResponse> {
        val userToken = jwtGenerator.generateUserJWT(userIdentification.userId)
        return execute("GET", "/v1/credits/balance", authToken = userToken)
    }

    suspend fun getEstimate(url: String): Result<EstimateResponse> {
        val userToken = jwtGenerator.generateUserJWT(userIdentification.userId)
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        return execute("GET", "/estimate?url=$encodedUrl", authToken = userToken)
    }

    suspend fun vendToken(clientRequestId: String = "req_${System.currentTimeMillis()}"): Result<VendTokenResponse> {
        val userToken = jwtGenerator.generateUserJWT(userIdentification.userId)
        val payload = mapOf(
            "userId" to userIdentification.userId,
            "clientRequestId" to clientRequestId
        )
        return execute("POST", "/v1/vend-token", payload, userToken)
    }


    suspend fun getMetadata(url: String, timeoutMs: Long? = null): Result<MetadataResponse> {
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        val userToken = jwtGenerator.generateUserJWT(userIdentification.userId)
        return execute("GET", "/meta?url=$encodedUrl", authToken = userToken, timeoutMs = timeoutMs)
    }

    suspend fun submitTranscriptionJob(url: String, serviceToken: String, clientRequestId: String): Result<TranscriptionResponse> {
        val payload = mapOf("url" to url, "clientRequestId" to clientRequestId)
        return execute("POST", "/ttt/transcribe", payload, serviceToken)
    }

    suspend fun checkTranscriptionStatus(jobId: String, serviceToken: String): Result<TranscriptionStatusResponse> {
        return execute("GET", "/ttt/status/$jobId", authToken = serviceToken)
    }

    fun preWarmVideoProcessing(url: String) {
        val validation = validator.validateUrl(url)
        if (!validation.isValid) return
        val sanitizedUrl = validation.sanitizedValue
        if (!validator.isTikTokUrl(sanitizedUrl)) return
        val now = System.currentTimeMillis()
        val lastPrewarm = prewarmTimestamps[sanitizedUrl]
        if (lastPrewarm != null && now - lastPrewarm < PREWARM_THROTTLE_MS) return
        prewarmTimestamps[sanitizedUrl] = now
        transcriptionScope.launch {
            Log.d(TAG, "Triggering background pre-warming for URL: $sanitizedUrl")
            getMetadata(sanitizedUrl, timeoutMs = 5000L)
        }
    }

    suspend fun processTikTokVideo(url: String, isBackground: Boolean = false): Result<TranscriptionStatusResponse> {
        val validation = validator.validateUrl(url)
        if (!validation.isValid) return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid URL"))
        var sanitizedUrl = validation.sanitizedValue
        val flowRequestId = "flow_${System.currentTimeMillis()}"
        val clientRequestId = "client_${System.currentTimeMillis()}"

        val timeline = mutableListOf<OperationTimelineEntry>()
        fun updateDebug(
            step: OperationStep,
            jobId: String? = null,
            pollingAttempt: Int? = null,
            maxPolling: Int? = null,
            newEntry: OperationTimelineEntry? = null
        ) {
            newEntry?.let { timeline.add(it) }
            _transcriptionDebugFlow.value = TranscriptionDebugInfo(
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
        }

        fun extractVendToken(response: VendTokenResponse): String? {
            return listOf(response.token, response.serviceToken, response.pollingToken)
                .firstOrNull { !it.isNullOrBlank() }
                ?.trim()
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
                // Fallback regex for jobId patterns
                Regex("""jobId"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
            }
        }

        suspend fun vendAndLog(label: String, jobId: String? = null, force: Boolean = false): Result<String> {
            if (!force) {
                val cached = tokenCache.getValidToken()
                if (cached != null) {
                    Log.d(TAG, "Using cached token")
                    debugLogManager.logInfo(
                        category = "TOKEN_MANAGEMENT",
                        operation = "token_reuse",
                        message = "Reusing cached token",
                        details = "Label: $label; JobId: $jobId"
                    )
                    return Result.success(cached)
                }
            }
            
            // Only vend if cache miss or forced
            val vendStart = System.currentTimeMillis()
            debugLogManager.logInfo(
                category = "TOKEN_MANAGEMENT",
                operation = "token_vend",
                message = "Vending new token",
                details = "Label: $label; Force: $force; JobId: $jobId"
            )
            val vendResult = vendToken(clientRequestId)
            if (vendResult.isSuccess) {
                val vendResponse = vendResult.getOrNull()!!
                val token = extractVendToken(vendResponse)
                if (token.isNullOrBlank()) {
                    val msg = "Vend token missing in response"
                    updateDebug(
                        OperationStep.FAILED,
                        newEntry = OperationTimelineEntry(
                            OperationStep.VEND_TOKEN,
                            vendStart,
                            System.currentTimeMillis(),
                            null,
                            null,
                            null,
                            msg
                        )
                    )
                    debugLogManager.logError(
                        category = "TRANSCRIPTION",
                        operation = "vend_token_missing",
                        message = msg,
                        requestUrl = "$BASE_URL/v1/vend-token",
                        responseBody = vendResponse.toString()
                    )
                    return Result.failure(Exception(msg))
                }
                updateDebug(
                    OperationStep.VEND_TOKEN,
                    jobId = jobId,
                    newEntry = OperationTimelineEntry(
                        step = OperationStep.VEND_TOKEN,
                        startTime = vendStart,
                        endTime = System.currentTimeMillis(),
                        duration = System.currentTimeMillis() - vendStart,
                        request = RequestDebugDetails("POST", "$BASE_URL/v1/vend-token", "/v1/vend-token", "Authorization: Bearer ...", """{"userId":"${userIdentification.userId}"}""", vendStart),
                    response = ResponseDebugDetails(200, label, "Refreshed for polling auth", System.currentTimeMillis(), System.currentTimeMillis() - vendStart),
                    error = null,
                    expected = "200 OK with serviceToken/pollingToken",
                    received = "token present? ${!token.isNullOrBlank()}",
                    nextAction = "Cache token and continue",
                    correlationId = flowRequestId
                )
            )
            tokenCache.cacheToken(token, vendResponse.expiresIn)
            return Result.success(token)
        } else {
            updateDebug(
                OperationStep.FAILED,
                newEntry = OperationTimelineEntry(
                    OperationStep.VEND_TOKEN,
                    vendStart,
                    System.currentTimeMillis(),
                    null,
                    null,
                    null,
                    vendResult.exceptionOrNull()?.message,
                    expected = "200 OK with token",
                    received = "Vend failed",
                    nextAction = "Stop flow; surface auth guidance",
                    correlationId = flowRequestId
                )
            )
            return Result.failure(vendResult.exceptionOrNull() ?: Exception("Token vending failed"))
        }
    }

        // Canonicalize short TikTok links before hitting metadata to avoid upstream ambiguity
        val canonicalizeStart = System.currentTimeMillis()
        updateDebug(
            OperationStep.CANONICALIZE,
            newEntry = OperationTimelineEntry(
                step = OperationStep.CANONICALIZE,
                startTime = canonicalizeStart,
                endTime = null,
                duration = null,
                request = RequestDebugDetails(
                    method = "RESOLVE",
                    url = sanitizedUrl,
                    endpoint = "resolve",
                    headers = "None",
                    payload = """{"originalUrl":"$sanitizedUrl"}""",
                    timestamp = canonicalizeStart
                ),
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
                    request = RequestDebugDetails(
                        method = "RESOLVE",
                        url = resolutionResult.originalUrl,
                        endpoint = "resolve",
                        headers = "None",
                        payload = """{"redirectChain":${resolutionResult.redirectChain}}""",
                        timestamp = canonicalizeStart
                    ),
                    response = ResponseDebugDetails(
                        statusCode = 200,
                        statusMessage = "Canonicalized",
                        body = "Resolved to $sanitizedUrl",
                        timestamp = resolutionEnd,
                        duration = resolutionEnd - canonicalizeStart,
                        serverRequestId = flowRequestId
                    ),
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

        // Metadata
        val metadataStart = System.currentTimeMillis()
        updateDebug(OperationStep.METADATA)
        val metadataResult = getMetadata(sanitizedUrl, timeoutMs = 8000L)
        if (metadataResult.isSuccess) {
            val metadata = metadataResult.getOrNull()!!
            updateDebug(
                OperationStep.METADATA,
                newEntry = OperationTimelineEntry(
                    step = OperationStep.METADATA,
                    startTime = metadataStart,
                    endTime = System.currentTimeMillis(),
                    duration = System.currentTimeMillis() - metadataStart,
                    request = RequestDebugDetails("GET", "$BASE_URL/meta", "/meta", "None", null, metadataStart),
                    response = ResponseDebugDetails(200, "OK", "Title=${metadata.title}; Author=${metadata.author}", System.currentTimeMillis(), System.currentTimeMillis() - metadataStart),
                    error = null,
                    expected = "200 OK with metadata (title, author, duration)",
                    received = "title=${metadata.title}; author=${metadata.author}; duration=${metadata.duration ?: "n/a"}",
                    nextAction = "Proceed to vend token",
                    correlationId = flowRequestId
                )
            )
        } else {
            val metaError = metadataResult.exceptionOrNull()
            val message = metaError?.message ?: "Metadata unavailable (timeout/fallback)"
            // Don't fail the flow on metadata issues; proceed directly to vending/transcription
            updateDebug(
                OperationStep.METADATA,
                newEntry = OperationTimelineEntry(
                    step = OperationStep.METADATA,
                    startTime = metadataStart,
                    endTime = System.currentTimeMillis(),
                    duration = System.currentTimeMillis() - metadataStart,
                    request = RequestDebugDetails("GET", "$BASE_URL/meta", "/meta", "None", null, metadataStart),
                    response = null,
                    error = message,
                    expected = "200 OK metadata response",
                    received = "Metadata failed; proceeding without metadata",
                    nextAction = "Proceed to vend token even without metadata",
                    correlationId = flowRequestId
                )
            )
        }

        // Vend token
        val initialToken = vendAndLog("Token Issued")
        if (initialToken.isFailure) return initialToken.map { throw Exception("unreachable") }
        var vendToken = initialToken.getOrNull()!!
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
                // Fast-fail on server-side token expiry to avoid repeated vend loops.
                tokenCache.clearToken()
                updateDebug(
                    OperationStep.FAILED,
                    newEntry = OperationTimelineEntry(
                        OperationStep.SUBMIT,
                        submitStart,
                        System.currentTimeMillis(),
                        null,
                        null,
                        null,
                        "Business Engine returned token_expired; please retry after backend refresh"
                    )
                )
                return Result.failure(
                    Exception(
                        "Business Engine session expired. Please retry in a moment while we refresh access."
                    )
                )
            }
            if (authSubmit) {
                // Only vend ONCE if auth fails during submit
                tokenCache.clearToken()
                val retryToken = vendAndLog("Submit Auth Retry", force = true)
                if (retryToken.isSuccess) {
                    vendToken = retryToken.getOrNull()!!
                    hasRefreshedAuth = true // Mark that we've already refreshed auth
                    submitResult = submitTranscriptionJob(sanitizedUrl, vendToken, clientRequestId)
                }
                // If retry still fails, log warning but don't vend again
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
                    null,
                    null,
                    null,
                    submitResult.exceptionOrNull()?.message,
                    expected = "202 Accepted with jobId",
                    received = submitResult.exceptionOrNull()?.message ?: "No response",
                    nextAction = "Surface submit failure; suggest retry after session refresh",
                    correlationId = flowRequestId
                )
            )
            return Result.failure(submitResult.exceptionOrNull() ?: Exception("Submit failed"))
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
                request = RequestDebugDetails("POST", "$BASE_URL/ttt/transcribe", "/ttt/transcribe", "Authorization: Bearer ...", """{"url":"$sanitizedUrl"}""", submitStart),
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

        // Token from initial vend (line 345) is valid for 15 minutes.
        // Polling takes max 40 seconds (20 attempts * 2s), so no need to vend again.
        // Continue using the same token from initial vend.

        val pollInterval = if (isBackground) POLL_INTERVAL_MS * 2 else POLL_INTERVAL_MS

        // Poll for completion
        repeat(MAX_POLL_ATTEMPTS) { attempt ->
            val pollingAttempt = attempt + 1
            if (circuitBreaker.isOpen()) {
                debugLogManager.logWarning(
                    category = "TRANSCRIPTION",
                    operation = "polling_circuit_open",
                    message = "Circuit breaker open during polling; waiting to retry",
                    details = "Attempt $pollingAttempt of $MAX_POLL_ATTEMPTS"
                )
                delay(pollInterval)
                return@repeat
            }
            updateDebug(OperationStep.POLLING, jobId = jobId, pollingAttempt = pollingAttempt, maxPolling = MAX_POLL_ATTEMPTS)
            delay(pollInterval)
            val statusResult = checkTranscriptionStatus(jobId, vendToken)
            if (statusResult.isFailure) {
                val cause = statusResult.exceptionOrNull()
                val authError = cause?.message?.contains("401", ignoreCase = true) == true ||
                    cause?.message?.contains("authentication failed", ignoreCase = true) == true ||
                    (cause?.message?.contains("500", ignoreCase = true) == true && cause.message?.contains("authentication", ignoreCase = true) == true)

                if (authError) {
                    // Only vend ONCE if auth fails, not multiple times
                    if (!hasRefreshedAuth) {
                        tokenCache.clearToken()
                        val inlineVend = vendAndLog("Auth Retry Token", jobId, force = true)
                        if (inlineVend.isSuccess) {
                            vendToken = inlineVend.getOrNull()!!
                            hasRefreshedAuth = true
                            val retryStatus = checkTranscriptionStatus(jobId, vendToken)
                            if (retryStatus.isSuccess) {
                                val status = retryStatus.getOrNull()!!
                                if (status.status.equals("completed", ignoreCase = true) || status.transcript != null) {
                                    updateDebug(OperationStep.COMPLETED, jobId = jobId, newEntry = OperationTimelineEntry(OperationStep.POLLING, System.currentTimeMillis(), System.currentTimeMillis(), null, null, null, null))
                                    return Result.success(status)
                                }
                                if (status.status.equals("failed", ignoreCase = true)) {
                                    updateDebug(OperationStep.FAILED, jobId = jobId, newEntry = OperationTimelineEntry(OperationStep.POLLING, System.currentTimeMillis(), System.currentTimeMillis(), null, null, null, "Remote status failed"))
                                    return Result.failure(Exception("Transcription failed remotely"))
                                }
                            }
                        }
                        // If retry fails, continue to next poll attempt (don't vend again)
                        return@repeat
                    }
                    // If already refreshed once, don't vend again - just continue polling
                    return@repeat
                } else {
                    if (pollingAttempt < MAX_POLL_ATTEMPTS) {
                        debugLogManager.logWarning(
                            category = "TRANSCRIPTION",
                            operation = "polling_transient_failure",
                            message = cause?.message ?: "Polling failed; retrying",
                            details = "Attempt $pollingAttempt of $MAX_POLL_ATTEMPTS"
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
                        null,
                        null,
                        null,
                        cause?.message,
                        expected = "2xx status with transcript or in-progress",
                        received = cause?.message ?: "Polling failure",
                        nextAction = "Stop polling; surface error",
                        correlationId = flowRequestId,
                        retryCount = pollingAttempt
                    )
                )
                return Result.failure(cause ?: Exception("Status check failed"))
            }
            val status = statusResult.getOrNull()!!
            // Check multiple possible transcript fields
            val transcript = status.transcript 
                ?: status.result?.transcription
                ?: null
            
            if (status.status.equals("completed", ignoreCase = true) || transcript != null) {
                // Return response with normalized transcript field
                val normalizedStatus = status.copy(
                    transcript = transcript ?: status.transcript
                )
                updateDebug(
                    OperationStep.COMPLETED,
                    jobId = jobId,
                    newEntry = OperationTimelineEntry(
                        OperationStep.POLLING,
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        null,
                        null,
                        null,
                        null,
                        expected = "status=completed with transcript",
                        received = "status=${status.status}",
                        nextAction = "Render transcript",
                        correlationId = flowRequestId,
                        retryCount = pollingAttempt
                    )
                )
                return Result.success(normalizedStatus)
            }
            if (status.status.equals("failed", ignoreCase = true)) {
                updateDebug(
                    OperationStep.FAILED,
                    jobId = jobId,
                    newEntry = OperationTimelineEntry(
                        OperationStep.POLLING,
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        null,
                        null,
                        null,
                        "Remote status failed",
                        expected = "status=completed or transcript",
                        received = "status=${status.status}",
                        nextAction = "Stop polling; surface remote failure",
                        correlationId = flowRequestId,
                        retryCount = pollingAttempt
                    )
                )
                return Result.failure(Exception("Transcription failed remotely"))
            }
        }

        updateDebug(
            OperationStep.FAILED,
            jobId = jobId,
            newEntry = OperationTimelineEntry(
                OperationStep.POLLING,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                null,
                null,
                null,
                "Polling timeout",
                expected = "status=completed within ${MAX_POLL_ATTEMPTS * pollInterval}ms",
                received = "Timed out at attempt ${MAX_POLL_ATTEMPTS}",
                nextAction = "Stop; ask user to retry",
                correlationId = flowRequestId,
                retryCount = MAX_POLL_ATTEMPTS
            )
        )
        // Removed final vend before timeout - it wastes credits unnecessarily
        return Result.failure(Exception("Transcription timed out. Please retry in a moment."))
    }

    private suspend fun <T> execute(
        method: String,
        endpoint: String,
        payload: Map<String, Any>? = null,
        authToken: String? = null,
        timeoutMs: Long? = null
    ): Result<T> {
        val requestId = "req_${System.currentTimeMillis()}"
        val requestUrl = "$BASE_URL$endpoint"
        if (circuitBreaker.isOpen()) {
            val msg = "Circuit breaker is open"
            Log.e(TAG, msg)
            return Result.failure(Exception(msg))
        }
        val startTime = System.currentTimeMillis()
        debugLogManager.logInfo(
            category = "API_REQUEST",
            operation = endpoint,
            message = "Request started",
            details = buildString {
                appendLine("ID: $requestId")
                appendLine("Method: $method")
                appendLine("Payload: ${payload ?: "None"}")
            },
            requestUrl = requestUrl,
            requestMethod = method,
            requestPayload = payload?.toString() ?: ""
        )

        val outcome = retryHandler.executeWithRetry(requestId, startTime) {
            httpClient.executeRequest(method, endpoint, payload, authToken, timeoutMs)
        }
        val result = outcome.result
        if (result.isSuccess) {
            circuitBreaker.recordSuccess()
            val duration = System.currentTimeMillis() - startTime
            debugLogManager.logInfo(
                category = "API_RESPONSE",
                operation = endpoint,
                message = "Request completed",
                details = buildString {
                    appendLine("ID: $requestId")
                    appendLine("Method: $method")
                    appendLine("Duration: ${duration}ms")
                    appendLine("Attempts: ${outcome.attempts}")
                },
                requestUrl = requestUrl,
                requestMethod = method,
                requestPayload = payload?.toString() ?: ""
            )
        } else {
            val error = result.exceptionOrNull()
            if (shouldCountFailureForCircuitBreaker(error)) {
                circuitBreaker.recordFailure(isRetryableFailure(error))
            }
            val duration = System.currentTimeMillis() - startTime
            if (error is PluctCoreAPIDetailedError) {
                debugLogManager.logAPIError(error, "API_RESPONSE")
            } else {
                debugLogManager.logError(
                    category = "API_RESPONSE",
                    operation = endpoint,
                    message = error?.message ?: "Request failed",
                    exception = error,
                    requestUrl = requestUrl,
                    requestPayload = payload?.toString() ?: "",
                    responseBody = buildString {
                        appendLine("Duration: ${duration}ms; Attempts: ${outcome.attempts}")
                        appendLine("Request ID: $requestId")
                        if (circuitBreaker.isOpen()) {
                            appendLine("Circuit breaker currently OPEN")
                        }
                    }
                )
            }
        }
        metrics.updateMetrics(result.isSuccess, outcome.attempts - 1)
        @Suppress("UNCHECKED_CAST")
        return result.mapCatching { it as T }
    }

    private fun isRetryableFailure(error: Throwable?): Boolean {
        if (error is PluctCoreAPIDetailedError) {
            val statusCode = error.technicalDetails.responseStatusCode
            if (error.isRetryable) return true
            if (statusCode == 408 || statusCode == 429 || statusCode >= 500) return true
            if (error.userMessage.contains("timeout", ignoreCase = true)) return true
        }
        val message = error?.message ?: return false
        val lower = message.lowercase()
        return lower.contains("timeout") ||
            lower.contains("timed out") ||
            lower.contains("temporarily") ||
            lower.contains("connection") ||
            lower.contains("unavailable")
    }

    private fun shouldCountFailureForCircuitBreaker(error: Throwable?): Boolean {
        if (error is PluctCoreAPIDetailedError) {
            val statusCode = error.technicalDetails.responseStatusCode
            if (statusCode in 400..499 && statusCode != 408 && statusCode != 429) {
                return false
            }
            if (error.userMessage.contains("circuit breaker", ignoreCase = true)) return false
        }
        val message = error?.message ?: return true
        if (message.contains("circuit breaker", ignoreCase = true)) return false
        return true
    }

    private fun startHealthMonitoring() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val healthResult = withContext(Dispatchers.IO) { httpClient.executeRequest("GET", "/health", null, null) }
                _healthStatus.update { mapOf("api" to if (healthResult.isSuccess) HealthStatus.HEALTHY else HealthStatus.UNHEALTHY) }
                delay(30000)
            }
        }
    }
}
