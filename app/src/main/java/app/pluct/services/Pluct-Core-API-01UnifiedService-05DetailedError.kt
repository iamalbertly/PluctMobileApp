package app.pluct.services

/**
 * Pluct-Core-API-01UnifiedService-05DetailedError - Detailed API error with request/response information.
 */
data class PluctCoreAPIDetailedError(
    val userMessage: String,
    val technicalDetails: TechnicalErrorDetails,
    val isRetryable: Boolean = false
) : Exception(userMessage) {

    fun getDetailedMessage(): String {
        return buildString {
            appendLine("===== TRANSCRIPTION ERROR DETAILS =====")
            appendLine("WHAT WAS SENT:")
            appendLine("  Method: ${technicalDetails.requestMethod}")
            appendLine("  URL: ${technicalDetails.requestUrl}")
            appendLine("  Endpoint: ${technicalDetails.endpoint}")
            if (technicalDetails.requestPayload.isNotEmpty()) appendLine("  Payload: ${technicalDetails.requestPayload}")
            if (technicalDetails.requestHeaders.isNotEmpty()) appendLine("  Headers: ${technicalDetails.requestHeaders}")
            appendLine()
            appendLine("WHAT WAS RECEIVED:")
            appendLine("  Status Code: ${technicalDetails.responseStatusCode}")
            appendLine("  Status Message: ${technicalDetails.responseStatusMessage}")
            if (technicalDetails.responseBody.isNotEmpty()) {
                appendLine("  Response Body:")
                appendLine("  ${technicalDetails.responseBody.replace("\n", "\n  ")}")
            }
            if (technicalDetails.responseHeaders.isNotEmpty()) {
                appendLine("  Response Headers: ${technicalDetails.responseHeaders}")
            }
            appendLine()
            appendLine("ERROR ANALYSIS:")
            appendLine("  Service: ${technicalDetails.serviceName}")
            appendLine("  Operation: ${technicalDetails.operation}")
            appendLine("  Error Type: ${technicalDetails.errorType}")
            if (technicalDetails.errorCode.isNotEmpty()) appendLine("  Error Code: ${technicalDetails.errorCode}")
            appendLine("  Timestamp: ${technicalDetails.timestamp}")
            if (technicalDetails.expectedFormat.isNotEmpty()) {
                appendLine("  Expected Format:")
                appendLine("  ${technicalDetails.expectedFormat}")
            }
            appendLine("=======================================")
        }
    }
}

data class TechnicalErrorDetails(
    val serviceName: String,
    val operation: String,
    val endpoint: String,
    val requestMethod: String,
    val requestUrl: String,
    val requestPayload: String = "",
    val requestHeaders: String = "",
    val responseStatusCode: Int = 0,
    val responseStatusMessage: String = "",
    val responseBody: String = "",
    val responseHeaders: String = "",
    val errorType: String = "API_ERROR",
    val errorCode: String = "",
    val expectedFormat: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
