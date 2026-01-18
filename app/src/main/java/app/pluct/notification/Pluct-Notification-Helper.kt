package app.pluct.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import app.pluct.PluctUIScreen01MainActivity
import app.pluct.R
import app.pluct.core.permission.PluctCorePermission01Manager
import app.pluct.ui.components.PluctUIComponent05Notification02Toast01Helper

object PluctNotificationHelper {
    private const val CHANNEL_ID_PROGRESS = "pluct_processing"
    const val CHANNEL_ID_COMPLETE = "pluct_complete" // Exposed for QueueNotificationManager
    private const val CHANNEL_ID_ERROR = "pluct_error"
    const val CHANNEL_ID_QUEUE = "pluct_queue" // Exposed for QueueNotificationManager
    
    /**
     * UX FIX: Safe icon retrieval with fallback
     * Returns app icon if available, falls back to system icon with error logging
     */
    private fun getNotificationIcon(context: Context): Int {
        return try {
            // Verify icon resource exists
            context.resources.getResourceName(R.mipmap.ic_launcher)
            R.mipmap.ic_launcher
        } catch (e: android.content.res.Resources.NotFoundException) {
            Log.w("PluctNotificationHelper", "App icon resource not found, using fallback: ${e.message}")
            android.R.drawable.ic_dialog_info // Fallback to system icon
        } catch (e: Exception) {
            Log.w("PluctNotificationHelper", "Error loading app icon, using fallback: ${e.message}")
            android.R.drawable.ic_dialog_info // Fallback to system icon
        }
    }
    
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
            .setSmallIcon(getNotificationIcon(context)) // UX FIX: Use app icon with safe fallback
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
            .setSmallIcon(getNotificationIcon(context)) // UX FIX: Use app icon with safe fallback
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
            .setSmallIcon(getNotificationIcon(context)) // UX FIX: Use app icon with safe fallback
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
     * Checks permission first and falls back to toast if permission denied
     */
    fun showTranscriptionCompleteNotification(
        context: Context,
        url: String,
        transcript: String,
        notificationId: Int
    ) {
        // Check permission first
        if (!PluctCorePermission01Manager.hasNotificationPermission(context)) {
            Log.w("PluctNotificationHelper", "Notification permission denied, showing toast instead")
            // Fall back to toast notification when app is in foreground
            PluctUIComponent05Notification02Toast01Helper.showTranscriptionFinished(context, url, true)
            return
        }
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
            .setSmallIcon(getNotificationIcon(context)) // UX FIX: Use app icon with safe fallback
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
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w("PluctNotificationHelper", "Failed to show completion notification: ${e.message}")
            // Fall back to toast
            PluctUIComponent05Notification02Toast01Helper.showTranscriptionFinished(context, url, true)
        }
    }
    
    /**
     * Show transcription error notification
     * Checks permission first and falls back to toast if permission denied
     */
    fun showTranscriptionErrorNotification(
        context: Context,
        url: String,
        error: String,
        notificationId: Int
    ) {
        // Check permission first
        if (!PluctCorePermission01Manager.hasNotificationPermission(context)) {
            Log.w("PluctNotificationHelper", "Notification permission denied, showing toast instead")
            // Fall back to toast notification when app is in foreground
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
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ERROR)
            .setSmallIcon(getNotificationIcon(context)) // UX FIX: Use app icon with safe fallback
            .setContentTitle("❌ Transcription Failed")
            .setContentText(error)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w("PluctNotificationHelper", "Failed to show error notification: ${e.message}")
            // Fall back to toast
            PluctUIComponent05Notification02Toast01Helper.showTranscriptionFinished(context, url, false)
        }
    }
    
    /**
     * Show notification when transcription starts from intent
     * Checks permission first and falls back to toast if permission denied
     * 
     * @deprecated This method creates duplicate notifications. The background worker
     * handles showing the initial progress notification. Use showTranscriptionProgressNotification
     * with progress=0 instead.
     */
    @Deprecated(
        message = "Creates duplicate notifications. Use showTranscriptionProgressNotification instead.",
        replaceWith = ReplaceWith("showTranscriptionProgressNotification(context, url, 0, \"Starting transcription...\", url.hashCode().and(0x7FFFFFFF))")
    )
    fun showTranscriptionStartedNotification(
        context: Context,
        url: String
    ) {
        // UX FIX: Redirect to progress notification to avoid duplicates
        // The background worker will handle showing the initial notification
        val notificationId = url.hashCode().and(0x7FFFFFFF) // Ensure positive
        showTranscriptionProgressNotification(
            context = context,
            url = url,
            progress = 0,
            message = "Starting transcription...",
            notificationId = notificationId
        )
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
            .setSmallIcon(getNotificationIcon(context)) // UX FIX: Use app icon with safe fallback
            .setContentTitle("Transcribing...")
            .setContentText(message)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
