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
import app.pluct.core.error.PluctCoreError03UserMessageFormatter
import app.pluct.core.permission.PluctCorePermission01Manager
import app.pluct.notification.PluctNotificationHelper
import app.pluct.services.PluctCoreTranscription01Dedupe01Facade
import app.pluct.services.PluctCoreBackground01TranscriptionWorker
import app.pluct.services.PluctCoreBackground01TranscriptionWorker.Companion.KEY_URL
import app.pluct.services.PluctCoreBackground01TranscriptionWorker.Companion.NOTIFICATION_ID_PROGRESS
import app.pluct.services.PluctCoreBackground01TranscriptionWorkerJobDeduplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
            if (PluctCoreTranscription01Dedupe01Facade.hasActiveWorkForUrl(context, normalizedUrl)) {
                Log.d(tag, "Existing job for URL: $normalizedUrl (dedupe facade)")
                PluctCoreTranscription01Dedupe01Facade.onDuplicateBackgroundRequest(context, normalizedUrl)
                return
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
            
            // Minimize only when progress can remain visible in the notification shade.
            if (context is Activity && PluctCorePermission01Manager.hasNotificationPermission(context)) {
                context.moveTaskToBack(true)
            } else if (context is Activity) {
                Log.w(tag, "Keeping app foreground because progress notification permission is not ready")
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

        val notificationId = PluctNotificationHelper.generateNotificationId(normalizedUrl)
        CoroutineScope(Dispatchers.IO).launch {
            val heartbeat = context?.let {
                launch heartbeat@{
                    var progress = 0
                    val labels = listOf("Video -> Text", "Link -> Ready", "Video -> Audio", "Audio -> Text", "Text -> Soon", "Almost done")
                    var index = 0
                    while (isActive) {
                        PluctNotificationHelper.showTranscriptionProgressNotification(
                            context = it,
                            url = normalizedUrl,
                            progress = progress,
                            message = labels[index % labels.size],
                            notificationId = notificationId
                        )
                        delay(3500)
                        index++
                        progress = (progress + if (progress < 72) 12 else 3).coerceAtMost(91)
                    }
                }
            }
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
                heartbeat?.cancel()
                val duration = System.currentTimeMillis() - startTime
                Log.d(tag, "API call completed in ${duration}ms")

            result.getOrNull()?.let { status ->
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
                context?.let {
                    val transcript = status.transcript.orEmpty()
                    if (transcript.isNotBlank()) {
                        PluctNotificationHelper.showTranscriptionCompleteNotification(
                            context = it,
                            url = normalizedUrl,
                            transcript = transcript,
                            notificationId = notificationId
                        )
                    } else {
                        PluctNotificationHelper.showTranscriptionProgressNotification(
                            context = it,
                            url = normalizedUrl,
                            progress = 96,
                            message = "Almost done",
                            notificationId = notificationId
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    onSuccess("Transcription completed successfully!")
                }
            } ?: run {
                val error = result.exceptionOrNull()
                if (error is PluctCoreAPIDetailedError) {
                    PluctCoreAPIUnifiedServiceErrorCache.cacheError(normalizedUrl, error)
                }
                val errorMessage = resolveErrorMessage(error, normalizedUrl, debugLogManager, tag)
                context?.let {
                    PluctNotificationHelper.showTranscriptionErrorNotification(
                        context = it,
                        url = normalizedUrl,
                        error = errorMessage,
                        notificationId = notificationId
                    )
                }
                withContext(Dispatchers.Main) {
                    onError(errorMessage)
                }
            }
            } catch (e: Exception) {
                heartbeat?.cancel()
                if (e is PluctCoreAPIDetailedError) {
                    PluctCoreAPIUnifiedServiceErrorCache.cacheError(normalizedUrl, e)
                }
                val errorMessage = resolveErrorMessage(e, normalizedUrl, debugLogManager, tag)
                context?.let {
                    PluctNotificationHelper.showTranscriptionErrorNotification(
                        context = it,
                        url = normalizedUrl,
                        error = errorMessage,
                        notificationId = notificationId
                    )
                }
                withContext(Dispatchers.Main) {
                    onError(errorMessage)
                }
            }
        }
    }

    /**
     * Technical Debt #3: Consolidate error message formatting using UserMessageFormatter
     * Single source of truth for user-facing error messages
     */
    private fun resolveErrorMessage(
        error: Throwable?,
        normalizedUrl: String,
        debugLogManager: PluctCoreDebug01LogManager?,
        logTag: String
    ): String {
        if (error is PluctCoreAPIDetailedError) {
            Log.e(logTag, "Detailed API error while processing $normalizedUrl", error)
            debugLogManager?.logAPIError(error, "TRANSCRIPTION")
            
            // Use UserMessageFormatter for consistent error messages
            val formattedMessage = PluctCoreError03UserMessageFormatter.formatUserMessage(
                error = error,
                technicalMessage = error.userMessage,
                errorCode = error.technicalDetails.errorCode,
                httpStatus = error.technicalDetails.responseStatusCode,
                context = "video processing"
            )
            return formattedMessage.message
        }

        if (error != null) {
            val msg = error.message ?: "Unknown error occurred while processing the video."
            Log.e(logTag, "Complete API flow failed: $msg", error)
            debugLogManager?.logError(
                category = "TRANSCRIPTION",
                operation = "processTikTokVideo",
                message = msg,
                exception = error,
                requestUrl = normalizedUrl
            )
            
            // Use UserMessageFormatter for consistent error messages
            val formattedMessage = PluctCoreError03UserMessageFormatter.formatUserMessage(
                error = error,
                technicalMessage = msg,
                errorCode = error.javaClass.simpleName,
                httpStatus = null,
                context = "video processing"
            )
            return formattedMessage.message
        }

        return "Unknown error occurred while processing the video."
    }
}
