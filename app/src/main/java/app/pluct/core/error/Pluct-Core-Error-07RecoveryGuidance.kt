package app.pluct.core.error

/**
 * Pluct-Core-Error-07RecoveryGuidance
 * Follows naming convention: [Project]-[Core]-[Error]-[Sequence][RecoveryGuidance]
 * 5 scope layers: Project, Core, Error, Sequence, RecoveryGuidance
 * 
 * UX IMPROVEMENT #1: Provides specific, actionable recovery guidance for different error types
 * Single source of truth for error recovery steps
 */
object PluctCoreError07RecoveryGuidance {
    
    /**
     * Get specific recovery steps for an error
     */
    fun getRecoverySteps(errorMessage: String, errorCode: String? = null): List<String> {
        val message = errorMessage.lowercase()
        val code = (errorCode ?: "").lowercase()
        
        return when {
            message.contains("insufficient", ignoreCase = true) || 
            message.contains("credits", ignoreCase = true) || 
            code.contains("402") -> listOf(
                "Go to Settings to add credits",
                "Or queue this video for later processing",
                "Queued videos process automatically when credits are available"
            )
            
            message.contains("rate limit", ignoreCase = true) || 
            code.contains("429") -> listOf(
                "Wait for the rate limit to reset (typically 1 hour)",
                "Or queue this video for later processing",
                "Queued videos process automatically when the limit resets"
            )
            
            message.contains("network", ignoreCase = true) || 
            message.contains("connection", ignoreCase = true) -> listOf(
                "Check your internet connection",
                "The video will be queued automatically if connection is lost",
                "Queued videos process when connection is restored"
            )
            
            message.contains("timeout", ignoreCase = true) -> listOf(
                "Check your internet connection",
                "Try again - the request may have been interrupted",
                "If it persists, the video will be queued automatically"
            )
            
            message.contains("authentication", ignoreCase = true) || 
            code.contains("401") -> listOf(
                "Your session may have expired",
                "The app will automatically refresh your session",
                "Try again in a moment"
            )
            
            message.contains("invalid", ignoreCase = true) || 
            message.contains("validation", ignoreCase = true) -> listOf(
                "Check the URL format",
                "Ensure it's a valid TikTok video link",
                "Try copying the link again from TikTok"
            )
            
            else -> listOf(
                "Try again in a moment",
                "If the problem persists, check your connection",
                "You can view error details in Settings > Debug Logs"
            )
        }
    }
    
    /**
     * Get primary recovery action label
     */
    fun getPrimaryAction(errorMessage: String, errorCode: String? = null): String {
        val message = errorMessage.lowercase()
        val code = (errorCode ?: "").lowercase()
        
        return when {
            message.contains("insufficient", ignoreCase = true) || 
            message.contains("credits", ignoreCase = true) || 
            code.contains("402") -> "Add Credits"
            
            message.contains("rate limit", ignoreCase = true) || 
            code.contains("429") -> "Queue for Later"
            
            message.contains("network", ignoreCase = true) || 
            message.contains("connection", ignoreCase = true) -> "Check Connection"
            
            message.contains("timeout", ignoreCase = true) -> "Retry"
            
            message.contains("authentication", ignoreCase = true) || 
            code.contains("401") -> "Retry"
            
            else -> "Retry"
        }
    }
}
