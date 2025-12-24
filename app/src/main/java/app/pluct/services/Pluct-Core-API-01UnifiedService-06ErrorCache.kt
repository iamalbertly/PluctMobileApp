package app.pluct.services

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Pluct-Core-API-01UnifiedService-06ErrorCache - Cache for API error details
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[SubScope]-[Separation]-[Responsibility]
 * Single source of truth for error caching to provide faster error display on retries
 */
object PluctCoreAPIUnifiedServiceErrorCache {
    
    private const val TAG = "ErrorCache"
    private val errorCache = ConcurrentHashMap<String, CachedErrorDetails>()
    private const val CACHE_EXPIRY_MS = 3600000L // 1 hour
    
    /**
     * Cache error details for a URL
     */
    fun cacheError(url: String, error: PluctCoreAPIDetailedError) {
        if (!isCacheableError(error)) {
            Log.d(TAG, "Skipping cache for transient error on URL: $url")
            return
        }
        errorCache[url] = CachedErrorDetails(
            error = error,
            timestamp = System.currentTimeMillis()
        )
        Log.d(TAG, "Cached error details for URL: $url")
    }
    
    /**
     * Get cached error details for a URL
     */
    fun getCachedError(url: String): PluctCoreAPIDetailedError? {
        val cached = errorCache[url] ?: return null
        if (!isCacheableError(cached.error)) {
            errorCache.remove(url)
            Log.d(TAG, "Cleared transient cached error for URL: $url")
            return null
        }
        
        // Check if cache is expired
        val age = System.currentTimeMillis() - cached.timestamp
        if (age > CACHE_EXPIRY_MS) {
            errorCache.remove(url)
            Log.d(TAG, "Cached error expired for URL: $url (age: ${age}ms)")
            return null
        }
        
        Log.d(TAG, "Retrieved cached error details for URL: $url (age: ${age}ms)")
        return cached.error
    }
    
    /**
     * Clear cache for a specific URL
     */
    fun clearCache(url: String) {
        errorCache.remove(url)
        Log.d(TAG, "Cleared error cache for URL: $url")
    }
    
    /**
     * Clear all cached errors
     */
    fun clearAllCache() {
        errorCache.clear()
        Log.d(TAG, "Cleared all error cache")
    }
    
    /**
     * Check if we have cached error for URL (for fast response)
     */
    fun hasCachedError(url: String): Boolean {
        val cached = errorCache[url] ?: return false
        if (!isCacheableError(cached.error)) {
            errorCache.remove(url)
            return false
        }
        val age = System.currentTimeMillis() - cached.timestamp
        return age <= CACHE_EXPIRY_MS
    }

    private fun isCacheableError(error: PluctCoreAPIDetailedError): Boolean {
        if (error.isRetryable) return false
        val statusCode = error.technicalDetails.responseStatusCode
        if (statusCode == 408 || statusCode == 429 || statusCode >= 500) return false
        val message = error.userMessage.lowercase()
        if (message.contains("timeout") || message.contains("timed out")) return false
        if (message.contains("circuit breaker")) return false
        if (message.contains("temporarily") || message.contains("unavailable")) return false
        return true
    }
}

/**
 * Cached error details
 */
private data class CachedErrorDetails(
    val error: PluctCoreAPIDetailedError,
    val timestamp: Long
)

