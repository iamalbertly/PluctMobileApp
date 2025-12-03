package app.pluct.services

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

/**
 * Pluct-Core-API-01UnifiedService-02RetryHandler - Retry logic handler.
 */
class PluctCoreAPIUnifiedServiceRetryHandler {

    companion object {
        private const val MAX_RETRIES = 3
        private const val BASE_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 10000L
        private const val RETRY_MULTIPLIER = 2.0
    }

    data class RetryOutcome<T>(val result: Result<T>, val attempts: Int)

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
                    return RetryOutcome(result, attempt + 1)
                } else {
                    lastException = result.exceptionOrNull() as? Exception ?: Exception("Unknown error")
                }
            } catch (e: Exception) {
                lastException = e
            }

            if (attempt < MAX_RETRIES - 1) {
                delay(calculateRetryDelay(attempt))
            }
        }

        return RetryOutcome(Result.failure(lastException ?: Exception("Request failed after $MAX_RETRIES attempts")), MAX_RETRIES)
    }

    private fun calculateRetryDelay(attempt: Int): Long {
        val delay = BASE_RETRY_DELAY_MS * RETRY_MULTIPLIER.pow(attempt)
        return min(delay.toLong(), MAX_RETRY_DELAY_MS)
    }
}
