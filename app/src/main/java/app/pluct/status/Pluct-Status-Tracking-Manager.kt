package app.pluct.status

import android.content.Context
import android.util.Log
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Status-Tracking-Manager - Comprehensive status tracking for pending work
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Manages real-time status of TTTranscribe integration and background processing
 */
@Singleton
class PluctStatusTrackingManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "PluctStatusTracking"
    }
    
    private val _statusItems = MutableStateFlow<List<StatusItem>>(emptyList())
    val statusItems: StateFlow<List<StatusItem>> = _statusItems.asStateFlow()
    
    private val _activeProcessing = MutableStateFlow<Map<String, StatusItem>>(emptyMap())
    val activeProcessing: StateFlow<Map<String, StatusItem>> = _activeProcessing.asStateFlow()
    
    /**
     * Add or update a status item
     */
    fun updateStatus(
        id: String,
        title: String,
        description: String = "",
        details: String = "",
        status: ProcessingStatus,
        progress: Int = 0,
        tier: ProcessingTier? = null
    ) {
        val statusItem = StatusItem(
            id = id,
            title = title,
            description = description,
            details = details,
            status = status,
            progress = progress,
            timestamp = Date(),
            tier = tier
        )
        
        Log.d(TAG, "Updating status for $id: $status (${progress}%)")
        
        // Update active processing
        val currentActive = _activeProcessing.value.toMutableMap()
        if (status == ProcessingStatus.COMPLETED || status == ProcessingStatus.FAILED) {
            currentActive.remove(id)
        } else {
            currentActive[id] = statusItem
        }
        _activeProcessing.value = currentActive
        
        // Update status items list
        val currentItems = _statusItems.value.toMutableList()
        val existingIndex = currentItems.indexOfFirst { it.id == id }
        if (existingIndex >= 0) {
            currentItems[existingIndex] = statusItem
        } else {
            currentItems.add(statusItem)
        }
        _statusItems.value = currentItems
    }
    
    /**
     * Update progress for a specific item
     */
    fun updateProgress(id: String, progress: Int, details: String = "") {
        val currentItems = _statusItems.value.toMutableList()
        val existingIndex = currentItems.indexOfFirst { it.id == id }
        if (existingIndex >= 0) {
            val existingItem = currentItems[existingIndex]
            val updatedItem = existingItem.copy(
                progress = progress,
                details = details,
                timestamp = Date()
            )
            currentItems[existingIndex] = updatedItem
            _statusItems.value = currentItems
            
            // Update active processing
            val currentActive = _activeProcessing.value.toMutableMap()
            currentActive[id] = updatedItem
            _activeProcessing.value = currentActive
            
            Log.d(TAG, "Updated progress for $id: $progress%")
        }
    }
    
    /**
     * Mark item as completed
     */
    fun markCompleted(id: String, details: String = "Processing completed successfully") {
        updateStatus(
            id = id,
            title = getStatusTitle(id),
            status = ProcessingStatus.COMPLETED,
            progress = 100,
            details = details
        )
    }
    
    /**
     * Mark item as failed
     */
    fun markFailed(id: String, error: String) {
        updateStatus(
            id = id,
            title = getStatusTitle(id),
            status = ProcessingStatus.FAILED,
            progress = 0,
            details = "Error: $error"
        )
    }
    
    /**
     * Clear completed items
     */
    fun clearCompleted() {
        val currentItems = _statusItems.value.filter { 
            it.status != ProcessingStatus.COMPLETED && it.status != ProcessingStatus.FAILED 
        }
        _statusItems.value = currentItems
        Log.d(TAG, "Cleared completed items")
    }
    
    /**
     * Get status for a specific item
     */
    fun getStatus(id: String): StatusItem? {
        return _statusItems.value.find { it.id == id }
    }
    
    /**
     * Check if there are any active processing items
     */
    fun hasActiveProcessing(): Boolean {
        return _activeProcessing.value.isNotEmpty()
    }
    
    /**
     * Get active processing count
     */
    fun getActiveProcessingCount(): Int {
        return _activeProcessing.value.size
    }
    
    private fun getStatusTitle(id: String): String {
        return when {
            id.contains("transcription") -> "Video Transcription"
            id.contains("analysis") -> "AI Analysis"
            id.contains("metadata") -> "Metadata Extraction"
            id.contains("tttranscribe") -> "TTTranscribe Processing"
            else -> "Processing"
        }
    }
}

data class StatusItem(
    val id: String,
    val title: String,
    val description: String = "",
    val details: String = "",
    val status: ProcessingStatus,
    val progress: Int = 0,
    val timestamp: Date? = null,
    val tier: ProcessingTier? = null
)
