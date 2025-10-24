package app.pluct.services

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-ErrorHandling-01RetryLogic - Comprehensive error handling and retry logic
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation][CoreResponsibility]
 * Implements exponential backoff and intelligent retry strategies
 */
@Singleton
class PluctErrorHandlingRetryLogic @Inject constructor() {
    
    companion object {
        private const val TAG = "PluctErrorHandling"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 8000L
    }
    
    data class RetryConfig(
        val maxAttempts: Int = MAX_RETRY_ATTEMPTS,
        val baseDelayMs: Long = BASE_DELAY_MS,
        val maxDelayMs: Long = MAX_DELAY_MS,
        val exponentialBackoff: Boolean = true,
        val jitter: Boolean = true
    )
    
    data class RetryResult<T>(
        val success: Boolean,
        val result: T? = null,
        val error: Exception? = null,
        val attempts: Int,
        val totalDurationMs: Long
    )
    
    /**
     * Execute operation with retry logic
     */
    suspend fun <T> executeWithRetry(
        operation: suspend () -> T,
        config: RetryConfig = RetryConfig(),
        operationName: String = "operation"
    ): RetryResult<T> = withContext(Dispatchers.IO) {
        
        val startTime = System.currentTimeMillis()
        var lastException: Exception? = null
        
        Log.d(TAG, "🔄 Starting $operationName with retry logic (max ${config.maxAttempts} attempts)")
        
        for (attempt in 1..config.maxAttempts) {
            try {
                Log.d(TAG, "🔄 Attempt $attempt/$config.maxAttempts for $operationName")
                
                val result = operation()
                
                val totalDuration = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ $operationName succeeded on attempt $attempt (${totalDuration}ms)")
                
                return@withContext RetryResult(
                    success = true,
                    result = result,
                    attempts = attempt,
                    totalDurationMs = totalDuration
                )
                
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "❌ Attempt $attempt/$config.maxAttempts failed for $operationName: ${e.message}")
                
                // Don't retry on certain types of errors
                if (shouldNotRetry(e)) {
                    Log.e(TAG, "🚫 Not retrying $operationName due to non-retryable error: ${e.message}")
                    break
                }
                
                // Don't delay after the last attempt
                if (attempt < config.maxAttempts) {
                    val delayMs = calculateDelay(attempt, config)
                    Log.d(TAG, "⏳ Waiting ${delayMs}ms before retry...")
                    delay(delayMs)
                }
            }
        }
        
        val totalDuration = System.currentTimeMillis() - startTime
        Log.e(TAG, "❌ $operationName failed after $config.maxAttempts attempts (${totalDuration}ms)")
        
        RetryResult(
            success = false,
            error = lastException,
            attempts = config.maxAttempts,
            totalDurationMs = totalDuration
        )
    }
    
    /**
     * Execute operation with exponential backoff
     */
    suspend fun <T> executeWithExponentialBackoff(
        operation: suspend () -> T,
        operationName: String = "operation"
    ): RetryResult<T> {
        return executeWithRetry(
            operation = operation,
            config = RetryConfig(
                maxAttempts = MAX_RETRY_ATTEMPTS,
                baseDelayMs = BASE_DELAY_MS,
                maxDelayMs = MAX_DELAY_MS,
                exponentialBackoff = true,
                jitter = true
            ),
            operationName = operationName
        )
    }
    
    /**
     * Execute operation with fixed delay
     */
    suspend fun <T> executeWithFixedDelay(
        operation: suspend () -> T,
        delayMs: Long = 2000L,
        operationName: String = "operation"
    ): RetryResult<T> {
        return executeWithRetry(
            operation = operation,
            config = RetryConfig(
                maxAttempts = MAX_RETRY_ATTEMPTS,
                baseDelayMs = delayMs,
                maxDelayMs = delayMs,
                exponentialBackoff = false,
                jitter = false
            ),
            operationName = operationName
        )
    }
    
    /**
     * Execute operation with custom retry strategy
     */
    suspend fun <T> executeWithCustomStrategy(
        operation: suspend () -> T,
        maxAttempts: Int,
        delays: List<Long>,
        operationName: String = "operation"
    ): RetryResult<T> = withContext(Dispatchers.IO) {
        
        val startTime = System.currentTimeMillis()
        var lastException: Exception? = null
        
        Log.d(TAG, "🔄 Starting $operationName with custom retry strategy (max $maxAttempts attempts)")
        
        for (attempt in 1..maxAttempts) {
            try {
                Log.d(TAG, "🔄 Attempt $attempt/$maxAttempts for $operationName")
                
                val result = operation()
                
                val totalDuration = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ $operationName succeeded on attempt $attempt (${totalDuration}ms)")
                
                return@withContext RetryResult(
                    success = true,
                    result = result,
                    attempts = attempt,
                    totalDurationMs = totalDuration
                )
                
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "❌ Attempt $attempt/$maxAttempts failed for $operationName: ${e.message}")
                
                // Don't retry on certain types of errors
                if (shouldNotRetry(e)) {
                    Log.e(TAG, "🚫 Not retrying $operationName due to non-retryable error: ${e.message}")
                    break
                }
                
                // Don't delay after the last attempt
                if (attempt < maxAttempts) {
                    val delayMs = delays.getOrElse(attempt - 1) { delays.lastOrNull() ?: 1000L }
                    Log.d(TAG, "⏳ Waiting ${delayMs}ms before retry...")
                    delay(delayMs)
                }
            }
        }
        
        val totalDuration = System.currentTimeMillis() - startTime
        Log.e(TAG, "❌ $operationName failed after $maxAttempts attempts (${totalDuration}ms)")
        
        RetryResult(
            success = false,
            error = lastException,
            attempts = maxAttempts,
            totalDurationMs = totalDuration
        )
    }
    
    /**
     * Calculate delay with exponential backoff and jitter
     */
    private fun calculateDelay(attempt: Int, config: RetryConfig): Long {
        val baseDelay = if (config.exponentialBackoff) {
            config.baseDelayMs * (1L shl (attempt - 1)) // 2^(attempt-1)
        } else {
            config.baseDelayMs
        }
        
        val cappedDelay = minOf(baseDelay, config.maxDelayMs)
        
        return if (config.jitter) {
            // Add jitter to prevent thundering herd
            val jitterRange = cappedDelay / 4
            val jitter = (Math.random() * jitterRange * 2 - jitterRange).toLong()
            maxOf(0, cappedDelay + jitter)
        } else {
            cappedDelay
        }
    }
    
    /**
     * Determine if an error should not be retried
     */
    private fun shouldNotRetry(exception: Exception): Boolean {
        return when {
            // Authentication errors (401) - don't retry
            exception.message?.contains("401") == true -> true
            exception.message?.contains("Authentication failed") == true -> true
            
            // Authorization errors (403) - don't retry
            exception.message?.contains("403") == true -> true
            exception.message?.contains("Forbidden") == true -> true
            
            // Not found errors (404) - don't retry
            exception.message?.contains("404") == true -> true
            exception.message?.contains("Not found") == true -> true
            
            // Payment required (402) - don't retry
            exception.message?.contains("402") == true -> true
            exception.message?.contains("Insufficient credits") == true -> true
            
            // Method not allowed (405) - don't retry
            exception.message?.contains("405") == true -> true
            exception.message?.contains("Method not allowed") == true -> true
            
            // Client errors (4xx) - generally don't retry
            exception.message?.contains("4") == true && 
            exception.message?.contains("Bad request") == true -> true
            
            // Network connectivity issues - retry
            exception.message?.contains("Network") == true -> false
            exception.message?.contains("Connection") == true -> false
            exception.message?.contains("Timeout") == true -> false
            
            // Server errors (5xx) - retry
            exception.message?.contains("500") == true -> false
            exception.message?.contains("502") == true -> false
            exception.message?.contains("503") == true -> false
            exception.message?.contains("504") == true -> false
            
            // Rate limiting (429) - retry with longer delay
            exception.message?.contains("429") == true -> false
            exception.message?.contains("Rate limit") == true -> false
            
            // Default: retry
            else -> false
        }
    }
    
    /**
     * Get retry delay for specific error types
     */
    fun getRetryDelayForError(exception: Exception, attempt: Int): Long {
        return when {
            // Rate limiting - longer delay
            exception.message?.contains("429") == true -> 
                minOf(30000L, 5000L * attempt) // 5s, 10s, 15s, 30s max
            
            // Server errors - exponential backoff
            exception.message?.contains("5") == true -> 
                minOf(8000L, 1000L * (1L shl (attempt - 1)))
            
            // Network errors - moderate delay
            exception.message?.contains("Network") == true -> 
                minOf(4000L, 1000L * attempt)
            
            // Default exponential backoff
            else -> 
                minOf(8000L, 1000L * (1L shl (attempt - 1)))
        }
    }
    
    /**
     * Check if error is retryable
     */
    fun isRetryableError(exception: Exception): Boolean {
        return !shouldNotRetry(exception)
    }
    
    /**
     * Get human-readable error message
     */
    fun getErrorMessage(exception: Exception): String {
        return when {
            exception.message?.contains("401") == true -> "Authentication failed. Please check your credentials."
            exception.message?.contains("402") == true -> "Insufficient credits. Please add more credits to continue."
            exception.message?.contains("403") == true -> "Access forbidden. You don't have permission to perform this action."
            exception.message?.contains("404") == true -> "Resource not found. The requested item doesn't exist."
            exception.message?.contains("429") == true -> "Rate limit exceeded. Please wait before trying again."
            exception.message?.contains("500") == true -> "Server error. Please try again later."
            exception.message?.contains("Network") == true -> "Network error. Please check your internet connection."
            exception.message?.contains("Timeout") == true -> "Request timeout. Please try again."
            else -> "An error occurred: ${exception.message ?: "Unknown error"}"
        }
    }
}
