package app.pluct.core.error

import android.util.Log

/**
 * Pluct-Core-Error-04TimeoutHandler - Handle timeout scenarios with user-friendly messages
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Single source of truth for timeout error handling
 */
object PluctCoreError04TimeoutHandler {
    
    private const val TAG = "TimeoutHandler"
    
    /**
     * Format timeout error message
     */
    fun formatTimeoutError(
        operation: String,
        timeoutDurationMs: Long,
        elapsedTimeMs: Long? = null
    ): String {
        val timeoutSeconds = timeoutDurationMs / 1000
        val elapsedSeconds = elapsedTimeMs?.let { it / 1000 } ?: timeoutSeconds
        
        return when {
            elapsedSeconds < 60 -> 
                "The $operation timed out after $elapsedSeconds seconds. This may be due to network issues or server load. Please try again."
            
            elapsedSeconds < 300 -> 
                "The $operation timed out after ${elapsedSeconds / 60} minutes. The server may be experiencing high load. Please try again in a few moments."
            
            else -> 
                "The $operation timed out after ${elapsedSeconds / 60} minutes. This is longer than expected. Please check your connection and try again, or contact support if the issue persists."
        }
    }
    
    /**
     * Get timeout warning message (shown before timeout)
     */
    fun getTimeoutWarning(
        operation: String,
        timeoutDurationMs: Long,
        elapsedTimeMs: Long
    ): String? {
        val progress = elapsedTimeMs.toFloat() / timeoutDurationMs
        val remainingSeconds = (timeoutDurationMs - elapsedTimeMs) / 1000
        
        return when {
            progress >= 0.9f -> 
                "⚠️ The $operation is taking longer than expected. It may timeout in ${remainingSeconds} seconds."
            
            progress >= 0.8f -> 
                "⚠️ The $operation is taking longer than usual. Estimated time remaining: ${remainingSeconds} seconds."
            
            else -> null
        }
    }
    
    /**
     * Check if operation should show timeout warning
     */
    fun shouldShowTimeoutWarning(
        timeoutDurationMs: Long,
        elapsedTimeMs: Long
    ): Boolean {
        val progress = elapsedTimeMs.toFloat() / timeoutDurationMs
        return progress >= 0.8f
    }
}

