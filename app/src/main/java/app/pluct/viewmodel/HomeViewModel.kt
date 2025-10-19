package app.pluct.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctRepository
import app.pluct.orchestrator.OrchestratorResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HomeViewModel - Main screen view model
 * Refactored to use focused components following naming convention
 */

data class CaptureRequest(
    val url: String,
    val caption: String?
)

data class HomeUiState(
    val videos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val videoUrl: String = "",
    val creditBalance: Int = 0,
    val isCreditBalanceLoading: Boolean = false,
    val creditBalanceError: String? = null,
    val captureRequest: CaptureRequest? = null,
    val lastProcessedUrl: String? = null,
    val currentStage: String = "IDLE",
    val progress: Float = 0f,
    val error: String? = null,
    val processError: OrchestratorResult.Failure? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PluctRepository,
    private val videoOperations: PluctVideoOperations,
    private val creditManager: PluctCreditManager,
    private val processingEngine: PluctProcessingEngine
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()
    
    init {
        loadVideos()
        loadCreditBalance()
    }
    
    private fun loadVideos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val videosList = repository.getAllVideos()
                _videos.value = videosList
                _uiState.value = _uiState.value.copy(
                    videos = videosList,
                    isLoading = false
                )
                android.util.Log.i("HomeViewModel", "🎯 VIDEOS LOADED: ${videosList.size} videos")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ ERROR LOADING VIDEOS: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    private fun loadCreditBalance() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreditBalanceLoading = true, creditBalanceError = null)
            try {
                val balance = creditManager.loadCreditBalance()
                _uiState.value = _uiState.value.copy(
                    creditBalance = balance,
                    isCreditBalanceLoading = false,
                    creditBalanceError = null
                )
                android.util.Log.i("HomeViewModel", "🎯 CREDIT BALANCE LOADED: $balance")
            } catch (e: Throwable) {
                android.util.Log.e("HomeViewModel", "❌ ERROR LOADING CREDIT BALANCE: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isCreditBalanceLoading = false,
                    creditBalanceError = e.message
                )
            }
        }
    }
    
    fun refreshCreditBalance() {
        android.util.Log.i("HomeViewModel", "🎯 REFRESHING CREDIT BALANCE")
        loadCreditBalance()
    }
    
    fun setCaptureRequest(url: String, caption: String?) {
        android.util.Log.i("HomeViewModel", "🎯 SETTING CAPTURE REQUEST: url=$url, caption=$caption")
        _uiState.value = _uiState.value.copy(
            captureRequest = CaptureRequest(url, caption)
        )
        android.util.Log.i("HomeViewModel", "🎯 CAPTURE REQUEST SET SUCCESSFULLY")
    }
    
    fun clearCaptureRequest() {
        android.util.Log.i("HomeViewModel", "🎯 CLEARING CAPTURE REQUEST")
        _uiState.value = _uiState.value.copy(
            captureRequest = null
        )
    }
    
    fun createVideoWithTier(url: String, processingTier: ProcessingTier, context: android.content.Context? = null) {
        viewModelScope.launch {
            try {
                android.util.Log.i("HomeViewModel", "🎯 CREATING VIDEO WITH TIER: $url, tier=$processingTier")
                processingEngine.createVideoWithTier(url, processingTier, context)
                android.util.Log.i("HomeViewModel", "🎯 VIDEO CREATION COMPLETED")
                // Reload videos to show the new one
                loadVideos()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ ERROR CREATING VIDEO: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message
                )
            }
        }
    }
    
    fun updateVideoUrl(url: String) {
        _uiState.value = _uiState.value.copy(videoUrl = url)
    }
    
    fun retryVideo(videoId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.i("HomeViewModel", "🎯 RETRYING VIDEO: $videoId")
                videoOperations.retryVideo(videoId)
                android.util.Log.i("HomeViewModel", "🎯 VIDEO RETRY COMPLETED")
                // Reload videos to show updated status
                loadVideos()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ ERROR RETRYING VIDEO: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message
                )
            }
        }
    }
    
    fun archiveVideo(videoId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.i("HomeViewModel", "🎯 ARCHIVING VIDEO: $videoId")
                videoOperations.archiveVideo(videoId)
                android.util.Log.i("HomeViewModel", "🎯 VIDEO ARCHIVED")
                // Reload videos to show updated status
                loadVideos()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ ERROR ARCHIVING VIDEO: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message
                )
            }
        }
    }
    
    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.i("HomeViewModel", "🎯 DELETING VIDEO: $videoId")
                videoOperations.deleteVideo(videoId)
                android.util.Log.i("HomeViewModel", "🎯 VIDEO DELETED")
                // Reload videos to show updated status
                loadVideos()
                // Refresh credit balance after deletion
                refreshCreditBalance()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ ERROR DELETING VIDEO: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message
                )
            }
        }
    }
    
    fun processVideo(url: String, processingTier: ProcessingTier = ProcessingTier.QUICK_SCAN) {
        viewModelScope.launch {
            try {
                android.util.Log.i("HomeViewModel", "🎬 PROCESSING VIDEO: $url with tier $processingTier")
                _uiState.value = _uiState.value.copy(
                    videoUrl = url,
                    isProcessing = true
                )
                
                // Create video with processing tier
                createVideoWithTier(url, processingTier)
                
                // Clear the URL input after processing
                _uiState.value = _uiState.value.copy(
                    videoUrl = "",
                    isProcessing = false
                )
                
                // Refresh credit balance after processing
                refreshCreditBalance()
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ ERROR PROCESSING VIDEO: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isProcessing = false
                )
            }
        }
    }
}