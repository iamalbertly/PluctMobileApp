package app.pluct.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.repository.PluctRepository
import app.pluct.web.WebTranscriptActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class IngestState {
    PENDING,
    NEEDS_TRANSCRIPT,
    READY,
    TRANSCRIPT_SUCCESS
}

data class IngestUiState(
    val state: IngestState = IngestState.PENDING,
    val url: String = "",
    val videoId: String? = null,
    val error: String? = null,
    val processedUrl: String? = null,
    val transcript: String? = null,
    val summary: String? = null,
    val webErrorCode: String? = null,
    val webErrorMessage: String? = null,
    val hasLaunchedWebActivity: Boolean = false,
    val showPostProcessingOptions: Boolean = false,
    val runId: String = "",
    val providerUsed: String? = null
)

@HiltViewModel
class IngestViewModel @Inject constructor(
    private val repository: PluctRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    companion object {
        private const val TAG = "IngestViewModel"
    }
    
    private val url: String = checkNotNull(savedStateHandle["url"])
    private val runId: String = savedStateHandle["run_id"] ?: UUID.randomUUID().toString()
    
    private val _uiState = MutableStateFlow(IngestUiState(url = url, runId = runId))
    val uiState: StateFlow<IngestUiState> = _uiState.asStateFlow()
    
    init {
        Log.i(TAG, "WV:A:run_id=$runId")
        
        // Initialize transcript processing
        Log.i(TAG, "IngestViewModel initialized for URL: $url")
        
        Log.i(TAG, "Initializing IngestViewModel with URL: $url")
        processUrl()
    }
    
    private fun processUrl() {
        Log.i(TAG, "Starting URL processing for: $url")
        
        // Accept any non-blank TikTok URL; no strict equality gate
        Log.i(TAG, "WV:A:url=$url run=$runId")
        
        viewModelScope.launch {
            try {
                // Normalize URL
                val normalizedUrl = app.pluct.share.UrlProcessingUtils.normalizeUrl(url)
                Log.i(TAG, "Normalized URL: $normalizedUrl")
                _uiState.value = _uiState.value.copy(processedUrl = normalizedUrl)
                
                // Immediately validate if it's a TikTok URL
                if (!app.pluct.share.UrlProcessingUtils.isValidTikTokUrl(normalizedUrl)) {
                    Log.w(TAG, "Invalid TikTok URL: $normalizedUrl")
                    _uiState.value = _uiState.value.copy(
                        state = IngestState.PENDING,
                        error = "Invalid TikTok URL. Please provide a valid TikTok video link.",
                        webErrorCode = "invalid_tiktok_url",
                        webErrorMessage = "Not a valid TikTok URL"
                    )
                    return@launch
                }
                
                // Check if video already exists
                Log.i(TAG, "Checking if video already exists")
                val existingVideo = repository.findByUrl(normalizedUrl)
                if (existingVideo != null) {
                    handleExistingVideo(existingVideo, normalizedUrl)
                } else {
                    // Create new video
                    Log.i(TAG, "Creating new video")
                    val videoId = repository.upsertVideo(normalizedUrl)
                    Log.i(TAG, "Created video with ID: $videoId")
                    _uiState.value = _uiState.value.copy(
                        state = IngestState.NEEDS_TRANSCRIPT,
                        videoId = videoId
                    )
                }
                
            } catch (e: CancellationException) {
                Log.i(TAG, "Job was cancelled, but this is expected when ShareIngestActivity finishes")
                // Don't update the state on cancellation, let the next IngestViewModel instance handle it
            } catch (e: Exception) {
                Log.e(TAG, "Error processing URL: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    state = IngestState.PENDING,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    private suspend fun handleExistingVideo(existingVideo: app.pluct.data.entity.VideoItem, normalizedUrl: String) {
        Log.i(TAG, "Video already exists with ID: ${existingVideo.id}")
        
        // Check if this URL was previously marked as invalid
        if (existingVideo.isInvalid) {
            Log.w(TAG, "URL was previously marked as invalid: ${existingVideo.errorMessage}")
            
            // For TikTok URLs, let's retry them as the website might have changed
            if (app.pluct.utils.UrlUtils.isValidTikTokUrl(normalizedUrl)) {
                Log.i(TAG, "Retrying previously invalid TikTok URL: $normalizedUrl")
                // Clear the invalid status and retry
                repository.markUrlAsValid(existingVideo.id)
                _uiState.value = _uiState.value.copy(
                    state = IngestState.NEEDS_TRANSCRIPT,
                    videoId = existingVideo.id
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    state = IngestState.PENDING,
                    error = "This URL was previously marked as invalid: ${existingVideo.errorMessage}",
                    webErrorCode = "invalid_url",
                    webErrorMessage = existingVideo.errorMessage
                )
            }
            return
        }
        
        // Check if this video already has a transcript
        val (video, transcript) = repository.getVideoWithTranscript(existingVideo.id)
        if (transcript != null) {
            Log.i(TAG, "Video already has transcript, setting state to READY")
            val artifacts = repository.getArtifactsFlow(existingVideo.id).first()
            val summary = artifacts.find { it.kind == app.pluct.data.entity.ArtifactKind.SUMMARY }?.content
            
            _uiState.value = _uiState.value.copy(
                state = IngestState.READY,
                videoId = existingVideo.id,
                transcript = transcript.text,
                summary = summary
            )
        } else {
            Log.i(TAG, "Video exists but no transcript, setting state to NEEDS_TRANSCRIPT")
            _uiState.value = _uiState.value.copy(
                state = IngestState.NEEDS_TRANSCRIPT,
                videoId = existingVideo.id
            )
        }
    }
    
    fun saveTranscript(text: String, language: String? = null, setStateToReady: Boolean = false) {
        val videoId = _uiState.value.videoId ?: return
        
        viewModelScope.launch {
            try {
                Log.i(TAG, "Saving transcript for video: $videoId")
                repository.saveTranscript(videoId, text, language)
                Log.i(TAG, "Transcript saved successfully")
                
                // Only set state to READY if explicitly requested
                if (setStateToReady) {
                    _uiState.value = _uiState.value.copy(
                        state = IngestState.READY,
                        error = null
                    )
                } else {
                    // Just clear any errors without changing state
                    _uiState.value = _uiState.value.copy(
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving transcript: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to save transcript"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, webErrorCode = null, webErrorMessage = null)
    }
    
    fun setProviderUsed(provider: String) {
        _uiState.value = _uiState.value.copy(providerUsed = provider)
    }
    
    fun tryAnotherProvider(context: android.content.Context) {
        val currentProvider = app.pluct.ui.utils.ProviderSettings.getSelectedProvider(context)
            val newProvider = when (currentProvider) {
                app.pluct.ui.utils.TranscriptProvider.TOKAUDIT -> app.pluct.ui.utils.TranscriptProvider.GETTRANSCRIBE
                app.pluct.ui.utils.TranscriptProvider.GETTRANSCRIBE -> app.pluct.ui.utils.TranscriptProvider.OPENAI
                app.pluct.ui.utils.TranscriptProvider.OPENAI -> app.pluct.ui.utils.TranscriptProvider.TOKAUDIT
            }
        app.pluct.ui.utils.ProviderSettings.setSelectedProvider(context, newProvider)
        
        // Reset state to try again
        _uiState.value = _uiState.value.copy(
            state = IngestState.NEEDS_TRANSCRIPT,
            hasLaunchedWebActivity = false,
            error = null,
            webErrorCode = null,
            webErrorMessage = null,
            providerUsed = null
        )
    }
    
    fun markWebActivityLaunched() {
        Log.d("IngestViewModel", "Marking WebActivity as launched")
        _uiState.value = _uiState.value.copy(hasLaunchedWebActivity = true)
    }
    
    fun resetWebActivityLaunch() {
        Log.d("IngestViewModel", "Resetting WebActivity launch flag")
        _uiState.value = _uiState.value.copy(hasLaunchedWebActivity = false)
    }
    
    fun saveUrlForLaterProcessing(url: String) {
        Log.i(TAG, "Saving URL for later processing: $url")
        viewModelScope.launch {
            try {
                // Save to repository for later processing
                repository.upsertVideo(url)
                _uiState.value = _uiState.value.copy(
                    state = IngestState.PENDING,
                    error = "No internet connection. URL saved for later processing when internet returns."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error saving URL for later processing: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    state = IngestState.PENDING,
                    error = "Failed to save URL for later processing: ${e.message}"
                )
            }
        }
    }
    
    fun showTranscriptSuccess() {
        Log.i(TAG, "Showing transcript success state")
        _uiState.value = _uiState.value.copy(
            state = IngestState.TRANSCRIPT_SUCCESS,
            showPostProcessingOptions = true
        )
    }
    
    fun showTranscriptSuccess(transcript: String) {
        Log.i(TAG, "Showing transcript success state with transcript")
        _uiState.value = _uiState.value.copy(
            state = IngestState.TRANSCRIPT_SUCCESS,
            transcript = transcript,
            showPostProcessingOptions = true
        )
    }
    
    fun generateValueProposition() {
        val videoId = _uiState.value.videoId ?: return
        val transcript = _uiState.value.transcript ?: return
        
        Log.i(TAG, "Generating value proposition for video: $videoId")
        viewModelScope.launch {
            try {
                // Generate value proposition from transcript
                val valueProposition = ValuePropositionGenerator.generateValuePropositionFromTranscript(transcript)
                
                // Save as artifact
                repository.saveArtifact(
                    videoId = videoId,
                    kind = app.pluct.data.entity.ArtifactKind.VALUE_PROPOSITION,
                    content = valueProposition,
                    filename = "value_proposition.txt",
                    mime = "text/plain"
                )
                
                Log.i(TAG, "Value proposition generated and saved")
                _uiState.value = _uiState.value.copy(
                    state = IngestState.READY,
                    summary = valueProposition
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error generating value proposition: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to generate value proposition: ${e.message}"
                )
            }
        }
    }
    
    fun handleWebTranscriptResult(resultCode: Int, data: android.content.Intent?) {
        when (resultCode) {
            android.app.Activity.RESULT_OK -> {
                val videoId = data?.getStringExtra(WebTranscriptActivity.EXTRA_VIDEO_ID)
                if (videoId != null) {
                    Log.i(TAG, "Web transcript completed successfully for videoId: $videoId")
                    // Load transcript and summary data
                    viewModelScope.launch {
                        try {
                            val (video, transcript) = repository.getVideoWithTranscript(videoId)
                            val artifacts = repository.getArtifactsFlow(videoId).first()
                            val summary = artifacts.find { it.kind == app.pluct.data.entity.ArtifactKind.SUMMARY }?.content
                            
                            // Provider will be set when we have context access
                            val providerUsed = _uiState.value.providerUsed
                            _uiState.value = _uiState.value.copy(
                                state = IngestState.READY,
                                transcript = transcript?.text,
                                summary = summary,
                                error = null,
                                webErrorCode = null,
                                webErrorMessage = null,
                                providerUsed = providerUsed
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading transcript data: ${e.message}", e)
                            _uiState.value = _uiState.value.copy(
                                state = IngestState.READY,
                                error = null,
                                webErrorCode = null,
                                webErrorMessage = null
                            )
                        }
                    }
                }
            }
            android.app.Activity.RESULT_CANCELED -> {
                val errorCode = data?.getStringExtra(WebTranscriptActivity.EXTRA_ERROR_CODE)
                val errorMessage = data?.getStringExtra(WebTranscriptActivity.EXTRA_ERROR_MESSAGE)
                Log.w(TAG, "Web transcript failed: $errorCode - $errorMessage")
                _uiState.value = _uiState.value.copy(
                    webErrorCode = errorCode,
                    webErrorMessage = errorMessage
                )
            }
        }
    }
    
    // This method is kept for potential future use in other components
    fun launchWebTranscript(context: android.content.Context) {
        val videoId = _uiState.value.videoId
        val processedUrl = _uiState.value.processedUrl
        
        if (videoId != null && processedUrl != null) {
            Log.i(TAG, "Launching WebTranscriptActivity for videoId: $videoId, url: $processedUrl")
            val intent = WebTranscriptActivity.createIntent(context, videoId, processedUrl).apply {
                putExtra("run_id", runId)
            }
            context.startActivity(intent)
        } else {
            Log.e(TAG, "Cannot launch WebTranscriptActivity: missing videoId or processedUrl")
        }
    }
    
    /**
     * Save transcript from JavaScript bridge
     */
    fun saveTranscript(runId: String, sourceUrl: String, transcriptText: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Saving transcript for runId: $runId, url: $sourceUrl, length: ${transcriptText.length}")
                
                // Create transcript data object
                val transcriptData = mapOf(
                    "runId" to runId,
                    "sourceUrl" to sourceUrl,
                    "text" to transcriptText,
                    "createdAt" to System.currentTimeMillis(),
                    "tags" to listOf("source:tiktok", "host:script.tokaudit.io")
                )
                
                // Save to repository
                val rowId = repository.saveTranscript(transcriptData)
                
                // Log PLUCT:store_ok as required
                Log.d(TAG, "PLUCT:store_ok id=$rowId len=${transcriptText.length} tags=[source:tiktok, host:script.tokaudit.io] run=$runId url=$sourceUrl")
                app.pluct.ui.utils.RunRingBuffer.addLog(runId, "INFO", "store_ok id=$rowId len=${transcriptText.length} tags=[source:tiktok, host:script.tokaudit.io] url=$sourceUrl", "PLUCT")
                
                // Update UI state
                _uiState.value = _uiState.value.copy(
                    state = IngestState.TRANSCRIPT_SUCCESS,
                    transcript = transcriptText
                )
                
                Log.d(TAG, "PLUCT:ui_done run=$runId id=$rowId")
                app.pluct.ui.utils.RunRingBuffer.addLog(runId, "INFO", "ui_done id=$rowId", "PLUCT")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving transcript: ${e.message}", e)
                app.pluct.ui.utils.RunRingBuffer.addLog(runId, "ERROR", "save_error=${e.message}", "PLUCT")
            }
        }
    }
}

