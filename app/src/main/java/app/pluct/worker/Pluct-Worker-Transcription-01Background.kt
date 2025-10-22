package app.pluct.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import app.pluct.data.api.PluctBusinessEngineClient
import app.pluct.data.transcription.PluctTranscriptionProgressTracker
import app.pluct.data.transcription.TranscriptionStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * Pluct-Worker-Transcription-01Background - Background transcription worker
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@HiltWorker
class PluctTranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val businessEngineClient: PluctBusinessEngineClient,
    private val progressTracker: PluctTranscriptionProgressTracker
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val videoId = inputData.getString(KEY_VIDEO_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val jwtToken = inputData.getString(KEY_JWT_TOKEN) ?: return Result.failure()
        
        return try {
            // Start tracking
            progressTracker.startTracking(jobId, videoId)
            
            // Update to processing
            progressTracker.updateProgress(jobId, 10, TranscriptionStatus.PROCESSING)
            
            // Get short-lived token
            val tokenResult = businessEngineClient.vendToken(jwtToken, "worker_${System.currentTimeMillis()}")
            if (tokenResult.isFailure) {
                progressTracker.failTranscription(jobId, "Failed to get short-lived token")
                return Result.failure()
            }
            
            val shortLivedToken = tokenResult.getOrNull()?.token ?: return Result.failure()
            progressTracker.updateProgress(jobId, 30, TranscriptionStatus.PROCESSING)
            
            // Submit transcription
            val transcriptionResult = businessEngineClient.submitTranscription(shortLivedToken, url)
            if (transcriptionResult.isFailure) {
                progressTracker.failTranscription(jobId, "Failed to submit transcription")
                return Result.failure()
            }
            
            progressTracker.updateProgress(jobId, 50, TranscriptionStatus.PROCESSING)
            
            // Simulate processing time (in real implementation, this would poll the status)
            delay(5000) // 5 seconds simulation
            
            progressTracker.updateProgress(jobId, 80, TranscriptionStatus.PROCESSING)
            
            // Simulate completion
            delay(2000) // 2 seconds simulation
            
            // Complete transcription
            progressTracker.completeTranscription(jobId, "Sample transcript for $url")
            
            Result.success()
        } catch (e: Exception) {
            progressTracker.failTranscription(jobId, e.message ?: "Unknown error")
            Result.failure()
        }
    }
    
    companion object {
        const val KEY_JOB_ID = "job_id"
        const val KEY_VIDEO_ID = "video_id"
        const val KEY_URL = "url"
        const val KEY_JWT_TOKEN = "jwt_token"
        
        fun createWorkRequest(
            jobId: String,
            videoId: String,
            url: String,
            jwtToken: String
        ): OneTimeWorkRequest {
            val inputData = Data.Builder()
                .putString(KEY_JOB_ID, jobId)
                .putString(KEY_VIDEO_ID, videoId)
                .putString(KEY_URL, url)
                .putString(KEY_JWT_TOKEN, jwtToken)
                .build()
            
            return OneTimeWorkRequestBuilder<PluctTranscriptionWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1,
                    TimeUnit.MINUTES
                )
                .build()
        }
    }
}
