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
import app.pluct.ui.components.PluctUIComponent05Notification02Toast01Helper

/**
 * Pluct-UI-Component-03CaptureCard-02APIFlow - API flow handler
 * Single source of truth for API flow operations.
 */
object PluctUIComponent03CaptureCardAPIFlow {
    private const val CAPTURE_CARD_TAG = "CaptureCard"

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
        val tag = CAPTURE_CARD_TAG
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
            
            // Show toast notification when transcription starts
            PluctUIComponent05Notification02Toast01Helper.showTranscriptionStarted(context, normalizedUrl)
            
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
                Log.d(tag, "Calling processTikTokVideo API for URL: $normalizedUrl")
                debugLogManager?.logInfo(
                    category = "TRANSCRIPTION",
                    operation = "processTikTokVideo",
                    message = "Starting transcription flow",
                    details = "URL: $normalizedUrl"
                )

                val result = apiService.processTikTokVideo(normalizedUrl)
                val duration = System.currentTimeMillis() - startTime
                Log.d(tag, "API call completed in ${duration}ms")

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
                if (error is PluctCoreAPIDetailedError) {
                    PluctCoreAPIUnifiedServiceErrorCache.cacheError(normalizedUrl, error)
                }
                val errorMessage = resolveErrorMessage(error, normalizedUrl, debugLogManager, tag)
                withContext(Dispatchers.Main) {
                    onError(errorMessage)
                }
            }
            } catch (e: Exception) {
                if (e is PluctCoreAPIDetailedError) {
                    PluctCoreAPIUnifiedServiceErrorCache.cacheError(normalizedUrl, e)
                }
                val errorMessage = resolveErrorMessage(e, normalizedUrl, debugLogManager, tag)
                withContext(Dispatchers.Main) {
                    onError(errorMessage)
                }
            }
        }
    }

    private fun resolveErrorMessage(
        error: Throwable?,
        normalizedUrl: String,
        debugLogManager: PluctCoreDebug01LogManager?,
        logTag: String
    ): String {
        if (error is PluctCoreAPIDetailedError) {
            Log.e(logTag, "Detailed API error while processing $normalizedUrl", error)
            debugLogManager?.logAPIError(error, "TRANSCRIPTION")
            val statusCode = error.technicalDetails.responseStatusCode
            val userMessage = error.userMessage.orEmpty()
            val operation = error.technicalDetails.operation.orEmpty()
            return when {
                statusCode in listOf(401, 402, 403, 500) &&
                    error.technicalDetails.errorType.contains("AUTH", ignoreCase = true) ->
                    "Session expired. We refreshed your access; please retry."
                userMessage.contains("Circuit breaker", ignoreCase = true) ->
                    "Service is cooling down. Please retry in a few seconds."
                statusCode == 401 && operation.contains("/ttt/transcribe") ->
                    "We're revalidating your access. Please tap Extract again."
                userMessage.contains("timed out", ignoreCase = true) ->
                    "The transcription is taking longer than expected. It may still be processing in the background. Check your recent videos or retry in 30 seconds."
                statusCode == 402 ->
                    "Insufficient credits. Please add credits to continue transcribing videos."
                statusCode == 429 ->
                    "Too many requests. Please wait a moment and try again."
                userMessage.isNotBlank() -> userMessage
                else -> error.getDetailedMessage()
            }
        }

        if (error != null) {
            val msg = error.message ?: "Unknown error occurred while processing the video."
            if (msg.contains("already being processed", ignoreCase = true)) {
                return "This video is already being transcribed. Check your recent videos or wait for it to complete."
            }
            Log.e(logTag, "Complete API flow failed: $msg", error)
            debugLogManager?.logError(
                category = "TRANSCRIPTION",
                operation = "processTikTokVideo",
                message = msg,
                exception = error,
                requestUrl = normalizedUrl
            )
            return msg
        }

        return "Unknown error occurred while processing the video."
    }
}
