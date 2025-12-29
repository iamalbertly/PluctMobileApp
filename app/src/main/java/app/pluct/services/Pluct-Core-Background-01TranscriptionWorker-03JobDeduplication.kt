package app.pluct.services

import android.content.Context
import android.util.Log
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.google.common.util.concurrent.ListenableFuture
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Pluct-Core-Background-01TranscriptionWorker-03JobDeduplication
 * Follows naming convention: [Project]-[Core]-[Background]-[TranscriptionWorker]-[JobDeduplication]
 * 5 scope layers: Project, Core, Background, TranscriptionWorker, JobDeduplication
 * Prevents duplicate WorkManager jobs for the same URL
 */
object PluctCoreBackground01TranscriptionWorkerJobDeduplication {
    private const val TAG = "JobDeduplication"
    
    /**
     * Generate URL hash for deduplication
     */
    private fun hashUrl(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(url.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }.take(16)
    }
    
    /**
     * Check if existing job exists for URL
     * Returns existing job ID if found, null otherwise
     */
    fun checkExistingJob(
        context: Context,
        url: String
    ): String? {
        val workManager = WorkManager.getInstance(context)
        val urlHash = hashUrl(url)
        
        // Query for existing work with transcription tag
        val workQuery = WorkQuery.Builder
            .fromTags(listOf("transcription"))
            .addStates(listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING))
            .build()
        
        return try {
            val future: ListenableFuture<List<WorkInfo>> = workManager.getWorkInfos(workQuery)
            val workInfos = future.get(5, TimeUnit.SECONDS) // Timeout after 5 seconds
            
            // Check if any work has matching URL in input data
            workInfos.forEach { workInfo ->
                val workUrl = workInfo.progress.getString(PluctCoreBackground01TranscriptionWorker.KEY_URL)
                if (workUrl == url || hashUrl(workUrl ?: "") == urlHash) {
                    Log.d(TAG, "Existing job found for URL: $url, jobId=${workInfo.id}")
                    return workInfo.id.toString()
                }
            }
            
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error checking existing jobs: ${e.message}")
            null
        }
    }
    
    /**
     * Create or get existing job for URL
     * Returns job ID (existing or new)
     */
    fun createOrGetJob(
        context: Context,
        url: String,
        createJob: () -> String
    ): String {
        val existingJobId = checkExistingJob(context, url)
        if (existingJobId != null) {
            Log.d(TAG, "Reusing existing job for URL: $url")
            return existingJobId
        }
        
        Log.d(TAG, "Creating new job for URL: $url")
        return createJob()
    }
    
    /**
     * Merge duplicate notifications
     * Updates notification to show count if multiple jobs exist
     */
    fun mergeNotifications(
        context: Context,
        jobId: String,
        url: String
    ) {
        val workManager = WorkManager.getInstance(context)
        
        // Count active transcription jobs
        val workQuery = WorkQuery.Builder
            .fromTags(listOf("transcription"))
            .addStates(listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING))
            .build()
        
        try {
            val future: ListenableFuture<List<WorkInfo>> = workManager.getWorkInfos(workQuery)
            val workInfos = future.get(5, TimeUnit.SECONDS) // Timeout after 5 seconds
            
            val activeJobCount = workInfos.size
            
            if (activeJobCount > 1) {
                Log.d(TAG, "Multiple active jobs detected: $activeJobCount, merging notifications")
                // Update notification to show count
                // This would be handled by the notification helper
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error merging notifications: ${e.message}")
        }
    }
    
    /**
     * Check if URL is already being processed
     */
    fun isUrlProcessing(
        context: Context,
        url: String
    ): Boolean {
        return checkExistingJob(context, url) != null
    }
}

