package app.pluct.core.error

/**
 * Pluct-Core-Error-06MessageMapping - Map technical errors to user-friendly messages
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Single source of truth for error message mappings
 */
object PluctCoreError06MessageMapping {
    
    /**
     * Error code to user message mapping
     */
    private val errorCodeMap = mapOf(
        // Network errors
        "NETWORK_ERROR" to "Unable to connect to the server. Please check your internet connection.",
        "CONNECTION_TIMEOUT" to "The connection timed out. Please try again.",
        "UNKNOWN_HOST" to "Unable to reach the server. Please check your internet connection.",
        
        // Authentication errors
        "UNAUTHORIZED" to "Your session has expired. Please try again.",
        "AUTHENTICATION_FAILED" to "Authentication failed. Please check your credentials.",
        "TOKEN_EXPIRED" to "Your session has expired. Please try again.",
        
        // Payment errors
        "INSUFFICIENT_CREDITS" to "You don't have enough credits. Please add credits and try again.",
        "PAYMENT_REQUIRED" to "Payment required. Please add credits to continue.",
        
        // Validation errors
        "INVALID_URL" to "The URL format is invalid. Please check the URL and try again.",
        "VALIDATION_ERROR" to "The input is invalid. Please check and try again.",
        
        // Server errors
        "SERVER_ERROR" to "The server is temporarily unavailable. Please try again later.",
        "SERVICE_UNAVAILABLE" to "The service is temporarily unavailable. Please try again in a few moments.",
        "INTERNAL_ERROR" to "An internal error occurred. Please try again later.",
        
        // Rate limiting
        "RATE_LIMIT_EXCEEDED" to "Too many requests. Please wait a moment and try again.",
        
        // Timeout errors
        "TIMEOUT" to "The operation timed out. Please try again.",
        "REQUEST_TIMEOUT" to "The request took too long. Please try again.",
        
        // Database errors
        "DATABASE_ERROR" to "A database error occurred. Please try again.",
        "DATABASE_LOCKED" to "The database is temporarily locked. Please try again in a moment.",
        
        // Generic errors
        "UNKNOWN_ERROR" to "An unexpected error occurred. Please try again.",
        "OPERATION_FAILED" to "The operation failed. Please try again."
    )
    
    /**
     * Get user-friendly message for error code
     */
    fun getUserMessage(errorCode: String): String {
        return errorCodeMap[errorCode] ?: "An unexpected error occurred. Please try again."
    }
    
    /**
     * Get actionable guidance for error code
     */
    fun getActionableGuidance(errorCode: String): String {
        return when (errorCode) {
            "NETWORK_ERROR", "CONNECTION_TIMEOUT", "UNKNOWN_HOST" -> 
                "Check your internet connection and try again."
            "UNAUTHORIZED", "AUTHENTICATION_FAILED", "TOKEN_EXPIRED" -> 
                "Your session may have expired. Try again or restart the app."
            "INSUFFICIENT_CREDITS", "PAYMENT_REQUIRED" -> 
                "Add credits to your account to continue."
            "INVALID_URL", "VALIDATION_ERROR" -> 
                "Check the input format and try again."
            "SERVER_ERROR", "SERVICE_UNAVAILABLE", "INTERNAL_ERROR" -> 
                "The server may be experiencing issues. Try again in a few moments."
            "RATE_LIMIT_EXCEEDED" -> 
                "Wait a few moments before trying again."
            "TIMEOUT", "REQUEST_TIMEOUT" -> 
                "The operation took too long. Check your connection and try again."
            "DATABASE_ERROR", "DATABASE_LOCKED" -> 
                "A temporary database issue occurred. Try again in a moment."
            else -> 
                "If the problem persists, please contact support."
        }
    }
}

