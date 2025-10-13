package app.pluct.background

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class ProcessStage {
    QUEUED, VENDING_TOKEN, REQUEST_SUBMITTED, REMOTE_ACK,
    DOWNLOADING, TRANSCRIBING, SUMMARIZING,
    COMPLETED, FAILED
}

data class ProcessProgress(
    val requestId: String?,
    val stage: ProcessStage,
    val message: String? = null,
    val percent: Int? = null
)

/**
 * Simple in-memory status repository to surface background processing progress to UI.
 */
object StatusRepository {
    private val _inFlight: MutableStateFlow<Map<String, ProcessProgress>> = MutableStateFlow(emptyMap())
    val inFlight: StateFlow<Map<String, ProcessProgress>> = _inFlight

    fun update(url: String, progress: ProcessProgress) {
        _inFlight.update { current -> current + (url to progress) }
    }

    fun clear(url: String) {
        _inFlight.update { current -> current - url }
    }
}


