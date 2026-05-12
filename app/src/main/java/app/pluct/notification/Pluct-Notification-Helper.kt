package app.pluct.notification

import android.content.Context
import kotlin.jvm.JvmField

/**
 * Public façade for Pluct notifications. Implementation is split under [PluctNotification03Transcription01Channels]
 * and related internal modules (≤300 lines each; Speed: one stable import surface for the app).
 *
 * Edge cases: progress requires POST_NOTIFICATIONS where applicable; complete/error fall back to toast when denied.
 */
object PluctNotificationHelper {
    @JvmField
    val CHANNEL_ID_COMPLETE = PluctNotification01Primitives.CHANNEL_ID_COMPLETE

    @JvmField
    val CHANNEL_ID_QUEUE = PluctNotification01Primitives.CHANNEL_ID_QUEUE

    fun generateNotificationId(url: String): Int = url.hashCode().and(0x7FFFFFFF)

    fun createNotificationChannels(context: Context) {
        PluctNotification03Transcription01Channels.createAllTranscriptionChannels(context)
    }

    fun createNotificationChannel(context: Context) {
        createNotificationChannels(context)
    }

    fun showProcessingCompleteNotification(
        context: Context,
        videoTitle: String,
        processingTier: String
    ) {
        PluctNotification03Transcription04TierBanners.showProcessingCompleteNotification(
            context, videoTitle, processingTier
        )
    }

    fun showQuickScanCompleteNotification(context: Context, videoTitle: String) {
        PluctNotification03Transcription04TierBanners.showQuickScanCompleteNotification(context, videoTitle)
    }

    fun showTranscriptionProgressNotification(
        context: Context,
        url: String,
        progress: Int,
        message: String,
        notificationId: Int
    ) {
        PluctNotification03Transcription02Progress.showTranscriptionProgressNotification(
            context, url, progress, message, notificationId
        )
    }

    fun showTranscriptionCompleteNotification(
        context: Context,
        url: String,
        transcript: String,
        notificationId: Int
    ) {
        PluctNotification03Transcription03CompleteAndError.showTranscriptionCompleteNotification(
            context, url, transcript, notificationId
        )
    }

    fun showTranscriptionErrorNotification(
        context: Context,
        url: String,
        error: String,
        notificationId: Int
    ) {
        PluctNotification03Transcription03CompleteAndError.showTranscriptionErrorNotification(
            context, url, error, notificationId
        )
    }

    @Deprecated(
        message = "Creates duplicate notifications. Use showTranscriptionProgressNotification instead.",
        replaceWith = ReplaceWith(
            "showTranscriptionProgressNotification(context, url, 0, \"Starting transcription...\", url.hashCode().and(0x7FFFFFFF))"
        )
    )
    fun showTranscriptionStartedNotification(context: Context, url: String) {
        PluctNotification03Transcription03CompleteAndError.showTranscriptionStartedRedirectToProgress(context, url)
    }

    /**
     * Foreground service progress notification. [notificationRequestCode] must match the live progress notification id so taps route consistently.
     */
    fun createProgressNotification(
        context: Context,
        url: String,
        progress: Int,
        message: String,
        notificationRequestCode: Int = 0
    ): android.app.Notification {
        return PluctNotification03Transcription02Progress.createProgressNotification(
            context, url, progress, message, notificationRequestCode
        )
    }
}
