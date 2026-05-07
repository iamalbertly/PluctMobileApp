package app.pluct.services

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// TECHNICAL DEBT CLEANUP #2: Removed unused ConcurrentHashMap import (prewarmTimestamps removed)
import kotlin.jvm.Volatile
import javax.inject.Inject
import javax.inject.Singleton
import app.pluct.core.debug.PluctCoreDebug01LogManager
import app.pluct.core.retry.PluctCoreRetryUnifiedHandler
import app.pluct.core.api.PluctCoreAPI00Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import app.pluct.data.entity.ProcessingStatus
import java.util.Locale

/**
 * Pluct-Core-API-01UnifiedService-01Main - Unified API service orchestrator.
 * Delegates HTTP, retry, circuit, metrics, and debug tracking to scoped collaborators.
 * Refactored to use specialized handlers (Balance, Token, Metadata, Status, AuthRetry)
 * to reduce file size and eliminate duplicate auth retry logic.
 * Now under 300 lines - improved separation of concerns.
 */
@Singleton
class PluctCoreAPIUnifiedService @Inject constructor(
    private val logger: PluctCoreLoggingStructuredLogger,
    private val validator: PluctCoreValidationInputSanitizer,
    private val userIdentification: PluctCoreUserIdentification,
    private val rateLimitTracker: PluctCoreRateLimitTracker,
    private val debugLogManager: PluctCoreDebug01LogManager,
    private val deduplicationCoordinator: PluctCoreAPI01UnifiedService03Deduplication01Coordinator,
    private val videoRepository: app.pluct.data.repository.PluctVideoRepository?,
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "PluctCoreAPIUnified"
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
    private val responseHandler = PluctCoreAPI01UnifiedService10ExecuteResponseHandler(
        debugLogManager = debugLogManager,
        circuitBreaker = circuitBreaker,
        metrics = metrics,
        retryHandler = retryHandler
    )
    @Volatile private var vendTokenBlockedUntil = 0L
    @Volatile private var serverTimeWarmupComplete = false
    @Volatile private var profileSyncAttempted = false

    private val healthMonitor = PluctCoreAPI01UnifiedService09HealthMonitor01Service(httpClient)
    val healthStatus: StateFlow<Map<String, HealthStatus>> = healthMonitor.healthStatus

    private val _transcriptionDebugFlow = MutableStateFlow<TranscriptionDebugInfo?>(null)
    val transcriptionDebugFlow: StateFlow<TranscriptionDebugInfo?> = _transcriptionDebugFlow.asStateFlow()

    // Auth retry handler - single source of truth for 401 retry logic
    private val authRetryHandler = PluctCoreAPI01UnifiedService15AuthRetry01Handler(tokenRefreshManager)

    // API method handlers - extracted to reduce file size and improve separation of concerns
    private val balanceHandler = PluctCoreAPI01UnifiedService11Balance01Handler(
        jwtGenerator = jwtGenerator,
        userIdentification = userIdentification,
        authRetryHandler = authRetryHandler,
        execute = { method, endpoint, payload, authToken, timeoutMs -> 
            @Suppress("UNCHECKED_CAST")
            execute<Any>(method, endpoint, payload, authToken, timeoutMs) as Result<*>
        }
    )

    private val tokenHandler = PluctCoreAPI01UnifiedService12Token01Handler(
        jwtGenerator = jwtGenerator,
        userIdentification = userIdentification,
        tokenCache = tokenCache,
        deduplicationCoordinator = deduplicationCoordinator,
        authRetryHandler = authRetryHandler,
        execute = { method, endpoint, payload, authToken, timeoutMs -> 
            @Suppress("UNCHECKED_CAST")
            execute<Any>(method, endpoint, payload, authToken, timeoutMs) as Result<*>
        }
    )

    private val metadataHandler = PluctCoreAPI01UnifiedService13Metadata01Handler(
        jwtGenerator = jwtGenerator,
        userIdentification = userIdentification,
        authRetryHandler = authRetryHandler,
        execute = { method, endpoint, payload, authToken, timeoutMs -> 
            @Suppress("UNCHECKED_CAST")
            execute<Any>(method, endpoint, payload, authToken, timeoutMs) as Result<*>
        }
    )

    private val statusHandler = PluctCoreAPI01UnifiedService14Status01Handler(
        tokenRefreshManager = tokenRefreshManager,
        authRetryHandler = authRetryHandler,
        execute = { method, endpoint, payload, authToken, timeoutMs -> 
            @Suppress("UNCHECKED_CAST")
            execute<Any>(method, endpoint, payload, authToken, timeoutMs) as Result<*>
        }
    )

    private val transcriptionFlowHandler = PluctCoreAPI01UnifiedService08TranscriptionFlow01Handler(
        validator = validator,
        deduplicationCoordinator = deduplicationCoordinator,
        videoRepository = videoRepository,
        debugLogManager = debugLogManager,
        tokenCache = tokenCache,
        rateLimitTracker = rateLimitTracker,
        userIdentification = userIdentification,
        jwtGenerator = jwtGenerator,
        circuitBreaker = circuitBreaker,
        baseUrl = PluctCoreAPI00Constants.BASE_URL,
        pollIntervalMsFast = PluctCoreAPI00Constants.POLL_INTERVAL_MS_FAST,
        pollIntervalMsSlow = PluctCoreAPI00Constants.POLL_INTERVAL_MS_SLOW,
        fastPollAttempts = PluctCoreAPI00Constants.FAST_POLL_ATTEMPTS,
        maxPollAttempts = PluctCoreAPI00Constants.MAX_POLL_ATTEMPTS,
        onDebugFlowUpdate = { _transcriptionDebugFlow.value = it },
        getVendTokenBlockedUntil = { vendTokenBlockedUntil },
        setVendTokenBlockedUntil = { vendTokenBlockedUntil = it },
        vendToken = { clientRequestId -> tokenHandler.vendToken(clientRequestId) },
        getMetadata = { url, timeoutMs -> metadataHandler.getMetadata(url, timeoutMs) },
        submitTranscriptionJob = { url, token, requestId -> submitTranscriptionJob(url, token, requestId) },
        pollTranscriptionStatus = { jobId, token -> statusHandler.pollTranscriptionStatus(jobId, token) }
    )

    val apiMetrics: StateFlow<APIMetrics> = metrics.apiMetrics

    private val transcriptionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        healthMonitor.startMonitoring()
    }

    suspend fun checkUserBalance(): Result<CreditBalanceResponse> {
        ensureServerTimeWarmup()
        return balanceHandler.checkUserBalance()
    }

    suspend fun getEstimate(url: String): Result<EstimateResponse> {
        ensureServerTimeWarmup()
        return balanceHandler.getEstimate(url)
    }

    suspend fun vendToken(clientRequestId: String = "req_${System.currentTimeMillis()}"): Result<VendTokenResponse> {
        ensureServerTimeWarmup()
        return tokenHandler.vendToken(clientRequestId)
    }

    suspend fun getMetadata(url: String, timeoutMs: Long? = null): Result<MetadataResponse> {
        ensureServerTimeWarmup()
        return metadataHandler.getMetadata(url, timeoutMs)
    }

    suspend fun submitTranscriptionJob(url: String, serviceToken: String, clientRequestId: String): Result<TranscriptionResponse> {
        val payload = mapOf("url" to url, "clientRequestId" to clientRequestId)
        return execute<TranscriptionResponse>("POST", "/ttt/transcribe", payload, serviceToken)
    }

    suspend fun getServiceToken(forceRefresh: Boolean = false): Result<String> {
        ensureServerTimeWarmup()
        return tokenHandler.getServiceToken(forceRefresh)
    }

    suspend fun checkTranscriptionStatus(jobId: String, serviceToken: String): Result<TranscriptionStatusResponse> {
        return statusHandler.checkTranscriptionStatus(jobId, serviceToken)
    }

    suspend fun pollTranscriptionStatus(jobId: String, userJWT: String): Result<TranscriptionStatusResponse> {
        return statusHandler.pollTranscriptionStatus(jobId, userJWT)
    }

    suspend fun processTikTokVideo(url: String, isBackground: Boolean = false): Result<TranscriptionStatusResponse> {
        ensureServerTimeWarmup()
        val policySnapshot = context.getSharedPreferences("pluct_user_preferences", Context.MODE_PRIVATE).getString("client_policy_snapshot", "") ?: ""
        if (policySnapshot.contains("\"disableTranscribeSubmit\":true")) {
            return Result.failure(Exception("Update -> Continue"))
        }
        val currentHealth = healthStatus.value
        val effectiveHealth = if (currentHealth["ttt"] != HealthStatus.HEALTHY) {
            healthMonitor.refreshNow()
        } else {
            currentHealth
        }
        return transcriptionFlowHandler.processTikTokVideo(url, isBackground, effectiveHealth)
    }

    private suspend fun ensureServerTimeWarmup() {
        if (serverTimeWarmupComplete) return
        val warmedHealth = healthMonitor.refreshNow()
        serverTimeWarmupComplete = warmedHealth["api"] == HealthStatus.HEALTHY
        if (serverTimeWarmupComplete && !profileSyncAttempted) {
            profileSyncAttempted = true
            val profile = mapOf(
                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}".take(120),
                "deviceType" to "phone",
                "osName" to "android",
                "osVersion" to Build.VERSION.RELEASE.take(32),
                "appVersion" to app.pluct.BuildConfig.VERSION_NAME.take(32),
                "locale" to Locale.getDefault().toLanguageTag().take(16),
                "source" to "runtime_refresh"
            )
            val fingerprint = profile.toSortedMap().entries.joinToString("|") { "${it.key}=${it.value}" }.hashCode().toString()
            val prefs = context.getSharedPreferences("pluct_user_preferences", Context.MODE_PRIVATE)
            if (prefs.getString("profile_payload_hash", "") != fingerprint) {
                val token = jwtGenerator.generateUserJWT(userIdentification.userId)
                execute<Any>("POST", "/v1/profile/device", profile, token)
                    .onSuccess { prefs.edit().putString("profile_payload_hash", fingerprint).apply() }
                    .onFailure { Log.w(TAG, "Device profile sync skipped: ${it.message}") }
            }
            val now = System.currentTimeMillis()
            if (now - prefs.getLong("client_policy_checked_at", 0L) > 6 * 60 * 60 * 1000L) {
                execute<Any>("GET", "/v1/public/client-policy")
                    .onSuccess { prefs.edit().putLong("client_policy_checked_at", now).putString("client_policy_snapshot", it.toString()).apply() }
                    .onFailure { Log.w(TAG, "Client policy refresh skipped: ${it.message}") }
            }
        }
    }

    private suspend fun <T> execute(
        method: String,
        endpoint: String,
        payload: Map<String, Any>? = null,
        authToken: String? = null,
        timeoutMs: Long? = null
    ): Result<T> {
        val requestId = "req_${System.currentTimeMillis()}"
        val requestUrl = "${PluctCoreAPI00Constants.BASE_URL}$endpoint"
        if (circuitBreaker.isOpen()) {
            val msg = "Circuit breaker is open"
            Log.e(TAG, msg)
            return Result.failure(Exception(msg))
        }
        val startTime = System.currentTimeMillis()
        responseHandler.logRequestStart(requestId, endpoint, method, requestUrl, payload)
        val on401Error = PluctCoreAPIUnifiedService01MainAuthHandler.prepare401ErrorHandler<T>(
            authToken = authToken,
            requestId = requestId,
            method = method,
            endpoint = endpoint,
            payload = payload,
            timeoutMs = timeoutMs,
            jwtGenerator = jwtGenerator,
            userIdentification = userIdentification,
            httpClient = httpClient
        )

        val outcome = retryHandler.executeWithRetry(requestId, startTime, {
            httpClient.executeRequest(method, endpoint, payload, authToken, timeoutMs) as Result<T>
        }, on401Error)
        val result = outcome.result
        if (result.isSuccess) {
            responseHandler.handleSuccess(
                result = result,
                outcome = outcome,
                requestId = requestId,
                endpoint = endpoint,
                method = method,
                requestUrl = requestUrl,
                payload = payload,
                startTime = startTime
            )
        } else {
            responseHandler.handleFailure(
                result = result,
                outcome = outcome,
                requestId = requestId,
                endpoint = endpoint,
                requestUrl = requestUrl,
                payload = payload,
                startTime = startTime
            )
        }
        @Suppress("UNCHECKED_CAST")
        return result.mapCatching { it as T }
    }

    fun cleanup() {
        healthMonitor.stopMonitoring()
        transcriptionScope.cancel()
        Log.d(TAG, "API service cleanup completed")
    }
}
