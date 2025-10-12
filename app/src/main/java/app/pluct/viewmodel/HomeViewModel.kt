package app.pluct.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctRepository
import app.pluct.data.service.VideoMetadataService
import app.pluct.worker.WorkManagerUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CaptureRequest(
    val url: String,
    val caption: String?
)

data class HomeUiState(
    val videos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastProcessedUrl: String? = null,
    val captureRequest: CaptureRequest? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PluctRepository,
    private val metadataService: VideoMetadataService,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    // Use StateFlow with proper sharing strategy
    val videos: StateFlow<List<VideoItem>> = repository.streamAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        loadVideos()
    }
    
    private fun loadVideos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // The videos StateFlow will automatically update
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    fun setLastProcessedUrl(url: String) {
        _uiState.value = _uiState.value.copy(lastProcessedUrl = url)
    }
    
    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            try {
                repository.deleteVideo(videoId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete video"
                )
            }
        }
    }
    
    // NEW METHODS for Choice Engine
    fun setCaptureRequest(url: String, caption: String?) {
        android.util.Log.i("HomeViewModel", "Setting capture request: url=$url, caption=$caption")
        _uiState.value = _uiState.value.copy(
            captureRequest = CaptureRequest(url, caption)
        )
        android.util.Log.d("HomeViewModel", "Capture request set in UI state")
    }
    
    fun clearCaptureRequest() {
        _uiState.value = _uiState.value.copy(
            captureRequest = null
        )
    }
    
    fun createVideoWithTier(url: String, processingTier: ProcessingTier) {
        viewModelScope.launch {
            try {
                val videoId = repository.createVideoWithTier(url, processingTier)
                // Enqueue the background worker
                WorkManagerUtils.enqueueTranscriptionWork(context, videoId, processingTier)
                
                // Add a delay to allow the user to see the toast message before clearing the capture request
                kotlinx.coroutines.delay(2000) // 2 seconds delay
                clearCaptureRequest()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to create video"
                )
            }
        }
    }
}

