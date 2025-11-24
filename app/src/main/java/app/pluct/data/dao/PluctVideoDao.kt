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
}
