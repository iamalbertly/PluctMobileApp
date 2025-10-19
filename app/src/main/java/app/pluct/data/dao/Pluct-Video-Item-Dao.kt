package app.pluct.data.dao

import androidx.room.*
import app.pluct.data.entity.VideoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoItemDao {
    @Query("SELECT * FROM video_items WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun streamAll(): Flow<List<VideoItem>>

    @Query("SELECT * FROM video_items WHERE sourceUrl = :url LIMIT 1")
    suspend fun findByUrl(url: String): VideoItem?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideo(video: VideoItem): Long

    @Query("SELECT * FROM video_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): VideoItem?
    
    @Query("SELECT * FROM video_items ORDER BY createdAt DESC")
    suspend fun getAll(): List<VideoItem>

    @Update
    suspend fun updateVideo(video: VideoItem)

    @Delete
    suspend fun deleteVideo(video: VideoItem)

    @Query("DELETE FROM video_items WHERE id = :videoId")
    suspend fun deleteVideoById(videoId: String)

    @Transaction
    suspend fun upsertVideo(video: VideoItem): String {
        val existingId = insertVideo(video)
        return if (existingId == -1L) {
            // Insert failed due to conflict, get the existing ID
            findByUrl(video.sourceUrl)?.id ?: video.id
        } else {
            video.id
        }
    }
}

