package app.pluct.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import app.pluct.PluctUIScreen01MainActivity

/**
 * Non-transcription tier completion banners (kept separate from TikTok URL progress SSOT).
 */
internal object PluctNotification03Transcription04TierBanners {
    fun showProcessingCompleteNotification(
        context: Context,
        videoTitle: String,
        processingTier: String
    ) {
        val intent = Intent(context, PluctUIScreen01MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PluctNotification01Primitives.CHANNEL_ID_COMPLETE)
            .setSmallIcon(PluctNotification01Primitives.smallIconResId(context))
            .setContentTitle("100% -> Pluct")
            .setContentText("Tap -> Open")
            .setSubText("${videoTitle.take(24)} ${processingTier.take(12)}".trim())
            .setTicker("100% -> Pluct")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showQuickScanCompleteNotification(
        context: Context,
        videoTitle: String
    ) {
        val intent = Intent(context, PluctUIScreen01MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PluctNotification01Primitives.CHANNEL_ID_COMPLETE)
            .setSmallIcon(PluctNotification01Primitives.smallIconResId(context))
            .setContentTitle("100% -> Text")
            .setContentText("Tap -> Open")
            .setSubText(videoTitle.take(24))
            .setTicker("100% -> Text")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
