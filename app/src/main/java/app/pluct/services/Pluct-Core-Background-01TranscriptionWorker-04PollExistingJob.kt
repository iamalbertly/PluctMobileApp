package app.pluct.services

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker.Result
import androidx.work.workDataOf
import app.pluct.core.error.PluctCoreError01AuthErrorDetector
import app.pluct.services.api.PluctCoreAPITranscriptionResult01Extractor
import app.pluct.ui.components.PluctUIComponent05Notification02Toast01Helper
import kotlinx.coroutines.delay

private const val POLL_TAG = "TranscriptionWorker"

internal data class PluctWorkerPollEnvironment(
    val context: Context,
    val url: String,
    val jobId: String,
    val notificationId: Int,
    val networkMonitor: PluctCoreBackground01TranscriptionWorkerNetworkMonitor,
    val apiService: PluctCoreAPIUnifiedService,
    val videoRepository: app.pluct.data.repository.PluctVideoRepository,
    val logWorkerTerminalPain: (String, String) -> Unit,
    val showProgressNotification: (String, Int, String, Int) -> Unit,
    val showCompletionNotification: (String, String, Int) -> Unit,
    val showErrorNotification: (String, String, Int) -> Unit,
    val isNetworkRelatedError: (Throwable?) -> Boolean,
)

internal suspend fun pluctPollExistingTranscriptionJob(env: PluctWorkerPollEnvironment): Result {
    val context = env.context
    val url = env.url
    val jobId = env.jobId
    val notificationId = env.notificationId
    val networkMonitor = env.networkMonitor
    val apiService = env.apiService
    val videoRepository = env.videoRepository
    val showProgressNotification = env.showProgressNotification
    val showCompletionNotification = env.showCompletionNotification
    val showErrorNotification = env.showErrorNotification
    val logWorkerTerminalPain = env.logWorkerTerminalPain
    val isNetworkRelatedError = env.isNetworkRelatedError

    val existingVideo = videoRepository.getVideoByUrl(url)
    if (existingVideo != null) {
        val existingTranscript = existingVideo.transcript
        if (existingVideo.status == app.pluct.data.entity.ProcessingStatus.COMPLETED && existingTranscript != null) {
            Log.d(POLL_TAG, "Video $url already completed in database, returning existing transcript")
            showCompletionNotification(url, existingTranscript, notificationId)
            return Result.success(
                workDataOf(
                    "transcript" to existingVideo.transcript,
                    "job_id" to jobId,
                    "status" to "completed"
                )
            )
        }
        if (existingVideo.status == app.pluct.data.entity.ProcessingStatus.FAILED) {
            Log.d(POLL_TAG, "Video $url already failed in database, skipping poll")
            return Result.failure(workDataOf("error" to "Transcription already failed"))
        }
    }

    val vendResult = apiService.vendToken("background_${System.currentTimeMillis()}")
    if (vendResult.isFailure) {
        val error = vendResult.exceptionOrNull()
        Log.e(POLL_TAG, "Failed to vend token for polling: ${error?.message}", error)
        showErrorNotification(url, "Failed to get authentication token: ${error?.message}", notificationId)
        return Result.failure(workDataOf("error" to "Authentication failed: ${error?.message}"))
    }

    val vendResponse = vendResult.getOrNull()
    var serviceToken = vendResponse?.token
        ?: vendResponse?.serviceToken
        ?: vendResponse?.pollingToken
        ?: run {
            Log.e(POLL_TAG, "No token found in vend response: $vendResponse")
            return Result.failure(workDataOf("error" to "No token in response"))
        }

    var progress = 0
    var consecutiveNetworkFailures = 0
    val maxNetworkRetries = 5

    repeat(60) { _attempt ->
        delay(5000)

        if (networkMonitor.checkAndQueueOnNetworkLoss()) {
            Log.d(POLL_TAG, "Video queued due to network loss, stopping polling")
            showErrorNotification(url, "Network lost. Video queued and will process when connection is restored.", notificationId)
            return Result.failure(workDataOf("error" to "Network lost, video queued"))
        }

        if (!networkMonitor.isNetworkCurrentlyAvailable()) {
            networkMonitor.markNetworkLossDetected()
            consecutiveNetworkFailures++
            Log.w(POLL_TAG, "Network unavailable (attempt $consecutiveNetworkFailures/$maxNetworkRetries), waiting for reconnection...")

            if (networkMonitor.checkAndQueueOnNetworkLoss()) {
                showProgressNotification(url, progress, "Paused - Waiting for connection...", notificationId)
                showErrorNotification(url, "Network lost. Video queued and will process when connection is restored.", notificationId)
                return Result.failure(workDataOf("error" to "Network lost, video queued"))
            }

            showProgressNotification(url, progress, "Paused - Reconnecting...", notificationId)
            return@repeat
        }

        if (consecutiveNetworkFailures > 0) {
            Log.d(POLL_TAG, "Network restored after $consecutiveNetworkFailures failures, resuming polling")
            consecutiveNetworkFailures = 0
            networkMonitor.handleNetworkRestored()
        }

        val statusResult = apiService.checkTranscriptionStatus(jobId, serviceToken)

        if (statusResult.isSuccess) {
            val status = statusResult.getOrNull() ?: run {
                Log.w(POLL_TAG, "Status result was null despite success flag, retrying...")
                return@repeat
            }
            progress = status.progress ?: 0

            val phaseLabel = when {
                status.status == "completed" -> "Complete!"
                status.status == "failed" -> "Failed"
                progress < 15 -> "Preparing..."
                progress < 40 -> "Downloading..."
                progress < 60 -> "Extracting audio..."
                progress < 90 -> "Transcribing..."
                else -> "Finalizing..."
            }
            val phaseMessage = if (progress > 0 && progress < 100) "$progress% - $phaseLabel" else phaseLabel

            showProgressNotification(url, progress, phaseMessage, notificationId)

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
                    val videoForCompletion = videoRepository.getVideoByUrl(url)
                    if (videoForCompletion != null) {
                        val completedVideo = videoForCompletion.copy(
                            status = app.pluct.data.entity.ProcessingStatus.COMPLETED,
                            progress = 100,
                            transcript = transcriptText,
                            jobId = jobId,
                            transcriptCachedAt = System.currentTimeMillis()
                        )
                        videoRepository.insertVideo(completedVideo)
                        Log.d(POLL_TAG, "Updated video to completed status with cache timestamp")
                    }

                    showCompletionNotification(url, transcriptText, notificationId)

                    PluctUIComponent05Notification02Toast01Helper.showTranscriptionFinished(
                        context = context,
                        url = url,
                        success = true
                    )

                    return Result.success(
                        workDataOf(
                            "transcript" to transcriptText,
                            "job_id" to jobId,
                            "status" to "completed"
                        )
                    )
                }
            } else if (status.status == "failed") {
                PluctUIComponent05Notification02Toast01Helper.showTranscriptionFinished(
                    context = context,
                    url = url,
                    success = false
                )

                val videoForFailure = videoRepository.getVideoByUrl(url)
                if (videoForFailure != null) {
                    val failedVideo = videoForFailure.copy(
                        status = app.pluct.data.entity.ProcessingStatus.FAILED,
                        failureReason = "Transcription failed"
                    )
                    videoRepository.insertVideo(failedVideo)
                }

                logWorkerTerminalPain("poll_status_failed", url)
                showErrorNotification(url, "Transcription failed", notificationId)
                return Result.failure(workDataOf("error" to "Transcription failed"))
            }
        } else {
            val error = statusResult.exceptionOrNull()
            Log.w(POLL_TAG, "Status check failed (attempt $_attempt/60), continuing to poll: ${error?.message}")
            if (error is PluctCoreAPIDetailedError) {
                Log.w(
                    POLL_TAG,
                    "API Error details - Status: ${error.technicalDetails.responseStatusCode}, Code: ${error.technicalDetails.errorCode}, Type: ${error.technicalDetails.errorType}"
                )
            }

            val isNetworkRelated = isNetworkRelatedError(error)

            val is5xxServerError = (error is PluctCoreAPIDetailedError &&
                error.technicalDetails.responseStatusCode >= 500) ||
                error?.message?.contains("500") == true ||
                error?.message?.contains("502") == true ||
                error?.message?.contains("503") == true ||
                error?.message?.contains("504") == true

            if (is5xxServerError) {
                val backoffDelay = app.pluct.core.checks.PluctCoreChecks01RetryabilityDecider
                    .calculateRetryDelayMs(_attempt + 1)
                Log.w(POLL_TAG, "5xx server error detected, applying backoff delay: ${backoffDelay}ms")
                showProgressNotification(url, progress, "Server busy - Retrying in ${backoffDelay / 1000}s...", notificationId)
                delay(backoffDelay)
            } else if (isNetworkRelated) {
                networkMonitor.markNetworkLossDetected()
                consecutiveNetworkFailures++
                Log.w(POLL_TAG, "Network-related API error (attempt $consecutiveNetworkFailures/$maxNetworkRetries)")
                if (networkMonitor.checkAndQueueOnNetworkLoss()) {
                    showProgressNotification(url, progress, "Paused - Waiting for connection...", notificationId)
                    showErrorNotification(url, "Network lost. Video queued and will process when connection is restored.", notificationId)
                    return Result.failure(workDataOf("error" to "Network lost, video queued"))
                }
                showProgressNotification(url, progress, "Connection issue - Retrying...", notificationId)
            }

            if (PluctCoreError01AuthErrorDetector.is401Unauthorized(error)) {
                Log.w(POLL_TAG, "401 during status poll, vending fresh service token")
                val refreshResult = apiService.vendToken("background_refresh_${System.currentTimeMillis()}")
                if (refreshResult.isSuccess) {
                    val refreshResponse = refreshResult.getOrNull()
                    val newToken = refreshResponse?.token
                        ?: refreshResponse?.serviceToken
                        ?: refreshResponse?.pollingToken
                    if (newToken != null) {
                        serviceToken = newToken
                        return@repeat
                    }
                }
            }
        }
    }

    showErrorNotification(url, "Still working on it - check back soon!", notificationId)
    return Result.failure(workDataOf("error" to "Timeout - processing may still complete"))
}
