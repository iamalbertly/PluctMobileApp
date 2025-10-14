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
 * Pluct-Cache-Core-Manager - Core cache management functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctCacheCoreManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "PluctCacheCoreManager"
        private const val CACHE_DIR = "pluct_cache"
        private const val MAX_CACHE_SIZE = 100 * 1024 * 1024 // 100MB
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
        artifacts: List<OutputArtifact> = emptyList()
    ) = withContext(Dispatchers.IO) {
        try {
            val cacheEntry = CacheEntry(
                video = video,
                transcript = transcript,
                artifacts = artifacts,
                timestamp = System.currentTimeMillis(),
                accessCount = 0
            )
            
            memoryCache[videoId] = cacheEntry
            saveToDisk(videoId, cacheEntry)
            
            Log.d(TAG, "Cached video data for: $videoId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching video data: ${e.message}")
        }
    }

    /**
     * Retrieve video data from cache
     */
    suspend fun getVideoData(videoId: String): CacheEntry? = withContext(Dispatchers.IO) {
        try {
            // Try memory cache first
            memoryCache[videoId]?.let { entry ->
                entry.accessCount++
                return@withContext entry
            }
            
            // Try disk cache
            loadFromDisk(videoId)?.let { entry ->
                memoryCache[videoId] = entry
                entry.accessCount++
                return@withContext entry
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving video data: ${e.message}")
            null
        }
    }

    /**
     * Clear cache for specific video
     */
    suspend fun clearVideoCache(videoId: String) = withContext(Dispatchers.IO) {
        try {
            memoryCache.remove(videoId)
            val file = File(cacheDir, "$videoId.cache")
            if (file.exists()) {
                file.delete()
            }
            Log.d(TAG, "Cleared cache for video: $videoId")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing video cache: ${e.message}")
        }
    }

    /**
     * Clear all cache
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        try {
            memoryCache.clear()
            cacheDir.listFiles()?.forEach { it.delete() }
            Log.d(TAG, "Cleared all cache")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all cache: ${e.message}")
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        val totalSize = memoryCache.values.sumOf { it.getSize() }
        val entryCount = memoryCache.size.toLong()
        
        return CacheStats(
            totalSize = totalSize,
            entryCount = entryCount,
            maxSize = MAX_CACHE_SIZE.toLong()
        )
    }

    private suspend fun saveToDisk(videoId: String, entry: CacheEntry) {
        try {
            val file = File(cacheDir, "$videoId.cache")
            FileOutputStream(file).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(entry)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to disk: ${e.message}")
        }
    }

    private suspend fun loadFromDisk(videoId: String): CacheEntry? {
        return try {
            val file = File(cacheDir, "$videoId.cache")
            if (!file.exists()) return null
            
            FileInputStream(file).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    ois.readObject() as CacheEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from disk: ${e.message}")
            null
        }
    }
}

data class CacheEntry(
    val video: VideoItem,
    val transcript: Transcript?,
    val artifacts: List<OutputArtifact>,
    val timestamp: Long,
    var accessCount: Int
) {
    fun getSize(): Long {
        return video.toString().length.toLong() + 
               (transcript?.text?.length ?: 0).toLong() + 
               artifacts.sumOf { it.toString().length.toLong() }
    }
}

data class CacheStats(
    val totalSize: Long,
    val entryCount: Long,
    val maxSize: Long
)
