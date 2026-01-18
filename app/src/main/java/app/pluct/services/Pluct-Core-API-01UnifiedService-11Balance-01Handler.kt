package app.pluct.services

import android.util.Log
import app.pluct.core.api.PluctCoreAPI00Constants
import java.net.URLEncoder

/**
 * Pluct-Core-API-01UnifiedService-11Balance-01Handler
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[Balance]-[Handler]
 * 6 scope layers: Project, Core, API, UnifiedService, Balance, Handler
 * Handles balance and estimate API operations
 * Extracted from UnifiedService-01Main to reduce file size and improve separation of concerns
 */
class PluctCoreAPI01UnifiedService11Balance01Handler(
    private val jwtGenerator: PluctCoreAPIJWTGenerator,
    private val userIdentification: PluctCoreUserIdentification,
    private val authRetryHandler: PluctCoreAPI01UnifiedService15AuthRetry01Handler,
    private val execute: suspend (String, String, Map<String, Any>?, String?, Long?) -> Result<*>
) {
    companion object {
        private const val TAG = "BalanceHandler"
    }

    /**
     * Check user credit balance
     */
    suspend fun checkUserBalance(): Result<CreditBalanceResponse> {
        val userToken = jwtGenerator.generateUserJWT(userIdentification.userId)
        return authRetryHandler.executeWithProactiveRefresh(
            currentToken = userToken,
            apiCall = { token ->
                @Suppress("UNCHECKED_CAST")
                execute("GET", "/v1/credits/balance", null, token, null) as Result<CreditBalanceResponse>
            },
            retryBlock = { newToken ->
                @Suppress("UNCHECKED_CAST")
                execute("GET", "/v1/credits/balance", null, newToken, null) as Result<CreditBalanceResponse>
            }
        )
    }

    /**
     * Get cost estimate for transcription
     */
    suspend fun getEstimate(url: String): Result<EstimateResponse> {
        val userToken = jwtGenerator.generateUserJWT(userIdentification.userId)
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        return authRetryHandler.executeWithProactiveRefresh(
            currentToken = userToken,
            apiCall = { token ->
                @Suppress("UNCHECKED_CAST")
                execute("GET", "/estimate?url=$encodedUrl", null, token, null) as Result<EstimateResponse>
            },
            retryBlock = { newToken ->
                @Suppress("UNCHECKED_CAST")
                execute("GET", "/estimate?url=$encodedUrl", null, newToken, null) as Result<EstimateResponse>
            }
        )
    }
}
