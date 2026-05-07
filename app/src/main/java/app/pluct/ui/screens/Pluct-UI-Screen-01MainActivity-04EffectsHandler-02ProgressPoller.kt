package app.pluct.ui.screens

import android.content.Context
import android.util.Log
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.preferences.PluctUserPreferences
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.api.PluctCoreAPITranscriptionResult01Extractor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Pluct-UI-Screen-01MainActivity-04EffectsHandler-02ProgressPoller
 * Follows naming convention: [Project]-[UI]-[Screen]-[MainActivity]-[EffectsHandler]-[Sequence][ProgressPoller]
 * 6 scope layers: Project, UI, Screen, MainActivity, EffectsHandler, Sequence, ProgressPoller
 * 
 * UX IMPROVEMENT #1 & #5: Simplified and optimized progress polling with early exit and adaptive intervals
 * Technical Debt #1: Extracted polling logic from EffectsHandler for better maintainability
 */
object PluctUIScreen01MainActivity04EffectsHandler02ProgressPoller {
    private const val TAG = "ProgressPoller"
    
    private const val MAX_RETRY_FAILURES = 3 // Stop after 3 consecutive failures
    private const val BASE_POLL_INTERVAL_MS = 5000L // 5 seconds base interval
    private const val FAST_POLL_INTERVAL_MS = 2000L // 2 seconds when near completion
    private const val PROGRESS_THRESHOLD_FAST = 80 // Use fast polling when progress > 80%
    
    /**
     * UX IMPROVEMENT #1: Poll transcription status with early exit and adaptive intervals
     * Stops immediately if video is already completed/failed before starting poll loop
     * Uses adaptive polling intervals (faster when near completion)
     * Stops after multiple consecutive failures
     */
    suspend fun pollTranscriptionProgress(
        video: app.pluct.data.entity.VideoItem,
        apiService: PluctCoreAPIUnifiedService,
        videoRepository: PluctVideoRepository,
        context: Context? = null,
        onFirstTranscriptCompleted: (() -> Unit)? = null
    ) {
        if (video.jobId == null || video.jobId.isBlank()) {
            Log.d(TAG, "Video ${video.url} has no jobId yet, skipping poll")
            return
        }
        
        // UX IMPROVEMENT #1: Check status before starting poll loop
        val initialCheck = videoRepository.getVideoByUrl(video.url)
        if (initialCheck != null && 
            (initialCheck.status == ProcessingStatus.COMPLETED || 
             initialCheck.status == ProcessingStatus.FAILED)) {
            Log.d(TAG, "Video ${video.url} already ${initialCheck.status}, skipping poll")
            return
        }
        
        var consecutiveFailures = 0

        while (coroutineContext.isActive && consecutiveFailures < MAX_RETRY_FAILURES) {
            try {
                // Check database status before each poll
                val currentVideo = videoRepository.getVideoByUrl(video.url)
                if (currentVideo != null && 
                    (currentVideo.status == ProcessingStatus.COMPLETED || 
                     currentVideo.status == ProcessingStatus.FAILED)) {
                    Log.d(TAG, "Video ${video.url} now ${currentVideo.status}, stopping poll")
                    break
                }
                
                val tokenResult = apiService.getServiceToken()
                if (tokenResult.isFailure) {
                    consecutiveFailures++
                    Log.w(TAG, "Failed to get service token (failure $consecutiveFailures/$MAX_RETRY_FAILURES) for ${video.url}")
                    if (consecutiveFailures >= MAX_RETRY_FAILURES) {
                        Log.w(TAG, "Max failures reached, stopping poll for ${video.url}")
                        break
                    }
                    delay(10000) // Wait longer on token failure
                    continue
                }
                
                val serviceToken = tokenResult.getOrNull()
                if (serviceToken == null || serviceToken.isBlank()) {
                    consecutiveFailures++
                    if (consecutiveFailures >= MAX_RETRY_FAILURES) {
                        break
                    }
                    delay(10000)
                    continue
                }
                
                val statusResult = apiService.checkTranscriptionStatus(video.jobId, serviceToken)
                if (statusResult.isFailure) {
                    consecutiveFailures++
                    Log.w(TAG, "Status check failed (failure $consecutiveFailures/$MAX_RETRY_FAILURES) for ${video.url}")
                    if (consecutiveFailures >= MAX_RETRY_FAILURES) {
                        Log.w(TAG, "Max failures reached, stopping poll for ${video.url}")
                        break
                    }
                    // UX IMPROVEMENT #2: Exponential backoff on failures
                    delay(10000L * consecutiveFailures)
                    continue
                }
                
                // Reset failure count on success
                consecutiveFailures = 0
                
                val status = statusResult.getOrNull() ?: continue
                val progress = status.progress
                val extraction = PluctCoreAPITranscriptionResult01Extractor.extract(status)
                val transcript = extraction.transcript
                
                val updatedVideo = when {
                    status.status == "completed" && transcript != null -> {
                        // Track first transcript completion for onboarding milestone
                        context?.let { ctx ->
                            val prefs = PluctUserPreferences(ctx)
                            if (!prefs.isFirstTranscriptCompleted()) {
                                prefs.markFirstTranscriptCompleted()
                                Log.d(TAG, "First transcript completed - onboarding milestone achieved")
                                onFirstTranscriptCompleted?.invoke()
                            }
                        }
                        video.copy(
                            status = ProcessingStatus.COMPLETED,
                            progress = 100,
                            transcript = transcript
                        )
                    }
                    status.status == "failed" -> {
                        video.copy(
                            status = ProcessingStatus.FAILED,
                            failureReason = status.error ?: "Transcription failed"
                        )
                    }
                    else -> {
                        video.copy(progress = progress)
                    }
                }
                videoRepository.insertVideo(updatedVideo)
                Log.d(TAG, "Updated progress for video ${video.url}: $progress%")
                
                if (status.status == "completed" || status.status == "failed") {
                    Log.d(TAG, "Transcription ${status.status} for ${video.url}, stopping poll")
                    break
                }
                
                // UX IMPROVEMENT #5: Adaptive polling interval based on progress
                val pollInterval = if (progress >= PROGRESS_THRESHOLD_FAST) {
                    FAST_POLL_INTERVAL_MS
                } else {
                    BASE_POLL_INTERVAL_MS
                }
                
                delay(pollInterval)
                
            } catch (e: Exception) {
                consecutiveFailures++
                Log.w(TAG, "Exception during status check (failure $consecutiveFailures/$MAX_RETRY_FAILURES) for ${video.url}: ${e.message}")
                if (consecutiveFailures >= MAX_RETRY_FAILURES) {
                    Log.w(TAG, "Max failures reached due to exceptions, stopping poll for ${video.url}")
                    break
                }
                delay(10000L * consecutiveFailures) // Exponential backoff
            }
        }
        
        if (consecutiveFailures >= MAX_RETRY_FAILURES) {
            Log.w(TAG, "Stopped polling for ${video.url} after $MAX_RETRY_FAILURES consecutive failures")
        }
    }
}
