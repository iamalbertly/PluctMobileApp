package app.pluct.services.api

import app.pluct.services.TranscriptionStatusResponse

/**
 * Pluct-Core-API-Transcription-Result-01Extractor - Shared transcript extraction helper.
 * Ensures all callers honor the same field precedence and source attribution.
 */
object PluctCoreAPITranscriptionResult01Extractor {

    data class ExtractionResult(
        val transcript: String?,
        val source: String
    )

    fun extract(status: TranscriptionStatusResponse): ExtractionResult {
        val candidates = listOf(
            status.transcript to "status.transcript",
            status.result?.transcription to "status.result.transcription",
            status.result?.transcript to "status.result.transcript",
            status.result?.text to "status.result.text",
            status.text to "status.text"
        )

        candidates.forEach { (value, label) ->
            if (value.isMeaningful()) {
                // Safe to use value here since isMeaningful() ensures it's not null or blank
                val trimmedValue = value?.trim() ?: return@forEach
                return ExtractionResult(trimmedValue, label)
            }
        }

        return ExtractionResult(null, "none")
    }

    private fun String?.isMeaningful(): Boolean = !this.isNullOrBlank()
}
