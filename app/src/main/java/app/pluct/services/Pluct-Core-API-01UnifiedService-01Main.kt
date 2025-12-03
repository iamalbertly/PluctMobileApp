package app.pluct.services

import android.util.Log
import app.pluct.architecture.PluctComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Core-API-01UnifiedService-01Main - Unified API service orchestrator.
 * Delegates HTTP, retry, circuit, metrics, and debug tracking to scoped collaborators.
 */
@Singleton
class PluctCoreAPIUnifiedService @Inject constructor(
    private val logger: PluctCoreLoggingStructuredLogger,
    private val validator: PluctCoreValidationInputSanitizer,
    private val userIdentification: PluctCoreUserIdentification,
    private val rateLimitTracker: PluctCoreRateLimitTracker
) : PluctComponent {

    companion object {
        private const val TAG = "PluctCoreAPIUnified"
        private const val BASE_URL = "https://pluct-business-engine.romeo-lya2.workers.dev"
        private const val POLL_INTERVAL_MS = 2000L
        private const val MAX_POLL_ATTEMPTS = 12
    }

    private val httpClient = PluctCoreAPIHTTPClientImpl(logger, validator, userIdentification)
    private val retryHandler = PluctCoreAPIUnifiedServiceRetryHandler()
    private val circuitBreaker = PluctCoreAPIUnifiedServiceCircuitBreaker()
    private val metrics = PluctCoreAPIUnifiedServiceMetrics()
    private val jwtGenerator = PluctCoreAPIJWTGenerator()

    private val _healthStatus = MutableStateFlow<Map<String, HealthStatus>>(emptyMap())
    val healthStatus: StateFlow<Map<String, HealthStatus>> = _healthStatus.asStateFlow()

    private val _transcriptionDebugFlow = MutableStateFlow<TranscriptionDebugInfo?>(null)
    val transcriptionDebugFlow: StateFlow<TranscriptionDebugInfo?> = _transcriptionDebugFlow.asStateFlow()

    val apiMetrics: StateFlow<APIMetrics> = metrics.apiMetrics

    private val transcriptionJobs = ConcurrentHashMap<String, kotlinx.coroutines.Deferred<Result<TranscriptionStatusResponse>>>()
    private val transcriptionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

    suspend fun getMetadata(url: String): Result<MetadataResponse> {
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        return execute("GET", "/meta?url=$encodedUrl")
    }

    suspend fun submitTranscriptionJob(url: String, serviceToken: String, clientRequestId: String): Result<TranscriptionResponse> {
        val payload = mapOf("url" to url, "clientRequestId" to clientRequestId)
        return execute("POST", "/ttt/transcribe", payload, serviceToken)
    }

    suspend fun checkTranscriptionStatus(jobId: String, serviceToken: String): Result<TranscriptionStatusResponse> {
        return execute("GET", "/ttt/status/$jobId", authToken = serviceToken)
    }

    fun preWarmVideoProcessing(url: String) {
        if (transcriptionJobs.containsKey(url)) return
        transcriptionJobs[url] = transcriptionScope.async { processTikTokVideo(url, isBackground = true) }
    }

    suspend fun processTikTokVideo(url: String, isBackground: Boolean = false): Result<TranscriptionStatusResponse> {
        val validation = validator.validateUrl(url)
        if (!validation.isValid) return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid URL"))
        val sanitizedUrl = validation.sanitizedValue
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

        // Metadata
        val metadataStart = System.currentTimeMillis()
        updateDebug(OperationStep.METADATA)
        val metadataResult = getMetadata(sanitizedUrl)
        if (metadataResult.isFailure) {
            updateDebug(OperationStep.FAILED, newEntry = OperationTimelineEntry(OperationStep.METADATA, metadataStart, System.currentTimeMillis(), null, null, null, metadataResult.exceptionOrNull()?.message))
            return Result.failure(metadataResult.exceptionOrNull() ?: Exception("Metadata failed"))
        }
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
                error = null
            )
        )

        // Vend token
        val vendStart = System.currentTimeMillis()
        updateDebug(OperationStep.VEND_TOKEN)
        val vendResult = vendToken(clientRequestId)
        if (vendResult.isFailure) {
            updateDebug(OperationStep.FAILED, newEntry = OperationTimelineEntry(OperationStep.VEND_TOKEN, vendStart, System.currentTimeMillis(), null, null, null, vendResult.exceptionOrNull()?.message))
            return Result.failure(vendResult.exceptionOrNull() ?: Exception("Token vending failed"))
        }
        var vendToken = vendResult.getOrNull()!!.token
        var hasRefreshedAuth = false

        // Submit transcription
        val submitStart = System.currentTimeMillis()
        updateDebug(OperationStep.SUBMIT)
        val submitResult = submitTranscriptionJob(sanitizedUrl, vendToken, clientRequestId)
        if (submitResult.isFailure) {
            updateDebug(OperationStep.FAILED, newEntry = OperationTimelineEntry(OperationStep.SUBMIT, submitStart, System.currentTimeMillis(), null, null, null, submitResult.exceptionOrNull()?.message))
            return Result.failure(submitResult.exceptionOrNull() ?: Exception("Submit failed"))
        }
        val jobId = submitResult.getOrNull()!!.jobId
        updateDebug(
            OperationStep.SUBMIT,
            jobId = jobId,
            newEntry = OperationTimelineEntry(
                step = OperationStep.SUBMIT,
                startTime = submitStart,
                endTime = System.currentTimeMillis(),
                duration = System.currentTimeMillis() - submitStart,
                request = RequestDebugDetails("POST", "$BASE_URL/ttt/transcribe", "/ttt/transcribe", "Authorization: Bearer ...", """{"url":"$sanitizedUrl"}""", submitStart),
                response = ResponseDebugDetails(202, "Accepted", "jobId=$jobId", System.currentTimeMillis(), System.currentTimeMillis() - submitStart),
                error = null
            )
        )

        // Poll for completion
        repeat(MAX_POLL_ATTEMPTS) { attempt ->
            val pollingAttempt = attempt + 1
            updateDebug(OperationStep.POLLING, jobId = jobId, pollingAttempt = pollingAttempt, maxPolling = MAX_POLL_ATTEMPTS)
            delay(POLL_INTERVAL_MS)
            val statusResult = checkTranscriptionStatus(jobId, vendToken)
            if (statusResult.isFailure) {
                val cause = statusResult.exceptionOrNull()
                val authError = cause?.message?.contains("401", ignoreCase = true) == true ||
                    cause?.message?.contains("authentication failed", ignoreCase = true) == true

                if (authError && !hasRefreshedAuth) {
                    // Retry once with a fresh vend token to avoid stale auth failures during polling.
                    val refreshStart = System.currentTimeMillis()
                    val refreshed = vendToken(clientRequestId)
                    if (refreshed.isSuccess) {
                        vendToken = refreshed.getOrNull()!!.token
                        hasRefreshedAuth = true
                        updateDebug(
                            OperationStep.VEND_TOKEN,
                            jobId = jobId,
                            newEntry = OperationTimelineEntry(
                                OperationStep.VEND_TOKEN,
                                refreshStart,
                                System.currentTimeMillis(),
                                duration = System.currentTimeMillis() - refreshStart,
                                request = RequestDebugDetails("POST", "$BASE_URL/v1/vend-token", "/v1/vend-token", "Authorization: Bearer ...", """{"userId":"${userIdentification.userId}"}""", refreshStart),
                                response = ResponseDebugDetails(200, "Refreshed Token", "Refreshed for polling auth", System.currentTimeMillis(), System.currentTimeMillis() - refreshStart),
                                error = null
                            )
                        )
                        return@repeat
                    }
                }

                updateDebug(OperationStep.FAILED, jobId = jobId, newEntry = OperationTimelineEntry(OperationStep.POLLING, System.currentTimeMillis(), System.currentTimeMillis(), null, null, null, cause?.message))
                return Result.failure(cause ?: Exception("Status check failed"))
            }
            val status = statusResult.getOrNull()!!
            if (status.status.equals("completed", ignoreCase = true) || status.transcript != null) {
                updateDebug(OperationStep.COMPLETED, jobId = jobId, newEntry = OperationTimelineEntry(OperationStep.POLLING, System.currentTimeMillis(), System.currentTimeMillis(), null, null, null, null))
                return Result.success(status)
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
                        "Remote status failed"
                    )
                )
                return Result.failure(Exception("Transcription failed remotely"))
            }
        }

        updateDebug(
            OperationStep.FAILED,
            jobId = null,
            newEntry = OperationTimelineEntry(
                OperationStep.POLLING,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                null,
                null,
                null,
                "Polling timeout"
            )
        )
        return Result.failure(Exception("Transcription timed out"))
    }

    private suspend fun <T> execute(
        method: String,
        endpoint: String,
        payload: Map<String, Any>? = null,
        authToken: String? = null
    ): Result<T> {
        val requestId = "req_${System.currentTimeMillis()}"
        if (circuitBreaker.isOpen()) {
            val msg = "Circuit breaker is open"
            Log.e(TAG, msg)
            return Result.failure(Exception(msg))
        }
        val startTime = System.currentTimeMillis()
        val outcome = retryHandler.executeWithRetry(requestId, startTime) {
            httpClient.executeRequest(method, endpoint, payload, authToken)
        }
        val result = outcome.result
        if (result.isSuccess) {
            circuitBreaker.recordSuccess()
        } else {
            circuitBreaker.recordFailure()
        }
        metrics.updateMetrics(result.isSuccess, outcome.attempts - 1)
        @Suppress("UNCHECKED_CAST")
        return result.mapCatching { it as T }
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
