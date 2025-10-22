package app.pluct.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.core.error.ErrorCenter
import app.pluct.core.error.ErrorEnvelope
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pluct-ViewModel-Home-01State - Home screen state management
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@HiltViewModel
class PluctHomeViewModel @Inject constructor(
    private val errorCenter: ErrorCenter
) : ViewModel() {
    
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _currentError = MutableStateFlow<ErrorEnvelope?>(null)
    val currentError: StateFlow<ErrorEnvelope?> = _currentError.asStateFlow()
    
    init {
        // Observe errors
        viewModelScope.launch {
            errorCenter.errors.collect { error ->
                _currentError.value = error
            }
        }
    }
    
    fun addVideo(url: String) {
        android.util.Log.d("PluctViewModel", "addVideo called with url: $url")
        val videoId = System.currentTimeMillis().toString()
        val newVideo = VideoItem(
            id = videoId,
            url = url,
            title = "TikTok Video",
            thumbnailUrl = "",
            author = "Unknown",
            duration = 0,
            status = ProcessingStatus.QUEUED,
            progress = 0,
            transcript = null,
            timestamp = System.currentTimeMillis()
        )
        
        android.util.Log.d("PluctViewModel", "Adding video: $newVideo")
        _videos.value = _videos.value + newVideo
        android.util.Log.d("PluctViewModel", "Videos count after add: ${_videos.value.size}")
        
        // Start background processing simulation
        startTranscription(videoId, url)
    }
    
    private fun startTranscription(videoId: String, url: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Simulate processing steps
                simulateTranscriptionProcess(videoId)
                
            } catch (e: Exception) {
                errorCenter.emitError(
                    ErrorEnvelope(
                        code = "TRANSCRIPTION_START_FAILED",
                        message = "Failed to start transcription: ${e.message}",
                        details = mapOf("videoId" to videoId)
                    )
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun simulateTranscriptionProcess(videoId: String) {
        // Update video to processing
        updateVideoStatus(videoId, ProcessingStatus.PROCESSING, 25)
        kotlinx.coroutines.delay(2000)
        
        updateVideoStatus(videoId, ProcessingStatus.PROCESSING, 50)
        kotlinx.coroutines.delay(2000)
        
        updateVideoStatus(videoId, ProcessingStatus.PROCESSING, 75)
        kotlinx.coroutines.delay(2000)
        
        updateVideoStatus(videoId, ProcessingStatus.PROCESSING, 90)
        kotlinx.coroutines.delay(1000)
        
        // Complete transcription
        updateVideoStatus(videoId, ProcessingStatus.COMPLETED, 100, "Sample transcript for video $videoId")
    }
    
    private fun updateVideoStatus(videoId: String, status: ProcessingStatus, progress: Int, transcript: String? = null) {
        _videos.value = _videos.value.map { video ->
            if (video.id == videoId) {
                video.copy(
                    status = status,
                    progress = progress,
                    transcript = transcript
                )
            } else {
                video
            }
        }
    }
    
    fun dismissError() {
        _currentError.value = null
    }
}
