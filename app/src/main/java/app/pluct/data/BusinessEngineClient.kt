package app.pluct.data

import android.util.Log
import java.util.UUID

/**
 * Centralized Business Engine Client - Refactored to use focused components
 * Single point of communication with the Business Engine backend
 * Now uses Pluct-Business-Engine-Core for all operations
 */
class BusinessEngineClient(
    private val baseUrl: String
) {
    private val core = PluctBusinessEngineCore(baseUrl)
    
    companion object {
        private const val TAG = "BusinessEngineClient"
    }

    /**
     * Check Business Engine health with enhanced logging
     */
    suspend fun health(): Health {
        return core.health()
    }

    /**
     * Get user credit balance with enhanced logging
     */
    suspend fun balance(userJwt: String): Balance {
        return core.balance(userJwt)
    }

    /**
     * Vend a short-lived token for transcription
     */
    suspend fun vendShortToken(userJwt: String, clientRequestId: String = UUID.randomUUID().toString()): VendResult {
        return core.vendShortToken(userJwt, clientRequestId)
    }

    /**
     * Legacy method for backward compatibility
     */
    suspend fun vendToken(): VendResult {
        return core.vendToken()
    }

    /**
     * Start transcription process
     */
    suspend fun transcribe(url: String, token: String): String {
        return core.transcribe(url, token)
    }

    /**
     * Fetch enhanced metadata for a video URL
     */
    suspend fun fetchMetadata(url: String): Metadata {
        return core.fetchMetadata(url)
    }
}

// Data classes for backward compatibility
data class Health(val isHealthy: Boolean)
data class Balance(val balance: Int)
data class VendResult(
    val token: String,
    val scope: String,
    val expiresAt: String,
    val balanceAfter: Int
)
data class Metadata(
    val title: String,
    val description: String
)
sealed class EngineError : Throwable() {
    data object Network : EngineError()
    data object Auth : EngineError()
    data object InsufficientCredits : EngineError()
    data object RateLimited : EngineError()
    data class Upstream(val code: Int, val errorMessage: String?) : EngineError()
    data object InvalidUrl : EngineError()
    data class Unexpected(val errorCause: Throwable) : EngineError()
}