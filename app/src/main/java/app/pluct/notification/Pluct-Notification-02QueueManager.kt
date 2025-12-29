package app.pluct.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.pluct.PluctUIScreen01MainActivity
import app.pluct.data.entity.QueueReason
import app.pluct.notification.PluctNotificationHelper

/**
 * Pluct-Notification-02QueueManager
 * Manages persistent notifications for queued videos
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Sequence][Responsibility]
 */
object PluctQueueNotificationManager {
    private const val CHANNEL_ID_QUEUE = "pluct_queue"
    private const val CHANNEL_NAME_QUEUE = "Pluct Queue"
    private const val CHANNEL_DESCRIPTION_QUEUE = "Notifications for queued videos"
    private const val NOTIFICATION_ID_QUEUE = 9999 // Persistent ID
    
    /**
     * Create queue notification channel
     */
    fun createQueueNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        }
    }
    
    /**
     * Show simple queue notification (for compatibility)
     */
    fun showQueueNotification(
        context: Context,
        queuedCount: Int,
        message: String
    ) {
        showQueueStatusNotification(context, queuedCount, 0)
    }
    
    /**
     * Show queue status notification
     */
    fun showQueueStatusNotification(
        context: Context,
        queuedCount: Int,
        processingCount: Int,
        onTap: () -> Unit = {}
    ) {
        val intent = Intent(context, PluctUIScreen01MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("show_queue", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val message = when {
            queuedCount > 0 && processingCount > 0 -> 
                "$queuedCount in queue, $processingCount processing"
            queuedCount > 0 -> "$queuedCount video(s) waiting for credits/connection"
            processingCount > 0 -> "$processingCount video(s) processing"
            else -> return // Don't show if nothing queued/processing
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_QUEUE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📋 Pluct Queue")
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Persistent
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setShowWhen(false)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_QUEUE, notification)
    }
    
    /**
     * Update queue notification with detailed status
     */
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
                // Build detailed notification text
                val notificationText = buildString {
                    when {
                        processingCount > 0 -> append("Processing $processingCount video(s)...")
                        queuedCount == 1 -> {
                            val reason = queueReasons.values.firstOrNull()
                            append(when (reason) {
                                QueueReason.NO_INTERNET -> "Waiting for internet connection"
                                QueueReason.INSUFFICIENT_CREDITS -> "Waiting for credits"
                                QueueReason.RATE_LIMITED -> "Rate limited, will retry"
                                QueueReason.SERVICE_UNAVAILABLE -> "Service unavailable, will retry"
                                null -> "Queued for processing"
                            })
                        }
                        else -> {
                            val noInternetCount = queueReasons.values.count { it == QueueReason.NO_INTERNET }
                            val noCreditsCount = queueReasons.values.count { it == QueueReason.INSUFFICIENT_CREDITS }
                            when {
                                noInternetCount > 0 && noCreditsCount > 0 -> 
                                    append("$noInternetCount waiting for internet, $noCreditsCount waiting for credits")
                                noInternetCount > 0 -> append("$noInternetCount waiting for internet")
                                noCreditsCount > 0 -> append("$noCreditsCount waiting for credits")
                                else -> append("Will process when ready")
                            }
                        }
                    }
                }
                
                // Create persistent notification
                val intent = Intent(context, PluctUIScreen01MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("show_queue", true)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val notification = NotificationCompat.Builder(context, CHANNEL_ID_QUEUE)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("📋 $queuedCount video(s) queued")
                    .setContentText(notificationText)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true) // Persistent
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
                    .build()
                
                notificationManager.notify(NOTIFICATION_ID_QUEUE, notification)
            }
            lastProcessedUrl != null -> {
                // Show completion notification
                val intent = Intent(context, PluctUIScreen01MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("url", lastProcessedUrl)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, System.currentTimeMillis().toInt(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val notification = NotificationCompat.Builder(context, PluctNotificationHelper.CHANNEL_ID_COMPLETE)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("✅ Queued video processed")
                    .setContentText("Tap to view transcript")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
                
                notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            }
            else -> {
                // Clear notification if queue is empty
                removeQueueNotification(context)
            }
        }
    }
    
    /**
     * Remove queue notification
     */
    fun removeQueueNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_QUEUE)
    }
}

