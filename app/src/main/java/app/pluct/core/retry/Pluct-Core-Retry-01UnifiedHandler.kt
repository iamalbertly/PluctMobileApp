package app.pluct.core.retry

import android.util.Log
import app.pluct.services.PluctCoreAPIDetailedError
import app.pluct.core.error.PluctCoreError01AuthErrorDetector
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Core-Retry-01UnifiedHandler - Single source of truth for all retry logic
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Consolidates all retry logic from PluctCoreAPIUnifiedService and PluctErrorUnifiedHandler
 */
@Singleton
class PluctCoreRetryUnifiedHandler @Inject constructor() {

    companion object {
        private const val TAG = "PluctRetryHandler"
        private const val MAX_RETRIES = 3
        private const val BASE_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 10000L
        private const val RETRY_MULTIPLIER = 2.0
    }

    data class RetryOutcome<T>(
        val result: Result<T>,
        val attempts: Int,
        val totalDurationMs: Long = 0L
    )

    data class RetryResult<T>(
        val success: Boolean,
        val result: T? = null,
        val error: Exception? = null,
        val attempts: Int,
        val totalDurationMs: Long
    )

    data class RetryConfig(
        val maxAttempts: Int = MAX_RETRIES,
        val baseDelayMs: Long = BASE_RETRY_DELAY_MS,
        val maxDelayMs: Long = MAX_RETRY_DELAY_MS,
        val exponentialBackoff: Boolean = true,
        val jitter: Boolean = false
    )

    /**
     * Determine if an error is retryable
     */
    fun isRetryable(error: Throwable?): Boolean {
        if (error == null) return false
        if (error is PluctCoreAPIDetailedError) {
            val statusCode = error.technicalDetails.responseStatusCode
            if (statusCode == 401 || statusCode == 403) return false
            if (statusCode in 400..499 && statusCode != 408 && statusCode != 429) return false
        }
        val errorMessage = error.message?.lowercase() ?: ""
        
        // Network/timeout errors are retryable
        if (errorMessage.contains("network", ignoreCase = true) ||
            errorMessage.contains("timeout", ignoreCase = true) ||
            errorMessage.contains("connection", ignoreCase = true) ||
            errorMessage.contains("timed out", ignoreCase = true)) {
            return true
        }
        
        // Server errors are retryable
        if (errorMessage.contains("500") ||
            errorMessage.contains("502") ||
            errorMessage.contains("503") ||
            errorMessage.contains("504") ||
            errorMessage.contains("server error", ignoreCase = true) ||
            errorMessage.contains("service unavailable", ignoreCase = true)) {
            return true
        }
        
        // Rate limits are retryable
        if (errorMessage.contains("429") || errorMessage.contains("rate limit", ignoreCase = true)) {
            return true
        }
        
        // Auth errors are NOT retryable (handled separately)
        if (errorMessage.contains("401") ||
            errorMessage.contains("unauthorized", ignoreCase = true) ||
            errorMessage.contains("authentication", ignoreCase = true)) {
            return false
        }
        
        // Client errors are NOT retryable
        if (errorMessage.contains("invalid", ignoreCase = true) ||
            errorMessage.contains("validation", ignoreCase = true) ||
            errorMessage.contains("400", ignoreCase = true) ||
            errorMessage.contains("402", ignoreCase = true) ||
            errorMessage.contains("insufficient", ignoreCase = true) ||
            errorMessage.contains("payment", ignoreCase = true)) {
            return false
        }
        
        return true
    }

    /**
     * Execute operation with retry logic (Result-based API for API service)
     * @param on401Error Optional callback to handle 401 errors (e.g., token refresh)
     *                   Should return a new operation to retry with refreshed credentials
     */
    suspend fun <T> executeWithRetry(
        requestId: String,
        startTime: Long,
        operation: suspend () -> Result<T>,
        on401Error: (suspend () -> Result<T>)? = null
    ): RetryOutcome<T> {
        var lastException: Exception? = null
        var currentOperation = operation

        for (attempt in 0 until MAX_RETRIES) {
            try {
                val result = currentOperation()
                if (result.isSuccess) {
                    val duration = System.currentTimeMillis() - startTime
                    return RetryOutcome(result, attempt + 1, duration)
                } else {
                    val exception = result.exceptionOrNull() as? Exception ?: Exception("Unknown error")
                    lastException = exception
                    
                    // Check if this is a 401 error and we have a token refresh handler
                    val is401Error = PluctCoreError01AuthErrorDetector.is401Unauthorized(exception)
                    if (is401Error && on401Error != null && attempt < MAX_RETRIES - 1) {
                        Log.d(TAG, "401 error detected, refreshing token before retry (attempt ${attempt + 1}/$MAX_RETRIES)")
                        currentOperation = on401Error
                        // Retry immediately with new token (no delay needed for auth refresh)
                        continue
                    }
                    
                    if (!isRetryable(exception)) {
                        Log.d(TAG, "Error is not retryable: ${exception.message}")
                        val duration = System.currentTimeMillis() - startTime
                        return RetryOutcome(Result.failure(exception), attempt + 1, duration)
                    }
                }
            } catch (e: Exception) {
                lastException = e
                
                // Check if this is a 401 error and we have a token refresh handler
                val is401Error = PluctCoreError01AuthErrorDetector.is401Unauthorized(e)
                if (is401Error && on401Error != null && attempt < MAX_RETRIES - 1) {
                    Log.d(TAG, "401 exception detected, refreshing token before retry (attempt ${attempt + 1}/$MAX_RETRIES)")
                    currentOperation = on401Error
                    // Retry immediately with new token (no delay needed for auth refresh)
                    continue
                }
                
                if (!isRetryable(e)) {
                    Log.d(TAG, "Exception is not retryable: ${e.message}")
                    val duration = System.currentTimeMillis() - startTime
                    return RetryOutcome(Result.failure(e), attempt + 1, duration)
                }
            }

            if (attempt < MAX_RETRIES - 1) {
                val delayMs = calculateRetryDelay(attempt)
                Log.d(TAG, "Retrying request $requestId in ${delayMs}ms (attempt ${attempt + 1}/$MAX_RETRIES)...")
                delay(delayMs)
            }
        }

        val duration = System.currentTimeMillis() - startTime
        Log.e(TAG, "Request $requestId failed after $MAX_RETRIES attempts")
        val finalException = lastException ?: Exception("Request failed after $MAX_RETRIES attempts")
        return RetryOutcome(Result.failure(finalException), MAX_RETRIES, duration)
    }


    /**
     * Handle operation with retry logic (direct value API for UI components)
     */
    suspend fun <T> handleWithRetry(
        operation: suspend () -> T,
        config: RetryConfig = RetryConfig(),
        operationName: String = "operation"
    ): RetryResult<T> {
        val startTime = System.currentTimeMillis()
        var lastException: Exception? = null

        repeat(config.maxAttempts) { attempt ->
            try {
                val result = operation()
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ $operationName succeeded on attempt ${attempt + 1}")
                return RetryResult(
                    success = true,
                    result = result,
                    attempts = attempt + 1,
                    totalDurationMs = duration
                )
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "❌ $operationName failed on attempt ${attempt + 1}: ${e.message}")

                if (!isRetryable(e)) {
                    val duration = System.currentTimeMillis() - startTime
                    return RetryResult(
                        success = false,
                        error = e,
                        attempts = attempt + 1,
                        totalDurationMs = duration
                    )
                }

                if (attempt < config.maxAttempts - 1) {
                    val delay = calculateRetryDelay(attempt, config)
                    Log.i(TAG, "⏳ Retrying $operationName in ${delay}ms")
                    delay(delay)
                }
            }
        }

        val totalDuration = System.currentTimeMillis() - startTime
        Log.e(TAG, "❌ $operationName failed after ${config.maxAttempts} attempts")
        return RetryResult(
            success = false,
            error = lastException,
            attempts = config.maxAttempts,
            totalDurationMs = totalDuration
        )
    }

    /**
     * Calculate retry delay with exponential backoff
     */
    private fun calculateRetryDelay(attempt: Int): Long {
        val delay = BASE_RETRY_DELAY_MS * RETRY_MULTIPLIER.pow(attempt)
        return min(delay.toLong(), MAX_RETRY_DELAY_MS)
    }

    /**
     * Calculate retry delay with configurable backoff and jitter
     */
    private fun calculateRetryDelay(attempt: Int, config: RetryConfig): Long {
        val baseDelay = if (config.exponentialBackoff) {
            config.baseDelayMs * RETRY_MULTIPLIER.pow(attempt)
        } else {
            config.baseDelayMs
        }

        val cappedDelay = minOf(baseDelay.toLong(), config.maxDelayMs)

        return if (config.jitter) {
            val jitter = (cappedDelay * 0.1 * Math.random()).toLong()
            cappedDelay + jitter
        } else {
            cappedDelay
        }
    }

    /**
     * Execute operation with exponential backoff retry (consolidated from ExponentialBackoffHandler)
     * @param maxAttempts Maximum number of attempts
     * @param retryCheck Function to check if error is retryable
     * @param operation Operation to execute
     * @return Result of operation
     */
    suspend fun <T> executeWithBackoff(
        maxAttempts: Int = 3,
        retryCheck: (Throwable?) -> Boolean = { error -> isRetryable(error) },
        operation: suspend () -> Result<T>
    ): Result<T> {
        var lastError: Throwable? = null
        
        repeat(maxAttempts) { attempt ->
            val result = operation()
            
            if (result.isSuccess) {
                return result
            }
            
            lastError = result.exceptionOrNull()
            
            // Check if error is retryable
            if (!retryCheck(lastError)) {
                Log.d(TAG, "Error is not retryable, stopping retries: ${lastError?.message}")
                return result
            }
            
            // Don't delay after last attempt
            if (attempt < maxAttempts - 1) {
                val delayMs = calculateRetryDelay(attempt)
                Log.d(TAG, "Retry attempt ${attempt + 1}/$maxAttempts failed, waiting ${delayMs}ms before retry")
                delay(delayMs)
            }
        }
        
        Log.w(TAG, "All $maxAttempts retry attempts exhausted")
        return Result.failure(lastError ?: Exception("Retry exhausted"))
    }
}

