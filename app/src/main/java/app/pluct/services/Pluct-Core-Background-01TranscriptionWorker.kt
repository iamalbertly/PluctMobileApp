package app.pluct.services

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.pluct.notification.PluctNotificationHelper
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.PluctCoreBackground01TranscriptionWorkerNetworkMonitor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay

/**
 * Entry point for accessing Hilt dependencies in WorkManager
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkManagerEntryPoint {
    fun apiService(): PluctCoreAPIUnifiedService
    fun videoRepository(): app.pluct.data.repository.PluctVideoRepository
    fun queueManager(): app.pluct.services.PluctQueueManager
}

/**
 * Pluct-Core-Background-01TranscriptionWorker - Background transcription processing worker
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Handles transcription processing in the background with progress notifications
 */
class PluctCoreBackground01TranscriptionWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val entryPoint: WorkManagerEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WorkManagerEntryPoint::class.java
        )
    }
    
    private val apiService: PluctCoreAPIUnifiedService by lazy {
        entryPoint.apiService()
    }
    
    private val videoRepository: app.pluct.data.repository.PluctVideoRepository by lazy {
        entryPoint.videoRepository()
    }
    
    private val queueManager: app.pluct.services.PluctQueueManager by lazy {
        entryPoint.queueManager()
    }

    companion object {
        private const val TAG = "TranscriptionWorker"
        const val KEY_URL = "url"
        const val KEY_JOB_ID = "job_id"
        const val KEY_NOTIFICATION_ID = "notification_id"
        
        // Notification IDs
        const val NOTIFICATION_ID_PROGRESS = 1000
        private const val NOTIFICATION_ID_COMPLETE = 1001
        private const val NOTIFICATION_ID_ERROR = 1002
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val jobId = inputData.getString(KEY_JOB_ID)
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, NOTIFICATION_ID_PROGRESS)
        
        Log.d(TAG, "Starting background transcription for URL: $url, JobId: $jobId")
        
        // Start network monitoring
        val networkMonitor = PluctCoreBackground01TranscriptionWorkerNetworkMonitor(
            context = context,
            url = url,
            videoRepository = videoRepository,
            queueManager = queueManager
        )
        networkMonitor.startMonitoring()
        
        return try {
            // Process transcription in background
            val result = if (jobId != null) {
                // If jobId exists, poll for completion
                pollExistingJob(jobId, url, notificationId)
            } else {
                // Start new transcription
                processNewTranscription(url, notificationId)
            }
            
            // Stop network monitoring on completion
            networkMonitor.stopMonitoring()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Background transcription failed: ${e.message}", e)
            networkMonitor.stopMonitoring()
            showErrorNotification(url, e.message ?: "Unknown error", notificationId)
            Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        }
    }
    
    private suspend fun processNewTranscription(url: String, notificationId: Int): Result {
        // Create video entry in database with PROCESSING status
        val videoId = System.currentTimeMillis().toString()
        val processingVideo = app.pluct.data.entity.VideoItem(
            id = videoId,
            url = url,
            title = "Processing...",
            thumbnailUrl = "",
            author = "",
            duration = 0L,
            status = app.pluct.data.entity.ProcessingStatus.PROCESSING,
            progress = 0,
            transcript = null,
            timestamp = System.currentTimeMillis(),
            tier = app.pluct.data.entity.ProcessingTier.EXTRACT_SCRIPT,
            createdAt = System.currentTimeMillis()
        )
        
        // Insert video into database
        val insertResult = videoRepository.insertVideo(processingVideo)
        if (insertResult.isFailure) {
            Log.w(TAG, "Failed to create video entry in database: ${insertResult.exceptionOrNull()?.message}")
        } else {
            Log.d(TAG, "Created video entry in database: $videoId for URL: $url")
        }
        
        // Show initial progress notification
        showProgressNotification(url, 0, "Starting transcription...", notificationId)
        
        // Process transcription with background flag
        val result = apiService.processTikTokVideo(url, isBackground = true)
        
        return result.fold(
            onSuccess = { statusResponse ->
                val transcript = statusResponse.transcript ?: statusResponse.result?.transcription ?: ""
                val jobId = statusResponse.jobId
                
                // Update video entry with jobId if available
                if (jobId != null) {
                    val existingVideo = videoRepository.getVideoByUrl(url)
                    if (existingVideo != null) {
                        val updatedVideo = existingVideo.copy(jobId = jobId)
                        videoRepository.insertVideo(updatedVideo)
                        Log.d(TAG, "Updated video with jobId: $jobId")
                    }
                }
                
                if (transcript.isNotEmpty()) {
                    // Update video entry with completed status and transcript
                    val currentVideo = videoRepository.getVideoByUrl(url)
                    if (currentVideo != null) {
                        val completedVideo = currentVideo.copy(
                            status = app.pluct.data.entity.ProcessingStatus.COMPLETED,
                            progress = 100,
                            transcript = transcript,
                            jobId = jobId
                        )
                        videoRepository.insertVideo(completedVideo)
                        Log.d(TAG, "Updated video to completed status")
                    }
                    
                    showCompletionNotification(url, transcript, notificationId)
                    Result.success(workDataOf(
                        "transcript" to transcript,
                        "job_id" to (jobId ?: ""),
                        "status" to "completed"
                    ))
                } else {
                    // Update video entry with failed status
                    val existingVideo = videoRepository.getVideoByUrl(url)
                    if (existingVideo != null) {
                        val failedVideo = existingVideo.copy(
                            status = app.pluct.data.entity.ProcessingStatus.FAILED,
                            failureReason = "Transcription completed but no transcript found"
                        )
                        videoRepository.insertVideo(failedVideo)
                    }
                    
                    showErrorNotification(url, "Transcription completed but no transcript found", notificationId)
                    Result.failure(workDataOf("error" to "No transcript found"))
                }
            },
            onFailure = { error ->
                // Update video entry with failed status
                val currentVideo = videoRepository.getVideoByUrl(url)
                if (currentVideo != null) {
                    val failedVideo = currentVideo.copy(
                        status = app.pluct.data.entity.ProcessingStatus.FAILED,
                        failureReason = error.message ?: "Transcription failed"
                    )
                    videoRepository.insertVideo(failedVideo)
                }
                
                showErrorNotification(url, error.message ?: "Transcription failed", notificationId)
                Result.failure(workDataOf("error" to (error.message ?: "Transcription failed")))
            }
        )
    }
    
    private suspend fun pollExistingJob(jobId: String, url: String, notificationId: Int): Result {
        // Get service token (will use cached if available)
        val vendResult = apiService.vendToken("background_${System.currentTimeMillis()}")
        if (vendResult.isFailure) {
            val error = vendResult.exceptionOrNull()
            Log.e(TAG, "Failed to vend token for polling: ${error?.message}", error)
            showErrorNotification(url, "Failed to get authentication token: ${error?.message}", notificationId)
            return Result.failure(workDataOf("error" to "Authentication failed: ${error?.message}"))
        }
        
        val vendResponse = vendResult.getOrNull()
        val serviceToken = vendResponse?.token 
            ?: vendResponse?.serviceToken 
            ?: vendResponse?.pollingToken
            ?: run {
                Log.e(TAG, "No token found in vend response: $vendResponse")
                return Result.failure(workDataOf("error" to "No token in response"))
            }
        
        var progress = 0
        
        // Poll for completion with progress updates
        repeat(60) { _attempt -> // Max 60 attempts (5 minutes with 5s intervals)
            delay(5000) // 5 second intervals
            
            val statusResult = apiService.checkTranscriptionStatus(jobId, serviceToken)
            
            if (statusResult.isSuccess) {
                val status = statusResult.getOrNull()!!
                progress = status.progress ?: 0
                
                // Update progress notification
                val phaseMessage = when {
                    status.status == "completed" -> "Transcription complete!"
                    status.status == "failed" -> "Transcription failed"
                    progress < 15 -> "Preparing transcription..."
                    progress < 40 -> "Downloading video..."
                    progress < 60 -> "Extracting audio..."
                    progress < 90 -> "Transcribing with AI..."
                    else -> "Finalizing..."
                }
                
                showProgressNotification(url, progress, phaseMessage, notificationId)
                
                // Update video entry in database with current progress
                val currentVideo = videoRepository.getVideoByUrl(url)
                if (currentVideo != null) {
                    val updatedVideo = currentVideo.copy(
                        progress = progress,
                        jobId = jobId
                    )
                    videoRepository.insertVideo(updatedVideo)
                }
                
                if (status.status == "completed") {
                    val transcript = status.transcript ?: status.result?.transcription ?: ""
                    if (transcript.isNotEmpty()) {
                        // Update video entry with completed status
                        val existingVideo = videoRepository.getVideoByUrl(url)
                        if (existingVideo != null) {
                            val completedVideo = existingVideo.copy(
                                status = app.pluct.data.entity.ProcessingStatus.COMPLETED,
                                progress = 100,
                                transcript = transcript,
                                jobId = jobId
                            )
                            videoRepository.insertVideo(completedVideo)
                            Log.d(TAG, "Updated video to completed status")
                        }
                        
                        showCompletionNotification(url, transcript, notificationId)
                        return Result.success(workDataOf(
                            "transcript" to transcript,
                            "job_id" to jobId,
                            "status" to "completed"
                        ))
                    }
                } else if (status.status == "failed") {
                    // Update video entry with failed status
                    val existingVideo = videoRepository.getVideoByUrl(url)
                    if (existingVideo != null) {
                        val failedVideo = existingVideo.copy(
                            status = app.pluct.data.entity.ProcessingStatus.FAILED,
                            failureReason = "Transcription failed"
                        )
                        videoRepository.insertVideo(failedVideo)
                    }
                    
                    showErrorNotification(url, "Transcription failed", notificationId)
                    return Result.failure(workDataOf("error" to "Transcription failed"))
                }
            } else {
                // Log error details for debugging
                val error = statusResult.exceptionOrNull()
                Log.w(TAG, "Status check failed (attempt $_attempt/60), continuing to poll: ${error?.message}")
                if (error is app.pluct.services.PluctCoreAPIDetailedError) {
                    Log.w(TAG, "API Error details - Status: ${error.technicalDetails.responseStatusCode}, Code: ${error.technicalDetails.errorCode}, Type: ${error.technicalDetails.errorType}")
                }
                
                // If it's a 401, try refreshing the token
                if (error?.message?.contains("401", ignoreCase = true) == true || 
                    error?.message?.contains("authentication", ignoreCase = true) == true) {
                    Log.w(TAG, "Authentication error detected, attempting to refresh token")
                    val refreshResult = apiService.vendToken("background_refresh_${System.currentTimeMillis()}")
                    if (refreshResult.isSuccess) {
                        val refreshResponse = refreshResult.getOrNull()
                        val newToken = refreshResponse?.token 
                            ?: refreshResponse?.serviceToken 
                            ?: refreshResponse?.pollingToken
                        if (newToken != null) {
                            // Continue with new token on next iteration
                            return@repeat
                        }
                    }
                }
            }
        }
        
        // Timeout
        showErrorNotification(url, "Transcription timed out", notificationId)
        return Result.failure(workDataOf("error" to "Timeout"))
    }
    
    private fun showProgressNotification(url: String, progress: Int, message: String, notificationId: Int) {
        PluctNotificationHelper.showTranscriptionProgressNotification(
            context = context,
            url = url,
            progress = progress,
            message = message,
            notificationId = notificationId
        )
    }
    
    private fun showCompletionNotification(url: String, transcript: String, notificationId: Int) {
        PluctNotificationHelper.showTranscriptionCompleteNotification(
            context = context,
            url = url,
            transcript = transcript,
            notificationId = notificationId
        )
    }
    
    private fun showErrorNotification(url: String, error: String, notificationId: Int) {
        PluctNotificationHelper.showTranscriptionErrorNotification(
            context = context,
            url = url,
            error = error,
            notificationId = notificationId
        )
    }
    
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val url = inputData.getString(KEY_URL) ?: "Unknown URL"
        return ForegroundInfo(
            NOTIFICATION_ID_PROGRESS,
            PluctNotificationHelper.createProgressNotification(
                context = context,
                url = url,
                progress = 0,
                message = "Starting transcription..."
            )
        )
    }
}

