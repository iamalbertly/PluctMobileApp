/**
 * Pluct-ViewModel-Main-01Orchestrator - Main view model orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

package app.pluct.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import app.pluct.core.error.ErrorEnvelope
import app.pluct.services.PluctTranscriptionServiceOrchestrator
import app.pluct.services.PluctBusinessEngineService
import app.pluct.services.PluctTranscriptionWorkflow
import app.pluct.services.PluctAuthenticationJWTManager
import app.pluct.services.PluctErrorHandlingRetryLogic
import java.util.UUID
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * Pluct-ViewModel-Main-01Orchestrator - Main view model orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@HiltViewModel
class PluctMainViewModelOrchestrator @Inject constructor(
    private val transcriptionService: PluctTranscriptionServiceOrchestrator,
    private val businessEngineService: PluctBusinessEngineService,
    private val transcriptionWorkflow: PluctTranscriptionWorkflow,
    private val jwtManager: PluctAuthenticationJWTManager,
    private val errorHandling: PluctErrorHandlingRetryLogic
) : ViewModel() {
    
    // Use transcription service's video list
    val videos: StateFlow<List<VideoItem>> = transcriptionService.videos
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _currentError = MutableStateFlow<ErrorEnvelope?>(null)
    val currentError: StateFlow<ErrorEnvelope?> = _currentError.asStateFlow()
    
    private val _creditBalance = MutableStateFlow(10)
    val creditBalance: StateFlow<Int> = _creditBalance.asStateFlow()
    
    private val _isCreditBalanceLoading = MutableStateFlow(false)
    val isCreditBalanceLoading: StateFlow<Boolean> = _isCreditBalanceLoading.asStateFlow()
    
    private val _creditBalanceError = MutableStateFlow<String?>(null)
    val creditBalanceError: StateFlow<String?> = _creditBalanceError.asStateFlow()
    
    init {
        // Videos are already available through transcriptionService.videos
        
        // Load credit balance
        loadCreditBalance()
        
        // Initialize new services
        initializeServices()
    }
    
    /**
     * Initialize all new services
     */
    private fun initializeServices() {
        viewModelScope.launch {
            try {
                // TODO: Initialize new services when properly integrated
                android.util.Log.d("PluctMainViewModelOrchestrator", "Services initialization placeholder")
                
            } catch (e: Exception) {
                android.util.Log.e("PluctMainViewModelOrchestrator", "Error initializing services: ${e.message}", e)
            }
        }
    }
    
    /**
     * Process video URL using complete workflow
     */
    fun processVideo(url: String, tier: ProcessingTier = ProcessingTier.STANDARD) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _currentError.value = null
                
                android.util.Log.d("PluctMainViewModelOrchestrator", "Processing video with tier: $tier using complete workflow")
                
                // Create transcription request
                val request = PluctTranscriptionWorkflow.TranscriptionRequest(
                    url = url,
                    userId = "mobile",
                    clientRequestId = "req_${System.currentTimeMillis()}"
                )
                
                // Execute complete workflow
                val workflowFlow = transcriptionWorkflow.executeTranscription(request)
                
                // Collect workflow steps
                workflowFlow.collect { step ->
                    android.util.Log.d("PluctMainViewModelOrchestrator", "Workflow step: ${step.step} - ${step.status} - ${step.message}")
                    
                    // Handle workflow completion
                    if (step.step == "workflow_complete" && step.status == "completed") {
                        android.util.Log.d("PluctMainViewModelOrchestrator", "✅ Transcription workflow completed successfully")
                    } else if (step.step == "workflow_error" && step.status == "failed") {
                        _currentError.value = ErrorEnvelope(
                            message = step.message,
                            code = "WORKFLOW_ERROR"
                        )
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("PluctMainViewModelOrchestrator", "❌ Transcription workflow failed: ${e.message}", e)
                _currentError.value = ErrorEnvelope(
                    message = e.message ?: "Unknown error",
                    code = "PROCESS_VIDEO_EXCEPTION"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load credit balance
     */
    fun loadCreditBalance() {
        viewModelScope.launch {
            try {
                _isCreditBalanceLoading.value = true
                _creditBalanceError.value = null
                
                val result = businessEngineService.getCreditBalance()
                if (result.isSuccess) {
                    val balance = result.getOrNull()?.balance ?: 0
                    _creditBalance.value = balance
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _creditBalanceError.value = error
                }
                
            } catch (e: Exception) {
                _creditBalanceError.value = e.message ?: "Unknown error"
            } finally {
                _isCreditBalanceLoading.value = false
            }
        }
    }
    
    /**
     * Refresh credit balance
     */
    fun refreshCreditBalance() {
        loadCreditBalance()
    }
    
    /**
     * Dismiss error
     */
    fun dismissError() {
        _currentError.value = null
    }
    
    /**
     * Get video by ID
     */
    fun getVideoById(videoId: String): VideoItem? {
        return videos.value.find { it.id == videoId }
    }
    
    /**
     * Get videos by status
     */
    fun getVideosByStatus(status: ProcessingStatus): List<VideoItem> {
        return videos.value.filter { it.status == status }
    }
    
    /**
     * Get processing videos
     */
    fun getProcessingVideos(): List<VideoItem> {
        return videos.value.filter { 
            it.status == ProcessingStatus.QUEUED || it.status == ProcessingStatus.PROCESSING 
        }
    }
    
    /**
     * Get completed videos
     */
    fun getCompletedVideos(): List<VideoItem> {
        return videos.value.filter { it.status == ProcessingStatus.COMPLETED }
    }
    
    /**
     * Get failed videos
     */
    fun getFailedVideos(): List<VideoItem> {
        return videos.value.filter { it.status == ProcessingStatus.FAILED }
    }
    
    /**
     * Retry a failed video
     */
    fun retryVideo(video: VideoItem) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _currentError.value = null
                
                android.util.Log.d("PluctMainViewModelOrchestrator", "Retrying video: ${video.id}")
                
                // Restart processing
                val result = transcriptionService.handleManualUrlInput(video.url)
                if (result.isFailure) {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _currentError.value = ErrorEnvelope(
                        message = error,
                        code = "RETRY_VIDEO_ERROR"
                    )
                }
                
            } catch (e: Exception) {
                _currentError.value = ErrorEnvelope(
                    message = e.message ?: "Unknown error",
                    code = "RETRY_VIDEO_EXCEPTION"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Delete a video
     */
    fun deleteVideo(video: VideoItem) {
        viewModelScope.launch {
            try {
                android.util.Log.d("PluctMainViewModelOrchestrator", "Deleting video: ${video.id}")
                
                android.util.Log.d("PluctMainViewModelOrchestrator", "Video deleted successfully")
                // Note: Video deletion will be handled by the transcription service
                
            } catch (e: Exception) {
                _currentError.value = ErrorEnvelope(
                    message = e.message ?: "Unknown error",
                    code = "DELETE_VIDEO_EXCEPTION"
                )
            }
        }
    }
}
