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
     * Resume incomplete transcriptions
     * Uses jobId stored in VideoItem to resume polling for existing transcription jobs
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
            
            // For each processing video, attempt to resume polling
            processingVideos.forEach { video ->
                Log.d(TAG, "Attempting to resume transcription for: ${video.url}")
                
                // Generate unique notification ID based on URL hash
                val notificationId = video.url.hashCode().and(0x7FFFFFFF) // Ensure positive
                
                if (video.jobId != null && video.jobId.isNotBlank()) {
                    // If jobId is available, use it to resume polling
                    Log.d(TAG, "Resuming with jobId: ${video.jobId}, notificationId: $notificationId")
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
                } else {
                    // Fallback: Schedule worker without jobId (will start new transcription)
                    Log.w(TAG, "No jobId available for video: ${video.url}, scheduling new transcription")
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
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming incomplete transcriptions: ${e.message}", e)
        }
    }
}

