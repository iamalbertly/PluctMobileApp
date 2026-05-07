package app.pluct.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import app.pluct.services.PluctCoreBackground01TranscriptionWorker
import java.util.concurrent.TimeUnit

/**
 * UX FIX #4: BroadcastReceiver to handle cancel action from progress notifications
 * Cancels the WorkManager job for the transcription
 */
class PluctNotificationCancelReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "NotificationCancel"
        const val ACTION_CANCEL_TRANSCRIPTION = "app.pluct.CANCEL_TRANSCRIPTION"
        const val EXTRA_URL = "url"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CANCEL_TRANSCRIPTION) {
            return
        }

        val url = intent.getStringExtra(EXTRA_URL) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        Log.d(TAG, "Cancel action received for URL: $url, notificationId: $notificationId")

        // Find and cancel the WorkManager job for this URL
        val workManager = WorkManager.getInstance(context)
        val workQuery = WorkQuery.Builder
            .fromTags(listOf("transcription"))
            .addStates(listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING))
            .build()

        try {
            val future = workManager.getWorkInfos(workQuery)
            val workInfos = future.get(5, TimeUnit.SECONDS)

            workInfos.forEach { workInfo ->
                val workUrl = workInfo.progress.getString(PluctCoreBackground01TranscriptionWorker.KEY_URL)
                if (workUrl == url) {
                    Log.d(TAG, "Cancelling work for URL: $url, workId=${workInfo.id}")
                    workManager.cancelWorkById(workInfo.id)
                    
                    // Dismiss the notification
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    notificationManager.cancel(notificationId)
                    
                    // Show cancellation toast
                    app.pluct.ui.components.PluctUIComponent05Notification02Toast01Helper.showTranscriptionCancelled(context, url)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling work: ${e.message}", e)
        }
    }
}
