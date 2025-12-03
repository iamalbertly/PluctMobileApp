package app.pluct.core.error

import android.util.Log

/**
 * Pluct-Core-Error-03UserMessageFormatter - Convert technical errors to user-friendly messages
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Single source of truth for user-facing error message formatting
 */
object PluctCoreError03UserMessageFormatter {
    
    private const val TAG = "UserMessageFormatter"
    
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
        
        return when {
            // Network errors
            isNetworkError(error, message) -> formatNetworkError(message, context)
            
            // Authentication errors
            status == 401 || code.contains("unauthorized", ignoreCase = true) -> 
                UserFriendlyMessage(
                    title = "Authentication Error",
                    message = "Your session has expired. Please try again.",
                    action = "Retry",
                    retryable = true
                )
            
            // Payment/credit errors
            status == 402 || message.contains("insufficient", ignoreCase = true) || 
            message.contains("credits", ignoreCase = true) -> 
                UserFriendlyMessage(
                    title = "Insufficient Credits",
                    message = "You don't have enough credits to complete this operation. Please add credits and try again.",
                    action = "Add Credits",
                    retryable = false
                )
            
            // Rate limiting
            status == 429 || message.contains("rate limit", ignoreCase = true) -> 
                UserFriendlyMessage(
                    title = "Too Many Requests",
                    message = "You've made too many requests. Please wait a moment and try again.",
                    action = "Wait and Retry",
                    retryable = true
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
            
            // Connection errors
            message.contains("connection", ignoreCase = true) || 
            message.contains("network", ignoreCase = true) -> 
                UserFriendlyMessage(
                    title = "Connection Error",
                    message = "Unable to connect to the server. Please check your internet connection and try again.",
                    action = "Retry",
                    retryable = true
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
    private fun formatNetworkError(message: String, context: String): UserFriendlyMessage {
        return when {
            message.contains("timeout", ignoreCase = true) -> 
                UserFriendlyMessage(
                    title = "Connection Timeout",
                    message = "The request took too long. Please check your internet connection and try again.",
                    action = "Retry",
                    retryable = true
                )
            message.contains("host", ignoreCase = true) -> 
                UserFriendlyMessage(
                    title = "Server Unreachable",
                    message = "Unable to reach the server. Please check your internet connection.",
                    action = "Retry",
                    retryable = true
                )
            else -> 
                UserFriendlyMessage(
                    title = "Network Error",
                    message = "A network error occurred. Please check your internet connection and try again.",
                    action = "Retry",
                    retryable = true
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

