package app.pluct.services

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.pluct.core.network.PluctNetworkConnectivityChecker
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.QueueReason
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.api.PluctCoreAPITranscriptionResult01Extractor
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
    companion object {
        private const val TAG = "PluctQueueProcessor"
        private const val WORK_NAME = "pluct_queue_processor"
        private const val IMMEDIATE_WORK_NAME = "pluct_queue_processor_immediate"
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
        
        /**
         * UX FIX #1: Trigger immediate queue processing (e.g., when network is restored)
         */
        fun triggerImmediateProcessing(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<PluctQueueProcessorWorker>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
            Log.d(TAG, "Triggered immediate queue processing")
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
            
            val queuedVideos = videoRepository.getVideosByStatus(ProcessingStatus.QUEUED).first()
            
            if (queuedVideos.isEmpty()) {
                Log.d(TAG, "No queued videos to process")
                return Result.success()
            }
            
            Log.d(TAG, "Found ${queuedVideos.size} queued video(s) to process")
            
            var processedCount = 0
            val eligibleVideo = queuedVideos.firstOrNull { video ->
                video.queueReason == QueueReason.NO_INTERNET || video.queueReason == QueueReason.SERVICE_UNAVAILABLE
            }

            if (eligibleVideo != null) {
                try {
                    val processingVideo = eligibleVideo.copy(
                        status = ProcessingStatus.PROCESSING,
                        queueReason = null,
                        queuedAt = null,
                        failureReason = null
                    )
                    videoRepository.updateVideo(processingVideo)

                    val transcriptionResult = apiService.processTikTokVideo(eligibleVideo.url, isBackground = true)
                    transcriptionResult.fold(
                        onSuccess = { statusResponse ->
                            val extraction = PluctCoreAPITranscriptionResult01Extractor.extract(statusResponse)
                            val transcriptText = extraction.transcript
                            if (!transcriptText.isNullOrBlank()) {
                                videoRepository.updateVideo(processingVideo.copy(
                                    status = ProcessingStatus.COMPLETED,
                                    progress = 100,
                                    transcript = transcriptText,
                                    jobId = statusResponse.jobId,
                                    transcriptCachedAt = System.currentTimeMillis()
                                ))
                                processedCount++
                                Log.d(TAG, "Completed queued video in background: ${eligibleVideo.url}")
                            } else {
                                videoRepository.updateVideo(processingVideo.copy(
                                    status = ProcessingStatus.FAILED,
                                    failureReason = "Transcription completed but no transcript found"
                                ))
                                Log.w(TAG, "Queued video completed without transcript: ${eligibleVideo.url}")
                            }
                        },
                        onFailure = { error ->
                            val message = error.message ?: "Transcription failed"
                            val retryReason = when {
                                message.contains("credit", ignoreCase = true) -> QueueReason.INSUFFICIENT_CREDITS
                                message.contains("network", ignoreCase = true) -> QueueReason.NO_INTERNET
                                message.contains("connection", ignoreCase = true) -> QueueReason.NO_INTERNET
                                message.contains("waking", ignoreCase = true) -> QueueReason.SERVICE_UNAVAILABLE
                                message.contains("temporarily", ignoreCase = true) -> QueueReason.SERVICE_UNAVAILABLE
                                message.contains("service", ignoreCase = true) -> QueueReason.SERVICE_UNAVAILABLE
                                else -> null
                            }

                            if (retryReason != null) {
                                videoRepository.updateVideo(eligibleVideo.copy(
                                    status = ProcessingStatus.QUEUED,
                                    queueReason = retryReason,
                                    queuedAt = System.currentTimeMillis(),
                                    failureReason = message
                                ))
                                Log.w(TAG, "Requeued video after retryable background failure: $message")
                            } else {
                                videoRepository.updateVideo(processingVideo.copy(
                                    status = ProcessingStatus.FAILED,
                                    failureReason = message
                                ))
                                Log.e(TAG, "Queued video failed in background: $message")
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing queued video ${eligibleVideo.url}: ${e.message}", e)
                    videoRepository.updateVideo(eligibleVideo.copy(
                        status = ProcessingStatus.QUEUED,
                        queuedAt = System.currentTimeMillis(),
                        failureReason = e.message ?: "Queue processing failed"
                    ))
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
