package app.pluct.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import app.pluct.PluctUIScreen01MainActivity
import app.pluct.R
import app.pluct.core.permission.PluctCorePermission01Manager
import app.pluct.ui.components.PluctUIComponent05Notification02Toast01Helper
import app.pluct.notification.PluctNotificationCancelReceiver

object PluctNotificationHelper {
    private const val CHANNEL_ID_PROGRESS = "pluct_processing_live"
    const val CHANNEL_ID_COMPLETE = "pluct_complete" // Exposed for QueueNotificationManager
    private const val CHANNEL_ID_ERROR = "pluct_error"
    const val CHANNEL_ID_QUEUE = "pluct_queue" // Exposed for QueueNotificationManager

    /**
     * TECH DEBT #2: Centralized notification ID generation
     * Ensures consistent, positive IDs across all notification paths
     * Uses URL hashCode masked to ensure positive value
     */
    fun generateNotificationId(url: String): Int {
        return url.hashCode().and(0x7FFFFFFF) // Mask to ensure positive
    }

    private fun buildOpenVideoPendingIntent(context: Context, url: String, requestCode: Int): PendingIntent? {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun progressTitle(progress: Int): String = "$progress% -> Text"

    private fun progressText(message: String): String {
        val lower = message.lowercase()
        return when {
            "network" in lower || "connection" in lower || "reconnect" in lower -> "Wi-Fi -> Resume"
            "server" in lower || "busy" in lower -> "Wait -> Retry"
            "download" in lower -> "Video -> Audio"
            "extract" in lower || "audio" in lower -> "Audio -> Text"
            "transcrib" in lower -> "Audio -> Text"
            "final" in lower || "complete" in lower -> "Almost done"
            "start" in lower || "prepar" in lower -> "Video -> Text"
            else -> message.take(48)
        }
    }

    private fun errorText(error: String): String {
        val lower = error.lowercase()
        return when {
            "credit" in lower || "insufficient" in lower -> "Add credits -> Continue"
            "network" in lower || "connection" in lower || "resolve host" in lower -> "No internet -> Saved"
            "tttranscribe" in lower || "service" in lower || "server" in lower || "500" in lower -> "Try again soon. Video saved."
            "timeout" in lower || "still working" in lower -> "Still working. Check soon."
            else -> error.take(72)
        }
    }

    /**
     * UX FIX #2: Safe notification icon retrieval with dedicated monochrome icon
     * Android 13+ requires monochrome icons for status bar visibility
     * Uses dedicated ic_stat_pluct (Pluct logo silhouette) with fallbacks
     */
    private fun getNotificationIcon(context: Context): Int {
        return try {
            // Primary: Use dedicated notification icon (monochrome Pluct logo)
            context.resources.getResourceName(R.drawable.ic_stat_pluct)
            R.drawable.ic_stat_pluct
        } catch (e: android.content.res.Resources.NotFoundException) {
            try {
                // Fallback 1: Use launcher foreground (may work on some devices)
                context.resources.getResourceName(R.drawable.ic_launcher_foreground)
                R.drawable.ic_launcher_foreground
            } catch (e2: android.content.res.Resources.NotFoundException) {
                try {
                    // Fallback 2: Mipmap launcher
                    context.resources.getResourceName(R.mipmap.ic_launcher)
                    R.mipmap.ic_launcher
                } catch (e3: Exception) {
                    Log.w("PluctNotificationHelper", "No app icon found, using system fallback")
                    android.R.drawable.ic_dialog_info
                }
            }
        } catch (e: Exception) {
            Log.w("PluctNotificationHelper", "Error loading notification icon: ${e.message}")
            android.R.drawable.ic_dialog_info
        }
    }
    
    private const val CHANNEL_NAME_PROGRESS = "Pluct Processing"
    private const val CHANNEL_NAME_COMPLETE = "Pluct Complete"
    private const val CHANNEL_NAME_ERROR = "Pluct Errors"

    private const val CHANNEL_DESCRIPTION_PROGRESS = "Notifications for transcription progress"
    private const val CHANNEL_DESCRIPTION_COMPLETE = "Notifications when transcriptions complete"
    private const val CHANNEL_DESCRIPTION_ERROR = "Notifications for transcription errors"

    // UX FIX #1: Vibration pattern for completion - short-long-short (celebratory)
    private val COMPLETION_VIBRATION_PATTERN = longArrayOf(0, 100, 100, 300, 100, 100)

    // UX FIX #4: Notification channel group for better Settings organization
    private const val CHANNEL_GROUP_ID = "pluct_transcription"
    private const val CHANNEL_GROUP_NAME = "Transcription"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // UX FIX #4: Create channel group for better organization in Settings > Notifications
            val channelGroup = android.app.NotificationChannelGroup(
                CHANNEL_GROUP_ID,
                CHANNEL_GROUP_NAME
            )
            notificationManager.createNotificationChannelGroup(channelGroup)

            // UX FIX #1: Get default notification sound for completion alerts
            val completionSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // Progress channel - silent but visible enough for users who wait from TikTok.
            val progressChannel = NotificationChannel(
                CHANNEL_ID_PROGRESS,
                CHANNEL_NAME_PROGRESS,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION_PROGRESS
                setShowBadge(true)
                enableVibration(false)
                setSound(null, null)
                group = CHANNEL_GROUP_ID // UX FIX #4: Assign to group
            }

            // UX FIX #1: Completion channel with sound and vibration
            val completeChannel = NotificationChannel(
                CHANNEL_ID_COMPLETE,
                CHANNEL_NAME_COMPLETE,
                NotificationManager.IMPORTANCE_HIGH // Elevated for visibility
            ).apply {
                description = CHANNEL_DESCRIPTION_COMPLETE
                setShowBadge(true)
                enableVibration(true)
                vibrationPattern = COMPLETION_VIBRATION_PATTERN
                setSound(completionSoundUri, audioAttributes)
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                group = CHANNEL_GROUP_ID // UX FIX #4: Assign to group
            }

            // Error channel with sound
            val errorChannel = NotificationChannel(
                CHANNEL_ID_ERROR,
                CHANNEL_NAME_ERROR,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION_ERROR
                setShowBadge(true)
                enableVibration(true)
                setSound(completionSoundUri, audioAttributes)
                group = CHANNEL_GROUP_ID // UX FIX #4: Assign to group
            }

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(completeChannel)
            notificationManager.createNotificationChannel(errorChannel)

            // Create queue notification channel
            PluctQueueNotificationManager.createQueueNotificationChannel(context)

            Log.d("PluctNotificationHelper", "Notification channels created with sound+vibration for completion")
        }
    }

    /**
     * UX FIX #1: Trigger vibration manually for devices/scenarios where channel vibration doesn't work
     */
    private fun vibrateForCompletion(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(COMPLETION_VIBRATION_PATTERN, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(COMPLETION_VIBRATION_PATTERN, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(COMPLETION_VIBRATION_PATTERN, -1)
                }
            }
        } catch (e: Exception) {
            Log.w("PluctNotificationHelper", "Vibration failed: ${e.message}")
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
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_COMPLETE)
            .setSmallIcon(getNotificationIcon(context)) // UX FIX: Use app icon with safe fallback
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
        
        // UX FIX #4: Add cancel action
        val cancelIntent = Intent(context, PluctNotificationCancelReceiver::class.java).apply {
            action = PluctNotificationCancelReceiver.ACTION_CANCEL_TRANSCRIPTION
            putExtra(PluctNotificationCancelReceiver.EXTRA_URL, url)
            putExtra(PluctNotificationCancelReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1000, // Different request code to avoid conflicts
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val safeProgress = progress.coerceIn(0, 99)
        val simpleText = progressText(message)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_PROGRESS)
            .setSmallIcon(getNotificationIcon(context)) // UX FIX: Use app icon with safe fallback
            .setContentTitle(progressTitle(safeProgress))
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
        buildOpenVideoPendingIntent(context, url, notificationId + 2000)?.let {
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
        
        // UX FIX #3: Enhanced preview with word count and first sentence for context
        val wordCount = transcript.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val titleWithContext = "100% -> Text"

        // UX FIX #1: Build notification with HIGH priority for sound+vibration
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_COMPLETE)
            .setSmallIcon(getNotificationIcon(context))
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
            .setPriority(NotificationCompat.PRIORITY_HIGH) // UX FIX #1: Elevated priority
            .setDefaults(NotificationCompat.DEFAULT_ALL) // UX FIX #1: Sound+vibration+lights
            .setCategory(NotificationCompat.CATEGORY_STATUS)
        buildOpenVideoPendingIntent(context, url, notificationId + 2000)?.let {
            builder.addAction(android.R.drawable.ic_media_play, "TikTok", it)
        }
        val notification = builder.build()

        // UX FIX #1: Trigger manual vibration as backup
        vibrateForCompletion(context)
        
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
        
        val simpleError = errorText(error)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ERROR)
            .setSmallIcon(getNotificationIcon(context)) // UX FIX: Use app icon with safe fallback
            .setContentTitle("! Pluct")
            .setContentText(simpleError)
            .setTicker("! Pluct $simpleError")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        buildOpenVideoPendingIntent(context, url, notificationId + 2000)?.let {
            builder.addAction(android.R.drawable.ic_media_play, "TikTok", it)
        }
        val notification = builder.build()
        
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
        
        val safeProgress = progress.coerceIn(0, 99)
        val simpleText = progressText(message)
        return NotificationCompat.Builder(context, CHANNEL_ID_PROGRESS)
            .setSmallIcon(getNotificationIcon(context)) // UX FIX: Use app icon with safe fallback
            .setContentTitle(progressTitle(safeProgress))
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
