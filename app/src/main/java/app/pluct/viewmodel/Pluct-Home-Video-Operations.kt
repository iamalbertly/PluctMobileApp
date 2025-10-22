package app.pluct.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctRepository
import app.pluct.services.PluctStatusMonitor
import app.pluct.error.PluctErrorHandler
import app.pluct.services.PluctAPIRetry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

/**
 * Pluct-Home-Video-Operations - Video operations for HomeViewModel
 * Single source of truth for video operations
 * Adheres to 300-line limit with smart separation of concerns
 */

class PluctHomeVideoOperations @Inject constructor(
    private val repository: PluctRepository,
    private val statusMonitor: PluctStatusMonitor,
    private val errorHandler: PluctErrorHandler,
    private val apiRetry: PluctAPIRetry
) : ViewModel() {
    
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()
    
    fun loadVideos() {
        viewModelScope.launch {
            try {
                repository.streamAll().collect { videoList ->
                    _videos.value = videoList
                }
            } catch (e: Exception) {
                Log.e("PluctHomeVideoOperations", "❌ Failed to load videos", e)
            }
        }
    }
    
    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            try {
                repository.deleteVideo(videoId)
                Log.d("PluctHomeVideoOperations", "✅ Video deleted: $videoId")
            } catch (e: Exception) {
                Log.e("PluctHomeVideoOperations", "❌ Failed to delete video", e)
            }
        }
    }
    
    fun updateVideoStatus(videoId: String, status: ProcessingStatus) {
        viewModelScope.launch {
            try {
                repository.updateVideoStatus(videoId, status)
                Log.d("PluctHomeVideoOperations", "✅ Video status updated: $videoId -> $status")
            } catch (e: Exception) {
                Log.e("PluctHomeVideoOperations", "❌ Failed to update video status", e)
            }
        }
    }
    
    fun getVideoById(videoId: String): VideoItem? {
        return _videos.value.find { it.id == videoId }
    }
    
    fun getVideosByStatus(status: ProcessingStatus): List<VideoItem> {
        return _videos.value.filter { it.status == status }
    }
    
    fun getVideosByTier(tier: ProcessingTier): List<VideoItem> {
        return _videos.value.filter { it.processingTier == tier }
    }
}
