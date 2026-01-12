package app.pluct.services.background.status

import android.util.Log
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.api.PluctCoreAPITranscriptionResult01Extractor

/**
 * Pluct-Core-Service-Background-Status-04StatusVerifier
 * Follows naming convention: [Project]-[Core]-[Service]-[Background]-[Status]-[Sequence][StatusVerifier]
 * 7 scope layers: Project, Core, Service, Background, Status, Sequence, StatusVerifier
 * 
 * UX IMPROVEMENT #4: Verifies transcription job status from API before resuming
 * Prevents re-processing of already completed or failed jobs
 * Provides reusable status verification logic for resumer and worker
 */
class PluctCoreServiceBackgroundStatus04StatusVerifier(
    private val apiService: PluctCoreAPIUnifiedService,
    private val videoRepository: PluctVideoRepository
) {
    companion object {
        private const val TAG = "StatusVerifier"
    }
    
    /**
     * Verify job status from API and update database if needed
     * Returns true if job is still active (processing/queued), false if completed/failed
     * 
     * Edge Cases:
     * - Missing jobId: Return true (assume active, cannot verify)
     * - Token refresh failure: Return true (assume active, may be transient)
     * - Unknown status: Return true (conservative, assume active)
     * - Database update failure: Log error, return status based on API response
     */
    suspend fun verifyAndUpdateStatus(video: app.pluct.data.entity.VideoItem): Boolean {
        if (video.jobId == null || video.jobId.isBlank()) {
            Log.d(TAG, "No jobId for video ${video.url}, cannot verify")
            return true // Assume active if no jobId
        }
        
        return try {
            val tokenResult = apiService.getServiceToken()
            if (tokenResult.isFailure) {
                Log.w(TAG, "Failed to get service token for ${video.url}")
                return true // Assume active on error
            }
            
            val serviceToken = tokenResult.getOrNull()
            if (serviceToken == null || serviceToken.isBlank()) {
                Log.w(TAG, "Empty service token for ${video.url}")
                return true // Assume active on error
            }
            
            val statusResult = apiService.checkTranscriptionStatus(video.jobId, serviceToken)
            if (statusResult.isFailure) {
                Log.w(TAG, "Status check failed for ${video.url}")
                return true // Assume active on error
            }
            
            val status = statusResult.getOrNull()!!
            val extraction = PluctCoreAPITranscriptionResult01Extractor.extract(status)
            val transcript = extraction.transcript
            
            when (status.status) {
                "completed" -> {
                    if (transcript != null) {
                        Log.d(TAG, "Video ${video.url} already completed, updating database")
                        videoRepository.updateVideo(video.copy(
                            status = ProcessingStatus.COMPLETED,
                            progress = 100,
                            transcript = transcript
                        ))
                        false // Not active - completed
                    } else {
                        Log.w(TAG, "Video ${video.url} marked completed but no transcript")
                        true // Assume active if no transcript
                    }
                }
                "failed" -> {
                    Log.d(TAG, "Video ${video.url} failed, updating database")
                    videoRepository.updateVideo(video.copy(
                        status = ProcessingStatus.FAILED,
                        failureReason = status.error ?: "Transcription failed"
                    ))
                    false // Not active - failed
                }
                "processing", "queued" -> {
                    Log.d(TAG, "Video ${video.url} still ${status.status}, keeping active")
                    true // Still active
                }
                else -> {
                    Log.w(TAG, "Unknown status for video ${video.url}: ${status.status}")
                    true // Assume active for unknown status
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying status for ${video.url}: ${e.message}", e)
            true // Assume active on error
        }
    }
}
