package app.pluct.data.dao

import androidx.room.*
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingStatus
import kotlinx.coroutines.flow.Flow

/**
 * Pluct-Data-DAO-01VideoDao - Video data access object
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Provides database operations for video items
 */
@Dao
interface PluctVideoDao {
    
    /**
     * Insert a new video item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoItem)
    
    /**
     * Update an existing video item
     */
    @Update
    suspend fun updateVideo(video: VideoItem)
    
    /**
     * Delete a video item
     */
    @Delete
    suspend fun deleteVideo(video: VideoItem)
    
    /**
     * Get all videos ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM videos ORDER BY timestamp DESC")
    fun getAllVideos(): Flow<List<VideoItem>>
    
    /**
     * Get videos by status
     */
    @Query("SELECT * FROM videos WHERE status = :status ORDER BY timestamp DESC")
    fun getVideosByStatus(status: ProcessingStatus): Flow<List<VideoItem>>
    
    /**
     * Get a single video by ID
     */
    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: String): VideoItem?
    
    /**
     * Get video by URL (for duplicate prevention)
     */
    @Query("SELECT * FROM videos WHERE url = :url ORDER BY timestamp DESC LIMIT 1")
    suspend fun getVideoByUrl(url: String): VideoItem?
    
    /**
     * Get processing videos by URL (to prevent duplicates)
     */
    @Query("SELECT * FROM videos WHERE url = :url AND status = 'PROCESSING' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getProcessingVideoByUrl(url: String): VideoItem?
    
    /**
     * Get count of videos by status
     */
    @Query("SELECT COUNT(*) FROM videos WHERE status = :status")
    suspend fun getVideoCountByStatus(status: ProcessingStatus): Int
    
    /**
     * Delete all videos
     */
    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos()
    
    /**
     * Get failed videos with error details
     */
    @Query("SELECT * FROM videos WHERE status = 'FAILED' AND errorDetails IS NOT NULL ORDER BY timestamp DESC")
    fun getFailedVideosWithErrors(): Flow<List<VideoItem>>
    
    /**
     * Get unique URLs with their latest status (for URL history)
     * Returns the most recent video item for each unique URL
     */
    @Query("""
        SELECT v1.* FROM videos v1
        INNER JOIN (
            SELECT url, MAX(timestamp) as max_timestamp
            FROM videos
            GROUP BY url
        ) v2 ON v1.url = v2.url AND v1.timestamp = v2.max_timestamp
        ORDER BY v1.timestamp DESC
        LIMIT 20
    """)
    suspend fun getUniqueUrlsWithLatestStatus(): List<VideoItem>
}
