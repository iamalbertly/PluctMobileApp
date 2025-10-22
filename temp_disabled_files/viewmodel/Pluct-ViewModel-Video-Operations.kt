package app.pluct.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pluct-ViewModel-Video-Operations - Video CRUD operations
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
class PluctVideoOperations @Inject constructor(
    private val repository: PluctRepository
) {
    
    /**
     * Delete a video
     */
    suspend fun deleteVideo(videoId: String) {
        try {
            android.util.Log.i("PluctVideoOperations", "🎯 DELETING VIDEO: $videoId")
            repository.deleteVideo(videoId)
            android.util.Log.i("PluctVideoOperations", "🎯 VIDEO DELETED SUCCESSFULLY: $videoId")
        } catch (e: Exception) {
            android.util.Log.e("PluctVideoOperations", "❌ ERROR DELETING VIDEO: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Retry a video processing
     */
    suspend fun retryVideo(videoId: String) {
        try {
            android.util.Log.i("PluctVideoOperations", "🎯 RETRYING VIDEO: $videoId")
            // Reset the video status to pending
            repository.updateVideoStatus(videoId, app.pluct.data.entity.ProcessingStatus.PENDING, null)
            android.util.Log.i("PluctVideoOperations", "🎯 VIDEO RETRY INITIATED: $videoId")
        } catch (e: Exception) {
            android.util.Log.e("PluctVideoOperations", "❌ ERROR RETRYING VIDEO: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Archive a video
     */
    suspend fun archiveVideo(videoId: String) {
        try {
            android.util.Log.i("PluctVideoOperations", "🎯 ARCHIVING VIDEO: $videoId")
            // Get the video first
            val video = repository.getVideoById(videoId)
            if (video != null) {
                // Archive the video
                val updatedVideo = video.copy(isArchived = true)
                repository.updateVideo(updatedVideo)
                android.util.Log.i("PluctVideoOperations", "🎯 VIDEO ARCHIVED SUCCESSFULLY: $videoId")
            } else {
                android.util.Log.w("PluctVideoOperations", "⚠️ VIDEO NOT FOUND FOR ARCHIVING: $videoId")
            }
        } catch (e: Exception) {
            android.util.Log.e("PluctVideoOperations", "❌ ERROR ARCHIVING VIDEO: ${e.message}", e)
            throw e
        }
    }
}
