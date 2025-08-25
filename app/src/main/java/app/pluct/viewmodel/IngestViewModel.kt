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
import java.net.URL
import javax.inject.Inject

enum class IngestState {
    PENDING,
    NEEDS_TRANSCRIPT,
    READY
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
    val hasLaunchedWebActivity: Boolean = false
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
    
    private val _uiState = MutableStateFlow(IngestUiState(url = url))
    val uiState: StateFlow<IngestUiState> = _uiState.asStateFlow()
    
    init {
        Log.i(TAG, "Initializing IngestViewModel with URL: $url")
        processUrl()
    }
    
    private fun processUrl() {
        Log.i(TAG, "Starting URL processing for: $url")
        viewModelScope.launch {
            try {
                // Normalize URL
                val normalizedUrl = normalizeUrl(url)
                Log.i(TAG, "Normalized URL: $normalizedUrl")
                _uiState.value = _uiState.value.copy(processedUrl = normalizedUrl)
                
                // Check if video already exists
                Log.i(TAG, "Checking if video already exists")
                val existingVideo = repository.findByUrl(normalizedUrl)
                if (existingVideo != null) {
                    Log.i(TAG, "Video already exists with ID: ${existingVideo.id}")
                    
                    // Check if this URL was previously marked as invalid
                    if (existingVideo.isInvalid) {
                        Log.w(TAG, "URL was previously marked as invalid: ${existingVideo.errorMessage}")
                        _uiState.value = _uiState.value.copy(
                            state = IngestState.PENDING,
                            error = "This URL was previously marked as invalid: ${existingVideo.errorMessage}",
                            webErrorCode = "invalid_url",
                            webErrorMessage = existingVideo.errorMessage
                        )
                        return@launch
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
                    return@launch
                }
                
                // Create new video
                Log.i(TAG, "Creating new video")
                val videoId = repository.upsertVideo(normalizedUrl)
                Log.i(TAG, "Created video with ID: $videoId")
                _uiState.value = _uiState.value.copy(
                    state = IngestState.NEEDS_TRANSCRIPT,
                    videoId = videoId
                )
                
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
    
    private fun normalizeUrl(url: String): String {
        return try {
            // Check if URL contains a deep link format and extract the actual URL
            val cleanUrl = if (url.contains("url=")) {
                try {
                    android.net.Uri.decode(url.substringAfter("url="))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to extract URL from deep link: ${e.message}", e)
                    url
                }
            } else {
                url
            }
            
            Log.d(TAG, "Normalizing URL. Original: $url, Pre-cleaned: $cleanUrl")
            
            val uri = URL(cleanUrl)
            val host = uri.host.lowercase()
            val path = uri.path
            val query = uri.query?.let { query ->
                // Remove UTM parameters
                query.split("&")
                    .filterNot { it.startsWith("utm_") }
                    .joinToString("&")
                    .takeIf { it.isNotEmpty() }
                    ?.let { "?$it" } ?: ""
            } ?: ""
            
            val normalizedUrl = "${uri.protocol}://$host$path$query".removeSuffix("/")
            Log.d(TAG, "Normalized URL: $normalizedUrl")
            normalizedUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error normalizing URL: ${e.message}", e)
            url
        }
    }
    
    fun saveTranscript(text: String, language: String? = null) {
        val videoId = _uiState.value.videoId ?: return
        
        viewModelScope.launch {
            try {
                Log.i(TAG, "Saving transcript for video: $videoId")
                repository.saveTranscript(videoId, text, language)
                Log.i(TAG, "Transcript saved successfully")
                _uiState.value = _uiState.value.copy(
                    state = IngestState.READY,
                    error = null
                )
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
    
    fun markWebActivityLaunched() {
        Log.d("IngestViewModel", "Marking WebActivity as launched")
        _uiState.value = _uiState.value.copy(hasLaunchedWebActivity = true)
    }
    
    fun resetWebActivityLaunch() {
        Log.d("IngestViewModel", "Resetting WebActivity launch flag")
        _uiState.value = _uiState.value.copy(hasLaunchedWebActivity = false)
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
                            
                            _uiState.value = _uiState.value.copy(
                                state = IngestState.READY,
                                transcript = transcript?.text,
                                summary = summary,
                                error = null,
                                webErrorCode = null,
                                webErrorMessage = null
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
            val intent = WebTranscriptActivity.createIntent(context, videoId, processedUrl)
            context.startActivity(intent)
        } else {
            Log.e(TAG, "Cannot launch WebTranscriptActivity: missing videoId or processedUrl")
        }
    }
}

