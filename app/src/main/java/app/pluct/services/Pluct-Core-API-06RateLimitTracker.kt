package app.pluct.services

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Pluct-Core-API-06RateLimitTracker - Client-side rate limit tracking
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Prevents hitting the 429 error by tracking requests locally
 */
@Singleton
class PluctCoreRateLimitTracker @Inject constructor() {

    companion object {
        private const val TAG = "RateLimitTracker"
        private const val MAX_REQUESTS_PER_HOUR = 10
        private const val ONE_HOUR_MS = 3600000L
    }

    // Thread-safe queue to store request timestamps
    private val requestTimestamps = ConcurrentLinkedDeque<Long>()

    /**
     * Check if a new request can be made
     */
    fun canMakeRequest(): Boolean {
        cleanupOldRequests()
        val count = requestTimestamps.size
        val canRequest = count < MAX_REQUESTS_PER_HOUR
        
        Log.d(TAG, "Rate limit check: $count/$MAX_REQUESTS_PER_HOUR requests in last hour. Allowed: $canRequest")
        return canRequest
    }

    /**
     * Record a new request
     */
    fun recordRequest() {
        requestTimestamps.add(System.currentTimeMillis())
        Log.d(TAG, "Request recorded. Total active requests: ${requestTimestamps.size}")
    }

    /**
     * Get time remaining until next slot opens (in milliseconds)
     */
    fun getTimeToReset(): Long {
        cleanupOldRequests()
        if (requestTimestamps.isEmpty()) return 0L
        
        // If we haven't hit the limit, no wait time
        if (requestTimestamps.size < MAX_REQUESTS_PER_HOUR) return 0L
        
        // If we hit the limit, the next slot opens when the oldest request expires
        val oldestRequest = requestTimestamps.peekFirst() ?: return 0L
        val expirationTime = oldestRequest + ONE_HOUR_MS
        val timeRemaining = expirationTime - System.currentTimeMillis()
        
        return timeRemaining.coerceAtLeast(0L)
    }

    /**
     * Remove requests older than 1 hour
     */
    private fun cleanupOldRequests() {
        val cutoffTime = System.currentTimeMillis() - ONE_HOUR_MS
        
        // Remove timestamps older than cutoff
        while (!requestTimestamps.isEmpty() && requestTimestamps.peekFirst()!! < cutoffTime) {
            requestTimestamps.pollFirst()
        }
    }
    
    /**
     * Reset tracker (for testing/debug)
     */
    fun reset() {
        requestTimestamps.clear()
        Log.d(TAG, "Rate limit tracker reset")
    }
}
