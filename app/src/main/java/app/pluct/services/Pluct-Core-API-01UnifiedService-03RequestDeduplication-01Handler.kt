package app.pluct.services

import android.util.Log
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pluct-Core-API-01UnifiedService-03RequestDeduplication-01Handler
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[RequestDeduplication]-[Handler]
 * 6 scope layers: Project, Core, API, UnifiedService, RequestDeduplication, Handler
 * Prevents duplicate API requests using clientRequestId for idempotency
 */
class PluctCoreAPI01UnifiedService03RequestDeduplication01Handler {
    private val TAG = "RequestDeduplication"
    private val activeRequests = ConcurrentHashMap<String, RequestState>()
    private val cachedResponses = ConcurrentHashMap<String, CachedResponse>()
    private val mutex = Mutex()
    
    data class RequestState(
        val requestId: String,
        val url: String,
        val timestamp: Long,
        val inProgress: Boolean = true
    )
    
    data class CachedResponse(
        val requestId: String,
        val response: Any,
        val timestamp: Long,
        val expiresAt: Long
    )
    
    /**
     * Generate or get existing request ID for URL
     * Returns existing request ID if request is in progress, otherwise generates new one
     */
    suspend fun generateOrGetRequestId(url: String): String {
        return mutex.withLock {
            // Check for existing in-progress request
            activeRequests.values.forEach { state ->
                if (state.url == url && state.inProgress) {
                    Log.d(TAG, "Reusing existing request ID for URL: ${state.requestId}")
                    return state.requestId
                }
            }
            
            // Generate new request ID
            val requestId = "req_${UUID.randomUUID()}"
            activeRequests[requestId] = RequestState(
                requestId = requestId,
                url = url,
                timestamp = System.currentTimeMillis()
            )
            Log.d(TAG, "Generated new request ID: $requestId for URL: $url")
            return requestId
        }
    }
    
    /**
     * Check if request is in progress
     */
    fun isRequestInProgress(requestId: String): Boolean {
        val state = activeRequests[requestId]
        return state?.inProgress == true
    }
    
    /**
     * Mark request as completed
     */
    suspend fun markRequestCompleted(requestId: String) {
        mutex.withLock {
            activeRequests.remove(requestId)
            Log.d(TAG, "Request completed: $requestId")
        }
    }
    
    /**
     * Cache response for idempotency
     */
    suspend fun cacheResponse(requestId: String, response: Any, ttlSeconds: Int = 300) {
        mutex.withLock {
            val expiresAt = System.currentTimeMillis() + (ttlSeconds * 1000L)
            cachedResponses[requestId] = CachedResponse(
                requestId = requestId,
                response = response,
                timestamp = System.currentTimeMillis(),
                expiresAt = expiresAt
            )
            Log.d(TAG, "Response cached for request ID: $requestId, expires in ${ttlSeconds}s")
        }
    }
    
    /**
     * Get cached response if available and not expired
     */
    suspend fun getCachedResponse(requestId: String): Any? {
        return mutex.withLock {
            val cached = cachedResponses[requestId]
            if (cached != null) {
                val now = System.currentTimeMillis()
                if (now < cached.expiresAt) {
                    Log.d(TAG, "Returning cached response for request ID: $requestId")
                    return cached.response
                } else {
                    // Expired, remove it
                    cachedResponses.remove(requestId)
                    Log.d(TAG, "Cached response expired for request ID: $requestId")
                }
            }
            null
        }
    }
    
    /**
     * Cleanup expired cached responses
     */
    suspend fun cleanupExpiredResponses() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val expired = cachedResponses.values.filter { it.expiresAt < now }
            expired.forEach { cached ->
                cachedResponses.remove(cached.requestId)
                Log.d(TAG, "Cleaned up expired cached response: ${cached.requestId}")
            }
        }
    }
    
    /**
     * Get active request count
     */
    fun getActiveRequestCount(): Int {
        return activeRequests.values.count { it.inProgress }
    }
}

