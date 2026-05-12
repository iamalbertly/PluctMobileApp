package app.pluct.notification

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log

/**
 * SSOT for transcription-related notification channels (Customer: one Settings grouping; Trust: stable ids via [PluctNotification01Primitives]).
 */
internal object PluctNotification03Transcription01Channels {
    private const val CHANNEL_NAME_PROGRESS = "Pluct Processing"
    private const val CHANNEL_NAME_COMPLETE = "Pluct Complete"
    private const val CHANNEL_NAME_ERROR = "Pluct Errors"

    private const val CHANNEL_DESCRIPTION_PROGRESS = "Notifications for transcription progress"
    private const val CHANNEL_DESCRIPTION_COMPLETE = "Notifications when transcriptions complete"
    private const val CHANNEL_DESCRIPTION_ERROR = "Notifications for transcription errors"

    internal val completionVibrationPattern: LongArray =
        longArrayOf(0, 100, 100, 300, 100, 100)

    private const val CHANNEL_GROUP_ID = "pluct_transcription"
    private const val CHANNEL_GROUP_NAME = "Transcription"

    fun createAllTranscriptionChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelGroup = NotificationChannelGroup(
            CHANNEL_GROUP_ID,
            CHANNEL_GROUP_NAME
        )
        notificationManager.createNotificationChannelGroup(channelGroup)

        val completionSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val progressChannel = NotificationChannel(
            PluctNotification01Primitives.CHANNEL_ID_PROGRESS,
            CHANNEL_NAME_PROGRESS,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION_PROGRESS
            setShowBadge(true)
            enableVibration(false)
            setSound(null, null)
            group = CHANNEL_GROUP_ID
        }

        val completeChannel = NotificationChannel(
            PluctNotification01Primitives.CHANNEL_ID_COMPLETE,
            CHANNEL_NAME_COMPLETE,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION_COMPLETE
            setShowBadge(true)
            enableVibration(true)
            vibrationPattern = completionVibrationPattern
            setSound(completionSoundUri, audioAttributes)
            enableLights(true)
            lightColor = android.graphics.Color.GREEN
            group = CHANNEL_GROUP_ID
        }

        val errorChannel = NotificationChannel(
            PluctNotification01Primitives.CHANNEL_ID_ERROR,
            CHANNEL_NAME_ERROR,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION_ERROR
            setShowBadge(true)
            enableVibration(true)
            setSound(completionSoundUri, audioAttributes)
            group = CHANNEL_GROUP_ID
        }

        notificationManager.createNotificationChannel(progressChannel)
        notificationManager.createNotificationChannel(completeChannel)
        notificationManager.createNotificationChannel(errorChannel)

        PluctNotification01Primitives.ensureQueueChannel(context)

        Log.d("PluctNotificationHelper", "Notification channels created with sound+vibration for completion")
        Log.i(PluctNotification01Primitives.LOG_TAG, "notification_channels_bootstrapped")
    }
}
