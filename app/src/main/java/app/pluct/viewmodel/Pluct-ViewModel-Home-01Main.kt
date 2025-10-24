package app.pluct.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingStatus
import app.pluct.core.error.ErrorEnvelope
import app.pluct.services.PluctTranscriptionService
import app.pluct.services.PluctBusinessEngineService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

/**
 * Pluct-ViewModel-Home-01Main - Consolidated home view model for the Pluct app
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Handles video processing and UI state management with single source of truth
 */
@HiltViewModel
class PluctHomeViewModel @Inject constructor(
    private val transcriptionService: PluctTranscriptionService,
    private val businessEngineService: PluctBusinessEngineService
) : ViewModel() {
    
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _currentError = MutableStateFlow<ErrorEnvelope?>(null)
    val currentError: StateFlow<ErrorEnvelope?> = _currentError.asStateFlow()
    
    init {
        Log.d("PluctHomeViewModel", "🎯 ViewModel initialized")
        viewModelScope.launch {
            transcriptionService.videos.collect {
                _videos.value = it
            }
        }
    }
    
    fun processVideo(url: String) {
        viewModelScope.launch {
            try {
                Log.d("PluctHomeViewModel", "🎯 Processing video: $url")
                _isLoading.value = true
                _currentError.value = null
                
                // Process the TikTok URL using the transcription service
                val result = transcriptionService.processTikTokUrl(url, "manual_input")
                
                result.fold(
                    onSuccess = { videoItem ->
                        Log.d("PluctHomeViewModel", "✅ Video processed successfully: ${videoItem.id}")
                        // The transcriptionService already updates its internal videos flow,
                        // which is collected by this ViewModel. So no need to add here.
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        Log.e("PluctHomeViewModel", "❌ Video processing failed", error)
                        _currentError.value = ErrorEnvelope(
                            code = "PROCESSING_ERROR",
                            message = "Failed to process video: ${error.message}",
                            details = mapOf("url" to url, "error" to error.toString())
                        )
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                Log.e("PluctHomeViewModel", "❌ Unexpected error in processVideo", e)
                _currentError.value = ErrorEnvelope(
                    code = "UNEXPECTED_ERROR",
                    message = "Unexpected error: ${e.message}",
                    details = mapOf("url" to url, "error" to e.toString())
                )
                _isLoading.value = false
            }
        }
    }
    
    fun addVideo(url: String) {
        processVideo(url)
    }
    
    fun dismissError() {
        _currentError.value = null
    }
    
    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            try {
                Log.d("PluctHomeViewModel", "🗑️ Deleting video: $videoId")
                _videos.value = _videos.value.filter { it.id != videoId }
            } catch (e: Exception) {
                Log.e("PluctHomeViewModel", "❌ Failed to delete video", e)
                _currentError.value = ErrorEnvelope(
                    code = "DELETE_ERROR",
                    message = "Failed to delete video: ${e.message}",
                    details = mapOf("videoId" to videoId, "error" to e.toString())
                )
            }
        }
    }
}
