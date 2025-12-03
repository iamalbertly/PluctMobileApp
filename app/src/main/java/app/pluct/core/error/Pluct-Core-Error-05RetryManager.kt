package app.pluct.core.error

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.math.pow

/**
 * Pluct-Core-Error-05RetryManager - Centralized retry logic for failed operations.
 */
object PluctCoreError05RetryManager {

    private const val TAG = "RetryManager"
    private const val DEFAULT_MAX_ATTEMPTS = 3
    private const val DEFAULT_BASE_DELAY_MS = 1000L
    private const val DEFAULT_MAX_DELAY_MS = 10000L
    private const val DEFAULT_RETRY_MULTIPLIER = 2.0

    data class RetryConfig(
        val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        val baseDelayMs: Long = DEFAULT_BASE_DELAY_MS,
        val maxDelayMs: Long = DEFAULT_MAX_DELAY_MS,
        val retryMultiplier: Double = DEFAULT_RETRY_MULTIPLIER,
        val exponentialBackoff: Boolean = true,
        val jitter: Boolean = true
    )

    sealed class RetryResult<T> {
        data class Success<T>(val result: T, val attempts: Int) : RetryResult<T>()
        data class Failure<T>(val error: Throwable, val attempts: Int) : RetryResult<T>()
    }

    fun isRetryable(error: Throwable?): Boolean {
        if (error == null) return false

        val errorMessage = error.message?.lowercase() ?: ""
        val errorType = error.javaClass.simpleName

        if (errorType.contains("Network", ignoreCase = true) ||
            errorType.contains("Timeout", ignoreCase = true) ||
            errorType.contains("Connection", ignoreCase = true) ||
            errorMessage.contains("network", ignoreCase = true) ||
            errorMessage.contains("timeout", ignoreCase = true) ||
            errorMessage.contains("connection", ignoreCase = true)) {
            return true
        }

        if (errorMessage.contains("500") ||
            errorMessage.contains("502") ||
            errorMessage.contains("503") ||
            errorMessage.contains("504") ||
            errorMessage.contains("server error", ignoreCase = true) ||
            errorMessage.contains("service unavailable", ignoreCase = true)) {
            return true
        }

        if (errorMessage.contains("429") || errorMessage.contains("rate limit", ignoreCase = true)) {
            return true
        }

        if (errorMessage.contains("401") ||
            errorMessage.contains("unauthorized", ignoreCase = true) ||
            errorMessage.contains("authentication", ignoreCase = true)) {
            return false
        }

        if (errorMessage.contains("invalid", ignoreCase = true) ||
            errorMessage.contains("validation", ignoreCase = true) ||
            errorMessage.contains("400", ignoreCase = true)) {
            return false
        }

        if (errorMessage.contains("402") ||
            errorMessage.contains("insufficient", ignoreCase = true) ||
            errorMessage.contains("payment", ignoreCase = true)) {
            return false
        }

        return true
    }

    fun calculateRetryDelay(attempt: Int, config: RetryConfig = RetryConfig()): Long {
        if (!config.exponentialBackoff) return config.baseDelayMs
        val delay = (config.baseDelayMs * config.retryMultiplier.pow(attempt - 1)).toLong()
        val capped = delay.coerceAtMost(config.maxDelayMs)
        return if (config.jitter) {
            val jitterAmount = capped * 0.1
            capped + (Math.random() * jitterAmount).toLong()
        } else {
            capped
        }
    }

    suspend fun <T> executeWithRetry(
        operation: suspend () -> T,
        config: RetryConfig = RetryConfig(),
        operationName: String = "operation"
    ): RetryResult<T> {
        var lastError: Throwable? = null

        for (attempt in 1..config.maxAttempts) {
            try {
                val result = operation()
                Log.d(TAG, "$operationName succeeded on attempt $attempt")
                return RetryResult.Success(result, attempt)
            } catch (e: Throwable) {
                lastError = e
                Log.w(TAG, "$operationName failed on attempt $attempt: ${e.message}")

                if (!isRetryable(e)) {
                    Log.d(TAG, "Error is not retryable: ${e.message}")
                    return RetryResult.Failure(e, attempt)
                }

                if (attempt < config.maxAttempts) {
                    val delayMs = calculateRetryDelay(attempt, config)
                    Log.d(TAG, "Retrying $operationName in ${delayMs}ms...")
                    delay(delayMs)
                }
            }
        }

        Log.e(TAG, "$operationName failed after ${config.maxAttempts} attempts")
        return RetryResult.Failure(
            lastError ?: Exception("Operation failed after ${config.maxAttempts} attempts"),
            config.maxAttempts
        )
    }
}
