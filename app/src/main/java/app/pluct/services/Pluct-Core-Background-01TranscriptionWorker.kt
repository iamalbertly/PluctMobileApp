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
import app.pluct.data.entity.ProcessingTier
import app.pluct.services.api.PluctCoreAPITranscriptionResult01Extractor
import app.pluct.ui.components.PluctUIComponent05Notification02Toast01Helper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
                pollExistingJob(jobId, url, notificationId, networkMonitor)
            } else {
                // Start new transcription
                processNewTranscription(url, notificationId, networkMonitor)
            }
            
            // Stop network monitoring on completion
            networkMonitor.stopMonitoring()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Background transcription failed: ${e.message}", e)
            networkMonitor.stopMonitoring()
            // Provide more helpful error message
            val errorMessage = when {
                e.message?.contains("already being processed", ignoreCase = true) == true ->
                    "This video is already being transcribed. Check your recent videos for progress."
                e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("connection", ignoreCase = true) == true ->
                    "Network error. The video has been queued and will be processed when your connection is restored."
                e.message?.contains("insufficient", ignoreCase = true) == true ||
                e.message?.contains("credits", ignoreCase = true) == true ->
                    "Insufficient credits. Please add credits to continue transcribing videos."
                else -> e.message ?: "Transcription failed. Please try again."
            }
            showErrorNotification(url, errorMessage, notificationId)
            Result.failure(workDataOf("error" to errorMessage))
        }
    }
    
    private suspend fun processNewTranscription(url: String, notificationId: Int, networkMonitor: PluctCoreBackground01TranscriptionWorkerNetworkMonitor): Result {
        // CRITICAL FIX #2: Check for existing video before creating duplicate
        val existingVideo = videoRepository.getVideoByUrl(url)
        if (existingVideo != null) {
            if (existingVideo.status == app.pluct.data.entity.ProcessingStatus.PROCESSING) {
                Log.w(TAG, "Video $url already processing, skipping duplicate background worker")
                return Result.failure(workDataOf("error" to "Already processing"))
            }
            if (existingVideo.status == app.pluct.data.entity.ProcessingStatus.COMPLETED && existingVideo.transcript != null) {
                Log.d(TAG, "Video $url already completed, returning existing transcript")
                // TECH DEBT #1: Use safe let instead of !! assertion
                val cachedTranscript = existingVideo.transcript
                showCompletionNotification(url, cachedTranscript, notificationId)
                return Result.success(workDataOf(
                    "transcript" to cachedTranscript,
                    "job_id" to (existingVideo.jobId ?: ""),
                    "status" to "completed",
                    "message" to "This video was already transcribed. Transcript is available in your recent videos."
                ))
            }
        }
        
        // Show initial progress notification
        showProgressNotification(url, 0, "Starting transcription...", notificationId)
        
        // Process transcription with a heartbeat so the notification never appears frozen.
        // Deduplication is handled internally by apiService.processTikTokVideo() via unified coordinator
        val result = coroutineScope {
            val heartbeat = launch {
                val stagedProgress = listOf(
                    6 to "Video -> Text",
                    14 to "Link -> Ready",
                    24 to "Video -> Audio",
                    38 to "Audio -> Text",
                    52 to "Text -> Soon",
                    68 to "Almost done"
                )
                for ((stageProgress, label) in stagedProgress) {
                    delay(4000)
                    if (!isActive) return@launch
                    showProgressNotification(url, stageProgress, label, notificationId)
                }
            }
            val apiResult = apiService.processTikTokVideo(url, isBackground = true)
            heartbeat.cancel()
            apiResult
        }
        
        return result.fold(
            onSuccess = { statusResponse ->
                val extraction = PluctCoreAPITranscriptionResult01Extractor.extract(statusResponse)
                val transcriptText = extraction.transcript
                val responseJobId = statusResponse.jobId // TECH DEBT #3: Non-null per data model

                // Update video entry with jobId (always available per data model)
                val videoForJobUpdate = videoRepository.getVideoByUrl(url)
                if (videoForJobUpdate != null) {
                    val updatedVideo = videoForJobUpdate.copy(jobId = responseJobId)
                    videoRepository.insertVideo(updatedVideo)
                    Log.d(TAG, "Updated video with jobId: $responseJobId")
                }
                
                if (!transcriptText.isNullOrBlank()) {
                    val completedTranscript = transcriptText
                    // Update video entry with completed status and transcript
                    val currentVideo = videoRepository.getVideoByUrl(url)
                    if (currentVideo != null) {
                        val completedVideo = currentVideo.copy(
                            status = app.pluct.data.entity.ProcessingStatus.COMPLETED,
                            progress = 100,
                            transcript = completedTranscript,
                            jobId = responseJobId, // TECH DEBT #2: Use renamed variable
                            transcriptCachedAt = System.currentTimeMillis()
                        )
                        videoRepository.insertVideo(completedVideo)
                        Log.d(TAG, "Updated video to completed status")
                    }

                    showCompletionNotification(url, completedTranscript, notificationId)
                    Result.success(workDataOf(
                        "transcript" to completedTranscript,
                        "job_id" to responseJobId, // TECH DEBT #2: Remove redundant Elvis (non-null)
                        "status" to "completed"
                    ))
                } else {
                    // TECH DEBT #2: Renamed to avoid shadowing
                    val videoForFailUpdate = videoRepository.getVideoByUrl(url)
                    if (videoForFailUpdate != null) {
                        val failedVideo = videoForFailUpdate.copy(
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
                if (isNetworkRelatedError(error)) {
                    networkMonitor.markNetworkLossDetected()
                    if (networkMonitor.checkAndQueueOnNetworkLoss()) {
                        showErrorNotification(url, "Network lost. Video queued and will process when connection is restored.", notificationId)
                        return@fold Result.failure(workDataOf("error" to "Network lost, video queued"))
                    }
                }

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
    
    private suspend fun pollExistingJob(jobId: String, url: String, notificationId: Int, networkMonitor: PluctCoreBackground01TranscriptionWorkerNetworkMonitor): Result {
        // UX IMPROVEMENT #5: First verify current status before polling
        val existingVideo = videoRepository.getVideoByUrl(url)
        if (existingVideo != null) {
            val existingTranscript = existingVideo.transcript
            if (existingVideo.status == app.pluct.data.entity.ProcessingStatus.COMPLETED && existingTranscript != null) {
                Log.d(TAG, "Video $url already completed in database, returning existing transcript")
                showCompletionNotification(url, existingTranscript, notificationId)
                return Result.success(workDataOf(
                    "transcript" to existingVideo.transcript,
                    "job_id" to jobId,
                    "status" to "completed"
                ))
            }
            if (existingVideo.status == app.pluct.data.entity.ProcessingStatus.FAILED) {
                Log.d(TAG, "Video $url already failed in database, skipping poll")
                return Result.failure(workDataOf("error" to "Transcription already failed"))
            }
        }
        
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
        // UX IMPROVEMENT: Enhanced polling with pause/resume for network issues
        var consecutiveNetworkFailures = 0
        val maxNetworkRetries = 5 // Allow 5 consecutive network failures before queueing

        repeat(60) { _attempt -> // Max 60 attempts (5 minutes with 5s intervals)
            delay(5000) // 5 second intervals

            // UX IMPROVEMENT #3: Check for network loss and queue proactively
            if (networkMonitor.checkAndQueueOnNetworkLoss()) {
                Log.d(TAG, "Video queued due to network loss, stopping polling")
                showErrorNotification(url, "Network lost. Video queued and will process when connection is restored.", notificationId)
                return Result.failure(workDataOf("error" to "Network lost, video queued"))
            }

            // Check if network is available before making API call
            // PAUSE/RESUME: Wait for network instead of immediately failing
            if (!networkMonitor.isNetworkCurrentlyAvailable()) {
                networkMonitor.markNetworkLossDetected()
                consecutiveNetworkFailures++
                Log.w(TAG, "Network unavailable (attempt $consecutiveNetworkFailures/$maxNetworkRetries), waiting for reconnection...")

                if (networkMonitor.checkAndQueueOnNetworkLoss()) {
                    showProgressNotification(url, progress, "Paused - Waiting for connection...", notificationId)
                    showErrorNotification(url, "Network lost. Video queued and will process when connection is restored.", notificationId)
                    return Result.failure(workDataOf("error" to "Network lost, video queued"))
                }

                // Show paused state in notification
                showProgressNotification(url, progress, "Paused - Reconnecting...", notificationId)
                return@repeat // Skip this iteration, wait for next
            }

            // Network restored - reset failure counter and trigger queue processing
            if (consecutiveNetworkFailures > 0) {
                Log.d(TAG, "Network restored after $consecutiveNetworkFailures failures, resuming polling")
                consecutiveNetworkFailures = 0
                
                // UX FIX #1: Trigger queue processor to handle any queued videos
                networkMonitor.handleNetworkRestored()
            }
            
            val statusResult = apiService.checkTranscriptionStatus(jobId, serviceToken)

            if (statusResult.isSuccess) {
                // TECH DEBT #2: Safe unwrap with continue instead of !! assertion
                val status = statusResult.getOrNull() ?: run {
                    Log.w(TAG, "Status result was null despite success flag, retrying...")
                    return@repeat
                }
                progress = status.progress ?: 0
                
                // UX FIX #2: Update progress notification with percentage in message
                val phaseLabel = when {
                    status.status == "completed" -> "Complete!"
                    status.status == "failed" -> "Failed"
                    progress < 15 -> "Preparing..."
                    progress < 40 -> "Downloading..."
                    progress < 60 -> "Extracting audio..."
                    progress < 90 -> "Transcribing..."
                    else -> "Finalizing..."
                }
                // Show percentage in notification text for user awareness
                val phaseMessage = if (progress > 0 && progress < 100) "$progress% - $phaseLabel" else phaseLabel

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
                    val extraction = PluctCoreAPITranscriptionResult01Extractor.extract(status)
                    val transcriptText = extraction.transcript
                    if (!transcriptText.isNullOrBlank()) {
                        // Update video entry with completed status
                        // TECH DEBT #1: Renamed to avoid shadowing outer existingVideo
                        val videoForCompletion = videoRepository.getVideoByUrl(url)
                        if (videoForCompletion != null) {
                            // UX FIX #5: Set cache timestamp for invalidation tracking
                            val completedVideo = videoForCompletion.copy(
                                status = app.pluct.data.entity.ProcessingStatus.COMPLETED,
                                progress = 100,
                                transcript = transcriptText,
                                jobId = jobId,
                                transcriptCachedAt = System.currentTimeMillis()
                            )
                            videoRepository.insertVideo(completedVideo)
                            Log.d(TAG, "Updated video to completed status with cache timestamp")
                        }
                        
                        showCompletionNotification(url, transcriptText, notificationId)
                        
                        // Show toast notification when transcription completes
                        PluctUIComponent05Notification02Toast01Helper.showTranscriptionFinished(
                            context = context,
                            url = url,
                            success = true
                        )
                        
                        return Result.success(workDataOf(
                            "transcript" to transcriptText,
                            "job_id" to jobId,
                            "status" to "completed"
                        ))
                    }
                } else if (status.status == "failed") {
                    // Show toast notification when transcription fails
                    PluctUIComponent05Notification02Toast01Helper.showTranscriptionFinished(
                        context = context,
                        url = url,
                        success = false
                    )

                    // Update video entry with failed status
                    // TECH DEBT #1: Renamed to avoid shadowing outer existingVideo
                    val videoForFailure = videoRepository.getVideoByUrl(url)
                    if (videoForFailure != null) {
                        val failedVideo = videoForFailure.copy(
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

                // UX IMPROVEMENT: Distinguish between network errors and upstream service errors
                val isNetworkRelated = isNetworkRelatedError(error)

                // UX FIX #4: Detect 5xx server errors for exponential backoff retry
                val is5xxServerError = (error is app.pluct.services.PluctCoreAPIDetailedError &&
                    error.technicalDetails.responseStatusCode >= 500) ||
                    error?.message?.contains("500") == true ||
                    error?.message?.contains("502") == true ||
                    error?.message?.contains("503") == true ||
                    error?.message?.contains("504") == true

                if (is5xxServerError) {
                    // Calculate exponential backoff delay using centralized decider
                    val backoffDelay = app.pluct.core.checks.PluctCoreChecks01RetryabilityDecider
                        .calculateRetryDelayMs(_attempt + 1)
                    Log.w(TAG, "5xx server error detected, applying backoff delay: ${backoffDelay}ms")
                    showProgressNotification(url, progress, "Server busy - Retrying in ${backoffDelay/1000}s...", notificationId)
                    kotlinx.coroutines.delay(backoffDelay)
                } else if (isNetworkRelated) {
                    networkMonitor.markNetworkLossDetected()
                    consecutiveNetworkFailures++
                    Log.w(TAG, "Network-related API error (attempt $consecutiveNetworkFailures/$maxNetworkRetries)")
                    if (networkMonitor.checkAndQueueOnNetworkLoss()) {
                        showProgressNotification(url, progress, "Paused - Waiting for connection...", notificationId)
                        showErrorNotification(url, "Network lost. Video queued and will process when connection is restored.", notificationId)
                        return Result.failure(workDataOf("error" to "Network lost, video queued"))
                    }
                    showProgressNotification(url, progress, "Connection issue - Retrying...", notificationId)
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

        // Timeout - friendlier message
        showErrorNotification(url, "Still working on it - check back soon!", notificationId)
        return Result.failure(workDataOf("error" to "Timeout - processing may still complete"))
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

    private fun isNetworkRelatedError(error: Throwable?): Boolean {
        val message = error?.message ?: return false
        return message.contains("network", ignoreCase = true) ||
            message.contains("connection", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("unable to resolve host", ignoreCase = true) ||
            message.contains("failed to connect", ignoreCase = true)
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
