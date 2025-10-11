package app.pluct.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.repository.PluctRepository
import app.pluct.utils.UrlProcessingUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Pluct-URL-Processor - Handles URL processing and validation
 */
class PluctUrlProcessor(
    private val repository: PluctRepository,
    private val stateManager: PluctIngestStateManager
) : ViewModel() {
    companion object {
        private const val TAG = "PluctUrlProcessor"
    }
    
    fun processUrl(url: String) {
        Log.i(TAG, "Starting URL processing for: $url")
        
        viewModelScope.launch {
            try {
                val normalizedUrl = UrlProcessingUtils.normalizeUrl(url)
                Log.i(TAG, "Normalized URL: $normalizedUrl")
                stateManager.updateState { it.copy(processedUrl = normalizedUrl) }
                
                if (!UrlProcessingUtils.isValidTikTokUrl(normalizedUrl)) {
                    Log.w(TAG, "Invalid TikTok URL: $normalizedUrl")
                    stateManager.updateState { it.copy(
                        state = IngestState.PENDING,
                        error = "Invalid TikTok URL. Please provide a valid TikTok video link.",
                        webErrorCode = "invalid_tiktok_url",
                        webErrorMessage = "Not a valid TikTok URL"
                    ) }
                    return@launch
                }
                
                Log.i(TAG, "Checking if video already exists")
                val existingVideo = repository.findByUrl(normalizedUrl)
                if (existingVideo != null) {
                    handleExistingVideo(existingVideo, normalizedUrl)
                } else {
                    Log.i(TAG, "Creating new video")
                    val videoId = repository.upsertVideo(normalizedUrl)
                    Log.i(TAG, "Created video with ID: $videoId")
                    stateManager.updateState { it.copy(
                        state = IngestState.NEEDS_TRANSCRIPT,
                        videoId = videoId
                    ) }
                }
                
            } catch (e: CancellationException) {
                Log.i(TAG, "Job was cancelled, but this is expected when ShareIngestActivity finishes")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing URL: ${e.message}", e)
                stateManager.updateState { it.copy(
                    state = IngestState.PENDING,
                    error = e.message ?: "Unknown error"
                ) }
            }
        }
    }
    
    private suspend fun handleExistingVideo(existingVideo: app.pluct.data.entity.VideoItem, normalizedUrl: String) {
        Log.i(TAG, "Video already exists with ID: ${existingVideo.id}")
        
        if (existingVideo.isInvalid) {
            Log.w(TAG, "URL was previously marked as invalid: ${existingVideo.errorMessage}")
            
            if (app.pluct.utils.UrlUtils.isValidTikTokUrl(normalizedUrl)) {
                Log.i(TAG, "Retrying previously invalid TikTok URL: $normalizedUrl")
                repository.markUrlAsValid(existingVideo.id)
                stateManager.updateState { it.copy(
                    state = IngestState.NEEDS_TRANSCRIPT,
                    videoId = existingVideo.id
                ) }
            } else {
                stateManager.updateState { it.copy(
                    state = IngestState.PENDING,
                    error = "This URL was previously marked as invalid: ${existingVideo.errorMessage}",
                    webErrorCode = "invalid_url",
                    webErrorMessage = existingVideo.errorMessage
                ) }
            }
            return
        }
        
        val (video, transcript) = repository.getVideoWithTranscript(existingVideo.id)
        if (transcript != null) {
            Log.i(TAG, "Video already has transcript, setting state to READY")
            val artifacts = repository.getArtifactsFlow(existingVideo.id).first()
            val summary = artifacts.find { it.kind == app.pluct.data.entity.ArtifactKind.SUMMARY }?.content
            
            stateManager.updateState { it.copy(
                state = IngestState.READY,
                videoId = existingVideo.id,
                transcript = transcript.text,
                summary = summary
            ) }
        } else {
            Log.i(TAG, "Video exists but no transcript, setting state to NEEDS_TRANSCRIPT")
            stateManager.updateState { it.copy(
                state = IngestState.NEEDS_TRANSCRIPT,
                videoId = existingVideo.id
            ) }
        }
    }
}
