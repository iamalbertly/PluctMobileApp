package app.pluct.ui.screens

import android.content.Context
import android.util.Log
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.QueueReason
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.notification.PluctNotificationHelper
import app.pluct.services.PluctQueueManager
import kotlinx.coroutines.flow.first

/**
 * Pluct-UI-Screen-01MainActivity-01IntentHandler-02QueueManager
 * Follows naming convention: [Project]-[UI]-[Screen]-[MainActivity]-[IntentHandler]-[QueueManager]
 * 6 scope layers: Project, UI, Screen, MainActivity, IntentHandler, QueueManager
 * Handles intent queueing when processing is active
 */
object PluctUIScreen01MainActivityIntentHandlerQueueManager {
    private const val TAG = "IntentQueueManager"
    
    /**
     * Check if processing is currently active
     * Must be called from coroutine scope
     */
    suspend fun isProcessingActive(
        videoRepository: PluctVideoRepository
    ): Boolean {
        val processingVideos = videoRepository.getVideosByStatus(ProcessingStatus.PROCESSING)
            .first() // Get current value from Flow
        return processingVideos.isNotEmpty()
    }
    
    /**
     * Queue intent if processing is active
     * Returns true if intent was queued, false if processing is not active
     * Must be called from coroutine scope
     */
    suspend fun queueIntentIfProcessing(
        url: String,
        context: Context,
        videoRepository: PluctVideoRepository,
        queueManager: PluctQueueManager
    ): Boolean {
        if (!isProcessingActive(videoRepository)) {
            Log.d(TAG, "Processing not active, intent can proceed immediately")
            return false
        }
        
        Log.d(TAG, "Processing active, queueing intent for URL: $url")
        
        // Queue the video
        queueManager.queueVideo(
            url = url,
            tier = app.pluct.data.entity.ProcessingTier.EXTRACT_SCRIPT,
            reason = QueueReason.SERVICE_UNAVAILABLE // Processing another video
        )
        
        // Show notification for queued video
        val queuedCount = videoRepository.getVideosByStatus(ProcessingStatus.QUEUED)
            .first().size
        app.pluct.notification.PluctQueueNotificationManager.showQueueNotification(
            context = context,
            queuedCount = queuedCount,
            message = "Video queued. Will process after current transcription completes."
        )
        
        return true
    }
    
    /**
     * Process queued intents when ready
     * Must be called from coroutine scope
     */
    suspend fun processQueuedIntents(
        videoRepository: PluctVideoRepository,
        queueManager: PluctQueueManager,
        onProcess: (String) -> Unit
    ) {
        if (!isProcessingActive(videoRepository)) {
            val queuedVideos = videoRepository.getVideosByStatus(ProcessingStatus.QUEUED)
                .first()
            
            queuedVideos.forEach { video ->
                if (video.queueReason == QueueReason.SERVICE_UNAVAILABLE) {
                    Log.d(TAG, "Processing queued intent: ${video.url}")
                    onProcess(video.url)
                }
            }
        }
    }
}

