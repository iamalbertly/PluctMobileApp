package app.pluct.services

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
// TECHNICAL DEBT CLEANUP #2: Removed unused ConcurrentHashMap import (prewarmTimestamps removed)
import kotlin.jvm.Volatile
import javax.inject.Inject
import javax.inject.Singleton
import app.pluct.core.debug.PluctCoreDebug01LogManager
import app.pluct.core.retry.PluctCoreRetryUnifiedHandler
import app.pluct.core.api.PluctCoreAPI00Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import app.pluct.data.entity.ProcessingStatus
import app.pluct.shared.PluctClientPolicyModels
import app.pluct.shared.PluctDeviceProfile
import app.pluct.shared.PluctRequestIds
import app.pluct.ui.polling.PluctUIPolling01AdaptiveIntervalCalculator
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

        /** True when BE policy disables transcribe; false on blank or parse failure (fail-open; see log ClientPolicy). */
        fun isPolicyBlockingTranscribe(raw: String): Boolean {
            return PluctClientPolicyModels.isTranscribeDisabled(raw) ||
                PluctClientPolicyModels.isHardUpdateRequiredByCode(raw, app.pluct.BuildConfig.VERSION_CODE)
        }
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
    private val mobileSyncJson = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val mobileSyncPrefs = context.getSharedPreferences("pluct_mobile_sync", Context.MODE_PRIVATE)
    private val mobileSyncMutex = Mutex()
    private val _mobileSyncState = MutableStateFlow(
        mobileSyncPrefs.getString("snapshot_json", null)?.let { raw ->
            runCatching { mobileSyncJson.decodeFromString<MobileSyncResponse>(raw) }.getOrNull()
        }
    )
    val mobileSyncState: StateFlow<MobileSyncResponse?> = _mobileSyncState.asStateFlow()

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

    suspend fun vendToken(clientRequestId: String = PluctRequestIds.generate()): Result<VendTokenResponse> {
        ensureServerTimeWarmup()
        return tokenHandler.vendToken(clientRequestId)
    }

    suspend fun requestCreditTopUp(reference: String, clientRequestId: String = PluctRequestIds.generateCreditRequestId()): Result<Unit> {
        ensureServerTimeWarmup()
        val token = jwtGenerator.generateUserJWT(userIdentification.userId)
        val payload = mapOf(
            "amount" to 0,
            "reason" to "top-up",
            "reference" to reference,
            "clientRequestId" to clientRequestId
        )
        return execute<Any>("POST", "/v1/credits/request", payload, token).map { Unit }
    }

    suspend fun getMetadata(url: String, timeoutMs: Long? = null): Result<MetadataResponse> {
        ensureServerTimeWarmup()
        return metadataHandler.getMetadata(url, timeoutMs)
    }

    suspend fun submitTranscriptionJob(url: String, serviceToken: String, clientRequestId: String): Result<TranscriptionResponse> {
        val payload = mapOf("url" to url, "clientRequestId" to clientRequestId)
        return execute<TranscriptionResponse>("POST", "/ttt/transcribe", payload, serviceToken)
    }

    private suspend fun requestQuote(url: String, clientRequestId: String): Result<QuoteResponse> {
        val token = jwtGenerator.generateUserJWT(userIdentification.userId)
        val payload = mapOf(
            "inputType" to "tiktok_url",
            "url" to url,
            "requestedProducts" to listOf("transcript"),
            "clientRequestId" to clientRequestId
        )
        return execute<QuoteResponse>("POST", "/v1/quote", payload, token)
    }

    private suspend fun fulfillQuote(quoteId: String, clientRequestId: String): Result<FulfillResponse> {
        val token = jwtGenerator.generateUserJWT(userIdentification.userId)
        val payload = mapOf("quoteId" to quoteId, "clientRequestId" to clientRequestId)
        return execute<FulfillResponse>("POST", "/v1/fulfill", payload, token)
    }

    private suspend fun pollCanonicalJob(jobId: String): Result<TranscriptionStatusResponse> {
        val token = jwtGenerator.generateUserJWT(userIdentification.userId)
        return execute<TranscriptionStatusResponse>("GET", "/v1/jobs/$jobId", null, token)
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

    suspend fun refreshClientPolicy(force: Boolean = false): Result<String> {
        val prefs = context.getSharedPreferences("pluct_user_preferences", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong("client_policy_checked_at", 0L) <= 30 * 60 * 1000L) {
            prefs.getString("client_policy_snapshot", "")?.takeIf { it.isNotBlank() }?.let {
                return Result.success(it)
            }
        }
        val endpoint = "/v1/public/client-policy?platform=android&versionCode=${app.pluct.BuildConfig.VERSION_CODE}&version=${app.pluct.BuildConfig.VERSION_NAME}"
        return execute<Any>("GET", endpoint).mapCatching { response ->
            val raw = response.toString()
            prefs.edit()
                .putLong("client_policy_checked_at", now)
                .putString("client_policy_snapshot", raw)
                .apply()
            raw
        }
    }

    suspend fun refreshMobileSync(force: Boolean = false): Result<MobileSyncResponse> = mobileSyncMutex.withLock {
        val prefs = mobileSyncPrefs
        val now = System.currentTimeMillis()
        val current = _mobileSyncState.value
        val active = current?.jobs?.changedSinceCursor?.any { it.status in setOf("reserved", "queued", "processing", "joined") } == true
        val maxAgeMs = if (active) 30_000L else 5 * 60_000L
        if (!force && current != null && now - prefs.getLong("checked_at", 0L) < maxAgeMs) return Result.success(current)
        val token = jwtGenerator.generateUserJWT(userIdentification.userId)
        val since = prefs.getLong("server_time_ms", 0L)
        val result = execute<MobileSyncResponse>("GET", "/v1/mobile/sync?since=$since", null, token)
        result.onSuccess { snapshot ->
            _mobileSyncState.value = snapshot
            prefs.edit()
                .putLong("checked_at", now)
                .putLong("server_time_ms", snapshot.serverTimeMs)
                .putString("revision", snapshot.revision)
                .putString("snapshot_json", mobileSyncJson.encodeToString(snapshot))
                .apply()
            Log.i("PluctSync", "snapshot mode=${snapshot.budgetMode} service=${snapshot.service.state} jobs=${snapshot.jobs.changedSinceCursor.size} next=${snapshot.nextSyncAfterSeconds}s")
        }
        result
    }

    /**
     * Foreground hook: refresh health TTL and emit a single log line for stuck queues (grep `PluctUserPain` / `PluctForeground` in adb logcat).
     */
    suspend fun onAppForegroundedForDiagnostics() {
        refreshMobileSync(force = false).onFailure { Log.w("PluctSync", "foreground_sync_failed ${it.message}") }
        try {
            val health = healthMonitor.refreshNow(force = true)
            Log.i("PluctForeground", "health_refresh api=${health["api"]} ttt=${health["ttt"]}")
        } catch (e: Exception) {
            Log.w("PluctForeground", "health_refresh_failed ${e.message}")
        }
        val repo = videoRepository ?: return
        try {
            val proc = repo.getVideoCountByStatus(ProcessingStatus.PROCESSING)
            val queued = repo.getVideoCountByStatus(ProcessingStatus.QUEUED)
            if (proc > 0 || queued > 0) {
                Log.i("PluctUserPain", "foreground_queue_snapshot processing=$proc queued=$queued")
            }
        } catch (e: Exception) {
            Log.w("PluctForeground", "queue_snapshot_failed ${e.message}")
        }
    }

    suspend fun processTikTokVideo(url: String, isBackground: Boolean = false): Result<TranscriptionStatusResponse> {
        ensureServerTimeWarmup()
        val control = refreshMobileSync(force = false).getOrNull()
        if (control != null && (!control.policy.transcriptionEnabled || control.budgetMode in setOf("EMERGENCY", "LOCKDOWN") || !control.service.acceptingJobs)) {
            return Result.failure(Exception("SAVED_FOR_LATER:${control.service.message}"))
        }
        val policySnapshot = context.getSharedPreferences("pluct_user_preferences", Context.MODE_PRIVATE).getString("client_policy_snapshot", "") ?: ""
        if (isPolicyBlockingTranscribe(policySnapshot)) {
            return Result.failure(Exception("ACTION_UPDATE_APP"))
        }
        val currentHealth = healthStatus.value
        val effectiveHealth = if (currentHealth["ttt"] != HealthStatus.HEALTHY) {
            healthMonitor.refreshNow()
        } else {
            currentHealth
        }
        val idempotencyPrefs = context.getSharedPreferences("pluct_active_requests", Context.MODE_PRIVATE)
        val requestKey = "request_${url.trim().lowercase(Locale.US).hashCode().toUInt()}"
        val journeyRequestId = idempotencyPrefs.getString(requestKey, null) ?: PluctRequestIds.generate("pluct").also {
            idempotencyPrefs.edit().putString(requestKey, it).apply()
        }
        val walletResult = processTikTokVideoWithWallet(url, isBackground, journeyRequestId)
        if (walletResult.isSuccess) {
            idempotencyPrefs.edit().remove(requestKey).apply()
            return walletResult
        }
        val walletError = walletResult.exceptionOrNull()
        val detailed = walletError as? PluctCoreAPIDetailedError
        val canUseLegacyFallback = detailed?.technicalDetails?.responseStatusCode == 404 ||
            detailed?.technicalDetails?.errorCode == "route_not_found" ||
            walletError?.message?.contains("quote_not_found", ignoreCase = true) == true
        if (!canUseLegacyFallback) {
            if (walletError?.message?.contains("refunded", ignoreCase = true) == true || walletError?.message?.contains("invalid", ignoreCase = true) == true) {
                idempotencyPrefs.edit().remove(requestKey).apply()
            }
            return walletResult
        }
        Log.w(TAG, "Wallet fulfillment unavailable, falling back to legacy TTTranscribe flow: ${walletError?.message}")
        return transcriptionFlowHandler.processTikTokVideo(url, isBackground, effectiveHealth)
    }

    private suspend fun processTikTokVideoWithWallet(url: String, isBackground: Boolean, quoteRequestId: String): Result<TranscriptionStatusResponse> {
        val quote = requestQuote(url, quoteRequestId).getOrElse { return Result.failure(it) }
        val reserveUnits = quote.estimated.reserveUnits
        debugLogManager.logInfo(
            category = "WALLET",
            operation = "quote_received",
            message = if (quote.estimated.cacheHit) "Already processed. Text is ready. Cost: 0" else "Quote received. Reserve: $reserveUnits",
            details = "quoteId=${quote.quoteId}; priceVersion=${quote.priceVersion}; available=${quote.balance?.availableUnits}; reserved=${quote.balance?.reservedUnits}"
        )

        val fulfillRequestId = quoteRequestId
        val fulfill = fulfillQuote(quote.quoteId, fulfillRequestId).getOrElse { return Result.failure(it) }
        debugLogManager.logInfo(
            category = "WALLET",
            operation = "reservation_submitted",
            message = "Reserved: ${fulfill.reservedUnits}. No charge if Pluct fails.",
            details = "jobId=${fulfill.jobId}; status=${fulfill.status}; balanceAfter=${fulfill.balanceAfterReservation}"
        )

        fun resultFromFulfill(finalFulfill: FulfillResponse): TranscriptionStatusResponse? {
            val text = finalFulfill.result?.transcription
                ?: finalFulfill.result?.transcript
                ?: finalFulfill.result?.text
            return if (!text.isNullOrBlank()) {
                TranscriptionStatusResponse(
                    jobId = finalFulfill.jobId,
                    status = "completed",
                    progress = 100,
                    transcript = text,
                    text = text,
                    result = finalFulfill.result,
                    _cacheHit = finalFulfill.settlement?.settledUnits == 0
                )
            } else null
        }

        resultFromFulfill(fulfill)?.let { return Result.success(it) }

        repeat(PluctCoreAPI00Constants.MAX_POLL_ATTEMPTS) { attempt ->
            val waitMs = PluctUIPolling01AdaptiveIntervalCalculator.calculateNextPollIntervalMs(
                attemptNumber = attempt + 1,
                isBackground = isBackground,
                config = PluctUIPolling01AdaptiveIntervalCalculator.PollingConfig()
            )
            delay(waitMs)
            val status = pollCanonicalJob(fulfill.jobId).getOrElse { error ->
                if (attempt >= PluctCoreAPI00Constants.MAX_POLL_ATTEMPTS - 1) return Result.failure(error)
                return@repeat
            }
            val transcript = app.pluct.services.api.PluctCoreAPITranscriptionResult01Extractor.extract(status).transcript
            if (!transcript.isNullOrBlank()) {
                debugLogManager.logInfo(
                    category = "WALLET",
                    operation = "settlement_completed",
                    message = "Done. Final wallet settlement received.",
                    details = "jobId=${fulfill.jobId}; status=${status.status}"
                )
                return Result.success(status.copy(status = "completed", progress = 100, transcript = transcript, text = transcript))
            }
            if (status.status.contains("failed_refunded", ignoreCase = true) || status.status.contains("cancelled_refunded", ignoreCase = true)) {
                return Result.failure(Exception("Processing failed and reserved units were refunded."))
            }
        }

        return Result.failure(Exception("Transcription timed out. Reserved units will settle or refund when the job finishes."))
    }

    private suspend fun ensureServerTimeWarmup() {
        if (serverTimeWarmupComplete) return
        val warmedHealth = healthMonitor.refreshNow()
        serverTimeWarmupComplete = warmedHealth["api"] == HealthStatus.HEALTHY
        if (serverTimeWarmupComplete && !profileSyncAttempted) {
            profileSyncAttempted = true
            val profile = PluctDeviceProfile(
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                deviceType = "phone",
                osName = "android",
                osVersion = Build.VERSION.RELEASE,
                appVersion = app.pluct.BuildConfig.VERSION_NAME,
                locale = Locale.getDefault().toLanguageTag(),
                source = "runtime_refresh"
            )
            val profilePayload: Map<String, Any> = profile.toApiPayload()
            val fingerprint = profile.stableFingerprint()
            val prefs = context.getSharedPreferences("pluct_user_preferences", Context.MODE_PRIVATE)
            if (prefs.getString("profile_payload_hash", "") != fingerprint) {
                val token = jwtGenerator.generateUserJWT(userIdentification.userId)
                execute<Any>("POST", "/v1/profile/device", profilePayload, token)
                    .onSuccess { prefs.edit().putString("profile_payload_hash", fingerprint).apply() }
                    .onFailure { Log.w(TAG, "Device profile sync skipped: ${it.message}") }
            }
            val now = System.currentTimeMillis()
            if (now - prefs.getLong("client_policy_checked_at", 0L) > 6 * 60 * 60 * 1000L) {
                refreshClientPolicy(force = true)
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
        val requestId = PluctRequestIds.generate()
        val requestUrl = "${PluctCoreAPI00Constants.BASE_URL}$endpoint"
        if (circuitBreaker.isOpen()) {
            try {
                healthMonitor.refreshNow(force = true)
                Log.w("PluctForeground", "circuit_open_health_refresh_attempted")
            } catch (_: Exception) {
                // ignore — still return cooldown
            }
            Log.e(TAG, "Circuit breaker is open")
            return Result.failure(Exception("SERVICE_COOLDOWN"))
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
