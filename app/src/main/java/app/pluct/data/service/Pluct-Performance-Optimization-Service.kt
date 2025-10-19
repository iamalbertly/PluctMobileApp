package app.pluct.data.service

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance optimization and caching service
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctPerformanceOptimizationService @Inject constructor() {
    
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val _performanceMetrics = MutableSharedFlow<PerformanceMetric>()
    val performanceMetrics: SharedFlow<PerformanceMetric> = _performanceMetrics.asSharedFlow()
    
    data class CacheEntry(
        val data: Any,
        val timestamp: Long,
        val ttl: Long
    )
    
    data class PerformanceMetric(
        val operation: String,
        val durationMs: Long,
        val memoryUsage: Long,
        val cacheHit: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Get data from cache or execute operation
     */
    suspend fun <T> getOrExecute(
        key: String,
        ttlMs: Long = 300000, // 5 minutes default
        operation: suspend () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        
        // Check cache first
        val cached = cache[key]
        if (cached != null && !isExpired(cached)) {
            val duration = System.currentTimeMillis() - startTime
            emitMetric("cache_hit", duration, 0, true)
            Log.i("Performance", "✅ Cache hit for key: $key")
            @Suppress("UNCHECKED_CAST")
            return cached.data as T
        }
        
        // Execute operation
        Log.i("Performance", "🔄 Cache miss for key: $key, executing operation")
        val result = operation()
        
        // Store in cache
        cache[key] = CacheEntry(
            data = result as Any,
            timestamp = System.currentTimeMillis(),
            ttl = ttlMs
        )
        
        val duration = System.currentTimeMillis() - startTime
        emitMetric("cache_miss", duration, getMemoryUsage(), false)
        
        Log.i("Performance", "✅ Operation completed for key: $key (${duration}ms)")
        return result
    }
    
    /**
     * Batch process multiple operations
     */
    suspend fun batchProcess(
        operations: List<suspend () -> Any>,
        batchSize: Int = 5
    ): List<Any> {
        val startTime = System.currentTimeMillis()
        Log.i("Performance", "🔄 Starting batch processing: ${operations.size} operations")
        
        val results = mutableListOf<Any>()
        
        operations.chunked(batchSize).forEach { batch ->
            val batchResults = batch.map { operation ->
                CoroutineScope(Dispatchers.IO).async { operation() }
            }.awaitAll()
            
            results.addAll(batchResults)
        }
        
        val duration = System.currentTimeMillis() - startTime
        emitMetric("batch_process", duration, getMemoryUsage(), false)
        
        Log.i("Performance", "✅ Batch processing completed: ${operations.size} operations (${duration}ms)")
        return results
    }
    
    /**
     * Optimize memory usage
     */
    fun optimizeMemory() {
        Log.i("Performance", "🧹 Optimizing memory usage")
        
        // Clear expired cache entries
        val now = System.currentTimeMillis()
        val expiredKeys = cache.entries.filter { isExpired(it.value) }.map { it.key }
        
        expiredKeys.forEach { key ->
            cache.remove(key)
        }
        
        Log.i("Performance", "🗑️ Removed ${expiredKeys.size} expired cache entries")
        
        // Force garbage collection
        System.gc()
        
        CoroutineScope(Dispatchers.IO).launch {
            emitMetric("memory_optimization", 0, getMemoryUsage(), false)
        }
    }
    
    /**
     * Preload data for better performance
     */
    suspend fun preloadData(keys: List<String>, operation: suspend (String) -> Any) {
        Log.i("Performance", "🚀 Preloading data for ${keys.size} keys")
        
        val startTime = System.currentTimeMillis()
        
        keys.forEach { key ->
            if (!cache.containsKey(key)) {
                try {
                    val data = operation(key)
                    cache[key] = CacheEntry(
                        data = data,
                        timestamp = System.currentTimeMillis(),
                        ttl = 600000 // 10 minutes for preloaded data
                    )
                } catch (e: Exception) {
                    Log.w("Performance", "⚠️ Failed to preload data for key $key: ${e.message}")
                }
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        emitMetric("preload", duration, getMemoryUsage(), false)
        
        Log.i("Performance", "✅ Preloading completed: ${keys.size} keys (${duration}ms)")
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        val now = System.currentTimeMillis()
        val totalEntries = cache.size
        val expiredEntries = cache.values.count { isExpired(it) }
        val activeEntries = totalEntries - expiredEntries
        
        return CacheStats(
            totalEntries = totalEntries,
            activeEntries = activeEntries,
            expiredEntries = expiredEntries,
            memoryUsage = getMemoryUsage()
        )
    }
    
    /**
     * Clear all cache
     */
    fun clearCache() {
        Log.i("Performance", "🧹 Clearing all cache")
        cache.clear()
        CoroutineScope(Dispatchers.IO).launch {
            emitMetric("cache_clear", 0, getMemoryUsage(), false)
        }
    }
    
    /**
     * Check if cache entry is expired
     */
    private fun isExpired(entry: CacheEntry): Boolean {
        return System.currentTimeMillis() - entry.timestamp > entry.ttl
    }
    
    /**
     * Get current memory usage
     */
    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    /**
     * Emit performance metric
     */
    private suspend fun emitMetric(operation: String, durationMs: Long, memoryUsage: Long, cacheHit: Boolean) {
        _performanceMetrics.emit(
            PerformanceMetric(
                operation = operation,
                durationMs = durationMs,
                memoryUsage = memoryUsage,
                cacheHit = cacheHit
            )
        )
    }
    
    data class CacheStats(
        val totalEntries: Int,
        val activeEntries: Int,
        val expiredEntries: Int,
        val memoryUsage: Long
    )
}
