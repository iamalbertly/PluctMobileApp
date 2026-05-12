package app.pluct.services

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.pluct.core.debug.PluctCoreDebug01LogManager
import app.pluct.notification.PluctNotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Background transcription processing worker (WorkManager contract only; logic in [pluctProcessNewTranscription] / [pluctPollExistingTranscriptionJob]).
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

    private val apiService: PluctCoreAPIUnifiedService by lazy { entryPoint.apiService() }

    private val videoRepository: app.pluct.data.repository.PluctVideoRepository by lazy {
        entryPoint.videoRepository()
    }

    private val queueManager: PluctQueueManager by lazy { entryPoint.queueManager() }

    private val debugLogManager: PluctCoreDebug01LogManager by lazy { entryPoint.debugLogManager() }

    private fun logWorkerTerminalPain(reason: String, url: String) {
        val fp = (url.hashCode() and 0x7fff_ffff).toString(16)
        Log.i("PluctUserPain", "worker_terminal reason=$reason fp=$fp")
        try {
            debugLogManager.logError(
                category = "BACKGROUND",
                operation = "TranscriptionWorker",
                message = reason,
                requestUrl = url.take(200)
            )
        } catch (e: Exception) {
            Log.w(TAG, "debugLogManager.logError skipped: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "TranscriptionWorker"
        const val KEY_URL = "url"
        const val KEY_JOB_ID = "job_id"
        const val KEY_NOTIFICATION_ID = "notification_id"

        const val NOTIFICATION_ID_PROGRESS = 1000
    }

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val url = inputData.getString(KEY_URL) ?: return androidx.work.ListenableWorker.Result.failure()
        val jobId = inputData.getString(KEY_JOB_ID)
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, NOTIFICATION_ID_PROGRESS)

        Log.d(TAG, "Starting background transcription for URL: $url, JobId: $jobId")

        val networkMonitor = PluctCoreBackground01TranscriptionWorkerNetworkMonitor(
            context = context,
            url = url,
            videoRepository = videoRepository,
            queueManager = queueManager
        )
        networkMonitor.startMonitoring()

        return try {
            val result = if (jobId != null) {
                pollExistingJob(jobId, url, notificationId, networkMonitor)
            } else {
                processNewTranscription(url, notificationId, networkMonitor)
            }
            networkMonitor.stopMonitoring()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Background transcription failed: ${e.message}", e)
            networkMonitor.stopMonitoring()
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
            logWorkerTerminalPain("doWork_exception: ${e.javaClass.simpleName}", url)
            showErrorNotification(url, errorMessage, notificationId)
            androidx.work.ListenableWorker.Result.failure(workDataOf("error" to errorMessage))
        }
    }

    private suspend fun processNewTranscription(
        url: String,
        notificationId: Int,
        networkMonitor: PluctCoreBackground01TranscriptionWorkerNetworkMonitor
    ): androidx.work.ListenableWorker.Result {
        return pluctProcessNewTranscription(
            PluctWorkerProcessNewEnvironment(
                context = context,
                url = url,
                notificationId = notificationId,
                networkMonitor = networkMonitor,
                apiService = apiService,
                videoRepository = videoRepository,
                logWorkerTerminalPain = { reason, u -> logWorkerTerminalPain(reason, u) },
                showProgressNotification = { u, p, m, id -> showProgressNotification(u, p, m, id) },
                showCompletionNotification = { u, t, id -> showCompletionNotification(u, t, id) },
                showErrorNotification = { u, e, id -> showErrorNotification(u, e, id) }
            )
        )
    }

    private suspend fun pollExistingJob(
        jobId: String,
        url: String,
        notificationId: Int,
        networkMonitor: PluctCoreBackground01TranscriptionWorkerNetworkMonitor
    ): androidx.work.ListenableWorker.Result {
        return pluctPollExistingTranscriptionJob(
            PluctWorkerPollEnvironment(
                context = context,
                url = url,
                jobId = jobId,
                notificationId = notificationId,
                networkMonitor = networkMonitor,
                apiService = apiService,
                videoRepository = videoRepository,
                logWorkerTerminalPain = { reason, u -> logWorkerTerminalPain(reason, u) },
                showProgressNotification = { u, p, m, id -> showProgressNotification(u, p, m, id) },
                showCompletionNotification = { u, t, id -> showCompletionNotification(u, t, id) },
                showErrorNotification = { u, e, id -> showErrorNotification(u, e, id) },
                isNetworkRelatedError = { err -> isNetworkRelatedError(err) }
            )
        )
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
        val nid = inputData.getInt(KEY_NOTIFICATION_ID, NOTIFICATION_ID_PROGRESS)
        return ForegroundInfo(
            nid,
            PluctNotificationHelper.createProgressNotification(
                context = context,
                url = url,
                progress = 0,
                message = "Starting transcription...",
                notificationRequestCode = nid
            )
        )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkManagerEntryPoint {
    fun apiService(): PluctCoreAPIUnifiedService
    fun videoRepository(): app.pluct.data.repository.PluctVideoRepository
    fun queueManager(): PluctQueueManager
    fun debugLogManager(): PluctCoreDebug01LogManager
}
