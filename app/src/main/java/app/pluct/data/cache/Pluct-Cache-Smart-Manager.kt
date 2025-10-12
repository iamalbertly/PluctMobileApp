package app.pluct.data.cache

import android.content.Context
import android.util.Log
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.Transcript
import app.pluct.data.entity.OutputArtifact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart cache manager with offline capabilities
 * Provides intelligent caching, offline access, and data synchronization
 */
@Singleton
class SmartCacheManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "SmartCacheManager"
        private const val CACHE_DIR = "pluct_cache"
        private const val MAX_CACHE_SIZE = 100 * 1024 * 1024 // 100MB
        private const val CACHE_EXPIRY_HOURS = 24
    }

    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()
    private val cacheDir = File(context.cacheDir, CACHE_DIR)

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    /**
     * Cache video data with intelligent prioritization
     */
    suspend fun cacheVideoData(
        videoId: String,
        video: VideoItem,
        transcript: Transcript? = null,
        artifacts: List<OutputArtifact> = emptyList(),
        priority: CachePriority = CachePriority.NORMAL
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Caching video data for ID: $videoId with priority: $priority")
            
            val cacheEntry = CacheEntry(
                videoId = videoId,
                video = video,
                transcript = transcript,
                artifacts = artifacts,
                timestamp = System.currentTimeMillis(),
                priority = priority,
                accessCount = 0
            )
            
            // Store in memory cache
            memoryCache[videoId] = cacheEntry
            
            // Persist to disk for offline access
            persistToDisk(cacheEntry)
            
            // Manage cache size
            manageCacheSize()
            
            Log.d(TAG, "Successfully cached video data for ID: $videoId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching video data: ${e.message}", e)
        }
    }

    /**
     * Retrieve cached video data
     */
    suspend fun getCachedVideoData(videoId: String): CachedVideoData? = withContext(Dispatchers.IO) {
        try {
            // First check memory cache
            val memoryEntry = memoryCache[videoId]
            if (memoryEntry != null && !isExpired(memoryEntry)) {
                memoryEntry.accessCount++
                Log.d(TAG, "Retrieved from memory cache: $videoId")
                return@withContext CachedVideoData(
                    video = memoryEntry.video,
                    transcript = memoryEntry.transcript,
                    artifacts = memoryEntry.artifacts,
                    isOffline = true
                )
            }
            
            // Check disk cache
            val diskEntry = loadFromDisk(videoId)
            if (diskEntry != null && !isExpired(diskEntry)) {
                // Restore to memory cache
                memoryCache[videoId] = diskEntry
                diskEntry.accessCount++
                Log.d(TAG, "Retrieved from disk cache: $videoId")
                return@withContext CachedVideoData(
                    video = diskEntry.video,
                    transcript = diskEntry.transcript,
                    artifacts = diskEntry.artifacts,
                    isOffline = true
                )
            }
            
            Log.d(TAG, "No cached data found for: $videoId")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving cached data: ${e.message}", e)
            null
        }
    }

    /**
     * Get offline-available videos
     */
    suspend fun getOfflineVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        try {
            val offlineVideos = mutableListOf<VideoItem>()
            
            // Check memory cache
            memoryCache.values.forEach { entry ->
                if (!isExpired(entry)) {
                    offlineVideos.add(entry.video)
                }
            }
            
            // Check disk cache
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".cache")) {
                    try {
                        val entry = loadFromDisk(file.nameWithoutExtension)
                        if (entry != null && !isExpired(entry)) {
                            offlineVideos.add(entry.video)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error loading cache file: ${file.name}")
                    }
                }
            }
            
            Log.d(TAG, "Found ${offlineVideos.size} offline videos")
            offlineVideos
        } catch (e: Exception) {
            Log.e(TAG, "Error getting offline videos: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Clear expired cache entries
     */
    suspend fun clearExpiredCache() = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val expiredKeys = mutableListOf<String>()
            
            // Clear expired memory cache
            memoryCache.entries.forEach { (key, entry) ->
                if (isExpired(entry)) {
                    expiredKeys.add(key)
                }
            }
            
            expiredKeys.forEach { key ->
                memoryCache.remove(key)
            }
            
            // Clear expired disk cache
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".cache")) {
                    try {
                        val entry = loadFromDisk(file.nameWithoutExtension)
                        if (entry != null && isExpired(entry)) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error checking cache file: ${file.name}")
                    }
                }
            }
            
            Log.d(TAG, "Cleared ${expiredKeys.size} expired cache entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing expired cache: ${e.message}", e)
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStatistics(): CacheStatistics {
        val memorySize = memoryCache.size
        val diskSize = cacheDir.listFiles()?.size ?: 0
        val totalSize = getCacheSize()
        
        return CacheStatistics(
            memoryEntries = memorySize,
            diskEntries = diskSize,
            totalSizeBytes = totalSize,
            maxSizeBytes = MAX_CACHE_SIZE.toLong(),
            utilizationPercentage = ((totalSize.toFloat() / MAX_CACHE_SIZE) * 100).toInt()
        )
    }

    private fun persistToDisk(entry: CacheEntry) {
        try {
            val file = File(cacheDir, "${entry.videoId}.cache")
            FileOutputStream(file).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(entry)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting to disk: ${e.message}", e)
        }
    }

    private fun loadFromDisk(videoId: String): CacheEntry? {
        return try {
            val file = File(cacheDir, "$videoId.cache")
            if (!file.exists()) return null
            
            FileInputStream(file).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    ois.readObject() as CacheEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from disk: ${e.message}", e)
            null
        }
    }

    private fun isExpired(entry: CacheEntry): Boolean {
        val expiryTime = CACHE_EXPIRY_HOURS * 60 * 60 * 1000L
        return (System.currentTimeMillis() - entry.timestamp) > expiryTime
    }

    private fun manageCacheSize() {
        val currentSize = getCacheSize()
        if (currentSize > MAX_CACHE_SIZE) {
            // Remove least recently used entries
            val sortedEntries = memoryCache.values.sortedBy { it.accessCount }
            val entriesToRemove = (memoryCache.size * 0.2).toInt() // Remove 20%
            
            for (i in 0 until entriesToRemove) {
                if (i < sortedEntries.size) {
                    val entry = sortedEntries[i]
                    memoryCache.remove(entry.videoId)
                    File(cacheDir, "${entry.videoId}.cache").delete()
                }
            }
        }
    }

    private fun getCacheSize(): Long {
        var totalSize = 0L
        cacheDir.listFiles()?.forEach { file ->
            totalSize += file.length()
        }
        return totalSize
    }
}

data class CacheEntry(
    val videoId: String,
    val video: VideoItem,
    val transcript: Transcript?,
    val artifacts: List<OutputArtifact>,
    val timestamp: Long,
    val priority: CachePriority,
    var accessCount: Int
) : java.io.Serializable

data class CachedVideoData(
    val video: VideoItem,
    val transcript: Transcript?,
    val artifacts: List<OutputArtifact>,
    val isOffline: Boolean
)

data class CacheStatistics(
    val memoryEntries: Int,
    val diskEntries: Int,
    val totalSizeBytes: Long,
    val maxSizeBytes: Long,
    val utilizationPercentage: Int
)

enum class CachePriority {
    LOW, NORMAL, HIGH, CRITICAL
}
