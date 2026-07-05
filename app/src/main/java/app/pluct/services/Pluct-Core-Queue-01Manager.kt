package app.pluct.services

import android.util.Log
import app.pluct.core.network.PluctNetworkConnectivityChecker
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.QueueReason
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctVideoRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Core-Queue-01Manager
 * Manages video queuing for offline/no-credit scenarios
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Sequence][Responsibility]
 */
class PluctQueueManager(
    private val videoRepository: PluctVideoRepository
) {
    companion object {
        private const val TAG = "PluctQueueManager"
    }
    
    /**
     * Queue a video for later processing
     * @param url The video URL to queue
     * @param tier The processing tier
     * @param reason The reason for queuing
     * @param metadata Optional metadata if available
     * @return Result with the queued VideoItem
     */
    suspend fun queueVideo(
        url: String,
        tier: ProcessingTier,
        reason: QueueReason,
        metadata: MetadataResponse? = null
    ): Result<VideoItem> {
        return try {
            Log.d(TAG, "Queueing video: $url, reason: $reason, tier: $tier")
            
            // Check if video already exists in queue
            val existingVideo = videoRepository.getVideoByUrl(url)
            if (existingVideo != null && existingVideo.status == ProcessingStatus.QUEUED) {
                Log.d(TAG, "Video already queued: $url")
                return Result.success(existingVideo)
            }
            if (existingVideo != null && existingVideo.status == ProcessingStatus.COMPLETED) {
                Log.d(TAG, "Video already completed, not queueing duplicate: $url")
                return Result.success(existingVideo)
            }
            if (existingVideo != null) {
                val queuedExisting = existingVideo.copy(
                    status = ProcessingStatus.QUEUED,
                    progress = 0,
                    tier = tier,
                    queueReason = reason,
                    queuedAt = System.currentTimeMillis(),
                    title = metadata?.title?.takeIf { it.isNotBlank() } ?: existingVideo.title,
                    thumbnailUrl = metadata?.thumbnail ?: existingVideo.thumbnailUrl,
                    author = metadata?.author?.removePrefix("@") ?: existingVideo.author,
                    duration = metadata?.duration?.toLong() ?: existingVideo.duration
                )
                return videoRepository.updateVideo(queuedExisting).map { queuedExisting }
            }
            
            val videoId = System.currentTimeMillis().toString()
            val inferredAuthor = metadata?.author?.takeIf { it.isNotBlank() }
                ?: Regex("""/(@[A-Za-z0-9_.-]+)/""").find(url)?.groupValues?.getOrNull(1).orEmpty()
            val queuedVideo = VideoItem(
                id = videoId,
                url = url,
                title = metadata?.title?.takeIf { it.isNotBlank() } ?: "Waiting for text",
                thumbnailUrl = metadata?.thumbnail ?: "",
                author = inferredAuthor.removePrefix("@"),
                duration = metadata?.duration?.toLong() ?: 0L,
                status = ProcessingStatus.QUEUED,
                progress = 0,
                transcript = null,
                timestamp = System.currentTimeMillis(),
                tier = tier,
                queueReason = reason,
                queuedAt = System.currentTimeMillis()
            )
            
            val insertResult = videoRepository.insertVideo(queuedVideo)
            if (insertResult.isSuccess) {
                Log.d(TAG, "Video queued successfully: $url, reason: $reason")
                Result.success(queuedVideo)
            } else {
                val error = insertResult.exceptionOrNull() ?: Exception("Unknown error")
                Log.e(TAG, "Failed to queue video: ${error.message}", error)
                Result.failure(error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error queueing video: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Process queued videos that meet conditions
     * @param apiService API service for processing
     * @param currentBalance Current credit balance
     * @param currentFreeUses Current free uses remaining
     * @param isNetworkAvailable Network availability status
     * @param onProcess Callback to process a video (should call TranscriptionOrchestrator.processVideo)
     * @return Number of videos processed
     */
    suspend fun processQueuedVideos(
        apiService: PluctCoreAPIUnifiedService,
        currentBalance: Int,
        currentFreeUses: Int,
        isNetworkAvailable: Boolean,
        onProcess: suspend (VideoItem) -> Unit
    ): Int {
        return try {
            Log.d(TAG, "Processing queued videos: balance=$currentBalance, freeUses=$currentFreeUses, network=$isNetworkAvailable")
            
            val queuedVideos = videoRepository.getVideosByStatus(ProcessingStatus.QUEUED).first()
            if (queuedVideos.isEmpty()) {
                Log.d(TAG, "No queued videos to process")
                return 0
            }
            
            var processedCount = 0
            var availableBudget = currentBalance + currentFreeUses
            queuedVideos.forEach { video ->
                val canProcess = when (video.queueReason) {
                    QueueReason.INSUFFICIENT_CREDITS -> {
                        val required = when (video.tier) {
                            ProcessingTier.EXTRACT_SCRIPT -> availableBudget >= 1
                            ProcessingTier.GENERATE_INSIGHTS,
                            ProcessingTier.AI_ANALYSIS -> availableBudget >= 2
                            else -> false
                        }
                        if (required) {
                            Log.d(TAG, "Video eligible for processing (credits available): ${video.url}")
                        }
                        required
                    }
                    QueueReason.NO_INTERNET -> {
                        if (isNetworkAvailable) {
                            Log.d(TAG, "Video eligible for processing (network available): ${video.url}")
                        }
                        isNetworkAvailable
                    }
                    QueueReason.RATE_LIMITED -> {
                        // Retry after delay - for now, allow retry
                        Log.d(TAG, "Video eligible for retry (rate limit): ${video.url}")
                        true
                    }
                    QueueReason.SERVICE_UNAVAILABLE -> {
                        // Retry - service may be back
                        Log.d(TAG, "Video eligible for retry (service): ${video.url}")
                        true
                    }
                    null -> {
                        Log.w(TAG, "Video has no queue reason: ${video.url}")
                        false
                    }
                }
                
                if (canProcess) {
                    try {
                        // Update status to PROCESSING before processing
                        val processingVideo = video.copy(
                            status = ProcessingStatus.PROCESSING,
                            queueReason = null,
                            queuedAt = null
                        )
                        videoRepository.updateVideo(processingVideo)
                        
                        // Process the video
                        onProcess(processingVideo)
                        availableBudget = (availableBudget - when (video.tier) {
                            ProcessingTier.EXTRACT_SCRIPT -> 1
                            ProcessingTier.GENERATE_INSIGHTS,
                            ProcessingTier.AI_ANALYSIS -> 2
                            else -> 1
                        }).coerceAtLeast(0)
                        processedCount++
                        Log.d(TAG, "Processed queued video: ${video.url}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing queued video ${video.url}: ${e.message}", e)
                        // Revert to QUEUED status on error
                        videoRepository.updateVideo(video.copy(status = ProcessingStatus.QUEUED))
                    }
                }
            }
            
            Log.d(TAG, "Processed $processedCount out of ${queuedVideos.size} queued videos")
            processedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error processing queued videos: ${e.message}", e)
            0
        }
    }
    
    /**
     * Get count of queued videos
     */
    suspend fun getQueuedCount(): Int {
        return try {
            videoRepository.getVideoCountByStatus(ProcessingStatus.QUEUED)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting queued count: ${e.message}", e)
            0
        }
    }
    
    /**
     * Remove video from queue
     */
    suspend fun removeFromQueue(video: VideoItem): Result<Unit> {
        return try {
            Log.d(TAG, "Removing video from queue: ${video.url}")
            videoRepository.deleteVideo(video)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing video from queue: ${e.message}", e)
            Result.failure(e)
        }
    }
}
