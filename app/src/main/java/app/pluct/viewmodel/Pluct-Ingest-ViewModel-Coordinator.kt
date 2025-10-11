package app.pluct.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.repository.PluctRepository
import app.pluct.data.manager.PluctTranscriptionManagerCoordinator
import app.pluct.web.WebTranscriptActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pluct-Ingest-ViewModel-Coordinator - Simplified coordinator for ingest functionality
 */
@HiltViewModel
class PluctIngestViewModelCoordinator @Inject constructor(
    private val repository: PluctRepository,
    private val transcriptionManager: PluctTranscriptionManagerCoordinator,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    companion object {
        private const val TAG = "PluctIngestViewModelCoordinator"
    }
    
    private val stateManager = PluctIngestStateManager(savedStateHandle)
    private val urlProcessor = PluctUrlProcessor(repository, stateManager)
    private val transcriptProcessor = PluctTranscriptProcessor(repository, transcriptionManager, stateManager)
    
    val uiState = stateManager.uiState
    
    init {
        Log.i(TAG, "PluctIngestViewModelCoordinator initialized")
        val url = checkNotNull(savedStateHandle["url"]) as String
        urlProcessor.processUrl(url)
    }
    
    fun saveTranscript(text: String, language: String? = null, setStateToReady: Boolean = false) {
        transcriptProcessor.saveTranscript(text, language, setStateToReady)
    }
    
    fun clearError() {
        stateManager.clearError()
    }
    
    fun setProviderUsed(provider: String) {
        stateManager.updateState { it.copy(providerUsed = provider) }
    }
    
    fun tryAnotherProvider(context: Context) {
        val availableProviders = app.pluct.ui.utils.ProviderSettings.getAvailableProviders(context)
        
        if (availableProviders.size <= 1) {
            stateManager.updateState { it.copy(error = "No other providers available to try") }
            return
        }
        
        stateManager.updateState { it.copy(
            state = IngestState.NEEDS_TRANSCRIPT,
            hasLaunchedWebActivity = false,
            error = null,
            webErrorCode = null,
            webErrorMessage = null,
            providerUsed = null
        ) }
    }
    
    fun markWebActivityLaunched() {
        Log.d("PluctIngestViewModelCoordinator", "Marking WebActivity as launched")
        stateManager.updateState { it.copy(hasLaunchedWebActivity = true) }
    }
    
    fun resetWebActivityLaunch() {
        Log.d("PluctIngestViewModelCoordinator", "Resetting WebActivity launch flag")
        stateManager.updateState { it.copy(hasLaunchedWebActivity = false) }
    }
    
    fun showTranscriptSuccess(transcript: String? = null) {
        Log.i(TAG, "Showing transcript success state")
        stateManager.updateState { it.copy(
            state = IngestState.TRANSCRIPT_SUCCESS,
            transcript = transcript ?: it.transcript,
            showPostProcessingOptions = true
        ) }
    }
    
    fun generateValueProposition() {
        transcriptProcessor.generateValueProposition()
    }
    
    fun handleWebTranscriptResult(resultCode: Int, data: android.content.Intent?) {
        when (resultCode) {
            android.app.Activity.RESULT_OK -> {
                val videoId = data?.getStringExtra(WebTranscriptActivity.EXTRA_VIDEO_ID)
                if (videoId != null) {
                    Log.i(TAG, "Web transcript completed successfully for videoId: $videoId")
                    viewModelScope.launch {
                        try {
                            val (video, transcript) = repository.getVideoWithTranscript(videoId)
                            val artifacts = repository.getArtifactsFlow(videoId).first()
                            val summary = artifacts.find { it.kind == app.pluct.data.entity.ArtifactKind.SUMMARY }?.content
                            
                            val providerUsed = stateManager.uiState.value.providerUsed
                            stateManager.updateState { it.copy(
                                state = IngestState.READY,
                                transcript = transcript?.text,
                                summary = summary,
                                error = null,
                                webErrorCode = null,
                                webErrorMessage = null,
                                providerUsed = providerUsed
                            ) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading transcript data: ${e.message}", e)
                            stateManager.updateState { it.copy(
                                state = IngestState.READY,
                                error = null,
                                webErrorCode = null,
                                webErrorMessage = null
                            ) }
                        }
                    }
                }
            }
            android.app.Activity.RESULT_CANCELED -> {
                val errorCode = data?.getStringExtra(WebTranscriptActivity.EXTRA_ERROR_CODE)
                val errorMessage = data?.getStringExtra(WebTranscriptActivity.EXTRA_ERROR_MESSAGE)
                Log.w(TAG, "Web transcript failed: $errorCode - $errorMessage")
                stateManager.updateState { it.copy(
                    webErrorCode = errorCode,
                    webErrorMessage = errorMessage
                ) }
            }
        }
    }
    
    fun launchWebTranscript(context: Context) {
        val videoId = stateManager.uiState.value.videoId
        val processedUrl = stateManager.uiState.value.processedUrl
        
        if (videoId != null && processedUrl != null) {
            Log.i(TAG, "Launching WebTranscriptActivity for videoId: $videoId, url: $processedUrl")
            val intent = WebTranscriptActivity.createIntent(context, videoId, processedUrl).apply {
                putExtra("run_id", stateManager.uiState.value.runId)
            }
            context.startActivity(intent)
        } else {
            Log.e(TAG, "Cannot launch WebTranscriptActivity: missing videoId or processedUrl")
        }
    }
    
    fun saveUrlForLaterProcessing(url: String) {
        Log.d(TAG, "Saving URL for later processing: $url")
        // This is a placeholder - in a real implementation, this would save to SharedPreferences or database
    }
}
