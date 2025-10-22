package app.pluct.data.cache

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Cache-Policy-Manager - Cache policy and optimization
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctCachePolicyManager @Inject constructor() {
    companion object {
        private const val TAG = "PluctCachePolicyManager"
        private const val CACHE_EXPIRY_HOURS = 24
        private const val MAX_ACCESS_COUNT = 100
    }

    /**
     * Check if cache entry is expired
     */
    fun isExpired(entry: CacheEntry): Boolean {
        val currentTime = System.currentTimeMillis()
        val expiryTime = entry.timestamp + (CACHE_EXPIRY_HOURS * 60 * 60 * 1000)
        return currentTime > expiryTime
    }

    /**
     * Check if cache entry should be evicted based on access patterns
     */
    fun shouldEvict(entry: CacheEntry): Boolean {
        return entry.accessCount > MAX_ACCESS_COUNT
    }

    /**
     * Get cache priority score for LRU eviction
     */
    fun getPriorityScore(entry: CacheEntry): Double {
        val age = System.currentTimeMillis() - entry.timestamp
        val accessCount = entry.accessCount
        
        // Higher score = higher priority (less likely to be evicted)
        return (accessCount.toDouble() / (age / 1000.0)) + (entry.accessCount * 0.1)
    }

    /**
     * Optimize cache by removing expired and low-priority entries
     */
    fun optimizeCache(memoryCache: ConcurrentHashMap<String, CacheEntry>): Int {
        var removedCount = 0
        
        val iterator = memoryCache.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val cacheEntry = entry.value
            
            if (isExpired(cacheEntry) || shouldEvict(cacheEntry)) {
                iterator.remove()
                removedCount++
                Log.d(TAG, "Evicted cache entry: ${entry.key}")
            }
        }
        
        Log.d(TAG, "Cache optimization completed. Removed $removedCount entries")
        return removedCount
    }

    /**
     * Get cache health metrics
     */
    fun getCacheHealth(memoryCache: ConcurrentHashMap<String, CacheEntry>): CacheHealth {
        val totalEntries = memoryCache.size
        val expiredEntries = memoryCache.values.count { isExpired(it) }
        val highAccessEntries = memoryCache.values.count { it.accessCount > 10 }
        
        val healthScore = when {
            expiredEntries == 0 && highAccessEntries > totalEntries * 0.5 -> 100
            expiredEntries < totalEntries * 0.2 -> 80
            expiredEntries < totalEntries * 0.5 -> 60
            else -> 40
        }
        
        return CacheHealth(
            totalEntries = totalEntries,
            expiredEntries = expiredEntries,
            highAccessEntries = highAccessEntries,
            healthScore = healthScore
        )
    }
}

data class CacheHealth(
    val totalEntries: Int,
    val expiredEntries: Int,
    val highAccessEntries: Int,
    val healthScore: Int
)
