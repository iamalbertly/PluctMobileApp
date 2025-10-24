/**
 * Pluct-Transcription-01Service-02Monitoring - Transcription monitoring functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

package app.pluct.services

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Transcription-01Service-02Monitoring - Transcription monitoring functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@Singleton
class PluctTranscriptionServiceMonitoring @Inject constructor() {
    
    private val _monitoringState = MutableStateFlow<Map<String, MonitoringInfo>>(emptyMap())
    val monitoringState: StateFlow<Map<String, MonitoringInfo>> = _monitoringState.asStateFlow()
    
    private val activeMonitoring = mutableMapOf<String, Job>()
    
    data class MonitoringInfo(
        val videoId: String,
        val startTime: Long,
        val status: MonitoringStatus,
        val progress: Int = 0,
        val error: String? = null
    )
    
    enum class MonitoringStatus {
        STARTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        TIMEOUT
    }
    
    /**
     * Start monitoring for a video
     */
    fun startMonitoring(videoId: String) {
        Log.d("PluctTranscriptionServiceMonitoring", "🔍 Starting monitoring for video: $videoId")
        
        val monitoringInfo = MonitoringInfo(
            videoId = videoId,
            startTime = System.currentTimeMillis(),
            status = MonitoringStatus.STARTED
        )
        
        val currentState = _monitoringState.value.toMutableMap()
        currentState[videoId] = monitoringInfo
        _monitoringState.value = currentState
        
        // Start background monitoring job
        val monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            monitorProgress(videoId)
        }
        
        activeMonitoring[videoId] = monitoringJob
        
        Log.d("PluctTranscriptionServiceMonitoring", "✅ Monitoring started for video: $videoId")
    }
    
    /**
     * Stop monitoring for a video
     */
    fun stopMonitoring(videoId: String) {
        Log.d("PluctTranscriptionServiceMonitoring", "🛑 Stopping monitoring for video: $videoId")
        
        // Cancel active monitoring job
        activeMonitoring[videoId]?.cancel()
        activeMonitoring.remove(videoId)
        
        // Update monitoring state
        val currentState = _monitoringState.value.toMutableMap()
        val monitoringInfo = currentState[videoId]
        if (monitoringInfo != null) {
            currentState[videoId] = monitoringInfo.copy(
                status = MonitoringStatus.COMPLETED
            )
            _monitoringState.value = currentState
        }
        
        Log.d("PluctTranscriptionServiceMonitoring", "✅ Monitoring stopped for video: $videoId")
    }
    
    /**
     * Monitor progress in background
     */
    private suspend fun monitorProgress(videoId: String) {
        val maxDuration = 160000L // 160 seconds
        val startTime = System.currentTimeMillis()
        
        try {
            while (System.currentTimeMillis() - startTime < maxDuration) {
                // Update progress
                val elapsed = System.currentTimeMillis() - startTime
                val progress = ((elapsed.toFloat() / maxDuration) * 100).toInt().coerceIn(0, 100)
                
                updateMonitoringInfo(videoId) { info ->
                    info.copy(
                        status = MonitoringStatus.IN_PROGRESS,
                        progress = progress
                    )
                }
                
                Log.d("PluctTranscriptionServiceMonitoring", "📊 Video $videoId progress: $progress%")
                
                delay(1000) // Check every second
            }
            
            // Timeout reached
            Log.w("PluctTranscriptionServiceMonitoring", "⏰ Monitoring timeout for video: $videoId")
            updateMonitoringInfo(videoId) { info ->
                info.copy(
                    status = MonitoringStatus.TIMEOUT,
                    error = "Monitoring timeout after ${maxDuration / 1000} seconds"
                )
            }
            
        } catch (e: Exception) {
            Log.e("PluctTranscriptionServiceMonitoring", "❌ Monitoring error for video: $videoId", e)
            updateMonitoringInfo(videoId) { info ->
                info.copy(
                    status = MonitoringStatus.FAILED,
                    error = e.message ?: "Unknown monitoring error"
                )
            }
        }
    }
    
    /**
     * Update monitoring info
     */
    private fun updateMonitoringInfo(videoId: String, update: (MonitoringInfo) -> MonitoringInfo) {
        val currentState = _monitoringState.value.toMutableMap()
        val currentInfo = currentState[videoId]
        if (currentInfo != null) {
            currentState[videoId] = update(currentInfo)
            _monitoringState.value = currentState
        }
    }
    
    /**
     * Get monitoring info for a video
     */
    fun getMonitoringInfo(videoId: String): MonitoringInfo? {
        return _monitoringState.value[videoId]
    }
    
    /**
     * Get all monitoring info
     */
    fun getAllMonitoringInfo(): Map<String, MonitoringInfo> {
        return _monitoringState.value
    }
    
    /**
     * Clear monitoring info for a video
     */
    fun clearMonitoringInfo(videoId: String) {
        val currentState = _monitoringState.value.toMutableMap()
        currentState.remove(videoId)
        _monitoringState.value = currentState
        
        // Cancel any active monitoring
        activeMonitoring[videoId]?.cancel()
        activeMonitoring.remove(videoId)
        
        Log.d("PluctTranscriptionServiceMonitoring", "🧹 Cleared monitoring info for video: $videoId")
    }
    
    /**
     * Clear all monitoring info
     */
    fun clearAllMonitoringInfo() {
        // Cancel all active monitoring
        activeMonitoring.values.forEach { it.cancel() }
        activeMonitoring.clear()
        
        _monitoringState.value = emptyMap()
        
        Log.d("PluctTranscriptionServiceMonitoring", "🧹 Cleared all monitoring info")
    }
}
