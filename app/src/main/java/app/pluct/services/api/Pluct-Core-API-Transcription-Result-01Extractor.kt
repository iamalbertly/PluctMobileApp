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
            status.text to "status.text"
        )

        candidates.forEach { (value, label) ->
            if (value.isMeaningful()) {
                return ExtractionResult(value!!.trim(), label)
            }
        }

        return ExtractionResult(null, "none")
    }

    private fun String?.isMeaningful(): Boolean = !this.isNullOrBlank()
}
