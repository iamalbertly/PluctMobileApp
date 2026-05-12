package app.pluct.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import app.pluct.PluctUIScreen01MainActivity
import app.pluct.R

/**
 * SSOT for notification resources shared by [PluctNotificationHelper] and [PluctQueueNotificationManager].
 * Customer: one icon fallback chain; Trust: one channel id for queue so Settings matches reality.
 */
object PluctNotification01Primitives {
    const val LOG_TAG = "PluctNotifSSOT"

    const val CHANNEL_ID_PROGRESS = "pluct_processing_live"
    const val CHANNEL_ID_COMPLETE = "pluct_complete"
    const val CHANNEL_ID_ERROR = "pluct_error"
    const val CHANNEL_ID_QUEUE = "pluct_queue"

    private const val CHANNEL_NAME_QUEUE = "Pluct Queue"
    private const val CHANNEL_DESCRIPTION_QUEUE = "Notifications for queued videos"

    fun smallIconResId(context: Context): Int {
        return try {
            context.resources.getResourceName(R.drawable.ic_stat_pluct)
            R.drawable.ic_stat_pluct
        } catch (e: android.content.res.Resources.NotFoundException) {
            try {
                context.resources.getResourceName(R.drawable.ic_launcher_monochrome)
                R.drawable.ic_launcher_monochrome
            } catch (e2: android.content.res.Resources.NotFoundException) {
                Log.w(LOG_TAG, "No stat icon, using system fallback: ${e2.message}")
                android.R.drawable.ic_dialog_info
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error loading notification icon: ${e.message}")
            android.R.drawable.ic_dialog_info
        }
    }

    fun ensureQueueChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val queueChannel = NotificationChannel(
            CHANNEL_ID_QUEUE,
            CHANNEL_NAME_QUEUE,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = CHANNEL_DESCRIPTION_QUEUE
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(queueChannel)
        Log.d(LOG_TAG, "queue_channel_ensured")
    }

    fun mainActivityPendingIntent(
        context: Context,
        requestCode: Int,
        configure: Intent.() -> Unit = {}
    ): PendingIntent {
        val intent = Intent(context, PluctUIScreen01MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            configure()
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun openUrlPendingIntent(context: Context, url: String, requestCode: Int): PendingIntent? {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
