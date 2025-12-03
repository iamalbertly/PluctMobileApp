package app.pluct.services

/**
 * Pluct-Core-API-02DebugInfo-01DataModels - Debug information data models
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single source of truth for transcription debug information
 */

/**
 * Operation steps in the transcription flow
 */
enum class OperationStep {
    METADATA,
    CHECK_BALANCE,
    VEND_TOKEN,
    SUBMIT,
    POLLING,
    COMPLETED,
    FAILED
}

/**
 * Request debug details
 */
data class RequestDebugDetails(
    val method: String,
    val url: String,
    val endpoint: String,
    val headers: String, // Sanitized headers (tokens truncated)
    val payload: String?, // Full payload for debugging
    val timestamp: Long
) {
    fun getPayloadPreview(maxLength: Int = 200): String? {
        return payload?.take(maxLength)?.let {
            if (payload.length > maxLength) "$it..." else it
        }
    }
}

/**
 * Response debug details
 */
data class ResponseDebugDetails(
    val statusCode: Int,
    val statusMessage: String,
    val body: String?, // Full response body for debugging
    val timestamp: Long,
    val duration: Long, // Request duration in ms
    val serverRequestId: String? = null // Server-side request ID
) {
    fun getBodyPreview(maxLength: Int = 200): String? {
        return body?.take(maxLength)?.let {
            if (body.length > maxLength) "$it..." else it
        }
    }
}

/**
 * Timeline entry for an operation
 */
data class OperationTimelineEntry(
    val step: OperationStep,
    val startTime: Long,
    val endTime: Long?,
    val duration: Long?, // Duration in ms
    val request: RequestDebugDetails?,
    val response: ResponseDebugDetails?,
    val error: String?
)

/**
 * Complete debug information for a transcription request
 */
data class TranscriptionDebugInfo(
    val url: String,
    val flowRequestId: String,
    val clientRequestId: String, // Unique UUID for this submission
    val currentStep: OperationStep,
    val timeline: List<OperationTimelineEntry>,
    val jobId: String?,
    val pollingAttempt: Int?,
    val maxPollingAttempts: Int?,
    val flowStartTime: Long,
    val totalDuration: Long? // Total duration in ms (null if still in progress)
) {
    /**
     * Get formatted debug text for clipboard sharing
     * Includes full payloads for developer debugging
     */
    fun getFormattedDebugText(): String {
        return buildString {
            appendLine("=== Pluct Transcription Debug Report ===")
            appendLine("Generated: ${System.currentTimeMillis()}")
            appendLine()
            appendLine("=== Request Summary ===")
            appendLine("URL: $url")
            appendLine("Flow ID: $flowRequestId")
            appendLine("Client Request ID: $clientRequestId")
            appendLine("Current Step: $currentStep")
            jobId?.let { appendLine("Job ID: $it") }
            pollingAttempt?.let { appendLine("Polling Progress: $it/$maxPollingAttempts") }
            totalDuration?.let { appendLine("Total Duration: ${it}ms (${it/1000.0}s)") }
            appendLine()
            appendLine("=== Detailed Operation Timeline ===")
            timeline.forEachIndexed { index, entry ->
                appendLine("\n[${ index + 1}] ${entry.step}")
                appendLine("    Start Time: ${entry.startTime}")
                entry.endTime?.let { appendLine("    End Time: $it") }
                entry.duration?.let { appendLine("    Duration: ${it}ms") }
                
                entry.request?.let { req ->
                    appendLine("    \n    REQUEST:")
                    appendLine("      Method: ${req.method}")
                    appendLine("      URL: ${req.url}")
                    appendLine("      Endpoint: ${req.endpoint}")
                    appendLine("      Headers: ${req.headers}")
                    req.payload?.let { 
                        appendLine("      Payload (Full):")
                        appendLine("      ${it.replace("\n", "\n      ")}")
                    }
                }
                
                entry.response?.let { res ->
                    appendLine("    \n    RESPONSE:")
                    appendLine("      Status: ${res.statusCode} ${res.statusMessage}")
                    appendLine("      Duration: ${res.duration}ms")
                    res.serverRequestId?.let { appendLine("      Server Request ID: $it") }
                    res.body?.let { 
                        appendLine("      Body (Full):")
                        appendLine("      ${it.replace("\n", "\n      ")}")
                    }
                }
                
                entry.error?.let { 
                    appendLine("    \n    ERROR:")
                    appendLine("      ${it.replace("\n", "\n      ")}")
                }
            }
            
            appendLine("\n=== End of Debug Report ===")
            appendLine("\nPlease share this report with support@pluct.app for assistance.")
        }
    }
    
    /**
     * Get current operation description for UI
     */
    fun getCurrentOperationDescription(): String {
        return when (currentStep) {
            OperationStep.METADATA -> "Fetching video metadata..."
            OperationStep.CHECK_BALANCE -> "Checking credit balance..."
            OperationStep.VEND_TOKEN -> "Vending service token..."
            OperationStep.SUBMIT -> "Submitting transcription job..."
            OperationStep.POLLING -> {
                if (pollingAttempt != null && maxPollingAttempts != null) {
                    "Polling for completion ($pollingAttempt/$maxPollingAttempts)..."
                } else {
                    "Polling for completion..."
                }
            }
            OperationStep.COMPLETED -> "Transcription completed"
            OperationStep.FAILED -> "Transcription failed"
        }
    }
    
    /**
     * Get latest request details
     */
    fun getLatestRequest(): RequestDebugDetails? {
        return timeline.lastOrNull()?.request
    }
    
    /**
     * Get latest response details
     */
    fun getLatestResponse(): ResponseDebugDetails? {
        return timeline.lastOrNull()?.response
    }
}
