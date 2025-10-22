package app.pluct.core.retry

import app.pluct.core.log.PluctLogger
import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.math.pow
import javax.inject.Inject

/**
 * Pluct-Core-Retry-01ExponentialBackoff - Centralized retry logic for API calls and operations
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Implements exponential backoff and circuit breaker patterns
 */
class PluctRetryEngine @Inject constructor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000, // 1 second
    private val maxDelayMs: Long = 60000, // 1 minute
    private val factor: Double = 2.0,
    private val jitter: Double = 0.1, // 10% jitter
    private val circuitBreakerThreshold: Int = 5, // Number of consecutive failures to open circuit
    private val circuitBreakerResetTimeMs: Long = 300000 // 5 minutes
) {
    private var consecutiveFailures: Int = 0
    private var lastFailureTime: Long = 0L
    private var isCircuitOpen: Boolean = false

    /**
     * Execute a suspend function with retry logic
     */
    suspend fun <T> executeWithRetry(
        tag: String,
        action: suspend () -> T
    ): T {
        if (isCircuitOpen) {
            if (System.currentTimeMillis() - lastFailureTime < circuitBreakerResetTimeMs) {
                PluctLogger.logWarning("Circuit breaker is open for $tag. Not attempting call.")
                throw CircuitBreakerOpenException("Circuit breaker is open for $tag")
            } else {
                PluctLogger.logInfo("Circuit breaker reset time elapsed for $tag. Attempting to close circuit.")
                isCircuitOpen = false
                consecutiveFailures = 0
            }
        }

        var currentDelay = initialDelayMs
        for (attempt in 0 until maxRetries) {
            try {
                val result = action()
                // If successful, reset circuit breaker
                consecutiveFailures = 0
                isCircuitOpen = false
                return result
            } catch (e: Exception) {
                PluctLogger.logWarning("Attempt ${attempt + 1} for $tag failed: ${e.message}")
                consecutiveFailures++
                lastFailureTime = System.currentTimeMillis()

                if (consecutiveFailures >= circuitBreakerThreshold) {
                    isCircuitOpen = true
                    PluctLogger.logWarning("Circuit breaker opened for $tag due to ${consecutiveFailures} consecutive failures.")
                }

                if (attempt < maxRetries - 1) {
                    val delayWithJitter = (currentDelay * (1 + (Math.random() * 2 - 1) * jitter)).toLong()
                    PluctLogger.logInfo("Retrying $tag in ${delayWithJitter}ms...")
                    delay(delayWithJitter)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
                } else {
                    PluctLogger.logError(app.pluct.core.error.ErrorEnvelope("RETRY_FAILED", "All ${maxRetries} attempts for $tag failed.", source = "retry_engine"))
                    throw e // Re-throw the last exception
                }
            }
        }
        // Should not reach here, but as a fallback
        throw IllegalStateException("Retry logic failed to complete for $tag")
    }
}

class CircuitBreakerOpenException(message: String) : IOException(message)
