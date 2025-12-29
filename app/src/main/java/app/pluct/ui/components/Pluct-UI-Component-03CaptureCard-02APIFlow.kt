package app.pluct.ui.components

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.PluctCoreAPIDetailedError
import app.pluct.services.PluctCoreAPIUnifiedServiceErrorCache
import app.pluct.core.debug.PluctCoreDebug01LogManager
import app.pluct.notification.PluctNotificationHelper
import app.pluct.services.PluctCoreBackground01TranscriptionWorker
import app.pluct.services.PluctCoreBackground01TranscriptionWorker.Companion.KEY_URL
import app.pluct.services.PluctCoreBackground01TranscriptionWorker.Companion.NOTIFICATION_ID_PROGRESS
import app.pluct.services.PluctCoreBackground01TranscriptionWorkerJobDeduplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pluct-UI-Component-03CaptureCard-02APIFlow - API flow handler
 * Single source of truth for API flow operations.
 */
object PluctUIComponent03CaptureCardAPIFlow {

    /**
     * Handle complete Business Engine API flow with comprehensive logging.
     */
    fun handleCompleteAPIFlow(
        normalizedUrl: String,
        apiService: PluctCoreAPIUnifiedService,
        debugLogManager: PluctCoreDebug01LogManager?,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        context: Context? = null,
        shouldMinimize: Boolean = false
    ) {
        val tag = "CaptureCard"
        Log.d(tag, "Starting complete Business Engine API flow for URL: $normalizedUrl")
        debugLogManager?.logInfo(
            category = "TRANSCRIPTION",
            operation = "processTikTokVideo",
            message = "Flow started",
            details = "URL: $normalizedUrl"
        )

        // If should minimize, start background worker and return early
        if (shouldMinimize && context != null) {
            Log.d(tag, "Starting background transcription for URL: $normalizedUrl")
            
            // Check for existing job before creating new one
            val existingJobId = PluctCoreBackground01TranscriptionWorkerJobDeduplication.checkExistingJob(
                context = context,
                url = normalizedUrl
            )
            
            if (existingJobId != null) {
                Log.d(tag, "Existing job found for URL: $normalizedUrl, jobId: $existingJobId")
                // Merge notifications if needed
                PluctCoreBackground01TranscriptionWorkerJobDeduplication.mergeNotifications(
                    context = context,
                    jobId = existingJobId,
                    url = normalizedUrl
                )
                return // Don't create duplicate job
            }
            
            // Generate unique notification ID based on URL hash
            val notificationId = normalizedUrl.hashCode().and(0x7FFFFFFF) // Ensure positive
            
            // Create or get job (with deduplication)
            val jobId = PluctCoreBackground01TranscriptionWorkerJobDeduplication.createOrGetJob(
                context = context,
                url = normalizedUrl
            ) {
                // Create new job with unique notification ID
                val workRequest = OneTimeWorkRequestBuilder<PluctCoreBackground01TranscriptionWorker>()
                    .setInputData(workDataOf(
                        KEY_URL to normalizedUrl,
                        PluctCoreBackground01TranscriptionWorker.KEY_NOTIFICATION_ID to notificationId
                    ))
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .addTag("transcription")
                    .build()
                
                WorkManager.getInstance(context).enqueue(workRequest)
                workRequest.id.toString()
            }
            
            Log.d(tag, "Background worker job created/enqueued: $jobId, notificationId: $notificationId")
            
            // Show immediate notification with unique ID
            PluctNotificationHelper.showTranscriptionProgressNotification(
                context = context,
                url = normalizedUrl,
                progress = 0,
                message = "Starting transcription...",
                notificationId = notificationId
            )
            
            // Minimize app (move to background)
            if (context is Activity) {
                (context as Activity).moveTaskToBack(true)
            }
            
            return // Don't continue with foreground processing
        }

        val cachedError = PluctCoreAPIUnifiedServiceErrorCache.getCachedError(normalizedUrl)
        if (cachedError != null) {
            Log.d(tag, "Found cached error details for URL: $normalizedUrl")
            CoroutineScope(Dispatchers.Main).launch {
                onError(cachedError.getDetailedMessage())
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val startTime = System.currentTimeMillis()
                Log.d(tag, "Calling processTikTokVideo API...")
                Log.d(tag, "URL: $normalizedUrl")

                val result = apiService.processTikTokVideo(normalizedUrl)
                val duration = System.currentTimeMillis() - startTime
                Log.d(tag, "Duration: ${duration}ms")

                if (result.isSuccess) {
                    val status = result.getOrNull()!!
                    Log.d(
                        tag,
                        "Flow successful. status=${status.status}, transcriptChars=${status.transcript?.length ?: 0}, confidence=${status.confidence}, language=${status.language}, duration=${status.duration}"
                    )
                    debugLogManager?.logInfo(
                        category = "TRANSCRIPTION",
                        operation = "processTikTokVideo",
                        message = "Flow completed",
                        details = "Status=${status.status}; Duration=${duration}ms"
                    )
                    withContext(Dispatchers.Main) {
                        onSuccess("Transcription completed successfully!")
                    }
                } else {
                    val error = result.exceptionOrNull()
                    val errorMessage = when (error) {
                        is PluctCoreAPIDetailedError -> {
                            Log.e(tag, "Complete API flow failed with detailed error", error)
                            PluctCoreAPIUnifiedServiceErrorCache.cacheError(normalizedUrl, error)
                            // Log to debug system
                            debugLogManager?.logAPIError(error, "TRANSCRIPTION")
                            if (error.technicalDetails.responseStatusCode in listOf(401, 402, 403, 500) &&
                                error.technicalDetails.errorType.contains("AUTH", ignoreCase = true)
                            ) {
                                "Session expired. We refreshed your access; please retry."
                            } else if (error.userMessage.contains("Circuit breaker", ignoreCase = true)) {
                                "Service is cooling down. Please retry in a few seconds."
                            } else if (error.technicalDetails.responseStatusCode == 401 && error.technicalDetails.operation.contains("/ttt/transcribe")) {
                                "We're revalidating your access. Please tap Extract again."
                            } else if (error.userMessage.contains("timed out", ignoreCase = true)) {
                                "Still processing. Please retry in 30 seconds."
                            } else {
                                error.getDetailedMessage()
                            }
                        }
                        else -> {
                            val msg = error?.message ?: "Unknown error"
                            Log.e(tag, "Complete API flow failed: $msg", error)
                            // Log to debug system
                            debugLogManager?.logError(
                                category = "TRANSCRIPTION",
                                operation = "processTikTokVideo",
                                message = msg,
                                exception = error,
                                requestUrl = normalizedUrl
                            )
                            msg
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onError(errorMessage)
                    }
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is PluctCoreAPIDetailedError -> {
                        Log.e(tag, "Exception with detailed error", e)
                        PluctCoreAPIUnifiedServiceErrorCache.cacheError(normalizedUrl, e)
                        // Log to debug system
                        debugLogManager?.logAPIError(e, "TRANSCRIPTION")
                        e.getDetailedMessage()
                    }
                    else -> {
                        val msg = e.message ?: "Unknown error occurred"
                        Log.e(tag, "Exception during complete API flow: $msg", e)
                        // Log to debug system
                        debugLogManager?.logError(
                            category = "TRANSCRIPTION",
                            operation = "processTikTokVideo_exception",
                            message = msg,
                            exception = e,
                            requestUrl = normalizedUrl
                        )
                        msg
                    }
                }
                withContext(Dispatchers.Main) {
                    onError(errorMessage)
                }
            }
        }
    }
}
