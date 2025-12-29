package app.pluct.services

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import kotlin.jvm.Volatile
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
) {

    companion object {
        private const val TAG = "PluctCoreAPIUnified"
        private const val BASE_URL = "https://pluct-business-engine.romeo-lya2.workers.dev"
        private const val POLL_INTERVAL_MS_FAST = 1500L // Faster polling for first 10 attempts
        private const val POLL_INTERVAL_MS_SLOW = 3000L // Slower polling after 10 attempts
        private const val FAST_POLL_ATTEMPTS = 10 // Use fast polling for quick jobs
        private const val MAX_POLL_ATTEMPTS = 30 // Increased from 20 to 30 for longer jobs
        private const val PREWARM_THROTTLE_MS = 60000L
    }

    private val httpClient = PluctCoreAPIHTTPClientImpl(logger, validator, userIdentification)
    private val retryHandler = PluctCoreRetryUnifiedHandler()
    private val circuitBreaker = PluctCoreAPIUnifiedServiceCircuitBreaker()
    private val metrics = PluctCoreAPIUnifiedServiceMetrics()
    private val jwtGenerator = PluctCoreAPIJWTGenerator()
    private val tokenCache = PluctCoreAPIUnifiedServiceTokenCache(context)
    private val tokenRefreshManager = PluctCoreAPI01UnifiedService02TokenRefresh01Manager(
        jwtGenerator = jwtGenerator,
        userIdentification = userIdentification
    )
    private val requestDeduplicationHandler = PluctCoreAPI01UnifiedService03RequestDeduplication01Handler()
    @Volatile private var vendTokenBlockedUntil = 0L

    private val _healthStatus = MutableStateFlow<Map<String, HealthStatus>>(emptyMap())
    val healthStatus: StateFlow<Map<String, HealthStatus>> = _healthStatus.asStateFlow()

    private val _transcriptionDebugFlow = MutableStateFlow<TranscriptionDebugInfo?>(null)
    val transcriptionDebugFlow: StateFlow<TranscriptionDebugInfo?> = _transcriptionDebugFlow.asStateFlow()

    val apiMetrics: StateFlow<APIMetrics> = metrics.apiMetrics

    private val transcriptionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prewarmTimestamps = ConcurrentHashMap<String, Long>()

    init {
        startHealthMonitoring()
    }

    suspend fun checkUserBalance(): Result<CreditBalanceResponse> {
        var userToken = jwtGenerator.generateUserJWT(userIdentification.userId)
        // Check if token should be refreshed proactively
        val refreshedToken = tokenRefreshManager.refreshTokenIfNeeded(userToken)
        if (refreshedToken != null) {
            userToken = refreshedToken
        }
        return execute<CreditBalanceResponse>("GET", "/v1/credits/balance", authToken = userToken)
    }

    suspend fun getEstimate(url: String): Result<EstimateResponse> {
        val userToken = jwtGenerator.generateUserJWT(userIdentification.userId)
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        return execute<EstimateResponse>("GET", "/estimate?url=$encodedUrl", authToken = userToken)
    }

    suspend fun vendToken(clientRequestId: String = "req_${System.currentTimeMillis()}"): Result<VendTokenResponse> {
        // Check for cached response (idempotency)
        val cachedResponse = requestDeduplicationHandler.getCachedResponse(clientRequestId)
        if (cachedResponse != null && cachedResponse is VendTokenResponse) {
            Log.d(TAG, "Returning cached vendToken response for requestId: $clientRequestId")
            return Result.success(cachedResponse)
        }
        
        // Check if request is in progress
        if (requestDeduplicationHandler.isRequestInProgress(clientRequestId)) {
            Log.d(TAG, "Request already in progress for requestId: $clientRequestId")
            // Wait a bit and check cache again
            kotlinx.coroutines.delay(100)
            val retryCached = requestDeduplicationHandler.getCachedResponse(clientRequestId)
            if (retryCached != null && retryCached is VendTokenResponse) {
                return Result.success(retryCached)
            }
            return Result.failure(Exception("Request already in progress"))
        }
        
        val userToken = jwtGenerator.generateUserJWT(userIdentification.userId)
        val payload = mapOf(
            "userId" to userIdentification.userId,
            "clientRequestId" to clientRequestId
        )
        val result = execute<VendTokenResponse>("POST", "/v1/vend-token", payload, userToken)
        
        // Cache successful response for idempotency
        if (result.isSuccess) {
            val response = result.getOrNull()!!
            requestDeduplicationHandler.cacheResponse(clientRequestId, response, ttlSeconds = 300)
            requestDeduplicationHandler.markRequestCompleted(clientRequestId)
        } else {
            // Mark as completed even on failure to allow retry
            requestDeduplicationHandler.markRequestCompleted(clientRequestId)
        }
        
        return result
    }


    suspend fun getMetadata(url: String, timeoutMs: Long? = null): Result<MetadataResponse> {
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        val userToken = jwtGenerator.generateUserJWT(userIdentification.userId)
        return execute<MetadataResponse>("GET", "/meta?url=$encodedUrl", authToken = userToken, timeoutMs = timeoutMs)
    }

    suspend fun submitTranscriptionJob(url: String, serviceToken: String, clientRequestId: String): Result<TranscriptionResponse> {
        val payload = mapOf("url" to url, "clientRequestId" to clientRequestId)
        return execute<TranscriptionResponse>("POST", "/ttt/transcribe", payload, serviceToken)
    }

    suspend fun checkTranscriptionStatus(jobId: String, serviceToken: String): Result<TranscriptionStatusResponse> {
        // Check if token should be refreshed before status check
        var token = serviceToken
        val refreshedToken = tokenRefreshManager.refreshTokenIfNeeded(token)
        if (refreshedToken != null) {
            token = refreshedToken
        }
        
        val result = execute<TranscriptionStatusResponse>("GET", "/ttt/status/$jobId", authToken = token)
        
        // Handle 401 errors with token refresh
        if (result.isFailure) {
            val error = result.exceptionOrNull()
            if (error?.message?.contains("401", true) == true || 
                error?.message?.contains("Unauthorized", true) == true) {
                return tokenRefreshManager.handle401Error { newToken ->
                    execute<TranscriptionStatusResponse>("GET", "/ttt/status/$jobId", authToken = newToken)
                } as Result<TranscriptionStatusResponse>
            }
        }
        
        return result
    }

    /**
     * Poll transcription status using the new /ttt/poll/:id endpoint
     * This endpoint accepts user JWT (long-lived, 1 hour) instead of service token (15 min)
     * Recommended for long-running transcriptions that may exceed 15 minutes
     */
    suspend fun pollTranscriptionStatus(jobId: String, userJWT: String): Result<TranscriptionStatusResponse> {
        // Check if token should be refreshed before polling
        var token = userJWT
        val refreshedToken = tokenRefreshManager.refreshTokenIfNeeded(token)
        if (refreshedToken != null) {
            token = refreshedToken
            Log.d(TAG, "Token refreshed proactively before polling")
        }
        
        val result = execute<TranscriptionStatusResponse>("GET", "/ttt/poll/$jobId", authToken = token)
        
        // Handle 401 errors with token refresh
        if (result.isFailure) {
            val error = result.exceptionOrNull()
            if (error?.message?.contains("401", true) == true || 
                error?.message?.contains("Unauthorized", true) == true) {
                return tokenRefreshManager.handle401Error<TranscriptionStatusResponse> { newToken ->
                    execute<TranscriptionStatusResponse>("GET", "/ttt/poll/$jobId", authToken = newToken)
                }
            }
        }
        
        return result
    }

    /**
     * Pre-warming removed for simplicity.
     * The Business Engine API is fast enough that pre-warming provides minimal benefit
     * while adding complexity and potential rate limit issues.
     * Metadata and token vending happen on-demand when user submits the URL.
     */
    @Deprecated("Pre-warming removed - API is fast enough for on-demand requests")
    fun preWarmVideoProcessing(url: String) {
        // No-op: Pre-warming removed to simplify codebase
        // Metadata and token vending happen on-demand when user submits
    }

    suspend fun processTikTokVideo(url: String, isBackground: Boolean = false): Result<TranscriptionStatusResponse> {
        val validation = validator.validateUrl(url)
        if (!validation.isValid) return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid URL"))
        var sanitizedUrl = validation.sanitizedValue
        val flowRequestId = "flow_${System.currentTimeMillis()}"
        
        // Generate or get existing request ID for URL (deduplication)
        val clientRequestId = requestDeduplicationHandler.generateOrGetRequestId(sanitizedUrl)

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
            val now = System.currentTimeMillis()
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

            val blockedForMs = (vendTokenBlockedUntil - now).coerceAtLeast(0)
            if (blockedForMs > 0) {
                val msg = "Token vending temporarily paused to respect rate limits. Retry after ${blockedForMs / 1000}s."
                debugLogManager.logWarning(
                    category = "TOKEN_MANAGEMENT",
                    operation = "token_vend_blocked",
                    message = msg,
                    details = "Label: $label; JobId: $jobId"
                )
                return Result.failure(Exception(msg))
            }

            if (!rateLimitTracker.canMakeRequest()) {
                val waitMs = rateLimitTracker.getTimeToReset().coerceAtLeast(10_000L)
                vendTokenBlockedUntil = now + waitMs
                val msg = "Token vending throttled locally; retry after ${waitMs / 1000}s"
                debugLogManager.logWarning(
                    category = "TOKEN_MANAGEMENT",
                    operation = "token_vend_local_throttle",
                    message = msg,
                    details = "Label: $label; JobId: $jobId; waitMs=$waitMs"
                )
                return Result.failure(Exception(msg))
            }
            rateLimitTracker.recordRequest()
            
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
            }

            val vendError = vendResult.exceptionOrNull()
            val detailed = vendError as? PluctCoreAPIDetailedError
            val isRateLimited = detailed?.technicalDetails?.responseStatusCode == 429
            if (isRateLimited) {
                val retryAfterSeconds = detailed?.technicalDetails?.responseBody?.let {
                    Regex("\"retryAfterSeconds\"\\s*:\\s*(\\d+)").find(it)?.groupValues?.getOrNull(1)?.toLongOrNull()
                }
                val backoffMs = ((retryAfterSeconds ?: 60L) * 1000L).coerceAtLeast(60_000L)
                vendTokenBlockedUntil = System.currentTimeMillis() + backoffMs
                debugLogManager.logWarning(
                    category = "TOKEN_MANAGEMENT",
                    operation = "token_vend_rate_limited",
                    message = detailed?.userMessage ?: "Token vending rate limited",
                    details = "BackoffMs=$backoffMs; Label=$label; JobId=$jobId"
                )
            } else if (detailed != null) {
                debugLogManager.logAPIError(detailed, "TRANSCRIPTION")
            } else {
                debugLogManager.logError(
                    category = "TOKEN_MANAGEMENT",
                    operation = "token_vend_failed",
                    message = vendError?.message ?: "Token vending failed",
                    exception = vendError
                )
            }

            updateDebug(
                OperationStep.FAILED,
                newEntry = OperationTimelineEntry(
                    OperationStep.VEND_TOKEN,
                    vendStart,
                    System.currentTimeMillis(),
                    null,
                    null,
                    null,
                    vendError?.message,
                    expected = "200 OK with token",
                    received = "Vend failed",
                    nextAction = "Stop flow; surface auth guidance",
                    correlationId = flowRequestId
                )
            )
            return Result.failure(vendError ?: Exception("Token vending failed"))
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

        // SPEED IMPROVEMENT: Parallel metadata fetch and token vending
        val parallelStart = System.currentTimeMillis()
        updateDebug(OperationStep.METADATA)
        
        // Launch both operations in parallel using coroutines
        val (metadataResult, tokenResult) = coroutineScope {
            val metadataDeferred = async { getMetadata(sanitizedUrl, timeoutMs = 8000L) }
            val tokenDeferred = async { 
                vendAndLog("Token Issued (Parallel)", force = false)
            }
            
            // Wait for both to complete
            Pair(metadataDeferred.await(), tokenDeferred.await())
        }
        
        // Process metadata result
        if (metadataResult.isSuccess) {
            val metadata = metadataResult.getOrNull()!!
            updateDebug(
                OperationStep.METADATA,
                newEntry = OperationTimelineEntry(
                    step = OperationStep.METADATA,
                    startTime = parallelStart,
                    endTime = System.currentTimeMillis(),
                    duration = System.currentTimeMillis() - parallelStart,
                    request = RequestDebugDetails("GET", "$BASE_URL/meta", "/meta", "None", null, parallelStart),
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
            // Don't fail the flow on metadata issues; proceed directly to vending/transcription
            updateDebug(
                OperationStep.METADATA,
                newEntry = OperationTimelineEntry(
                    step = OperationStep.METADATA,
                    startTime = parallelStart,
                    endTime = System.currentTimeMillis(),
                    duration = System.currentTimeMillis() - parallelStart,
                    request = RequestDebugDetails("GET", "$BASE_URL/meta", "/meta", "None", null, parallelStart),
                    response = null,
                    error = message,
                    expected = "200 OK metadata response",
                    received = "Metadata failed; proceeding without metadata",
                    nextAction = "Proceed to submit (token already vended in parallel)",
                    correlationId = flowRequestId
                )
            )
        }

        // Process token result
        if (tokenResult.isFailure) {
            return tokenResult.map { throw Exception("unreachable") }
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

        // Use /ttt/poll/:id endpoint with user JWT (long-lived, 1 hour) instead of service token
        // This allows transcriptions that take > 15 minutes to complete without token expiry issues
        // Business Engine's /ttt/poll/:id accepts user JWT and is rate-limited to 30 polls/min
        var pollAuthToken = jwtGenerator.generateUserJWT(userIdentification.userId)

        // Adaptive polling: fast for first attempts (quick jobs), slower after
        val getPollInterval = { attempt: Int ->
            if (isBackground) {
                POLL_INTERVAL_MS_SLOW * 2
            } else {
                if (attempt <= FAST_POLL_ATTEMPTS) POLL_INTERVAL_MS_FAST else POLL_INTERVAL_MS_SLOW
            }
        }

        // Poll for completion using user JWT (not service token)
        repeat(MAX_POLL_ATTEMPTS) { attempt ->
            val pollingAttempt = attempt + 1
            val currentPollInterval = getPollInterval(pollingAttempt)

            if (circuitBreaker.isOpen()) {
                debugLogManager.logWarning(
                    category = "TRANSCRIPTION",
                    operation = "polling_circuit_open",
                    message = "Circuit breaker open during polling; waiting to retry",
                    details = "Attempt $pollingAttempt of $MAX_POLL_ATTEMPTS"
                )
                delay(currentPollInterval)
                return@repeat
            }
            updateDebug(OperationStep.POLLING, jobId = jobId, pollingAttempt = pollingAttempt, maxPolling = MAX_POLL_ATTEMPTS)
            delay(currentPollInterval)
            // Use /ttt/poll/:id endpoint with long-lived user JWT to avoid additional vend-token calls
            val statusResult = pollTranscriptionStatus(jobId, pollAuthToken)
            if (statusResult.isFailure) {
                val cause = statusResult.exceptionOrNull()
                val authError = cause?.message?.contains("401", ignoreCase = true) == true ||
                    cause?.message?.contains("authentication failed", ignoreCase = true) == true ||
                    (cause?.message?.contains("500", ignoreCase = true) == true && cause.message?.contains("authentication", ignoreCase = true) == true)

                if (authError) {
                    // If JWT expired during polling, regenerate once instead of vending new service tokens
                    if (!hasRefreshedAuth) {
                        debugLogManager.logWarning(
                            category = "TRANSCRIPTION",
                            operation = "polling_auth_refresh",
                            message = "Polling auth expired; regenerating user JWT",
                            details = "JobId=$jobId; Attempt=$pollingAttempt"
                        )
                        pollAuthToken = jwtGenerator.generateUserJWT(userIdentification.userId)
                        hasRefreshedAuth = true
                        // Try next poll attempt with fresh JWT
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
            // EDGE CASE FIX #1: Safe extraction with detailed logging
            val status = statusResult.getOrNull()
            if (status == null) {
                debugLogManager.logError(
                    category = "TRANSCRIPTION",
                    operation = "polling_null_status",
                    message = "Status result was null despite successful HTTP response - JobId=$jobId; Attempt=$pollingAttempt"
                )
                return@repeat // Try next poll
            }

            // Log raw status for debugging
            debugLogManager.logInfo(
                category = "TRANSCRIPTION",
                operation = "polling_status_received",
                message = "Received status from server",
                details = "JobId=$jobId; Status=${status.status}; Progress=${status.progress}; " +
                    "HasTranscript=${status.transcript != null}; " +
                    "HasResult=${status.result != null}; " +
                    "HasResultTranscription=${status.result?.transcription != null}"
            )

            // EDGE CASE FIX #2: Check multiple possible transcript fields and normalize
            // TTTranscribe may return transcript in different locations:
            // - status.transcript (direct field)
            // - status.result.transcription (nested in result object)
            // - status.text (alternative field name)
            val transcript = status.transcript
                ?: status.result?.transcription
                ?: status.text

            if (transcript != null) {
                debugLogManager.logInfo(
                    category = "TRANSCRIPTION",
                    operation = "transcript_found",
                    message = "Transcript extracted successfully",
                    details = "JobId=$jobId; Length=${transcript.length}; Source=${
                        when {
                            status.transcript != null -> "status.transcript"
                            status.result?.transcription != null -> "status.result.transcription"
                            status.text != null -> "status.text"
                            else -> "unknown"
                        }
                    }"
                )
            }

            // Check if job is completed (either explicit status or presence of transcript)
            val isCompleted = status.status.equals("completed", ignoreCase = true)
                || status.status.equals("done", ignoreCase = true)
                || transcript != null
                || status._cacheHit == true // Job already completed and cached

            if (isCompleted) {
                // Log cache hit detection for faster UX
                if (status._cacheHit == true) {
                    debugLogManager.logInfo(
                        category = "TRANSCRIPTION",
                        operation = "cache_hit_detected",
                        message = "Job already completed and cached (instant result)",
                        details = "JobId=$jobId; Attempt=$pollingAttempt; TranscriptLength=${transcript?.length ?: 0}"
                    )
                }

                // Return response with normalized transcript field
                val normalizedStatus = status.copy(
                    transcript = transcript ?: status.transcript,
                    status = "completed" // Normalize status to "completed"
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
                        received = "status=${status.status}; hasTranscript=${transcript != null}; cacheHit=${status._cacheHit}",
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
                expected = "status=completed within ${MAX_POLL_ATTEMPTS * POLL_INTERVAL_MS_SLOW}ms",
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
