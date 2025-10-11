package app.pluct.viewmodel

/**
 * Pluct-Ingest-Types - Core types for ingest functionality
 */
enum class IngestState {
    PENDING,
    NEEDS_TRANSCRIPT,
    READY,
    TRANSCRIPT_SUCCESS,
    IDLE,
    LOADING,
    SUCCESS,
    ERROR
}

data class IngestUiState(
    val state: IngestState = IngestState.PENDING,
    val url: String = "",
    val videoId: String? = null,
    val error: String? = null,
    val processedUrl: String? = null,
    val transcript: String? = null,
    val summary: String? = null,
    val webErrorCode: String? = null,
    val webErrorMessage: String? = null,
    val hasLaunchedWebActivity: Boolean = false,
    val showPostProcessingOptions: Boolean = false,
    val runId: String = "",
    val providerUsed: String? = null,
    val message: String = ""
)
