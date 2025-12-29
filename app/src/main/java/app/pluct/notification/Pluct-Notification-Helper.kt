package app.pluct.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.pluct.PluctUIScreen01MainActivity
import app.pluct.R

object PluctNotificationHelper {
    private const val CHANNEL_ID_PROGRESS = "pluct_processing"
    const val CHANNEL_ID_COMPLETE = "pluct_complete" // Exposed for QueueNotificationManager
    private const val CHANNEL_ID_ERROR = "pluct_error"
    const val CHANNEL_ID_QUEUE = "pluct_queue" // Exposed for QueueNotificationManager
    
    private const val CHANNEL_NAME_PROGRESS = "Pluct Processing"
    private const val CHANNEL_NAME_COMPLETE = "Pluct Complete"
    private const val CHANNEL_NAME_ERROR = "Pluct Errors"
    
    private const val CHANNEL_DESCRIPTION_PROGRESS = "Notifications for transcription progress"
    private const val CHANNEL_DESCRIPTION_COMPLETE = "Notifications when transcriptions complete"
    private const val CHANNEL_DESCRIPTION_ERROR = "Notifications for transcription errors"
    
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Progress channel
            val progressChannel = NotificationChannel(
                CHANNEL_ID_PROGRESS,
                CHANNEL_NAME_PROGRESS,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION_PROGRESS
                setShowBadge(false)
            }
            
            // Completion channel
            val completeChannel = NotificationChannel(
                CHANNEL_ID_COMPLETE,
                CHANNEL_NAME_COMPLETE,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION_COMPLETE
                setShowBadge(true)
            }
            
            // Error channel
            val errorChannel = NotificationChannel(
                CHANNEL_ID_ERROR,
                CHANNEL_NAME_ERROR,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION_ERROR
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(completeChannel)
            notificationManager.createNotificationChannel(errorChannel)
            
            // Create queue notification channel
            PluctQueueNotificationManager.createQueueNotificationChannel(context)
        }
    }
    
    // Legacy method for backward compatibility
    fun createNotificationChannel(context: Context) {
        createNotificationChannels(context)
    }
    
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
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_COMPLETE)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using system icon for now
            .setContentTitle("✨ Your AI Analysis is Ready!")
            .setContentText("$videoTitle - $processingTier analysis completed")
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
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_COMPLETE)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using system icon for now
            .setContentTitle("✅ Quick Scan Complete")
            .setContentText("$videoTitle - Transcript ready")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    /**
     * Show transcription progress notification
     */
    fun showTranscriptionProgressNotification(
        context: Context,
        url: String,
        progress: Int,
        message: String,
        notificationId: Int
    ) {
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
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_PROGRESS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Transcribing...")
            .setContentText(message)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Show transcription completion notification with transcript preview
     */
    fun showTranscriptionCompleteNotification(
        context: Context,
        url: String,
        transcript: String,
        notificationId: Int
    ) {
        // Create intent to open video detail screen
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
        
        // Copy transcript action
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
        
        val preview = transcript.take(100) + if (transcript.length > 100) "..." else ""
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_COMPLETE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("✨ Transcription Complete!")
            .setContentText(preview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(transcript.take(500)))
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_share,
                "Copy Transcript",
                copyPendingIntent
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Show transcription error notification
     */
    fun showTranscriptionErrorNotification(
        context: Context,
        url: String,
        error: String,
        notificationId: Int
    ) {
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
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ERROR)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("❌ Transcription Failed")
            .setContentText(error)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Create progress notification for foreground service
     */
    fun createProgressNotification(
        context: Context,
        url: String,
        progress: Int,
        message: String
    ): android.app.Notification {
        val intent = Intent(context, PluctUIScreen01MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("url", url)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_ID_PROGRESS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Transcribing...")
            .setContentText(message)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
