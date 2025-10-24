package app.pluct.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * Pluct-Cache-01Manager - Performance optimization and caching system
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */

class PluctCacheManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: PluctCacheManager? = null
        
        fun getInstance(): PluctCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PluctCacheManager().also { INSTANCE = it }
            }
        }
    }
    
    private val cache = ConcurrentHashMap<String, CacheEntry<Any>>()
    private val _cacheStats = MutableStateFlow(CacheStats())
    val cacheStats: StateFlow<CacheStats> = _cacheStats.asStateFlow()
    
    /**
     * Stores a value in the cache with TTL
     */
    fun <T> put(key: String, value: T, ttlMinutes: Long = 60) {
        val entry = CacheEntry(
            value = value as Any,
            createdAt = LocalDateTime.now(),
            ttlMinutes = ttlMinutes
        )
        
        cache[key] = entry
        updateStats()
    }
    
    /**
     * Retrieves a value from the cache
     */
    fun <T> get(key: String): T? {
        val entry = cache[key] ?: return null
        
        if (isExpired(entry)) {
            cache.remove(key)
            updateStats()
            return null
        }
        
        return entry.value as? T
    }
    
    /**
     * Retrieves a value or computes it if not in cache
     */
    suspend fun <T> getOrCompute(
        key: String,
        compute: suspend () -> T,
        ttlMinutes: Long = 60
    ): T {
        val cached = get<T>(key)
        if (cached != null) {
            return cached
        }
        
        val computed = compute()
        put(key, computed, ttlMinutes)
        return computed
    }
    
    /**
     * Checks if a key exists in cache
     */
    fun contains(key: String): Boolean {
        val entry = cache[key] ?: return false
        
        if (isExpired(entry)) {
            cache.remove(key)
            updateStats()
            return false
        }
        
        return true
    }
    
    /**
     * Removes a key from cache
     */
    fun remove(key: String) {
        cache.remove(key)
        updateStats()
    }
    
    /**
     * Clears all cache entries
     */
    fun clear() {
        cache.clear()
        updateStats()
    }
    
    /**
     * Clears expired entries
     */
    fun clearExpired() {
        val now = LocalDateTime.now()
        val expiredKeys = cache.entries.filter { (_, entry) ->
            isExpired(entry)
        }.map { it.key }
        
        expiredKeys.forEach { key ->
            cache.remove(key)
        }
        
        updateStats()
    }
    
    /**
     * Gets cache size
     */
    fun size(): Int = cache.size
    
    /**
     * Checks if an entry is expired
     */
    private fun isExpired(entry: CacheEntry<Any>): Boolean {
        val now = LocalDateTime.now()
        val expirationTime = entry.createdAt.plus(entry.ttlMinutes, ChronoUnit.MINUTES)
        return now.isAfter(expirationTime)
    }
    
    /**
     * Updates cache statistics
     */
    private fun updateStats() {
        _cacheStats.value = CacheStats(
            totalEntries = cache.size,
            expiredEntries = cache.values.count { isExpired(it) },
            memoryUsage = estimateMemoryUsage()
        )
    }
    
    /**
     * Estimates memory usage of cache
     */
    private fun estimateMemoryUsage(): Long {
        return cache.entries.sumOf { (key, entry) ->
            key.length * 2L + // String overhead
            entry.value.toString().length * 2L + // Value size estimate
            100L // Entry overhead
        }
    }
}

/**
 * Cache entry data class
 */
data class CacheEntry<T>(
    val value: T,
    val createdAt: LocalDateTime,
    val ttlMinutes: Long
)

/**
 * Cache statistics
 */
data class CacheStats(
    val totalEntries: Int = 0,
    val expiredEntries: Int = 0,
    val memoryUsage: Long = 0
)

/**
 * Specialized cache for API responses
 */
class PluctApiCache {
    
    private val cacheManager = PluctCacheManager.getInstance()
    private val logger = PluctLogger.getInstance()
    
    /**
     * Caches API response
     */
    fun <T> cacheApiResponse(
        endpoint: String,
        params: Map<String, String> = emptyMap(),
        response: T,
        ttlMinutes: Long = 30
    ) {
        val key = generateCacheKey(endpoint, params)
        cacheManager.put(key, response, ttlMinutes)
        logger.debug("API response cached for $endpoint", "API_CACHE")
    }
    
    /**
     * Retrieves cached API response
     */
    fun <T> getCachedApiResponse(
        endpoint: String,
        params: Map<String, String> = emptyMap()
    ): T? {
        val key = generateCacheKey(endpoint, params)
        return cacheManager.get<T>(key)
    }
    
    /**
     * Generates cache key for API endpoint
     */
    private fun generateCacheKey(endpoint: String, params: Map<String, String>): String {
        val paramString = params.entries.sortedBy { it.key }
            .joinToString("&") { "${it.key}=${it.value}" }
        return "api:$endpoint:$paramString"
    }
}

/**
 * Specialized cache for transcription data
 */
class PluctTranscriptionCache {
    
    private val cacheManager = PluctCacheManager.getInstance()
    private val logger = PluctLogger.getInstance()
    
    /**
     * Caches transcription result
     */
    fun cacheTranscription(
        videoId: String,
        transcription: String,
        metadata: Map<String, Any> = emptyMap(),
        ttlMinutes: Long = 1440 // 24 hours
    ) {
        val key = "transcription:$videoId"
        val data = TranscriptionCacheData(
            transcription = transcription,
            metadata = metadata,
            cachedAt = LocalDateTime.now()
        )
        
        cacheManager.put(key, data, ttlMinutes)
        logger.debug("Transcription cached for video $videoId", "TRANSCRIPTION_CACHE")
    }
    
    /**
     * Retrieves cached transcription
     */
    fun getCachedTranscription(videoId: String): TranscriptionCacheData? {
        val key = "transcription:$videoId"
        return cacheManager.get<TranscriptionCacheData>(key)
    }
    
    /**
     * Caches video metadata
     */
    fun cacheVideoMetadata(
        videoId: String,
        metadata: Map<String, Any>,
        ttlMinutes: Long = 1440 // 24 hours
    ) {
        val key = "metadata:$videoId"
        cacheManager.put(key, metadata, ttlMinutes)
        logger.debug("Video metadata cached for $videoId", "METADATA_CACHE")
    }
    
    /**
     * Retrieves cached video metadata
     */
    fun getCachedVideoMetadata(videoId: String): Map<String, Any>? {
        val key = "metadata:$videoId"
        return cacheManager.get<Map<String, Any>>(key)
    }
}

/**
 * Transcription cache data
 */
data class TranscriptionCacheData(
    val transcription: String,
    val metadata: Map<String, Any>,
    val cachedAt: LocalDateTime
)

/**
 * Performance optimization utilities
 */
class PluctPerformanceOptimizer {
    
    private val cacheManager = PluctCacheManager.getInstance()
    private val logger = PluctLogger.getInstance()
    
    /**
     * Optimizes image loading with caching
     */
    suspend fun <T> optimizeImageLoading(
        imageUrl: String,
        loader: suspend (String) -> T,
        ttlMinutes: Long = 60
    ): T {
        val cacheKey = "image:$imageUrl"
        
        return cacheManager.getOrCompute(cacheKey, {
            logger.performance("Image loading", System.currentTimeMillis())
            loader(imageUrl)
        }, ttlMinutes)
    }
    
    /**
     * Optimizes API calls with caching
     */
    suspend fun <T> optimizeApiCall(
        endpoint: String,
        params: Map<String, String> = emptyMap(),
        apiCall: suspend () -> T,
        ttlMinutes: Long = 30
    ): T {
        val cacheKey = "api:$endpoint:${params.hashCode()}"
        
        return cacheManager.getOrCompute(cacheKey, {
            logger.performance("API call to $endpoint", System.currentTimeMillis())
            apiCall()
        }, ttlMinutes)
    }
    
    /**
     * Implements lazy loading for large datasets
     */
    fun <T> createLazyLoader(
        loader: suspend (Int, Int) -> List<T>,
        pageSize: Int = 20
    ): LazyDataLoader<T> {
        return LazyDataLoader(loader, pageSize)
    }
}

/**
 * Lazy data loader for large datasets
 */
class LazyDataLoader<T>(
    private val loader: suspend (Int, Int) -> List<T>,
    private val pageSize: Int
) {
    
    private val cache = mutableMapOf<Int, List<T>>()
    private val cacheManager = PluctCacheManager.getInstance()
    
    /**
     * Loads a page of data
     */
    suspend fun loadPage(page: Int): List<T> {
        val cached = cache[page]
        if (cached != null) {
            return cached
        }
        
        val offset = page * pageSize
        val data = loader(offset, pageSize)
        cache[page] = data
        
        return data
    }
    
    /**
     * Clears page cache
     */
    fun clearPageCache() {
        cache.clear()
    }
}
