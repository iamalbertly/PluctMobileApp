package app.pluct.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctRepository
import app.pluct.data.service.VideoMetadataService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val videos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastProcessedUrl: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PluctRepository,
    private val metadataService: VideoMetadataService
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
}

