package app.pluct.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for TokAudit flow to persist videoUrl and runId across configuration changes
 */
class TokAuditViewModel : ViewModel() {
    
    private val _videoUrl = MutableStateFlow<String?>(null)
    val videoUrl: StateFlow<String?> = _videoUrl.asStateFlow()
    
    private val _runId = MutableStateFlow<String?>(null)
    val runId: StateFlow<String?> = _runId.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    /**
     * Initialize the ViewModel with videoUrl and runId
     */
    fun initialize(videoUrl: String, runId: String) {
        viewModelScope.launch {
            _videoUrl.value = videoUrl
            _runId.value = runId
        }
    }
    
    /**
     * Get the current videoUrl
     */
    fun getVideoUrl(): String? = _videoUrl.value
    
    /**
     * Get the current runId
     */
    fun getRunId(): String? = _runId.value
    
    /**
     * Set processing state
     */
    fun setProcessing(processing: Boolean) {
        viewModelScope.launch {
            _isProcessing.value = processing
        }
    }
    
    /**
     * Clear the ViewModel data
     */
    fun clear() {
        viewModelScope.launch {
            _videoUrl.value = null
            _runId.value = null
            _isProcessing.value = false
        }
    }
    
    /**
     * Check if ViewModel has valid data
     */
    fun hasValidData(): Boolean {
        return _videoUrl.value?.isNotEmpty() == true && _runId.value?.isNotEmpty() == true
    }
}
