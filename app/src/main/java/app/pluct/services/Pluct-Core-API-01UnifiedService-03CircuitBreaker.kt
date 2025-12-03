package app.pluct.services

import android.util.Log
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Pluct-Core-API-01UnifiedService-03CircuitBreaker - Circuit breaker logic.
 */
class PluctCoreAPIUnifiedServiceCircuitBreaker {

    companion object {
        private const val TAG = "PluctCoreAPIUnified"
        const val API_LOG_TAG = "PluctAPI"
        private const val CIRCUIT_BREAKER_THRESHOLD = 5
        private const val CIRCUIT_BREAKER_RESET_MS = 60000L
    }

    private val consecutiveFailures = AtomicInteger(0)
    private val circuitBreakerOpen = AtomicLong(0)

    fun isOpen(): Boolean {
        val openTime = circuitBreakerOpen.get()
        if (openTime == 0L) return false

        val timeSinceOpen = System.currentTimeMillis() - openTime
        if (timeSinceOpen > CIRCUIT_BREAKER_RESET_MS) {
            Log.d(API_LOG_TAG, "Circuit breaker reset after ${timeSinceOpen}ms")
            Log.d(TAG, "Circuit breaker reset after ${timeSinceOpen}ms")
            circuitBreakerOpen.set(0)
            consecutiveFailures.set(0)
            return false
        }

        Log.w(API_LOG_TAG, "Circuit breaker is OPEN (opened ${timeSinceOpen}ms ago)")
        Log.w(TAG, "Circuit breaker is OPEN (opened ${timeSinceOpen}ms ago)")
        return true
    }

    fun recordSuccess() {
        consecutiveFailures.set(0)
    }

    fun recordFailure() {
        val failures = consecutiveFailures.incrementAndGet()
        if (failures >= CIRCUIT_BREAKER_THRESHOLD) {
            circuitBreakerOpen.set(System.currentTimeMillis())
            Log.e(API_LOG_TAG, "Circuit breaker OPENED - $failures consecutive failures (threshold: $CIRCUIT_BREAKER_THRESHOLD)")
            Log.e(TAG, "Circuit breaker OPENED - $failures consecutive failures (threshold: $CIRCUIT_BREAKER_THRESHOLD)")
        } else {
            Log.w(API_LOG_TAG, "Consecutive failures: $failures/$CIRCUIT_BREAKER_THRESHOLD")
            Log.w(TAG, "Consecutive failures: $failures/$CIRCUIT_BREAKER_THRESHOLD")
        }
    }

    fun getFailureCount(): Int = consecutiveFailures.get()
}
