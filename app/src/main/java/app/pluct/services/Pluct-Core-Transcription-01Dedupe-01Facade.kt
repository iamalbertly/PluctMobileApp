package app.pluct.services

import android.content.Context
import android.util.Log
import app.pluct.notification.PluctNotificationHelper

/**
 * Single entry for "should we skip starting another background job for this URL?"
 * Realism: WorkManager is the SSOT for in-flight work; UI/worker both consult this facade (Speed: one policy).
 *
 * Edge: duplicate tap while job RUNNING — returns true and refreshes progress notification so shade stays trustworthy.
 */
object PluctCoreTranscription01Dedupe01Facade {
    private const val TAG = "TranscriptionDedupe"

    fun hasActiveWorkForUrl(context: Context, url: String): Boolean {
        return PluctCoreBackground01TranscriptionWorkerJobDeduplication.checkExistingJob(context, url) != null
    }

    /**
     * When a duplicate enqueue is detected, keep user trust: one visible progress card, no silent no-op.
     */
    fun onDuplicateBackgroundRequest(context: Context, url: String) {
        val jobId = PluctCoreBackground01TranscriptionWorkerJobDeduplication.checkExistingJob(context, url)
        Log.d(TAG, "Duplicate background request for url=$url existingJob=$jobId")
        PluctCoreBackground01TranscriptionWorkerJobDeduplication.mergeNotifications(context, jobId ?: "", url)
        val notificationId = PluctNotificationHelper.generateNotificationId(url)
        PluctNotificationHelper.showTranscriptionProgressNotification(
            context = context,
            url = url,
            progress = 0,
            message = "Still processing…",
            notificationId = notificationId
        )
    }
}
