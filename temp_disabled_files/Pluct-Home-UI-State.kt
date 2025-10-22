package app.pluct.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctRepository
import app.pluct.orchestrator.OrchestratorResult
import app.pluct.services.PluctStatusMonitor
import app.pluct.error.PluctErrorHandler
import app.pluct.services.PluctAPIRetry
import app.pluct.ui.error.ErrorCenter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

/**
 * Pluct-Home-UI-State - UI state management for HomeViewModel
 * Single source of truth for UI state
 * Adheres to 300-line limit with smart separation of concerns
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

class PluctHomeUIState @Inject constructor(
    private val repository: PluctRepository,
    private val statusMonitor: PluctStatusMonitor,
    private val errorHandler: PluctErrorHandler,
    private val apiRetry: PluctAPIRetry,
    private val errorCenter: ErrorCenter
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()
    
    fun updateUiState(update: HomeUiState.() -> HomeUiState) {
        _uiState.value = _uiState.value.update()
    }
    
    fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
    }
    
    fun setProcessing(isProcessing: Boolean) {
        _uiState.value = _uiState.value.copy(isProcessing = isProcessing)
    }
    
    fun setError(error: String?) {
        _uiState.value = _uiState.value.copy(error = error)
        // Trigger error notification
        error?.let { errorMessage ->
            when {
                errorMessage.contains("network", ignoreCase = true) || 
                errorMessage.contains("connection", ignoreCase = true) -> {
                    // // errorNotificationManager.showNetworkError()
                }
                errorMessage.contains("timeout", ignoreCase = true) -> {
                    // errorNotificationManager.showTimeoutError()
                }
                errorMessage.contains("invalid", ignoreCase = true) || 
                errorMessage.contains("format", ignoreCase = true) -> {
                    // errorNotificationManager.showValidationError(errorMessage)
                }
                else -> {
                    // errorNotificationManager.showError(errorMessage, "GENERAL")
                }
            }
        }
    }
    
    fun setProcessError(error: OrchestratorResult.Failure?) {
        _uiState.value = _uiState.value.copy(processError = error)
        // Trigger API error notification
        error?.let { processError ->
            // errorNotificationManager.showApiError(processError.reason)
        }
    }
    
    fun setCreditBalanceError(error: String?) {
        _uiState.value = _uiState.value.copy(creditBalanceError = error)
        // Trigger API error notification for credit balance errors
        error?.let { creditError ->
            // errorNotificationManager.showApiError(creditError)
        }
    }
    
    fun setCurrentStage(stage: String) {
        _uiState.value = _uiState.value.copy(currentStage = stage)
    }
    
    fun setProgress(progress: Float) {
        _uiState.value = _uiState.value.copy(progress = progress)
    }
    
    fun setVideoUrl(url: String) {
        _uiState.value = _uiState.value.copy(videoUrl = url)
    }
    
    fun setLastProcessedUrl(url: String?) {
        _uiState.value = _uiState.value.copy(lastProcessedUrl = url)
    }
    
    fun loadVideos() {
        viewModelScope.launch {
            try {
                repository.streamAll().collect { videoList ->
                    _videos.value = videoList
                    _uiState.value = _uiState.value.copy(videos = videoList)
                }
            } catch (e: Exception) {
                Log.e("PluctHomeUIState", "❌ Failed to load videos", e)
                setError("Failed to load videos: ${e.message}")
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            error = null,
            processError = null,
            creditBalanceError = null
        )
    }
    
    fun resetState() {
        _uiState.value = HomeUiState()
    }
    
    fun setCaptureRequest(request: CaptureRequest?) {
        _uiState.value = _uiState.value.copy(captureRequest = request)
    }
    
    fun clearCaptureRequest() {
        _uiState.value = _uiState.value.copy(captureRequest = null)
    }
    
    fun updateCaptureRequestUrl(url: String) {
        val currentRequest = _uiState.value.captureRequest
        if (currentRequest != null) {
            _uiState.value = _uiState.value.copy(
                captureRequest = currentRequest.copy(url = url)
            )
        }
    }
    
    fun updateVideoUrl(url: String) {
        _uiState.value = _uiState.value.copy(videoUrl = url)
    }
}
