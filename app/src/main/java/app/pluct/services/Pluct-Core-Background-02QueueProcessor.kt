package app.pluct.services

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.pluct.core.network.PluctNetworkConnectivityChecker
import app.pluct.data.repository.PluctVideoRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Entry point for accessing Hilt dependencies in Queue Processor Worker
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface QueueProcessorEntryPoint {
    fun videoRepository(): PluctVideoRepository
    fun apiService(): PluctCoreAPIUnifiedService
    fun queueManager(): PluctQueueManager
}

/**
 * Pluct-Core-Background-02QueueProcessor
 * Background worker to process queued videos periodically
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Sequence][Responsibility]
 */
class PluctQueueProcessorWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val entryPoint: QueueProcessorEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            QueueProcessorEntryPoint::class.java
        )
    }
    
    private val videoRepository: PluctVideoRepository by lazy { entryPoint.videoRepository() }
    private val apiService: PluctCoreAPIUnifiedService by lazy { entryPoint.apiService() }
    private val queueManager: PluctQueueManager by lazy { 
        // Create queue manager with repository
        PluctQueueManager(entryPoint.videoRepository())
    }
    
    companion object {
        private const val TAG = "PluctQueueProcessor"
        private const val WORK_NAME = "pluct_queue_processor"
        private const val REPEAT_INTERVAL_MINUTES = 15L
        
        /**
         * Schedule periodic queue processing
         */
        fun scheduleQueueProcessing(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<PluctQueueProcessorWorker>(
                REPEAT_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            
            Log.d(TAG, "Scheduled periodic queue processing every $REPEAT_INTERVAL_MINUTES minutes")
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Queue processor worker started")
            
            // Check network availability
            val isNetworkAvailable = PluctNetworkConnectivityChecker.isNetworkAvailable(applicationContext)
            if (!isNetworkAvailable) {
                Log.d(TAG, "No network available, skipping queue processing")
                return Result.retry()
            }
            
            // Get current balance (simplified - would need to inject user identification)
            // For now, we'll process videos that don't require credits (NO_INTERNET queued)
            val queuedVideos = videoRepository.getVideosByStatus(app.pluct.data.entity.ProcessingStatus.QUEUED).first()
            
            if (queuedVideos.isEmpty()) {
                Log.d(TAG, "No queued videos to process")
                return Result.success()
            }
            
            Log.d(TAG, "Found ${queuedVideos.size} queued video(s) to process")
            
            // Process videos queued for network issues (they don't need credits)
            var processedCount = 0
            queuedVideos.forEach { video ->
                if (video.queueReason == app.pluct.data.entity.QueueReason.NO_INTERNET) {
                    try {
                        // Update status to PROCESSING
                        val processingVideo = video.copy(
                            status = app.pluct.data.entity.ProcessingStatus.PROCESSING,
                            queueReason = null,
                            queuedAt = null
                        )
                        videoRepository.updateVideo(processingVideo)
                        
                        // Start transcription (simplified - would need full processing logic)
                        // For now, just mark as processing - actual processing happens in foreground
                        Log.d(TAG, "Marked queued video for processing: ${video.url}")
                        processedCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing queued video ${video.url}: ${e.message}", e)
                    }
                }
            }
            
            Log.d(TAG, "Queue processor completed: processed $processedCount video(s)")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Queue processor failed: ${e.message}", e)
            Result.retry()
        }
    }
}

