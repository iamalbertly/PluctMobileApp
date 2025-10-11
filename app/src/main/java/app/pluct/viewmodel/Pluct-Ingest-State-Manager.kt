package app.pluct.viewmodel

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Pluct-Ingest-State-Manager - Manages ingest state and UI state
 */
class PluctIngestStateManager(savedStateHandle: SavedStateHandle) {
    
    private val url: String = checkNotNull(savedStateHandle["url"])
    private val runId: String = savedStateHandle["run_id"] ?: UUID.randomUUID().toString()
    
    private val _uiState = MutableStateFlow(IngestUiState(url = url, runId = runId))
    val uiState: StateFlow<IngestUiState> = _uiState.asStateFlow()
    
    fun updateState(newState: IngestUiState) {
        _uiState.value = newState
    }
    
    fun updateState(update: (IngestUiState) -> IngestUiState) {
        _uiState.value = update(_uiState.value)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            error = null, 
            webErrorCode = null, 
            webErrorMessage = null
        )
    }
}
