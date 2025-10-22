package app.pluct.utils

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Performance optimization utilities
 * Provides caching, connection pooling, and resource management
 */
object PerformanceOptimizer {
    
    private const val TAG = "PerformanceOptimizer"
    private const val CACHE_SIZE_LIMIT = 100
    private const val CACHE_TTL_MS = 300000L // 5 minutes
    
    // In-memory cache for API responses
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val cacheHits = AtomicInteger(0)
    private val cacheMisses = AtomicInteger(0)
    
    data class CacheEntry(
        val data: Any,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Get cached data if available and not expired
     */
    fun <T> getCached(key: String): T? {
        val entry = cache[key]
        return if (entry != null && !isExpired(entry)) {
            cacheHits.incrementAndGet()
            Log.d(TAG, "Cache hit for key: $key")
            entry.data as? T
        } else {
            cacheMisses.incrementAndGet()
            if (entry != null) {
                cache.remove(key)
                Log.d(TAG, "Cache expired for key: $key")
            } else {
                Log.d(TAG, "Cache miss for key: $key")
            }
            null
        }
    }
    
    /**
     * Cache data with TTL
     */
    fun <T> putCached(key: String, data: T) {
        // Clean up old entries if cache is getting too large
        if (cache.size >= CACHE_SIZE_LIMIT) {
            cleanupExpiredEntries()
        }
        
        cache[key] = CacheEntry(data as Any)
        Log.d(TAG, "Cached data for key: $key")
    }
    
    /**
     * Check if cache entry is expired
     */
    private fun isExpired(entry: CacheEntry): Boolean {
        return System.currentTimeMillis() - entry.timestamp > CACHE_TTL_MS
    }
    
    /**
     * Clean up expired cache entries
     */
    private fun cleanupExpiredEntries() {
        val now = System.currentTimeMillis()
        val iterator = cache.iterator()
        var removedCount = 0
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.timestamp > CACHE_TTL_MS) {
                iterator.remove()
                removedCount++
            }
        }
        
        if (removedCount > 0) {
            Log.d(TAG, "Cleaned up $removedCount expired cache entries")
        }
    }
    
    /**
     * Clear all cache entries
     */
    fun clearCache() {
        cache.clear()
        Log.d(TAG, "Cache cleared")
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        val totalRequests = cacheHits.get() + cacheMisses.get()
        val hitRate = if (totalRequests > 0) {
            (cacheHits.get().toFloat() / totalRequests * 100).toInt()
        } else 0
        
        return CacheStats(
            hits = cacheHits.get(),
            misses = cacheMisses.get(),
            hitRate = hitRate,
            size = cache.size
        )
    }
    
    data class CacheStats(
        val hits: Int,
        val misses: Int,
        val hitRate: Int,
        val size: Int
    )
    
    /**
     * Debounce function calls to prevent excessive API calls
     */
    class Debouncer(private val delayMs: Long) {
        private var job: Job? = null
        
        fun debounce(action: suspend () -> Unit) {
            job?.cancel()
            job = CoroutineScope(Dispatchers.IO).launch {
                delay(delayMs)
                action()
            }
        }
    }
    
    /**
     * Throttle function calls to limit rate
     */
    class Throttler(private val intervalMs: Long) {
        private var lastCallTime = 0L
        
        suspend fun throttle(action: suspend () -> Unit) {
            val now = System.currentTimeMillis()
            if (now - lastCallTime >= intervalMs) {
                lastCallTime = now
                action()
            } else {
                Log.d(TAG, "Throttled call - too frequent")
            }
        }
    }
    
    /**
     * Memory usage monitoring
     */
    fun getMemoryUsage(): MemoryUsage {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        return MemoryUsage(
            used = usedMemory,
            free = freeMemory,
            total = totalMemory,
            max = maxMemory,
            usagePercentage = (usedMemory.toFloat() / maxMemory * 100).toInt()
        )
    }
    
    data class MemoryUsage(
        val used: Long,
        val free: Long,
        val total: Long,
        val max: Long,
        val usagePercentage: Int
    )
}
