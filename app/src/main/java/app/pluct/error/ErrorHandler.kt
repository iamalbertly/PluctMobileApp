package app.pluct.error

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException
import java.net.UnknownHostException

/**
 * Comprehensive error handling and retry mechanisms
 */
object ErrorHandler {
    private const val TAG = "ErrorHandler"
    
    /**
     * Retry configuration for different types of operations
     */
    data class RetryConfig(
        val maxAttempts: Int = 3,
        val baseDelayMs: Long = 1000,
        val maxDelayMs: Long = 10000,
        val backoffMultiplier: Double = 2.0,
        val jitterMs: Long = 100
    )
    
    /**
     * Default retry configurations for different operation types
     */
    val NETWORK_RETRY_CONFIG = RetryConfig(
        maxAttempts = 3,
        baseDelayMs = 1000,
        maxDelayMs = 5000,
        backoffMultiplier = 2.0
    )
    
    val API_RETRY_CONFIG = RetryConfig(
        maxAttempts = 5,
        baseDelayMs = 2000,
        maxDelayMs = 10000,
        backoffMultiplier = 1.5
    )
    
    val SCRAPING_RETRY_CONFIG = RetryConfig(
        maxAttempts = 2,
        baseDelayMs = 3000,
        maxDelayMs = 8000,
        backoffMultiplier = 2.0
    )
    
    /**
     * Execute operation with retry logic
     */
    suspend fun <T> executeWithRetry(
        operation: suspend () -> T,
        config: RetryConfig = NETWORK_RETRY_CONFIG,
        operationName: String = "Operation"
    ): Result<T> {
        var attempt = 0
        var lastException: Exception? = null
        
        while (attempt < config.maxAttempts) {
            try {
                Log.d(TAG, "$operationName attempt ${attempt + 1}/${config.maxAttempts}")
                val result = operation()
                Log.i(TAG, "$operationName succeeded on attempt ${attempt + 1}")
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e
                attempt++
                
                if (attempt >= config.maxAttempts) {
                    Log.e(TAG, "$operationName failed after $attempt attempts", e)
                    break
                }
                
                if (!shouldRetry(e)) {
                    Log.w(TAG, "$operationName failed with non-retryable error", e)
                    break
                }
                
                val delayMs = calculateDelay(attempt, config)
                Log.w(TAG, "$operationName failed on attempt $attempt, retrying in ${delayMs}ms", e)
                delay(delayMs)
            }
        }
        
        return Result.failure(lastException ?: Exception("Operation failed after ${config.maxAttempts} attempts"))
    }
    
    /**
     * Determine if an exception should trigger a retry
     */
    private fun shouldRetry(exception: Exception): Boolean {
        return when (exception) {
            is IOException,
            is UnknownHostException,
            is java.net.SocketTimeoutException,
            is java.net.ConnectException -> true
            is IllegalArgumentException -> false // Don't retry invalid arguments
            else -> {
                // Check if it's a network-related error
                val message = exception.message?.lowercase() ?: ""
                message.contains("network") || 
                message.contains("timeout") || 
                message.contains("connection") ||
                message.contains("unreachable")
            }
        }
    }
    
    /**
     * Calculate delay with exponential backoff and jitter
     */
    private fun calculateDelay(attempt: Int, config: RetryConfig): Long {
        val exponentialDelay = (config.baseDelayMs * Math.pow(config.backoffMultiplier, (attempt - 1).toDouble())).toLong()
        val cappedDelay = minOf(exponentialDelay, config.maxDelayMs)
        val jitter = (Math.random() * config.jitterMs).toLong()
        return cappedDelay + jitter
    }
    
    /**
     * Create a flow with retry logic
     */
    fun <T> createRetryFlow(
        operation: suspend () -> T,
        config: RetryConfig = NETWORK_RETRY_CONFIG
    ) = flow {
        emit(Result.success(operation()))
    }.retryWhen { cause, attempt ->
        if (attempt >= config.maxAttempts) {
            false
        } else if (shouldRetry(cause as Exception)) {
            val delayMs = calculateDelay(attempt.toInt(), config)
            Log.w(TAG, "Retrying operation in ${delayMs}ms (attempt $attempt)", cause)
            delay(delayMs)
            true
        } else {
            false
        }
    }
}

/**
 * Error types for better error handling
 */
sealed class PluctError(override val message: String, override val cause: Throwable? = null) : Exception(message, cause) {
    class NetworkError(override val message: String, override val cause: Throwable? = null) : PluctError(message, cause)
    class APIError(val code: Int, override val message: String) : PluctError(message)
    class ScrapingError(override val message: String, override val cause: Throwable? = null) : PluctError(message, cause)
    class ValidationError(override val message: String) : PluctError(message)
    class InsufficientCoinsError(val required: Int, val available: Int) : PluctError("Insufficient coins: need $required, have $available")
    class UnknownError(override val message: String, override val cause: Throwable? = null) : PluctError(message, cause)
}

// Extension functions removed due to compilation issues
