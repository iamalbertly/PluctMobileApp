package app.pluct.services.background.status

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreBackground01TranscriptionWorker
import app.pluct.services.PluctCoreAPIUnifiedService
import kotlinx.coroutines.flow.first

/**
 * Pluct-Core-Service-Background-Status-03Resumer
 * Resumes incomplete transcriptions on app start
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[SubScope]-[Responsibility]
 */
class PluctStatusResumer(
    private val videoRepository: PluctVideoRepository,
    private val apiService: PluctCoreAPIUnifiedService,
    private val context: Context
) {
    companion object {
        private const val TAG = "StatusResumer"
    }
    
    /**
     * UX IMPROVEMENT #2: Resume incomplete transcriptions with API status verification
     * CRITICAL FIX: Verifies job status from API before resuming to prevent re-processing completed jobs
     * Uses StatusVerifier helper for reusable verification logic
     */
    suspend fun resumeIncompleteTranscriptions() {
        try {
            val processingVideos = videoRepository.getVideosByStatus(ProcessingStatus.PROCESSING)
                .first()
            
            if (processingVideos.isEmpty()) {
                Log.d(TAG, "No incomplete transcriptions to resume")
                return
            }
            
            Log.d(TAG, "Found ${processingVideos.size} incomplete transcription(s) to resume")
            
            // UX IMPROVEMENT #5: Filter out stale processing entries (older than 1 hour)
            val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
            val activeProcessingVideos = processingVideos.filter { 
                it.timestamp > oneHourAgo || it.jobId != null 
            }
            
            if (activeProcessingVideos.size < processingVideos.size) {
                val staleCount = processingVideos.size - activeProcessingVideos.size
                Log.w(TAG, "Filtered out $staleCount stale processing entries (older than 1 hour)")
                
                // Mark stale entries as failed
                processingVideos.filter { it.timestamp <= oneHourAgo && it.jobId == null }
                    .forEach { staleVideo ->
                        videoRepository.updateVideo(staleVideo.copy(
                            status = ProcessingStatus.FAILED,
                            failureReason = "Processing timed out (stale entry)"
                        ))
                    }
            }
            
            // UX IMPROVEMENT #2: Verify status from API before resuming
            val statusVerifier = PluctCoreServiceBackgroundStatus04StatusVerifier(apiService, videoRepository)
            
            activeProcessingVideos.forEach { video ->
                Log.d(TAG, "Verifying status for video: ${video.url}, jobId: ${video.jobId}")
                
                // If jobId exists, verify status from API first
                if (video.jobId != null && video.jobId.isNotBlank()) {
                    try {
                        val isStillActive = statusVerifier.verifyAndUpdateStatus(video)
                        
                        if (isStillActive) {
                            // Job is still active, resume polling
                            Log.d(TAG, "Video ${video.url} still active, resuming polling")
                            resumePolling(video)
                        } else {
                            // Job is completed or failed, already updated in database by verifier
                            Log.d(TAG, "Video ${video.url} already completed or failed, skipping resume")
                        }
                    } catch (e: Exception) {
                        // Enhanced error logging with video details
                        Log.e(TAG, "Error verifying status for video URL: ${video.url}, jobId: ${video.jobId}: ${e.message}", e)
                        // On error, resume polling anyway (may be transient)
                        Log.w(TAG, "Resuming polling for ${video.url} despite verification error (assumed transient)")
                        resumePolling(video)
                    }
                } else {
                    // No jobId - schedule new transcription (fallback)
                    Log.w(TAG, "No jobId available for video: ${video.url}, scheduling new transcription")
                    scheduleNewTranscription(video)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming incomplete transcriptions: ${e.message}", e)
        }
    }
    
    private fun resumePolling(video: app.pluct.data.entity.VideoItem) {
        val notificationId = video.url.hashCode().and(0x7FFFFFFF)
        val workRequest = OneTimeWorkRequestBuilder<PluctCoreBackground01TranscriptionWorker>()
            .setInputData(
                workDataOf(
                    PluctCoreBackground01TranscriptionWorker.KEY_URL to video.url,
                    PluctCoreBackground01TranscriptionWorker.KEY_JOB_ID to video.jobId,
                    PluctCoreBackground01TranscriptionWorker.KEY_NOTIFICATION_ID to notificationId
                )
            )
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d(TAG, "Scheduled background worker to resume transcription with jobId: ${video.jobId}")
    }
    
    private fun scheduleNewTranscription(video: app.pluct.data.entity.VideoItem) {
        val notificationId = video.url.hashCode().and(0x7FFFFFFF)
        val workRequest = OneTimeWorkRequestBuilder<PluctCoreBackground01TranscriptionWorker>()
            .setInputData(
                workDataOf(
                    PluctCoreBackground01TranscriptionWorker.KEY_URL to video.url,
                    PluctCoreBackground01TranscriptionWorker.KEY_NOTIFICATION_ID to notificationId
                )
            )
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d(TAG, "Scheduled background worker for new transcription: ${video.url}")
    }
}

