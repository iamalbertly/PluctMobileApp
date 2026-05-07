package app.pluct.core.circuit

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pluct-Core-Circuit-02EnhancedBreaker - Circuit breaker with auto-reset capability
 * Follows naming convention: [Project]-[Scope]-[Module]-[EnhancedFeature]-[CoreResponsibility]
 * 
 * RELIABILITY FIX #4: Enhanced circuit breaker pattern
 * - Tracks failure count and state (CLOSED/OPEN/HALF_OPEN)
 * - Auto-resets after 30 seconds of silence (no new failures)
 * - Resets immediately on successful operation
 * - Prevents indefinite open state that blocks all requests
 */
class PluctCoreCircuit02EnhancedBreaker {
    private companion object {
        private const val TAG = "CircuitBreaker"
        private const val FAILURE_THRESHOLD = 5
        private const val SILENCE_RESET_TIMEOUT_MS = 30000L // 30 seconds
    }

    enum class State {
        CLOSED,       // Operating normally
        OPEN,         // Rejecting requests due to failures
        HALF_OPEN     // Testing if service recovered
    }

    private var state = State.CLOSED
    private var failureCount = 0
    private var lastFailureTime: Long? = null
    private val mutex = Mutex()

    /**
     * Record a successful operation - immediately close circuit
     */
    suspend fun recordSuccess() = mutex.withLock {
        if (state != State.CLOSED) {
            Log.d(TAG, "✅ Circuit breaker CLOSED - recorded successful operation")
        }
        failureCount = 0
        lastFailureTime = null
        state = State.CLOSED
    }

    /**
     * Record a failure - may open circuit if threshold reached
     * @param isRetryable Whether this failure could succeed on retry
     */
    suspend fun recordFailure(isRetryable: Boolean = true) = mutex.withLock {
        lastFailureTime = System.currentTimeMillis()

        // Non-retryable errors don't contribute to circuit breaker
        if (!isRetryable) {
            Log.d(TAG, "⚠️ Non-retryable error recorded (not counted for circuit breaker)")
            return@withLock
        }

        failureCount++
        Log.d(TAG, "⚠️ Failure recorded ($failureCount/$FAILURE_THRESHOLD)")

        if (failureCount >= FAILURE_THRESHOLD && state == State.CLOSED) {
            state = State.OPEN
            Log.e(TAG, "🔴 Circuit breaker OPENED - $failureCount consecutive failures")
        }
    }

    /**
     * Check if circuit breaker is open (should reject requests)
     * Also performs auto-reset check
     */
    suspend fun isOpen(): Boolean = mutex.withLock {
        // Auto-reset if silent for 30 seconds
        val lastFailure = lastFailureTime
        if (state == State.OPEN && lastFailure != null) {
            val timeSinceFailure = System.currentTimeMillis() - lastFailure
            if (timeSinceFailure > SILENCE_RESET_TIMEOUT_MS) {
                state = State.HALF_OPEN
                Log.d(TAG, "🔄 Circuit breaker auto-reset to HALF_OPEN after 30s silence")
                return@withLock false
            }
        }

        return@withLock state == State.OPEN
    }

    /**
     * Get current state for monitoring/debugging
     */
    suspend fun getState(): State = mutex.withLock { state }

    /**
     * Get failure count for monitoring
     */
    suspend fun getFailureCount(): Int = mutex.withLock { failureCount }

    /**
     * Manually reset circuit breaker (for testing or manual reset)
     */
    suspend fun reset() = mutex.withLock {
        Log.d(TAG, "🔄 Circuit breaker manually reset")
        state = State.CLOSED
        failureCount = 0
        lastFailureTime = null
    }
}
