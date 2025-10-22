package app.pluct.services

import android.util.Log
import app.pluct.error.PluctErrorHandler
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-API-Retry - API retry mechanisms for Business Engine and TTTranscribe
 * Single source of truth for API retry logic
 * Adheres to 300-line limit with smart separation of concerns
 */

data class APIRetryConfig(
    val maxRetries: Int = 3,
    val baseDelayMs: Long = 1000,
    val maxDelayMs: Long = 10000,
    val backoffMultiplier: Double = 2.0,
    val timeoutMs: Long = 30000
)

@Singleton
class PluctAPIRetry @Inject constructor(
    private val errorHandler: PluctErrorHandler
) {
    
    suspend fun <T> executeAPICall(
        operation: String,
        config: APIRetryConfig = APIRetryConfig(),
        block: suspend () -> T
    ): Result<T> {
        return errorHandler.executeWithRetry(
            operation = block,
            config = app.pluct.error.PluctErrorHandler.RetryConfig(
                maxAttempts = config.maxRetries,
                baseDelayMs = config.baseDelayMs,
                maxDelayMs = config.maxDelayMs,
                backoffMultiplier = config.backoffMultiplier
            ),
            operationName = operation
        )
    }
    
    suspend fun executeBusinessEngineCall(
        operation: String,
        block: suspend () -> Any
    ): Result<Any> {
        return executeAPICall(
            operation = "Business Engine: $operation",
            config = APIRetryConfig(
                maxRetries = 3,
                baseDelayMs = 1000,
                maxDelayMs = 10000
            ),
            block = block
        )
    }
    
    suspend fun executeTTTranscribeCall(
        operation: String,
        block: suspend () -> Any
    ): Result<Any> {
        return executeAPICall(
            operation = "TTTranscribe: $operation",
            config = APIRetryConfig(
                maxRetries = 5,
                baseDelayMs = 2000,
                maxDelayMs = 30000
            ),
            block = block
        )
    }
    
    suspend fun executeMetadataCall(
        operation: String,
        block: suspend () -> Any
    ): Result<Any> {
        return executeAPICall(
            operation = "Metadata: $operation",
            config = APIRetryConfig(
                maxRetries = 2,
                baseDelayMs = 500,
                maxDelayMs = 5000
            ),
            block = block
        )
    }
    
    fun shouldRetry(error: Throwable): Boolean {
        return when (error) {
            is java.net.UnknownHostException,
            is java.net.ConnectException,
            is java.net.SocketTimeoutException,
            is java.io.IOException,
            is java.util.concurrent.TimeoutException -> true
            else -> false
        }
    }
    
    fun getRetryDelay(attempt: Int, baseDelay: Long = 1000): Long {
        return (baseDelay * Math.pow(2.0, attempt.toDouble())).toLong()
    }
}
