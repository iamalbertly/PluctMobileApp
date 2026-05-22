package app.pluct.notification

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import app.pluct.R
import app.pluct.data.entity.QueueReason

/**
 * Queue shade notifications only — domain queue persistence lives in [app.pluct.services.PluctQueueManager].
 */
object PluctQueueNotificationManager {
    private const val NOTIFICATION_ID_QUEUE = 9999

    fun createQueueNotificationChannel(context: Context) {
        PluctNotification01Primitives.ensureQueueChannel(context)
    }

    fun showQueueNotification(
        context: Context,
        queuedCount: Int,
        @Suppress("UNUSED_PARAMETER") message: String
    ) {
        showQueueStatusNotification(context, queuedCount, 0)
    }

    fun showQueueStatusNotification(
        context: Context,
        queuedCount: Int,
        processingCount: Int,
        @Suppress("UNUSED_PARAMETER") onTap: () -> Unit = {}
    ) {
        val message = when {
            queuedCount > 0 && processingCount > 0 ->
                "$queuedCount in queue, $processingCount processing"
            queuedCount > 0 -> "$queuedCount waiting - add balance or reconnect"
            processingCount > 0 -> "$processingCount video(s) processing"
            else -> return
        }

        val pendingIntent = PluctNotification01Primitives.mainActivityPendingIntent(context, 0) {
            putExtra("show_queue", true)
        }

        val notification = NotificationCompat.Builder(context, PluctNotification01Primitives.CHANNEL_ID_QUEUE)
            .setSmallIcon(PluctNotification01Primitives.smallIconResId(context))
            .setContentTitle("Pluct queue")
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setShowWhen(false)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_QUEUE, notification)
    }

    fun updateQueueNotification(
        context: Context,
        queuedCount: Int,
        processingCount: Int,
        lastProcessedUrl: String? = null,
        queueReasons: Map<String, QueueReason?> = emptyMap()
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when {
            queuedCount > 0 -> {
                val notificationText = buildString {
                    when {
                        processingCount > 0 -> append("Processing $processingCount video(s)...")
                        queuedCount == 1 -> {
                            val reason = queueReasons.values.firstOrNull()
                            append(
                                when (reason) {
                                    QueueReason.NO_INTERNET -> "Waiting for internet connection"
                                    QueueReason.INSUFFICIENT_CREDITS -> "Waiting for balance"
                                    QueueReason.RATE_LIMITED -> "Rate limited, will retry"
                                    QueueReason.SERVICE_UNAVAILABLE -> "Service unavailable, will retry"
                                    null -> "Queued for processing"
                                }
                            )
                        }
                        else -> {
                            val noInternetCount = queueReasons.values.count { it == QueueReason.NO_INTERNET }
                            val noCreditsCount = queueReasons.values.count { it == QueueReason.INSUFFICIENT_CREDITS }
                            when {
                                noInternetCount > 0 && noCreditsCount > 0 ->
                                    append("$noInternetCount waiting for internet, $noCreditsCount waiting for balance")
                                noInternetCount > 0 -> append("$noInternetCount waiting for internet")
                noCreditsCount > 0 -> append("$noCreditsCount waiting for balance")
                                else -> append("Will process when ready")
                            }
                        }
                    }
                }

                val pendingIntent = PluctNotification01Primitives.mainActivityPendingIntent(context, 0) {
                    putExtra("show_queue", true)
                }

                val notification = NotificationCompat.Builder(context, PluctNotification01Primitives.CHANNEL_ID_QUEUE)
                    .setSmallIcon(PluctNotification01Primitives.smallIconResId(context))
                    .setContentTitle("$queuedCount waiting in Pluct")
                    .setContentText(notificationText)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
                    .build()

                notificationManager.notify(NOTIFICATION_ID_QUEUE, notification)
            }
            lastProcessedUrl != null -> {
                val pendingIntent = PluctNotification01Primitives.mainActivityPendingIntent(
                    context,
                    System.currentTimeMillis().toInt()
                ) {
                    putExtra("url", lastProcessedUrl)
                }

                val notification = NotificationCompat.Builder(context, PluctNotification01Primitives.CHANNEL_ID_COMPLETE)
                    .setSmallIcon(PluctNotification01Primitives.smallIconResId(context))
                    .setContentTitle("Queued video processed")
                    .setContentText("Tap to view transcript")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

                notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            }
            else -> removeQueueNotification(context)
        }
    }

    fun removeQueueNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_QUEUE)
    }
}
