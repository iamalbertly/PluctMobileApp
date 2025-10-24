package app.pluct.services

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * Pluct-Status-01RealTimeUpdates - Real-time status updates and progress tracking
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@Singleton
class PluctStatusRealTimeUpdates @Inject constructor() {
    
    private val _statusUpdates = MutableStateFlow<Map<String, StatusUpdate>>(emptyMap())
    val statusUpdates: StateFlow<Map<String, StatusUpdate>> = _statusUpdates.asStateFlow()
    
    private val _progressUpdates = MutableStateFlow<Map<String, ProgressUpdate>>(emptyMap())
    val progressUpdates: StateFlow<Map<String, ProgressUpdate>> = _progressUpdates.asStateFlow()
    
    private val statusUpdateScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    data class StatusUpdate(
        val videoId: String,
        val status: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
        val details: Map<String, Any> = emptyMap()
    )
    
    data class ProgressUpdate(
        val videoId: String,
        val progress: Int,
        val stage: String,
        val estimatedTimeRemaining: Long? = null,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Update status for a video
     */
    fun updateStatus(videoId: String, status: String, message: String, details: Map<String, Any> = emptyMap()) {
        val update = StatusUpdate(videoId, status, message, System.currentTimeMillis(), details)
        val currentUpdates = _statusUpdates.value.toMutableMap()
        currentUpdates[videoId] = update
        _statusUpdates.value = currentUpdates
        
        Log.d("PluctStatusRealTimeUpdates", "Status updated for $videoId: $status - $message")
    }
    
    /**
     * Update progress for a video
     */
    fun updateProgress(videoId: String, progress: Int, stage: String, estimatedTimeRemaining: Long? = null) {
        val update = ProgressUpdate(videoId, progress, stage, estimatedTimeRemaining, System.currentTimeMillis())
        val currentUpdates = _progressUpdates.value.toMutableMap()
        currentUpdates[videoId] = update
        _progressUpdates.value = currentUpdates
        
        Log.d("PluctStatusRealTimeUpdates", "Progress updated for $videoId: $progress% - $stage")
    }
    
    /**
     * Get status for a specific video
     */
    fun getStatusForVideo(videoId: String): StatusUpdate? {
        return _statusUpdates.value[videoId]
    }
    
    /**
     * Get progress for a specific video
     */
    fun getProgressForVideo(videoId: String): ProgressUpdate? {
        return _progressUpdates.value[videoId]
    }
    
    /**
     * Clear updates for a completed video
     */
    fun clearUpdatesForVideo(videoId: String) {
        val currentStatusUpdates = _statusUpdates.value.toMutableMap()
        val currentProgressUpdates = _progressUpdates.value.toMutableMap()
        
        currentStatusUpdates.remove(videoId)
        currentProgressUpdates.remove(videoId)
        
        _statusUpdates.value = currentStatusUpdates
        _progressUpdates.value = currentProgressUpdates
        
        Log.d("PluctStatusRealTimeUpdates", "Cleared updates for $videoId")
    }
    
    /**
     * Start real-time monitoring for a video
     */
    fun startMonitoring(videoId: String, onStatusChange: (StatusUpdate) -> Unit, onProgressChange: (ProgressUpdate) -> Unit) {
        statusUpdateScope.launch {
            // Monitor status changes
            _statusUpdates.collect { updates ->
                updates[videoId]?.let { statusUpdate ->
                    onStatusChange(statusUpdate)
                }
            }
        }
        
        statusUpdateScope.launch {
            // Monitor progress changes
            _progressUpdates.collect { updates ->
                updates[videoId]?.let { progressUpdate ->
                    onProgressChange(progressUpdate)
                }
            }
        }
    }
    
    /**
     * Stop monitoring for a video
     */
    fun stopMonitoring(videoId: String) {
        clearUpdatesForVideo(videoId)
    }
    
    /**
     * Get all active videos being monitored
     */
    fun getActiveVideos(): Set<String> {
        return _statusUpdates.value.keys.union(_progressUpdates.value.keys)
    }
    
    /**
     * Clean up old updates (older than 1 hour)
     */
    fun cleanupOldUpdates() {
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        
        val currentStatusUpdates = _statusUpdates.value.filter { (_, update) ->
            update.timestamp > oneHourAgo
        }
        
        val currentProgressUpdates = _progressUpdates.value.filter { (_, update) ->
            update.timestamp > oneHourAgo
        }
        
        _statusUpdates.value = currentStatusUpdates
        _progressUpdates.value = currentProgressUpdates
        
        Log.d("PluctStatusRealTimeUpdates", "Cleaned up old updates")
    }
}
