package app.pluct.services

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pluct-Core-API-DetailedError - Detailed API error information
 * Provides comprehensive error context for debugging and user feedback
 */
@Serializable
data class DetailedAPIError(
    val serviceName: String,              // "TTTranscribe", "BusinessEngine", "Metadata"
    val httpStatus: Int,                  // HTTP status code
    val errorCode: String?,               // Service-specific error code
    val errorMessage: String,             // User-friendly message
    val responseBody: String?,            // Full response for debugging
    val expectedFormat: String?,          // What we expected to receive
    val actualFormat: String?,            // What we actually received
    val timestamp: Long,                  // When error occurred
    val retryAttempt: Int,                // Which retry attempt failed
    val isRetryable: Boolean,             // Can user retry?
    // Upstream error details (for proxy errors)
    val upstreamStatus: Int? = null,      // Upstream service HTTP status
    val upstreamErrorCode: String? = null, // Upstream error code
    val upstreamMessage: String? = null    // Upstream error message
) {
    /**
     * Get user-friendly error summary with upstream information
     */
    fun getUserFriendlyMessage(): String {
        return when {
            // Authentication errors - provide clear guidance
            httpStatus == 401 -> {
                if (isUpstreamAuthError()) {
                    "Authentication failed with video service. This may be a temporary server issue. Please try again."
                } else {
                    "Authentication failed. Please check your credentials."
                }
            }
            httpStatus == 429 -> "Too many requests. Please wait a moment and try again."
            httpStatus == 502 || httpStatus == 503 -> {
                val service = if (upstreamStatus != null) "downstream service" else serviceName
                "$service is temporarily unavailable. Please try again in a few moments."
            }
            httpStatus >= 500 -> "$serviceName service is temporarily unavailable. Please try again later."
            upstreamStatus != null && upstreamStatus >= 400 -> {
                "Video transcription service error (${upstreamStatus}): ${upstreamMessage ?: "Unknown error"}. Please try again."
            }
            else -> errorMessage
        }
    }
    
    /**
     * Check if this is an upstream authentication error (X-Engine-Auth related)
     */
    fun isUpstreamAuthError(): Boolean {
        return upstreamStatus == 401 || 
               upstreamErrorCode?.contains("unauthorized", ignoreCase = true) == true ||
               upstreamMessage?.contains("X-Engine-Auth", ignoreCase = true) == true
    }
    
    /**
     * Get detailed debug message with upstream error details
     */
    fun getDebugMessage(): String {
        val sb = StringBuilder()
        sb.appendLine("Service: $serviceName")
        sb.appendLine("HTTP Status: $httpStatus")
        errorCode?.let { sb.appendLine("Error Code: $it") }
        sb.appendLine("Message: $errorMessage")
        
        // Include upstream error details if available
        if (upstreamStatus != null) {
            sb.appendLine("--- Upstream Error ---")
            sb.appendLine("Upstream Status: $upstreamStatus")
            upstreamErrorCode?.let { sb.appendLine("Upstream Code: $it") }
            upstreamMessage?.let { sb.appendLine("Upstream Message: $it") }
        }
        
        responseBody?.let { sb.appendLine("Response: ${it.take(300)}${if (it.length > 300) "..." else ""}") }
        expectedFormat?.let { sb.appendLine("Expected: $it") }
        actualFormat?.let { sb.appendLine("Actual: $it") }
        sb.appendLine("Retry Attempt: $retryAttempt")
        sb.appendLine("Is Retryable: $isRetryable")
        return sb.toString()
    }
    
    /**
     * Serialize to JSON string for database storage
     */
    fun toJsonString(): String {
        return kotlinx.serialization.json.Json.encodeToString(serializer(), this)
    }
    
    companion object {
        /**
         * Deserialize from JSON string
         */
        fun fromJsonString(json: String): DetailedAPIError? {
            return try {
                kotlinx.serialization.json.Json.decodeFromString(serializer(), json)
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * Create from generic exception with error parsing
         */
        fun fromException(
            serviceName: String,
            exception: Exception,
            retryAttempt: Int = 0
        ): DetailedAPIError {
            // Try to parse error message for upstream details
            val errorMessage = exception.message ?: "Unknown error"
            val upstreamDetails = parseUpstreamError(errorMessage)
            
            return DetailedAPIError(
                serviceName = serviceName,
                httpStatus = 0,
                errorCode = exception.javaClass.simpleName,
                errorMessage = errorMessage,
                responseBody = exception.stackTraceToString().take(500),
                expectedFormat = null,
                actualFormat = null,
                timestamp = System.currentTimeMillis(),
                retryAttempt = retryAttempt,
                isRetryable = true,
                upstreamStatus = upstreamDetails.first,
                upstreamErrorCode = upstreamDetails.second,
                upstreamMessage = upstreamDetails.third
            )
        }
        
        /**
         * Parse upstream error details from error message
         */
        private fun parseUpstreamError(errorMessage: String): Triple<Int?, String?, String?> {
            var upstreamStatus: Int? = null
            var upstreamCode: String? = null
            var upstreamMessage: String? = null
            
            // Try to extract upstream status
            val statusRegex = Regex("Upstream Status: (\\d+)")
            statusRegex.find(errorMessage)?.let {
                upstreamStatus = it.groupValues[1].toIntOrNull()
            }
            
            // Try to extract upstream error code
            val codeRegex = Regex("Error Code: ([a-zA-Z_]+)")
            codeRegex.find(errorMessage)?.let {
                upstreamCode = it.groupValues[1]
            }
            
            // Try to extract upstream response/message
            val messageRegex = Regex("Upstream Response: (.+?)(?:\\||$)")
            messageRegex.find(errorMessage)?.let {
                upstreamMessage = it.groupValues[1].trim()
            }
            
            return Triple(upstreamStatus, upstreamCode, upstreamMessage)
        }
    }
}
