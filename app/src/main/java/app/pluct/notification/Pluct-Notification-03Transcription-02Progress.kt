package app.pluct.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import app.pluct.PluctUIScreen01MainActivity
import app.pluct.core.permission.PluctCorePermission01Manager

/**
 * Transcription progress notifications and foreground-service progress builder (single place for cancel action + TikTok deep link).
 */
internal object PluctNotification03Transcription02Progress {
    fun showTranscriptionProgressNotification(
        context: Context,
        url: String,
        progress: Int,
        message: String,
        notificationId: Int
    ) {
        if (!PluctCorePermission01Manager.hasNotificationPermission(context)) {
            Log.w("PluctNotificationHelper", "Progress notification blocked; permission or app notifications disabled")
            return
        }
        val intent = Intent(context, PluctUIScreen01MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("url", url)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(context, PluctNotificationCancelReceiver::class.java).apply {
            action = PluctNotificationCancelReceiver.ACTION_CANCEL_TRANSCRIPTION
            putExtra(PluctNotificationCancelReceiver.EXTRA_URL, url)
            putExtra(PluctNotificationCancelReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }

        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1000,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val safeProgress = progress.coerceIn(0, 99)
        val simpleText = PluctNotification02Copy01Formatter.progressText(message)
        val builder = NotificationCompat.Builder(context, PluctNotification01Primitives.CHANNEL_ID_PROGRESS)
            .setSmallIcon(PluctNotification01Primitives.smallIconResId(context))
            .setContentTitle(PluctNotification02Copy01Formatter.progressTitle(safeProgress))
            .setContentText(simpleText)
            .setSubText("$safeProgress%")
            .setTicker("$safeProgress% $simpleText")
            .setProgress(100, safeProgress, false)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                cancelPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        PluctNotification01Primitives.openUrlPendingIntent(context, url, notificationId + 2000)?.let {
            builder.addAction(android.R.drawable.ic_media_play, "TikTok", it)
        }
        val notification = builder.build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w("PluctNotificationHelper", "Failed to show progress notification: ${e.message}")
        }
    }

    fun createProgressNotification(
        context: Context,
        url: String,
        progress: Int,
        message: String,
        notificationRequestCode: Int
    ): android.app.Notification {
        val intent = Intent(context, PluctUIScreen01MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("url", url)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val safeProgress = progress.coerceIn(0, 99)
        val simpleText = PluctNotification02Copy01Formatter.progressText(message)
        return NotificationCompat.Builder(context, PluctNotification01Primitives.CHANNEL_ID_PROGRESS)
            .setSmallIcon(PluctNotification01Primitives.smallIconResId(context))
            .setContentTitle(PluctNotification02Copy01Formatter.progressTitle(safeProgress))
            .setContentText(simpleText)
            .setSubText("$safeProgress%")
            .setTicker("$safeProgress% $simpleText")
            .setProgress(100, safeProgress, false)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }
}
