package app.pluct.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctRepository
import app.pluct.data.service.VideoMetadataService
import app.pluct.data.BusinessEngineClient
import app.pluct.data.EngineError
import app.pluct.data.manager.UserManager
import app.pluct.worker.WorkManagerUtils
import app.pluct.orchestrator.OrchestratorResult
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
    val captureRequest: CaptureRequest? = null,
    val videoUrl: String = "",
    val currentStage: String = "IDLE",
    val progress: Float = 0f,
    val processError: OrchestratorResult.Failure? = null,
    val creditBalance: Int = 0,
    val isCreditBalanceLoading: Boolean = false,
    val creditBalanceError: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PluctRepository,
    private val metadataService: VideoMetadataService,
    private val businessEngineClient: BusinessEngineClient,
    private val userManager: UserManager,
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
        loadCreditBalance()
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
    
    private fun loadCreditBalance() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreditBalanceLoading = true, creditBalanceError = null)
            try {
                // Get user JWT for authentication
                val userJwt = userManager.getOrCreateUserJwt()
                android.util.Log.d("HomeViewModel", "Loading credit balance with JWT: ${userJwt.take(20)}...")
                
                val balance = businessEngineClient.balance(userJwt)
                _uiState.value = _uiState.value.copy(
                    creditBalance = balance.balance,
                    isCreditBalanceLoading = false,
                    creditBalanceError = null
                )
                android.util.Log.d("HomeViewModel", "Credit balance loaded: ${balance.balance}")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to load credit balance: ${e.message}", e)
                // Set a default balance of 0 instead of showing error
                _uiState.value = _uiState.value.copy(
                    creditBalance = 0,
                    isCreditBalanceLoading = false,
                    creditBalanceError = null
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
                android.util.Log.i("HomeViewModel", "Creating video with tier: $processingTier for URL: $url")
                val videoId = repository.createVideoWithTier(url, processingTier)
                android.util.Log.i("HomeViewModel", "Video created successfully with ID: $videoId")
                
                // Enqueue the background worker
                WorkManagerUtils.enqueueTranscriptionWork(context, videoId, processingTier)
                android.util.Log.i("HomeViewModel", "Background work enqueued for video: $videoId")
                
                // Add a delay to allow the user to see the toast message before clearing the capture request
                kotlinx.coroutines.delay(2000) // 2 seconds delay
                clearCaptureRequest()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error creating video: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to create video"
                )
            }
        }
    }
    
    // NEW METHODS for unified flow
    fun updateVideoUrl(url: String) {
        _uiState.value = _uiState.value.copy(videoUrl = url)
    }
    
    fun updateCurrentStage(stage: String) {
        _uiState.value = _uiState.value.copy(currentStage = stage)
    }
    
    fun updateProgress(progress: Float) {
        _uiState.value = _uiState.value.copy(progress = progress)
    }
    
    fun retry() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun setProcessError(error: OrchestratorResult.Failure) {
        _uiState.value = _uiState.value.copy(processError = error)
    }
    
    fun clearProcessError() {
        _uiState.value = _uiState.value.copy(processError = null)
    }
    
    fun reportIssue(message: String, logId: String?) {
        viewModelScope.launch {
            try {
                // TODO: Implement issue reporting API call
                android.util.Log.d("HomeViewModel", "Reporting issue: $message, Log ID: $logId")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to report issue: ${e.message}")
            }
        }
    }
    
    fun openLogs(logId: String?) {
        // TODO: Implement log viewing functionality
        android.util.Log.d("HomeViewModel", "Opening logs for ID: $logId")
    }
    
    fun refreshCreditBalance() {
        loadCreditBalance()
    }
}

