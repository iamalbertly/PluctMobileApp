package app.pluct.services

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker.Result
import androidx.work.workDataOf
import app.pluct.services.api.PluctCoreAPITranscriptionResult01Extractor
import kotlinx.coroutines.coroutineScope

private const val PROCESS_TAG = "TranscriptionWorker"

internal data class PluctWorkerProcessNewEnvironment(
    val context: Context,
    val url: String,
    val notificationId: Int,
    val networkMonitor: PluctCoreBackground01TranscriptionWorkerNetworkMonitor,
    val apiService: PluctCoreAPIUnifiedService,
    val videoRepository: app.pluct.data.repository.PluctVideoRepository,
    val logWorkerTerminalPain: (String, String) -> Unit,
    val showProgressNotification: (String, Int, String, Int) -> Unit,
    val showCompletionNotification: (String, String, Int) -> Unit,
    val showErrorNotification: (String, String, Int) -> Unit,
)

internal suspend fun pluctProcessNewTranscription(env: PluctWorkerProcessNewEnvironment): Result {
    val context = env.context
    val url = env.url
    val notificationId = env.notificationId
    val networkMonitor = env.networkMonitor
    val apiService = env.apiService
    val videoRepository = env.videoRepository
    val logWorkerTerminalPain = env.logWorkerTerminalPain
    val showProgressNotification = env.showProgressNotification
    val showCompletionNotification = env.showCompletionNotification
    val showErrorNotification = env.showErrorNotification

    val existingVideo = videoRepository.getVideoByUrl(url)
    if (existingVideo != null) {
        if (existingVideo.status == app.pluct.data.entity.ProcessingStatus.PROCESSING) {
            Log.w(PROCESS_TAG, "Video $url already processing, skipping duplicate background worker")
            PluctCoreTranscription01Dedupe01Facade.onDuplicateBackgroundRequest(context, url)
            return Result.failure(workDataOf("error" to "Already processing"))
        }
        if (existingVideo.status == app.pluct.data.entity.ProcessingStatus.COMPLETED && existingVideo.transcript != null) {
            Log.d(PROCESS_TAG, "Video $url already completed, returning existing transcript")
            val cachedTranscript = existingVideo.transcript
            showCompletionNotification(url, cachedTranscript, notificationId)
            return Result.success(
                workDataOf(
                    "transcript" to cachedTranscript,
                    "job_id" to (existingVideo.jobId ?: ""),
                    "status" to "completed",
                    "message" to "This video was already transcribed. Transcript is available in your recent videos."
                )
            )
        }
    }

    val videoForMeta = videoRepository.getVideoByUrl(url)
    if (videoForMeta != null) {
        try {
            val metaRes = apiService.getMetadata(url, timeoutMs = 12_000L)
            val meta = metaRes.getOrNull()
            if (meta != null) {
                val t = meta.title.trim()
                val a = meta.author.trim()
                val th = meta.thumbnail?.trim().orEmpty()
                val d = if (meta.duration > 0) meta.duration.toLong() else videoForMeta.duration
                val desc = meta.description.trim()
                val updated = videoForMeta.copy(
                    title = t.ifBlank { videoForMeta.title },
                    author = a.ifBlank { videoForMeta.author },
                    thumbnailUrl = th.ifBlank { videoForMeta.thumbnailUrl },
                    duration = d,
                    description = if (desc.isNotBlank()) desc else videoForMeta.description
                )
                if (updated != videoForMeta) {
                    videoRepository.insertVideo(updated)
                    Log.d(PROCESS_TAG, "Metadata prefetch (GET /meta) refreshed title/thumbnail for list UI")
                }
            }
        } catch (e: Exception) {
            Log.w(PROCESS_TAG, "Metadata prefetch skipped: ${e.message}")
        }
    }

    showProgressNotification(url, 0, "Starting transcription...", notificationId)

    val result = coroutineScope {
        val heartbeat = launchPluctTranscriptionProgressHeartbeat(
            startProgress = 6,
            onPulse = { p, label ->
                showProgressNotification(url, p, label, notificationId)
            }
        )
        val apiResult = apiService.processTikTokVideo(url, isBackground = true)
        heartbeat.cancel()
        apiResult
    }

    return result.fold(
        onSuccess = { statusResponse ->
            val extraction = PluctCoreAPITranscriptionResult01Extractor.extract(statusResponse)
            val transcriptText = extraction.transcript
            val responseJobId = statusResponse.jobId

            val videoForJobUpdate = videoRepository.getVideoByUrl(url)
            if (videoForJobUpdate != null) {
                val updatedVideo = videoForJobUpdate.copy(jobId = responseJobId)
                videoRepository.insertVideo(updatedVideo)
                Log.d(PROCESS_TAG, "Updated video with jobId: $responseJobId")
            }

            if (!transcriptText.isNullOrBlank()) {
                val completedTranscript = transcriptText
                val currentVideo = videoRepository.getVideoByUrl(url)
                if (currentVideo != null) {
                    val completedVideo = currentVideo.copy(
                        status = app.pluct.data.entity.ProcessingStatus.COMPLETED,
                        progress = 100,
                        transcript = completedTranscript,
                        jobId = responseJobId,
                        transcriptCachedAt = System.currentTimeMillis()
                    )
                    videoRepository.insertVideo(completedVideo)
                    Log.d(PROCESS_TAG, "Updated video to completed status")
                }

                showCompletionNotification(url, completedTranscript, notificationId)
                Result.success(
                    workDataOf(
                        "transcript" to completedTranscript,
                        "job_id" to responseJobId,
                        "status" to "completed"
                    )
                )
            } else {
                val videoForFailUpdate = videoRepository.getVideoByUrl(url)
                if (videoForFailUpdate != null) {
                    val failedVideo = videoForFailUpdate.copy(
                        status = app.pluct.data.entity.ProcessingStatus.FAILED,
                        failureReason = "Transcription completed but no transcript found"
                    )
                    videoRepository.insertVideo(failedVideo)
                }

                logWorkerTerminalPain("no_transcript_after_complete", url)
                showErrorNotification(url, "Transcription completed but no transcript found", notificationId)
                Result.failure(workDataOf("error" to "No transcript found"))
            }
        },
        onFailure = { error ->
            if (isNetworkRelatedErrorForWorker(error)) {
                networkMonitor.markNetworkLossDetected()
                if (networkMonitor.checkAndQueueOnNetworkLoss()) {
                    showErrorNotification(url, "Network lost. Video queued and will process when connection is restored.", notificationId)
                    return@fold Result.failure(workDataOf("error" to "Network lost, video queued"))
                }
            }

            val currentVideo = videoRepository.getVideoByUrl(url)
            if (currentVideo != null) {
                val failedVideo = currentVideo.copy(
                    status = app.pluct.data.entity.ProcessingStatus.FAILED,
                    failureReason = error.message ?: "Transcription failed"
                )
                videoRepository.insertVideo(failedVideo)
            }

            logWorkerTerminalPain("process_new_transcription: ${error.javaClass.simpleName}", url)
            showErrorNotification(url, error.message ?: "Transcription failed", notificationId)
            Result.failure(workDataOf("error" to (error.message ?: "Transcription failed")))
        }
    )
}

private fun isNetworkRelatedErrorForWorker(error: Throwable?): Boolean {
    val message = error?.message ?: return false
    return message.contains("network", ignoreCase = true) ||
        message.contains("connection", ignoreCase = true) ||
        message.contains("timeout", ignoreCase = true) ||
        message.contains("unable to resolve host", ignoreCase = true) ||
        message.contains("failed to connect", ignoreCase = true)
}
