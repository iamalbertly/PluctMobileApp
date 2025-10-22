package app.pluct.services

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Cache-Service - Performance optimization and caching service
 * Single source of truth for caching operations
 * Adheres to 300-line limit with smart separation of concerns
 */

data class CacheEntry<T>(
    val key: String,
    val value: T,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Long = 300000 // 5 minutes default TTL
)

data class CacheStats(
    val totalEntries: Int,
    val hitCount: Int,
    val missCount: Int,
    val hitRate: Double,
    val memoryUsage: Long
)

@Singleton
class PluctCacheService @Inject constructor() {
    private val _cache = MutableStateFlow<Map<String, CacheEntry<Any>>>(emptyMap())
    val cache: StateFlow<Map<String, CacheEntry<Any>>> = _cache.asStateFlow()
    
    private val _hitCount = MutableStateFlow(0)
    private val _missCount = MutableStateFlow(0)
    
    fun <T> put(key: String, value: T, ttl: Long = 300000) {
        val entry = CacheEntry(
            key = key,
            value = value as Any,
            ttl = ttl
        )
        
        _cache.value = _cache.value + (key to entry)
        Log.d("PluctCacheService", "💾 Cached: $key")
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = _cache.value[key]
        
        if (entry == null) {
            _missCount.value++
            Log.d("PluctCacheService", "❌ Cache miss: $key")
            return null
        }
        
        if (isExpired(entry)) {
            _cache.value = _cache.value - key
            _missCount.value++
            Log.d("PluctCacheService", "⏰ Cache expired: $key")
            return null
        }
        
        _hitCount.value++
        Log.d("PluctCacheService", "✅ Cache hit: $key")
        return entry.value as T
    }
    
    fun remove(key: String) {
        _cache.value = _cache.value - key
        Log.d("PluctCacheService", "🗑️ Removed from cache: $key")
    }
    
    fun clear() {
        _cache.value = emptyMap()
        _hitCount.value = 0
        _missCount.value = 0
        Log.d("PluctCacheService", "🧹 Cache cleared")
    }
    
    fun getStats(): CacheStats {
        val totalEntries = _cache.value.size
        val hitCount = _hitCount.value
        val missCount = _missCount.value
        val totalRequests = hitCount + missCount
        val hitRate = if (totalRequests > 0) hitCount.toDouble() / totalRequests else 0.0
        
        return CacheStats(
            totalEntries = totalEntries,
            hitCount = hitCount,
            missCount = missCount,
            hitRate = hitRate,
            memoryUsage = estimateMemoryUsage()
        )
    }
    
    private fun isExpired(entry: CacheEntry<Any>): Boolean {
        return System.currentTimeMillis() - entry.timestamp > entry.ttl
    }
    
    private fun estimateMemoryUsage(): Long {
        // Rough estimation of memory usage
        return _cache.value.size * 1024L // Assume 1KB per entry
    }
    
    fun cleanupExpired() {
        val now = System.currentTimeMillis()
        val expiredKeys = _cache.value.filter { (_, entry) ->
            now - entry.timestamp > entry.ttl
        }.keys
        
        if (expiredKeys.isNotEmpty()) {
            _cache.value = _cache.value - expiredKeys
            Log.d("PluctCacheService", "🧹 Cleaned up ${expiredKeys.size} expired entries")
        }
    }
}

