package app.pluct.core.retry

import android.util.Log
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
     */
    suspend fun <T> executeWithRetry(
        requestId: String,
        startTime: Long,
        operation: suspend () -> Result<T>
    ): RetryOutcome<T> {
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                val result = operation()
                if (result.isSuccess) {
                    val duration = System.currentTimeMillis() - startTime
                    return RetryOutcome(result, attempt + 1, duration)
                } else {
                    val exception = result.exceptionOrNull() as? Exception ?: Exception("Unknown error")
                    lastException = exception
                    if (!isRetryable(exception)) {
                        Log.d(TAG, "Error is not retryable: ${exception.message}")
                        val duration = System.currentTimeMillis() - startTime
                        return RetryOutcome(Result.failure(exception), attempt + 1, duration)
                    }
                }
            } catch (e: Exception) {
                lastException = e
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
}

