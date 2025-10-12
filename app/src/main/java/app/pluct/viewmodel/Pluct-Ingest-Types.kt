package app.pluct.viewmodel

/**
 * Ingest state types for Pluct
 */
enum class IngestState {
    IDLE,
    LOADING,
    READY,
    PROCESSING,
    SUCCESS,
    ERROR,
    NEEDS_TRANSCRIPT,
    TRANSCRIPT_SUCCESS
}

/**
 * UI state for ingest operations
 */
data class IngestUiState(
    val state: IngestState = IngestState.IDLE,
    val message: String? = null,
    val error: String? = null,
    val transcript: String? = null,
    val valueProposition: String? = null,
    val providerUsed: String? = null,
    val isWebActivityLaunched: Boolean = false
)
