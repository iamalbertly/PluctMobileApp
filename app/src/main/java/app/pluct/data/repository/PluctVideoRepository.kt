package app.pluct.data.repository

import android.util.Log
import app.pluct.data.dao.PluctVideoDao
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Data-Repository-01VideoRepository - Video data repository
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Provides abstraction layer over database operations with error handling
 */
@Singleton
class PluctVideoRepository @Inject constructor(
    private val videoDao: PluctVideoDao
) {
    companion object {
        private const val TAG = "PluctVideoRepository"
    }
    
    /**
     * Get all videos as Flow for reactive updates
     */
    fun getAllVideos(): Flow<List<VideoItem>> {
        return videoDao.getAllVideos()
            .catch { error ->
                Log.e(TAG, "Error getting all videos: ${error.message}", error)
                emit(emptyList())
            }
    }
    
    /**
     * Get videos by status
     */
    fun getVideosByStatus(status: ProcessingStatus): Flow<List<VideoItem>> {
        return videoDao.getVideosByStatus(status)
            .catch { error ->
                Log.e(TAG, "Error getting videos by status $status: ${error.message}", error)
                emit(emptyList())
            }
    }
    
    /**
     * Get failed videos with error details
     */
    fun getFailedVideosWithErrors(): Flow<List<VideoItem>> {
        return videoDao.getFailedVideosWithErrors()
            .catch { error ->
                Log.e(TAG, "Error getting failed videos: ${error.message}", error)
                emit(emptyList())
            }
    }
    
    /**
     * Get a single video by ID
     */
    suspend fun getVideoById(id: String): VideoItem? {
        return try {
            videoDao.getVideoById(id)
        } catch (error: Exception) {
            Log.e(TAG, "Error getting video by ID $id: ${error.message}", error)
            null
        }
    }
    
    /**
     * Get video by URL (for duplicate prevention)
     */
    suspend fun getVideoByUrl(url: String): VideoItem? {
        return try {
            videoDao.getVideoByUrl(url)
        } catch (error: Exception) {
            Log.e(TAG, "Error getting video by URL $url: ${error.message}", error)
            null
        }
    }
    
    /**
     * Get processing video by URL (to prevent duplicates)
     */
    suspend fun getProcessingVideoByUrl(url: String): VideoItem? {
        return try {
            videoDao.getProcessingVideoByUrl(url)
        } catch (error: Exception) {
            Log.e(TAG, "Error getting processing video by URL $url: ${error.message}", error)
            null
        }
    }
    
    /**
     * Insert a new video
     */
    suspend fun insertVideo(video: VideoItem): Result<Unit> {
        return try {
            videoDao.insertVideo(video)
            Log.d(TAG, "Video inserted successfully: ${video.id}")
            Result.success(Unit)
        } catch (error: Exception) {
            Log.e(TAG, "Error inserting video: ${error.message}", error)
            Result.failure(error)
        }
    }
    
    /**
     * Update an existing video
     */
    suspend fun updateVideo(video: VideoItem): Result<Unit> {
        return try {
            videoDao.updateVideo(video)
            Log.d(TAG, "Video updated successfully: ${video.id}")
            Result.success(Unit)
        } catch (error: Exception) {
            Log.e(TAG, "Error updating video: ${error.message}", error)
            Result.failure(error)
        }
    }
    
    /**
     * Delete a video
     */
    suspend fun deleteVideo(video: VideoItem): Result<Unit> {
        return try {
            videoDao.deleteVideo(video)
            Log.d(TAG, "Video deleted successfully: ${video.id}")
            Result.success(Unit)
        } catch (error: Exception) {
            Log.e(TAG, "Error deleting video: ${error.message}", error)
            Result.failure(error)
        }
    }
    
    /**
     * Get video count by status
     */
    suspend fun getVideoCountByStatus(status: ProcessingStatus): Int {
        return try {
            videoDao.getVideoCountByStatus(status)
        } catch (error: Exception) {
            Log.e(TAG, "Error getting video count: ${error.message}", error)
            0
        }
    }
    
    /**
     * Delete all videos (for testing purposes)
     */
    suspend fun deleteAllVideos(): Result<Unit> {
        return try {
            videoDao.deleteAllVideos()
            Log.d(TAG, "All videos deleted successfully")
            Result.success(Unit)
        } catch (error: Exception) {
            Log.e(TAG, "Error deleting all videos: ${error.message}", error)
            Result.failure(error)
        }
    }
    
    /**
     * Get unique URLs with their latest status (for URL history dropdown)
     * Only returns URL history - not full video data
     */
    suspend fun getUniqueUrlsWithLatestStatus(): List<VideoItem> {
        return try {
            Log.d(TAG, "🔍 Fetching unique URLs with latest status...")
            
            // Directly call the DAO method - it's already a suspend function
            val uniqueUrls = videoDao.getUniqueUrlsWithLatestStatus()
            Log.d(TAG, "✅ Found ${uniqueUrls.size} unique URLs in history")
            
            if (uniqueUrls.isEmpty()) {
                Log.d(TAG, "📭 No history found - database may be empty or URLs haven't been processed yet")
            } else {
                uniqueUrls.forEachIndexed { index, video ->
                    Log.d(TAG, "   [${index + 1}] URL: ${video.url}, Status: ${video.status}, Timestamp: ${video.timestamp}")
                }
            }
            
            uniqueUrls
        } catch (error: Exception) {
            Log.e(TAG, "❌ Error getting unique URLs: ${error.message}", error)
            Log.e(TAG, "   Stack trace: ${error.stackTraceToString()}")
            emptyList()
        }
    }
}
