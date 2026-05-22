package app.pluct.services

import android.util.Log
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.shared.PluctRequestIds
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Core-API-01UnifiedService-03Deduplication-01Coordinator
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[Deduplication]-[Coordinator]
 * 6 scope layers: Project, Core, API, UnifiedService, Deduplication, Coordinator
 * Single source of truth for duplicate prevention combining:
 * - ProcessingLock (active URLs with Jobs)
 * - RequestDeduplication (request IDs for idempotency)
 * - DuplicateGuard (database checks)
 */
@Singleton
class PluctCoreAPI01UnifiedService03Deduplication01Coordinator @Inject constructor() {
    companion object {
        private const val TAG = "DeduplicationCoordinator"
    }

    // ProcessingLock: Track active URLs with Jobs
    private val activeProcessingUrls = ConcurrentHashMap<String, Job>()
    
    // RequestDeduplication: Track request IDs and cached responses
    private val activeRequests = ConcurrentHashMap<String, RequestState>()
    private val cachedResponses = ConcurrentHashMap<String, CachedResponse>()
    
    private val mutex = Mutex()

    data class RequestState(
        val requestId: String,
        val url: String,
        val timestamp: Long,
        val inProgress: Boolean = true
    )

    data class CachedResponse(
        val requestId: String,
        val response: Any,
        val timestamp: Long,
        val expiresAt: Long
    )

    sealed class DeduplicationResult {
        data class AlreadyProcessing(val reason: String, val existingVideo: VideoItem? = null) : DeduplicationResult()
        data class Registered(val requestId: String, val video: VideoItem) : DeduplicationResult()
        data class Failure(val reason: String) : DeduplicationResult()
    }

    /**
     * Check and register processing - combines all three deduplication approaches
     * @param url The video URL to process
     * @param job The coroutine job for this processing
     * @param videoRepository Repository to check database for existing entries
     * @param tier Processing tier (default EXTRACT_SCRIPT)
     * @return DeduplicationResult indicating if processing can proceed
     */
    suspend fun checkAndRegisterProcessing(
        url: String,
        job: Job,
        videoRepository: PluctVideoRepository?,
        tier: app.pluct.data.entity.ProcessingTier = app.pluct.data.entity.ProcessingTier.EXTRACT_SCRIPT
    ): DeduplicationResult {
        return mutex.withLock {
            // 1. Check ProcessingLock: Is URL already being processed?
            val existingJob = activeProcessingUrls[url]
            if (existingJob != null && existingJob.isActive) {
                Log.w(TAG, "URL $url is already being processed (ProcessingLock check)")
                return DeduplicationResult.AlreadyProcessing("URL is already being processed")
            }

            // 2. Check DuplicateGuard: Is there a processing entry in database?
            if (videoRepository != null) {
                val processingVideo = videoRepository.getProcessingVideoByUrl(url)
                if (processingVideo != null && processingVideo.status == ProcessingStatus.PROCESSING) {
                    if (processingVideo.jobId.isNullOrBlank()) {
                        Log.w(TAG, "URL $url has stale processing entry without jobId; allowing retry")
                    } else {
                        Log.w(TAG, "URL $url has existing processing entry in database (DuplicateGuard check)")
                        return DeduplicationResult.AlreadyProcessing("Video is already being processed", processingVideo)
                    }
                }
            }

            // 3. Check RequestDeduplication: Is there an active request for this URL?
            val existingRequest = activeRequests.values.firstOrNull { 
                it.url == url && it.inProgress 
            }
            val requestId = if (existingRequest != null) {
                Log.d(TAG, "Reusing existing request ID for URL: ${existingRequest.requestId}")
                existingRequest.requestId
            } else {
                PluctRequestIds.generate()
            }

            // 4. Create or update database entry (DuplicateGuard logic)
            val now = System.currentTimeMillis()
            val existingVideo = videoRepository?.getVideoByUrl(url)
            val baselineVideo = existingVideo ?: VideoItem(
                id = now.toString(),
                url = url,
                title = "",
                thumbnailUrl = "",
                author = "",
                duration = 0L,
                status = ProcessingStatus.PROCESSING,
                progress = 0,
                transcript = null,
                timestamp = now,
                tier = tier,
                createdAt = now
            )

            val preparedVideo = baselineVideo.copy(
                status = ProcessingStatus.PROCESSING,
                progress = 0,
                transcript = null,
                failureReason = null,
                errorDetails = null,
                queueReason = null,
                queuedAt = null,
                jobId = null,
                timestamp = now,
                tier = tier
            )

            if (videoRepository != null) {
                val insertResult = videoRepository.insertVideo(preparedVideo)
                if (insertResult.isFailure) {
                    val reason = insertResult.exceptionOrNull()?.message ?: "Failed to persist processing entry"
                    Log.e(TAG, "Persist hook failed for $url: $reason")
                    return DeduplicationResult.Failure(reason)
                }
            }

            // 5. Register in ProcessingLock
            activeProcessingUrls[url] = job
            Log.d(TAG, "Registered processing for URL: $url (total active: ${getActiveProcessingCount()})")

            // 6. Register in RequestDeduplication
            if (existingRequest == null) {
                activeRequests[requestId] = RequestState(
                    requestId = requestId,
                    url = url,
                    timestamp = now
                )
                Log.d(TAG, "Generated new request ID: $requestId for URL: $url")
            }

            return DeduplicationResult.Registered(requestId, preparedVideo)
        }
    }

    /**
     * Check if URL is currently being processed (ProcessingLock check)
     */
    fun isUrlProcessing(url: String): Boolean {
        val job = activeProcessingUrls[url]
        val isProcessing = job != null && job.isActive
        if (isProcessing) {
            Log.d(TAG, "URL $url is currently being processed (duplicate prevention check)")
        }
        return isProcessing
    }

    /**
     * Unregister URL processing (call when done or failed)
     */
    suspend fun unregisterProcessing(url: String, requestId: String? = null) {
        mutex.withLock {
            val removed = activeProcessingUrls.remove(url)
            if (removed != null) {
                Log.d(TAG, "Unregistered processing for URL: $url (remaining active: ${getActiveProcessingCount()})")
            } else {
                Log.w(TAG, "Attempted to unregister URL $url but it was not in active processing list")
            }

            // Also mark request as completed
            requestId?.let {
                activeRequests.remove(it)
                Log.d(TAG, "Request completed: $it")
            }
        }
    }

    /**
     * Cache response for idempotency (RequestDeduplication)
     */
    suspend fun cacheResponse(requestId: String, response: Any, ttlSeconds: Int = 300) {
        mutex.withLock {
            val expiresAt = System.currentTimeMillis() + (ttlSeconds * 1000L)
            cachedResponses[requestId] = CachedResponse(
                requestId = requestId,
                response = response,
                timestamp = System.currentTimeMillis(),
                expiresAt = expiresAt
            )
            Log.d(TAG, "Response cached for request ID: $requestId, expires in ${ttlSeconds}s")
        }
    }

    /**
     * Get cached response if available and not expired (RequestDeduplication)
     */
    suspend fun getCachedResponse(requestId: String): Any? {
        return mutex.withLock {
            val cached = cachedResponses[requestId]
            if (cached != null) {
                val now = System.currentTimeMillis()
                if (now < cached.expiresAt) {
                    Log.d(TAG, "Returning cached response for request ID: $requestId")
                    return cached.response
                } else {
                    // Expired, remove it
                    cachedResponses.remove(requestId)
                    Log.d(TAG, "Cached response expired for request ID: $requestId")
                }
            }
            null
        }
    }

    /**
     * Clean up inactive jobs (ProcessingLock)
     */
    fun cleanupInactiveJobs() {
        val inactive = activeProcessingUrls.filter { !it.value.isActive }
        inactive.forEach { (url, _) ->
            activeProcessingUrls.remove(url)
            Log.d(TAG, "Cleaned up inactive job for URL: $url")
        }
    }

    /**
     * Cleanup expired cached responses (RequestDeduplication)
     */
    suspend fun cleanupExpiredResponses() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val expired = cachedResponses.values.filter { it.expiresAt < now }
            expired.forEach { cached ->
                cachedResponses.remove(cached.requestId)
                Log.d(TAG, "Cleaned up expired cached response: ${cached.requestId}")
            }
        }
    }

    /**
     * Get count of active processing jobs (ProcessingLock)
     */
    fun getActiveProcessingCount(): Int {
        cleanupInactiveJobs()
        return activeProcessingUrls.size
    }

    /**
     * Get active request count (RequestDeduplication)
     */
    fun getActiveRequestCount(): Int {
        return activeRequests.values.count { it.inProgress }
    }
}
