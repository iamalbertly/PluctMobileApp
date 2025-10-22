package app.pluct.data.token

import app.pluct.core.log.PluctLogger
import app.pluct.core.retry.PluctRetryEngine
import app.pluct.data.PluctBusinessEngineUnifiedClientNew
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Data-Token-01VendingSystem - Token vending system integration with Business Engine
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Manages credit-based token vending for TTTranscribe API access
 */
@Singleton
class PluctTokenVendingSystem @Inject constructor(
    private val businessEngineClient: PluctBusinessEngineUnifiedClientNew
) {
    private val _currentToken = MutableStateFlow<TokenInfo?>(null)
    val currentToken: StateFlow<TokenInfo?> = _currentToken.asStateFlow()
    
    private val _creditBalance = MutableStateFlow(0)
    val creditBalance: StateFlow<Int> = _creditBalance.asStateFlow()
    
    /**
     * Check current credit balance
     */
    suspend fun checkCreditBalance(userJwt: String): Int {
        return try {
            val startTime = System.currentTimeMillis()
            val balanceResult = businessEngineClient.getCreditBalance(userJwt)
            val responseTime = System.currentTimeMillis() - startTime
            
            _creditBalance.value = balanceResult.balance
            PluctLogger.logBusinessEngineCall("check_balance", true, responseTime, mapOf("balance" to balanceResult.balance))
            
            balanceResult.balance
        } catch (e: Exception) {
            PluctLogger.logError(app.pluct.core.error.ErrorEnvelope("BALANCE_CHECK_FAIL", "Failed to check credit balance: ${e.message}"))
            throw e
        }
    }
    
    /**
     * Vend a new token for TTTranscribe API access
     */
    suspend fun vendToken(userJwt: String): TokenInfo {
        return try {
            val startTime = System.currentTimeMillis()
            
            // Check if we have sufficient credits
            val currentBalance = _creditBalance.value
            if (currentBalance <= 0) {
                throw InsufficientCreditsException("No credits available for token vending")
            }
            
            // Request token from Business Engine
            val tokenResponse = businessEngineClient.vendToken(userJwt)
            val responseTime = System.currentTimeMillis() - startTime
            
            val tokenInfo = TokenInfo(
                token = tokenResponse.token,
                scope = tokenResponse.scope,
                expiresAt = System.currentTimeMillis() + (15 * 60 * 1000), // 15 minutes from now
                balanceAfter = tokenResponse.balanceAfter
            )
            
            _currentToken.value = tokenInfo
            _creditBalance.value = tokenResponse.balanceAfter
            
            PluctLogger.logBusinessEngineCall("vend_token", true, responseTime, mapOf(
                "balanceAfter" to tokenResponse.balanceAfter,
                "expiresAt" to tokenResponse.expiresAt
            ))
            
            tokenInfo
        } catch (e: Exception) {
            PluctLogger.logError("Failed to vend token: ${e.message}")
            throw e
        }
    }
    
    /**
     * Check if current token is valid and not expired
     */
    fun isTokenValid(): Boolean {
        val token = _currentToken.value
        return token != null && System.currentTimeMillis() < token.expiresAt
    }
    
    /**
     * Get current valid token or vend a new one if needed
     */
    suspend fun getValidToken(userJwt: String): TokenInfo {
        if (isTokenValid()) {
            return _currentToken.value!!
        }
        
        return vendToken(userJwt)
    }
    
    /**
     * Clear current token (force refresh on next request)
     */
    fun clearToken() {
        _currentToken.value = null
        PluctLogger.logInfo("Token cleared, will vend new token on next request")
    }
}

data class TokenInfo(
    val token: String,
    val scope: String,
    val expiresAt: Long,
    val balanceAfter: Int
)

class InsufficientCreditsException(message: String) : Exception(message)
