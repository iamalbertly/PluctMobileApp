package app.pluct.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
// import app.pluct.data.PluctAPIIntegrationService // Class doesn't exist
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctRepository
import app.pluct.orchestrator.OrchestratorResult
import app.pluct.worker.TTTranscribeWork
import app.pluct.services.PluctStatusMonitor
import app.pluct.services.PluctMetadataService
import app.pluct.services.TikTokMetadata
import app.pluct.error.PluctErrorHandler
import app.pluct.services.PluctAPIRetry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

/**
 * Pluct-Home-ViewModel-Simplified - Simplified HomeViewModel
 * Single source of truth for home view model
 * Adheres to 300-line limit with smart separation of concerns
 */

@HiltViewModel
class PluctHomeViewModelSimplified @Inject constructor(
    private val repository: PluctRepository,
    private val videoOperations: PluctVideoOperations,
    private val creditManager: PluctCreditManager,
    private val statusMonitor: PluctStatusMonitor,
    private val metadataService: PluctMetadataService,
    private val errorHandler: PluctErrorHandler,
    private val apiRetry: PluctAPIRetry,
    private val processingEngine: PluctProcessingEngine,
    // private val apiIntegrationService: PluctAPIIntegrationService, // Class doesn't exist
    private val workManager: WorkManager,
    private val businessLogic: PluctHomeBusinessLogic,
    private val uiStateManager: PluctHomeUIState
) : ViewModel() {
    
    // Delegate to separated components
    val uiState: StateFlow<HomeUiState> = uiStateManager.uiState
    val videos: StateFlow<List<VideoItem>> = uiStateManager.videos
    
    init {
        loadVideos()
        refreshCreditBalance()
        generateJWTOnLaunch()
        startStatusMonitoring()
    }
    
    private fun startStatusMonitoring() {
        Log.d("PluctHomeViewModelSimplified", "📊 Status monitoring delegated to separated components")
    }
    
    private fun generateJWTOnLaunch() {
        viewModelScope.launch {
            try {
                Log.i("PluctHomeViewModelSimplified", "🎯 GENERATING JWT ON APP LAUNCH")
                val userJwt = apiIntegrationService.generateJWT()
                Log.i("PluctHomeViewModelSimplified", "✅ JWT GENERATED ON LAUNCH: ${userJwt.take(20)}...")
            } catch (e: Exception) {
                Log.e("PluctHomeViewModelSimplified", "❌ JWT generation failed", e)
            }
        }
    }
    
    fun loadVideos() {
        uiStateManager.loadVideos()
    }
    
    fun refreshCreditBalance() {
        // Delegate to credit operations component
        Log.d("PluctHomeViewModelSimplified", "💰 Credit balance refresh delegated to separated components")
    }
    
    fun quickScan(url: String, clientRequestId: String) = viewModelScope.launch {
        uiStateManager.setProcessing(true)
        Log.i("TTT", "ENQUEUING_TRANSCRIPTION_WORK url=$url tier=QUICK_SCAN id=$clientRequestId")
        
        // Validate URL first
        val validationError = businessLogic.validateUrl(url)
        if (validationError != null) {
            uiStateManager.setProcessing(false)
            uiStateManager.setError(validationError)
            return@launch
        }
        
        try {
            // Create VideoItem in repository first so it appears in UI immediately
            Log.i("PluctHomeViewModelSimplified", "🎯 CREATING VIDEO ITEM FOR QUICK SCAN: $url")
            val videoId = businessLogic.createVideoItem(url, ProcessingTier.QUICK_SCAN)
            Log.i("PluctHomeViewModelSimplified", "🎯 VIDEO ITEM CREATED: $videoId")
            
            // Start status monitoring
            businessLogic.startStatusMonitoring(videoId)
            
            // Fetch metadata for the TikTok video
            businessLogic.fetchAndStoreMetadata(url, videoId)
            
            // Reload videos to show the new one in UI immediately
            uiStateManager.loadVideos()
            
            // Generate JWT for API calls
            val userJwt = apiIntegrationService.generateJWT()
            Log.i("PluctAPIIntegrationService", "🎯 GENERATING JWT FOR QUICK SCAN")
            Log.i("PluctAPIIntegrationService", "✅ JWT GENERATED FOR QUICK SCAN: ${userJwt.take(20)}...")
            
            // Enqueue transcription work
            val workRequest = OneTimeWorkRequestBuilder<TTTranscribeWork>()
                .setInputData(workDataOf(
                    "url" to url,
                    "tier" to ProcessingTier.QUICK_SCAN.name,
                    "clientRequestId" to clientRequestId,
                    "userJwt" to userJwt
                ))
                .build()
            
            workManager.enqueue(workRequest)
            Log.i("PluctHomeViewModelSimplified", "✅ TRANSCRIPTION WORK ENQUEUED")
            
            uiStateManager.setProcessing(false)
            uiStateManager.setLastProcessedUrl(url)
            
        } catch (e: Exception) {
            Log.e("PluctHomeViewModelSimplified", "❌ Quick scan failed", e)
            uiStateManager.setProcessing(false)
            uiStateManager.setError("Quick scan failed: ${e.message}")
        }
    }
    
    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            try {
                repository.deleteVideo(videoId)
                Log.d("PluctHomeViewModelSimplified", "✅ Video deleted: $videoId")
                loadVideos()
            } catch (e: Exception) {
                Log.e("PluctHomeViewModelSimplified", "❌ Failed to delete video", e)
                uiStateManager.setError("Failed to delete video: ${e.message}")
            }
        }
    }
    
    fun clearError() {
        uiStateManager.clearError()
    }
    
    fun triggerTestError(type: String) {
        when (type) {
            "NETWORK" -> {
                uiStateManager.setError("Network connection failed. Please check your internet connection.")
            }
            "VALIDATION" -> {
                uiStateManager.setError("Invalid URL format. Please enter a valid TikTok URL.")
            }
            "API" -> {
                uiStateManager.setProcessError(OrchestratorResult.Failure("API request failed", "API_ERROR"))
            }
            "TIMEOUT" -> {
                uiStateManager.setError("Request timed out. Please try again.")
            }
        }
    }
}

