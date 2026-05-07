package app.pluct.services

import android.content.Context
import android.util.Log

/**
 * Pluct-Core-API-01UnifiedService-12Token-01Handler
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[Token]-[Handler]
 * 6 scope layers: Project, Core, API, UnifiedService, Token, Handler
 * Handles token vending and service token retrieval
 * Extracted from UnifiedService-01Main to reduce file size and improve separation of concerns
 */
class PluctCoreAPI01UnifiedService12Token01Handler(
    private val jwtGenerator: PluctCoreAPIJWTGenerator,
    private val userIdentification: PluctCoreUserIdentification,
    private val tokenCache: PluctCoreAPIUnifiedServiceTokenCache,
    private val deduplicationCoordinator: PluctCoreAPI01UnifiedService03Deduplication01Coordinator,
    private val authRetryHandler: PluctCoreAPI01UnifiedService15AuthRetry01Handler,
    private val execute: suspend (String, String, Map<String, Any>?, String?, Long?) -> Result<*>
) {
    companion object {
        private const val TAG = "TokenHandler"
    }

    /**
     * Vend a service token for transcription
     */
    suspend fun vendToken(clientRequestId: String = "req_${System.currentTimeMillis()}"): Result<VendTokenResponse> {
        // Check for cached response (idempotency) using unified coordinator
        val cachedResponse = deduplicationCoordinator.getCachedResponse(clientRequestId)
        if (cachedResponse != null && cachedResponse is VendTokenResponse) {
            Log.d(TAG, "Returning cached vendToken response for requestId: $clientRequestId")
            return Result.success(cachedResponse)
        }
        
        val userToken = jwtGenerator.generateUserJWT(userIdentification.userId)
        val payload = mapOf(
            "userId" to userIdentification.userId,
            "clientRequestId" to clientRequestId
        )
        
        val result = authRetryHandler.executeWithProactiveRefresh(
            currentToken = userToken,
            apiCall = { token ->
                @Suppress("UNCHECKED_CAST")
                execute("POST", "/v1/vend-token", payload, token, null) as Result<VendTokenResponse>
            },
            retryBlock = { newToken ->
                @Suppress("UNCHECKED_CAST")
                execute("POST", "/v1/vend-token", payload, newToken, null) as Result<VendTokenResponse>
            }
        )
        
        // Cache successful response for idempotency using unified coordinator
        result.getOrNull()?.let { response ->
            deduplicationCoordinator.cacheResponse(clientRequestId, response, ttlSeconds = 300)
        }
        
        return result
    }

    /**
     * Get a service token, checking cache first to avoid rate limiting.
     * Only vends a new token if cache is empty or expired.
     */
    suspend fun getServiceToken(forceRefresh: Boolean = false): Result<String> {
        // Check cache first unless forced
        if (!forceRefresh) {
            val cached = tokenCache.getValidToken()
            if (cached != null) {
                Log.d(TAG, "Using cached service token for status check")
                return Result.success(cached)
            }
        }
        
        // Cache miss or forced refresh - vend new token
        val vendResult = vendToken("status_check_${System.currentTimeMillis()}")
        return vendResult.fold(
            onSuccess = { vend ->
                val token = vend.token ?: vend.serviceToken
                if (token.isNullOrBlank()) {
                    Log.e(TAG, "Vend token response did not contain a valid token")
                    Result.failure(IllegalStateException("Vend token response did not contain a valid token"))
                } else {
                    Result.success(token)
                }
            },
            onFailure = { error ->
                Log.w(TAG, "Failed to get service token: ${error.message}")
                Result.failure(error)
            }
        )
    }
}
