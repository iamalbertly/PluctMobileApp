package app.pluct.shared

enum class PluctTranscriptionPhase {
    PREPARING,
    DOWNLOADING,
    EXTRACTING,
    TRANSCRIBING,
    FINALIZING,
    COMPLETED
}

enum class PluctProcessingState {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED
}

object PluctTranscriptionState {
    fun normalizeStatus(status: String?): PluctProcessingState {
        return when (status.orEmpty().lowercase()) {
            "queued", "pending" -> PluctProcessingState.QUEUED
            "processing", "running", "transcribing" -> PluctProcessingState.PROCESSING
            "completed", "done", "success" -> PluctProcessingState.COMPLETED
            "failed", "error" -> PluctProcessingState.FAILED
            else -> PluctProcessingState.PROCESSING
        }
    }

    fun isTerminal(status: String?): Boolean {
        val normalized = normalizeStatus(status)
        return normalized == PluctProcessingState.COMPLETED || normalized == PluctProcessingState.FAILED
    }
}
