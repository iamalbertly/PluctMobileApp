package app.pluct.services

import android.util.Log
import app.pluct.core.debug.PluctCoreDebug01LogManager
import kotlinx.coroutines.delay

/**
 * Pluct-Core-API-01UnifiedService-08TranscriptionFlow-02TokenVending-01Handler
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[TranscriptionFlow]-[TokenVending]-[Handler]
 * 7 scope layers: Project, Core, API, UnifiedService, TranscriptionFlow, TokenVending, Handler
 * Handles token vending logic for transcription flow
 */
class PluctCoreAPI01UnifiedService08TranscriptionFlow02TokenVending01Handler(
    private val tokenCache: PluctCoreAPIUnifiedServiceTokenCache,
    private val rateLimitTracker: PluctCoreRateLimitTracker,
    private val userIdentification: PluctCoreUserIdentification,
    private val debugLogManager: PluctCoreDebug01LogManager,
    private val baseUrl: String,
    private val getVendTokenBlockedUntil: () -> Long,
    private val setVendTokenBlockedUntil: (Long) -> Unit,
    private val vendToken: suspend (String) -> Result<VendTokenResponse>,
    private val onDebugUpdate: (OperationStep, OperationTimelineEntry?) -> Unit
) {
    companion object {
        private const val TAG = "TokenVendingHandler"
    }

    fun extractVendToken(response: VendTokenResponse): String? {
        return listOf(response.token, response.serviceToken, response.pollingToken)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
    }

    suspend fun vendAndLog(
        label: String,
        clientRequestId: String,
        flowRequestId: String,
        jobId: String? = null,
        force: Boolean = false
    ): Result<String> {
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

        val blockedForMs = (getVendTokenBlockedUntil() - now).coerceAtLeast(0)
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
            setVendTokenBlockedUntil(now + waitMs)
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
        
        val vendStart = System.currentTimeMillis()
        debugLogManager.logInfo(
            category = "TOKEN_MANAGEMENT",
            operation = "token_vend",
            message = "Vending new token",
            details = "Label: $label; Force: $force; JobId: $jobId"
        )
        val vendResult = vendToken(clientRequestId)
        vendResult.getOrNull()?.let { vendResponse ->
            val token = extractVendToken(vendResponse)
            if (token.isNullOrBlank()) {
                val msg = "Vend token missing in response"
                onDebugUpdate(
                    OperationStep.FAILED,
                    OperationTimelineEntry(
                        OperationStep.VEND_TOKEN,
                        vendStart,
                        System.currentTimeMillis(),
                        null, null, null, msg
                    )
                )
                debugLogManager.logError(
                    category = "TRANSCRIPTION",
                    operation = "vend_token_missing",
                    message = msg,
                    requestUrl = "$baseUrl/v1/vend-token",
                    responseBody = vendResponse.toString()
                )
                return Result.failure(Exception(msg))
            }
            onDebugUpdate(
                OperationStep.VEND_TOKEN,
                OperationTimelineEntry(
                    step = OperationStep.VEND_TOKEN,
                    startTime = vendStart,
                    endTime = System.currentTimeMillis(),
                    duration = System.currentTimeMillis() - vendStart,
                    request = RequestDebugDetails("POST", "$baseUrl/v1/vend-token", "/v1/vend-token", "Authorization: Bearer ...", """{"userId":"${userIdentification.userId}"}""", vendStart),
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
            setVendTokenBlockedUntil(System.currentTimeMillis() + backoffMs)
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

        onDebugUpdate(
            OperationStep.FAILED,
            OperationTimelineEntry(
                OperationStep.VEND_TOKEN,
                vendStart,
                System.currentTimeMillis(),
                null, null, null,
                vendError?.message,
                expected = "200 OK with token",
                received = "Vend failed",
                nextAction = "Stop flow; surface auth guidance",
                correlationId = flowRequestId
            )
        )
        return Result.failure(vendError ?: Exception("Token vending failed"))
    }
}
