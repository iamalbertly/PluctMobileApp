package app.pluct.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import app.pluct.PluctUIScreen01MainActivity
import app.pluct.core.permission.PluctCorePermission01Manager
import app.pluct.ui.components.PluctUIComponent05Notification02Toast01Helper

/**
 * Completion and error notifications (Trust: cancel ongoing id before complete card; Customer: toast fallback if permission denied).
 */
internal object PluctNotification03Transcription03CompleteAndError {
    private fun vibrateForCompletion(context: Context) {
        val pattern = PluctNotification03Transcription01Channels.completionVibrationPattern
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.w("PluctNotificationHelper", "Vibration failed: ${e.message}")
        }
    }

    fun showTranscriptionCompleteNotification(
        context: Context,
        url: String,
        transcript: String,
        notificationId: Int
    ) {
        if (!PluctCorePermission01Manager.hasNotificationPermission(context)) {
            Log.w("PluctNotificationHelper", "Notification permission denied, showing toast instead")
            PluctUIComponent05Notification02Toast01Helper.showTranscriptionFinished(context, url, true)
            return
        }
        val intent = Intent(context, PluctUIScreen01MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("action", "view_transcript")
            putExtra("url", url)
            putExtra("transcript", transcript)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val copyIntent = Intent(context, PluctUIScreen01MainActivity::class.java).apply {
            action = "app.pluct.COPY_TRANSCRIPT"
            putExtra("transcript", transcript)
        }
        val copyPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val wordCount = transcript.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val titleWithContext = "100% -> Text"

        val builder = NotificationCompat.Builder(context, PluctNotification01Primitives.CHANNEL_ID_COMPLETE)
            .setSmallIcon(PluctNotification01Primitives.smallIconResId(context))
            .setContentTitle(titleWithContext)
            .setContentText("Tap -> Copy ($wordCount words)")
            .setTicker("100% -> Text")
            .setStyle(NotificationCompat.BigTextStyle().bigText(transcript.take(500)))
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_share,
                "Copy",
                copyPendingIntent
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
        PluctNotification01Primitives.openUrlPendingIntent(context, url, notificationId + 2000)?.let {
            builder.addAction(android.R.drawable.ic_media_play, "TikTok", it)
        }
        val notification = builder.build()

        vibrateForCompletion(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.cancel(notificationId)
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w("PluctNotificationHelper", "Failed to show completion notification: ${e.message}")
            PluctUIComponent05Notification02Toast01Helper.showTranscriptionFinished(context, url, true)
        }
    }

    fun showTranscriptionErrorNotification(
        context: Context,
        url: String,
        error: String,
        notificationId: Int
    ) {
        if (!PluctCorePermission01Manager.hasNotificationPermission(context)) {
            Log.w("PluctNotificationHelper", "Notification permission denied, showing toast instead")
            PluctUIComponent05Notification02Toast01Helper.showTranscriptionFinished(context, url, false)
            return
        }

        val intent = Intent(context, PluctUIScreen01MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("url", url)
            putExtra("error", error)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val simpleError = PluctNotification02Copy01Formatter.errorText(error)
        val builder = NotificationCompat.Builder(context, PluctNotification01Primitives.CHANNEL_ID_ERROR)
            .setSmallIcon(PluctNotification01Primitives.smallIconResId(context))
            .setContentTitle("! Pluct")
            .setContentText(simpleError)
            .setTicker("! Pluct $simpleError")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        PluctNotification01Primitives.openUrlPendingIntent(context, url, notificationId + 2000)?.let {
            builder.addAction(android.R.drawable.ic_media_play, "TikTok", it)
        }
        val notification = builder.build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w("PluctNotificationHelper", "Failed to show error notification: ${e.message}")
            PluctUIComponent05Notification02Toast01Helper.showTranscriptionFinished(context, url, false)
        }
    }

    fun showTranscriptionStartedRedirectToProgress(
        context: Context,
        url: String
    ) {
        val notificationId = url.hashCode().and(0x7FFFFFFF)
        PluctNotification03Transcription02Progress.showTranscriptionProgressNotification(
            context = context,
            url = url,
            progress = 0,
            message = "Starting transcription...",
            notificationId = notificationId
        )
    }
}
