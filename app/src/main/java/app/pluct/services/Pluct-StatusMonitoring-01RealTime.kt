package app.pluct.services

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-StatusMonitoring-01RealTime - Real-time status monitoring and updates
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation][CoreResponsibility]
 * Provides real-time status updates and progress tracking
 */
@Singleton
class PluctStatusMonitoringRealTime @Inject constructor(
    private val apiCommunication: PluctBusinessEngineAPICommunication,
    private val errorHandling: PluctErrorHandlingRetryLogic
) {
    
    companion object {
        private const val TAG = "PluctStatusMonitoring"
        private const val STATUS_CHECK_INTERVAL_MS = 2000L
        private const val MAX_STATUS_CHECKS = 150 // 5 minutes max
        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
    }
    
    data class StatusUpdate(
        val jobId: String,
        val status: String,
        val progress: Int,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
        val transcript: String? = null,
        val confidence: Double? = null,
        val language: String? = null,
        val duration: Int? = null,
        val error: String? = null
    )
    
    data class MonitoringSession(
        val jobId: String,
        val startTime: Long,
        val isActive: Boolean = true,
        val checkCount: Int = 0,
        val lastStatus: String? = null,
        val lastProgress: Int = 0
    )
    
    private val activeSessions = mutableMapOf<String, MonitoringSession>()
    private val statusUpdates = MutableSharedFlow<StatusUpdate>()
    private val monitoringScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Start monitoring a transcription job
     */
    suspend fun startMonitoring(
        jobId: String,
        shortLivedToken: String
    ): Flow<StatusUpdate> = withContext(Dispatchers.IO) {
        
        Log.d(TAG, "🔄 Starting real-time monitoring for job: $jobId")
        
        // Create monitoring session
        val session = MonitoringSession(
            jobId = jobId,
            startTime = System.currentTimeMillis()
        )
        activeSessions[jobId] = session
        
        // Start monitoring coroutine
        monitoringScope.launch {
            monitorJobStatus(jobId, shortLivedToken, session)
        }
        
        // Return status updates flow
        statusUpdates.asSharedFlow()
            .filter { it.jobId == jobId }
    }
    
    /**
     * Stop monitoring a specific job
     */
    suspend fun stopMonitoring(jobId: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "🛑 Stopping monitoring for job: $jobId")
        
        activeSessions[jobId]?.let { session ->
            activeSessions[jobId] = session.copy(isActive = false)
        }
    }
    
    /**
     * Stop all monitoring
     */
    suspend fun stopAllMonitoring() = withContext(Dispatchers.IO) {
        Log.d(TAG, "🛑 Stopping all monitoring sessions")
        
        activeSessions.keys.forEach { jobId ->
            stopMonitoring(jobId)
        }
        
        monitoringScope.cancel()
    }
    
    /**
     * Get current status of a job
     */
    suspend fun getCurrentStatus(jobId: String, shortLivedToken: String): StatusUpdate? = withContext(Dispatchers.IO) {
        val session = activeSessions[jobId]
        if (session == null || !session.isActive) {
            Log.w(TAG, "⚠️ No active monitoring session for job: $jobId")
            return@withContext null
        }
        
        try {
            // Get fresh status from API
            val statusResult = apiCommunication.checkTranscriptionStatus(shortLivedToken, jobId)
            
            if (statusResult.isFailure) {
                val error = statusResult.exceptionOrNull()?.message ?: "Status check failed"
                val errorUpdate = StatusUpdate(
                    jobId = jobId,
                    status = "error",
                    progress = 0,
                    message = "Failed to check status: $error",
                    error = error
                )
                return@withContext errorUpdate
            }
            
            val statusResponse = statusResult.getOrNull()!!
            val statusUpdate = StatusUpdate(
                jobId = jobId,
                status = statusResponse.status,
                progress = statusResponse.progress,
                message = getStatusMessage(statusResponse.status, statusResponse.progress),
                transcript = statusResponse.transcript,
                confidence = statusResponse.confidence,
                language = statusResponse.language,
                duration = statusResponse.duration,
                error = null
            )
            
            Log.d(TAG, "📊 Status update for $jobId: ${statusResponse.status} (${statusResponse.progress}%)")
            
            return@withContext statusUpdate
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get status for job $jobId: ${e.message}", e)
            
            val errorUpdate = StatusUpdate(
                jobId = jobId,
                status = "error",
                progress = 0,
                message = "Failed to check status: ${e.message}",
                error = e.message
            )
            
            return@withContext errorUpdate
        }
    }
    
    /**
     * Monitor job status with real-time updates
     */
    private suspend fun monitorJobStatus(
        jobId: String,
        shortLivedToken: String,
        session: MonitoringSession
    ) {
        var checkCount = 0
        var lastStatus: String? = null
        var lastProgress = 0
        
        Log.d(TAG, "🔄 Starting status monitoring for job: $jobId")
        
        try {
            while (session.isActive && checkCount < MAX_STATUS_CHECKS) {
                checkCount++
                
                Log.d(TAG, "🔍 Status check $checkCount/$MAX_STATUS_CHECKS for job: $jobId")
                
                // Get current status
                val statusUpdate = getCurrentStatus(jobId, shortLivedToken)
                if (statusUpdate != null) {
                    // Emit status update
                    statusUpdates.emit(statusUpdate)
                    
                    // Update session
                    activeSessions[jobId] = session.copy(
                        checkCount = checkCount,
                        lastStatus = statusUpdate.status,
                        lastProgress = statusUpdate.progress
                    )
                    
                    // Check if job is complete
                    if (isJobComplete(statusUpdate.status)) {
                        Log.d(TAG, "✅ Job $jobId completed with status: ${statusUpdate.status}")
                        break
                    }
                    
                    // Check for errors
                    if (statusUpdate.status == "failed" || statusUpdate.error != null) {
                        Log.e(TAG, "❌ Job $jobId failed: ${statusUpdate.error}")
                        break
                    }
                }
                
                // Wait before next check
                delay(STATUS_CHECK_INTERVAL_MS)
            }
            
            if (checkCount >= MAX_STATUS_CHECKS) {
                Log.w(TAG, "⏰ Monitoring timeout for job: $jobId")
                
                val timeoutUpdate = StatusUpdate(
                    jobId = jobId,
                    status = "timeout",
                    progress = lastProgress,
                    message = "Monitoring timeout - job may still be processing",
                    error = "Monitoring timeout after $MAX_STATUS_CHECKS checks"
                )
                
                statusUpdates.emit(timeoutUpdate)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Monitoring error for job $jobId: ${e.message}", e)
            
            val errorUpdate = StatusUpdate(
                jobId = jobId,
                status = "error",
                progress = lastProgress,
                message = "Monitoring error: ${e.message}",
                error = e.message
            )
            
            statusUpdates.emit(errorUpdate)
        } finally {
            // Clean up session
            activeSessions.remove(jobId)
            Log.d(TAG, "🧹 Cleaned up monitoring session for job: $jobId")
        }
    }
    
    /**
     * Check if job is complete
     */
    private fun isJobComplete(status: String): Boolean {
        return status in listOf("completed", "failed", "cancelled", "timeout")
    }
    
    /**
     * Get human-readable status message
     */
    private fun getStatusMessage(status: String, progress: Int): String {
        return when (status) {
            "queued" -> "Queued for processing..."
            "processing" -> "Processing... $progress%"
            "completed" -> "Transcription completed successfully"
            "failed" -> "Transcription failed"
            "cancelled" -> "Transcription cancelled"
            "timeout" -> "Processing timeout"
            "error" -> "Error occurred during processing"
            else -> "Status: $status ($progress%)"
        }
    }
    
    /**
     * Get monitoring statistics
     */
    fun getMonitoringStats(): Map<String, Any> {
        val activeCount = activeSessions.count { it.value.isActive }
        val totalChecks = activeSessions.values.sumOf { it.checkCount }
        val averageChecks = if (activeSessions.isNotEmpty()) totalChecks / activeSessions.size else 0
        
        return mapOf(
            "activeSessions" to activeCount,
            "totalSessions" to activeSessions.size,
            "totalChecks" to totalChecks,
            "averageChecks" to averageChecks,
            "sessions" to activeSessions.mapValues { (_, session) ->
                mapOf(
                    "jobId" to session.jobId,
                    "isActive" to session.isActive,
                    "checkCount" to session.checkCount,
                    "lastStatus" to session.lastStatus,
                    "lastProgress" to session.lastProgress,
                    "duration" to (System.currentTimeMillis() - session.startTime)
                )
            }
        )
    }
    
    /**
     * Force refresh all active sessions
     */
    suspend fun refreshAllSessions(shortLivedToken: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔄 Refreshing all active sessions")
        
        activeSessions.forEach { (jobId, session) ->
            if (session.isActive) {
                try {
                    val statusUpdate = getCurrentStatus(jobId, shortLivedToken)
                    if (statusUpdate != null) {
                        statusUpdates.emit(statusUpdate)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to refresh session $jobId: ${e.message}", e)
                }
            }
        }
    }
    
    /**
     * Get active session count
     */
    fun getActiveSessionCount(): Int {
        return activeSessions.count { it.value.isActive }
    }
    
    /**
     * Check if job is being monitored
     */
    fun isJobMonitored(jobId: String): Boolean {
        return activeSessions.containsKey(jobId) && activeSessions[jobId]?.isActive == true
    }
    
    /**
     * Get session information
     */
    fun getSessionInfo(jobId: String): MonitoringSession? {
        return activeSessions[jobId]
    }
}
