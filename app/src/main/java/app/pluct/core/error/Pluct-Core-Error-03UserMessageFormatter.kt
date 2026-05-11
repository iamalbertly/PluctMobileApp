package app.pluct.core.error

import android.util.Log
import app.pluct.services.PluctCoreAPIDetailedError

/**
 * Pluct-Core-Error-03UserMessageFormatter - Convert technical errors to user-friendly messages
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Single source of truth for user-facing error message formatting
 */
object PluctCoreError03UserMessageFormatter {
    
    private const val TAG = "UserMessageFormatter"

    private fun extractCreditsRefunded(error: Throwable?): Int? {
        val detailed = error as? PluctCoreAPIDetailedError ?: return null
        val body = detailed.technicalDetails.responseBody ?: return null
        val match = Regex("\"creditsRefunded\"\\s*:\\s*(\\d+)").find(body) ?: return null
        return match.groupValues.getOrNull(1)?.toIntOrNull()
    }
    
    /**
     * Format error message for user display
     */
    fun formatUserMessage(
        error: Throwable?,
        technicalMessage: String? = null,
        errorCode: String? = null,
        httpStatus: Int? = null,
        @Suppress("UNUSED_PARAMETER") context: String = "operation"
    ): UserFriendlyMessage {
        val message = technicalMessage ?: error?.message ?: "An unexpected error occurred"
        val code = errorCode ?: error?.javaClass?.simpleName ?: "UNKNOWN_ERROR"
        val status = httpStatus ?: 0

        // UX IMPROVEMENT: If BE already refunded credits (creditsRefunded > 0),
        // explicitly tell the user. This prevents “unfair depletion” perception.
        val creditsRefunded = extractCreditsRefunded(error)
        if (creditsRefunded != null &&
            creditsRefunded > 0 &&
            status in 400..499 &&
            status != 402
        ) {
            return UserFriendlyMessage(
                title = "No charge",
                message = "No credits used for this attempt. Tap Retry.",
                action = "Retry",
                retryable = true,
                technicalDetails = "creditsRefunded=$creditsRefunded"
            )
        }
        
        return when {
            message.equals("ACTION_UPDATE_APP", ignoreCase = true) ||
                message.contains("update -> continue", ignoreCase = true) ||
                message.contains("disabletranscribesubmit", ignoreCase = true) ->
                UserFriendlyMessage(
                    title = "Update",
                    message = "New Pluct version. Update in Play Store, then open again.",
                    action = "OK",
                    retryable = false
                )

            message.contains("SERVICE_COOLDOWN", ignoreCase = true) ||
                message.contains("circuit breaker", ignoreCase = true) ->
                UserFriendlyMessage(
                    title = "Wait",
                    message = "Too many tries. Wait 1 minute, then tap again.",
                    action = "OK",
                    retryable = true
                )

            // Network errors
            isNetworkError(error, message) -> formatNetworkError(message, context)
            
            // Authentication errors
            status == 401 || code.contains("unauthorized", ignoreCase = true) -> 
                UserFriendlyMessage(
                    title = "Session",
                    message = "Please tap Retry.",
                    action = "Retry",
                    retryable = true
                )
            
            // Payment/credit errors
            status == 402 || message.contains("insufficient", ignoreCase = true) || 
            message.contains("credits", ignoreCase = true) -> 
                UserFriendlyMessage(
                    title = "No credits",
                    message = "Add credits in Settings, or save link for later.",
                    action = "Add credits",
                    retryable = false
                )
            
            // Rate limiting - UX IMPROVEMENT #3: Better messaging with actionable guidance
            status == 429 || message.contains("rate limit", ignoreCase = true) -> 
                UserFriendlyMessage(
                    title = "Slow down",
                    message = "Too many requests this hour. Save to queue or wait.",
                    action = "Queue",
                    retryable = false
                )
            
            // Server errors
            status >= 500 || message.contains("server", ignoreCase = true) || 
            message.contains("service unavailable", ignoreCase = true) -> 
                UserFriendlyMessage(
                    title = "Service Unavailable",
                    message = "The transcription service is temporarily unavailable. Please try again in a few moments.",
                    action = "Retry",
                    retryable = true
                )
            
            // Timeout errors
            message.contains("timeout", ignoreCase = true) || 
            message.contains("timed out", ignoreCase = true) -> 
                UserFriendlyMessage(
                    title = "Request Timed Out",
                    message = "The operation took too long to complete. Please check your connection and try again.",
                    action = "Retry",
                    retryable = true
                )
            
            // Validation errors
            message.contains("invalid", ignoreCase = true) || 
            message.contains("validation", ignoreCase = true) || 
            code.contains("validation", ignoreCase = true) -> 
                UserFriendlyMessage(
                    title = "Invalid Input",
                    message = extractValidationMessage(message),
                    action = "Fix and Retry",
                    retryable = false
                )
            
            // Connection errors - UX-4: Enhanced with actionable recovery guidance
            message.contains("connection", ignoreCase = true) || 
            message.contains("network", ignoreCase = true) -> 
                UserFriendlyMessage(
                    title = "No connection",
                    message = "Internet off or weak. Link saved — will run when online. Open Queue to see.",
                    action = "Queue",
                    retryable = true,
                    technicalDetails = "Network connectivity issue. Video queued for automatic processing when connection restored."
                )
            
            // Duplicate processing errors - provide helpful guidance
            message.contains("already being processed", ignoreCase = true) ||
            message.contains("already being transcribed", ignoreCase = true) ->
                UserFriendlyMessage(
                    title = "Already Processing",
                    message = "This video is already being transcribed. Check your recent videos to see the progress, or wait for it to complete.",
                    action = "View Recent Videos",
                    retryable = false
                )
            
            // Duplicate intent errors
            (message.contains("duplicate", ignoreCase = true) && 
            (message.contains("intent", ignoreCase = true) || message.contains("request", ignoreCase = true))) ->
                UserFriendlyMessage(
                    title = "Request Already Received",
                    message = "This video link was already received. If it's not processing, check your recent videos.",
                    action = "View Recent Videos",
                    retryable = false
                )
            
            // Default - try to make it user-friendly
            else -> UserFriendlyMessage(
                title = "Error",
                message = sanitizeTechnicalMessage(message),
                action = "Retry",
                retryable = true
            )
        }
    }
    
    /**
     * Check if error is a network error
     */
    private fun isNetworkError(error: Throwable?, message: String): Boolean {
        if (error == null) return false
        
        val networkErrorTypes = listOf(
            "SocketTimeoutException",
            "UnknownHostException",
            "ConnectException",
            "SSLException",
            "IOException"
        )
        
        return networkErrorTypes.any { error.javaClass.simpleName.contains(it) } ||
               message.contains("network", ignoreCase = true) ||
               message.contains("connection", ignoreCase = true) ||
               message.contains("timeout", ignoreCase = true)
    }
    
    /**
     * Format network error message
     */
    private fun formatNetworkError(message: String, @Suppress("UNUSED_PARAMETER") context: String): UserFriendlyMessage {
        return when {
            message.contains("timeout", ignoreCase = true) -> 
                UserFriendlyMessage(
                    title = "Connection Timeout",
                    message = "The request took too long. Your video has been saved and will process automatically when your connection is stable. Check the Queue section for saved videos.",
                    action = "View Queue",
                    retryable = true,
                    technicalDetails = "Network timeout. Video queued for automatic retry."
                )
            message.contains("host", ignoreCase = true) -> 
                UserFriendlyMessage(
                    title = "Server Unreachable",
                    message = "Unable to reach the server. Please check your internet connection. Your video has been saved and will process when connection is restored.",
                    action = "Check Connection",
                    retryable = true,
                    technicalDetails = "Server unreachable. Video queued for automatic processing."
                )
            else -> 
                UserFriendlyMessage(
                    title = "Network Error",
                    message = "A network error occurred. Your video has been saved and will automatically process when your connection is restored. Check the Queue section to see saved videos.",
                    action = "View Queue",
                    retryable = true,
                    technicalDetails = "Network connectivity issue. Video queued for automatic processing."
                )
        }
    }
    
    /**
     * Extract validation message from technical error
     */
    private fun extractValidationMessage(message: String): String {
        // Try to extract the actual validation issue
        val patterns = listOf(
            Regex("Invalid URL.*?: (.+)"),
            Regex("URL.*?must.*?: (.+)"),
            Regex("validation.*?failed.*?: (.+)")
        )
        
        for (pattern in patterns) {
            pattern.find(message)?.let {
                return it.groupValues[1].trim()
            }
        }
        
        // Fallback to sanitized message
        return sanitizeTechnicalMessage(message)
    }
    
    /**
     * Sanitize technical message for user display
     */
    private fun sanitizeTechnicalMessage(message: String): String {
        // Remove technical details
        var sanitized = message
            .replace(Regex("Service:.*"), "")
            .replace(Regex("Operation:.*"), "")
            .replace(Regex("Error Code:.*"), "")
            .replace(Regex("HTTP \\d+.*"), "")
            .replace(Regex("Stack:.*"), "")
            .replace(Regex("Timestamp:.*"), "")
            .trim()
        
        // Remove common technical prefixes
        val technicalPrefixes = listOf(
            "Exception:",
            "Error:",
            "Failed:",
            "java.",
            "kotlin.",
            "android."
        )
        
        for (prefix in technicalPrefixes) {
            if (sanitized.startsWith(prefix, ignoreCase = true)) {
                sanitized = sanitized.substring(prefix.length).trim()
            }
        }
        
        // Capitalize first letter
        if (sanitized.isNotEmpty()) {
            sanitized = sanitized[0].uppercase() + sanitized.substring(1)
        }
        
        return sanitized.ifEmpty { "An unexpected error occurred. Please try again." }
    }
    
    /**
     * User-friendly error message data class
     */
    data class UserFriendlyMessage(
        val title: String,
        val message: String,
        val action: String,
        val retryable: Boolean,
        val technicalDetails: String? = null
    )
}

