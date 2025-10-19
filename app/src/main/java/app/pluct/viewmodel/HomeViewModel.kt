package app.pluct.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import app.pluct.data.PluctAPIIntegrationService
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctRepository
import app.pluct.orchestrator.OrchestratorResult
import app.pluct.worker.TTTranscribeWork
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

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
    private val processingEngine: PluctProcessingEngine,
    private val apiIntegrationService: PluctAPIIntegrationService,
    private val workManager: WorkManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()
    
    init {
        loadVideos()
        refreshCreditBalance()
        // Generate JWT on app launch for testing
        generateJWTOnLaunch()
    }
    
    private fun generateJWTOnLaunch() {
        viewModelScope.launch {
            try {
                Log.i("HomeViewModel", "🎯 GENERATING JWT ON APP LAUNCH")
                val jwt = apiIntegrationService.generateJWT()
                Log.i("HomeViewModel", "✅ JWT GENERATED ON LAUNCH: ${jwt.take(20)}...")
                Log.i("JWT_GENERATION", "🎯 JWT GENERATION COMPLETED: ${jwt.take(20)}...")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ JWT GENERATION FAILED: ${e.message}", e)
            }
        }
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
                // Generate JWT for API call
                val userJwt = apiIntegrationService.generateJWT()
                Log.i("PluctAPIIntegrationService", "🎯 GENERATING JWT FOR CREDIT BALANCE")
                Log.i("PluctAPIIntegrationService", "✅ JWT GENERATED FOR CREDIT BALANCE: ${userJwt.take(20)}...")
                
                // Get real credit balance from API
                Log.i("PluctAPIIntegrationService", "🎯 CALLING CREDIT BALANCE API")
                val balanceResult = apiIntegrationService.getCreditBalance(userJwt)
                
                if (balanceResult.success && balanceResult.data != null) {
                    val balance = balanceResult.data.balance
                    _uiState.value = _uiState.value.copy(
                        creditBalance = balance,
                        isCreditBalanceLoading = false,
                        creditBalanceError = null
                    )
                    Log.i("HomeViewModel", "🎯 REAL CREDIT BALANCE LOADED: $balance")
                    Log.i("CREDITS_BALANCE_UPDATED", "🎯 CREDITS_BALANCE_UPDATED: $balance")
                } else {
                    // Fallback to local credit manager
                    val balance = creditManager.loadCreditBalance()
                    _uiState.value = _uiState.value.copy(
                        creditBalance = balance,
                        isCreditBalanceLoading = false,
                        creditBalanceError = null
                    )
                    Log.w("HomeViewModel", "⚠️ API balance failed, using local: $balance")
                }
            } catch (e: Throwable) {
                Log.e("HomeViewModel", "❌ ERROR LOADING CREDIT BALANCE: ${e.message}", e)
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
    
    fun updateCaptureRequestUrl(url: String) {
        val currentRequest = _uiState.value.captureRequest
        if (currentRequest != null) {
            android.util.Log.i("HomeViewModel", "🎯 UPDATING CAPTURE REQUEST URL: $url")
            _uiState.value = _uiState.value.copy(
                captureRequest = currentRequest.copy(url = url)
            )
            android.util.Log.i("HomeViewModel", "🎯 CAPTURE REQUEST URL UPDATED SUCCESSFULLY")
        }
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
    
    fun quickScan(url: String, clientRequestId: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isProcessing = true) // guarantees a UI delta
        Log.i("TTT", "ENQUEUING_TRANSCRIPTION_WORK url=$url tier=QUICK_SCAN id=$clientRequestId")
        
        try {
            // Generate JWT for API calls
            val userJwt = apiIntegrationService.generateJWT()
            Log.i("PluctAPIIntegrationService", "🎯 GENERATING JWT FOR QUICK SCAN")
            Log.i("PluctAPIIntegrationService", "✅ JWT GENERATED FOR QUICK SCAN: ${userJwt.take(20)}...")
            
            // Step 1: Vend token from Business Engine
            Log.i("PluctAPIIntegrationService", "🎯 STEP 1: VENDING TOKEN FROM BUSINESS ENGINE")
            val vendResult = apiIntegrationService.vendToken(userJwt, clientRequestId)
            
            if (vendResult.success && vendResult.data != null) {
                val token = vendResult.data.token
                Log.i("PluctAPIIntegrationService", "✅ TOKEN VENDED SUCCESSFULLY: ${token.take(20)}...")
                Log.i("PluctAPIIntegrationService", "✅ BALANCE AFTER: ${vendResult.data.balanceAfter}")
                
                // Step 2: Start transcription with TTTranscribe
                Log.i("PluctAPIIntegrationService", "🎯 STEP 2: STARTING TTTRANSCRIBE TRANSCRIPTION")
                val transcriptionResult = apiIntegrationService.startTranscription(token, url)
                
                if (transcriptionResult.success && transcriptionResult.data != null) {
                    Log.i("PluctAPIIntegrationService", "✅ TTTRANSCRIBE STARTED SUCCESSFULLY")
                    Log.i("PluctAPIIntegrationService", "✅ JOB ID: ${transcriptionResult.data.jobId}")
                    Log.i("PluctAPIIntegrationService", "✅ STATUS: ${transcriptionResult.data.status}")
                } else {
                    Log.e("PluctAPIIntegrationService", "❌ TTTRANSCRIBE FAILED: ${transcriptionResult.error}")
                }
            } else {
                Log.e("PluctAPIIntegrationService", "❌ TOKEN VENDING FAILED: ${vendResult.error}")
            }
            
            // Enqueue work for background processing
            workManager.enqueue(
                OneTimeWorkRequestBuilder<TTTranscribeWork>()
                    .setInputData(workDataOf(
                        "url" to url,
                        "tier" to "QUICK_SCAN",
                        "clientRequestId" to clientRequestId,
                        "userJwt" to userJwt
                    ))
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .addTag("QUICK_SCAN")
                    .build()
            )
            
        } catch (e: Exception) {
            Log.e("HomeViewModel", "❌ QUICK SCAN API CALLS FAILED: ${e.message}", e)
        }
    }
}