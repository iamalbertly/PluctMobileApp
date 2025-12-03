package app.pluct.data.repository

import android.util.Log
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingStatus

/**
 * Pluct-Video-Data-Repository-02ErrorHandler - Handle all database errors gracefully
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Single source of truth for database error handling
 */
object PluctVideoDataRepository02ErrorHandler {
    
    private const val TAG = "RepositoryErrorHandler"
    
    /**
     * Handle database error and return safe default
     */
    fun <T> handleError(
        operation: String,
        error: Throwable?,
        defaultValue: T
    ): T {
        val errorMessage = error?.message ?: "Unknown database error"
        Log.e(TAG, "Database error in $operation: $errorMessage", error)
        return defaultValue
    }
    
    /**
     * Handle database error for Flow operations
     */
    fun handleFlowError(
        operation: String,
        error: Throwable
    ): List<VideoItem> {
        return handleError(operation, error, emptyList())
    }
    
    /**
     * Handle database error for suspend operations
     */
    suspend fun <T> handleSuspendError(
        operation: String,
        error: Throwable?,
        defaultValue: T
    ): T {
        return handleError(operation, error, defaultValue)
    }
    
    /**
     * Check if error is recoverable
     */
    fun isRecoverable(error: Throwable?): Boolean {
        if (error == null) return false
        
        val errorMessage = error.message?.lowercase() ?: ""
        val errorType = error.javaClass.simpleName
        
        // Network errors are recoverable
        if (errorType.contains("Network", ignoreCase = true) ||
            errorMessage.contains("network", ignoreCase = true)) {
            return true
        }
        
        // Timeout errors are recoverable
        if (errorType.contains("Timeout", ignoreCase = true) ||
            errorMessage.contains("timeout", ignoreCase = true)) {
            return true
        }
        
        // Database locked errors are recoverable
        if (errorMessage.contains("database is locked", ignoreCase = true) ||
            errorMessage.contains("locked", ignoreCase = true)) {
            return true
        }
        
        // Constraint violations are NOT recoverable
        if (errorMessage.contains("constraint", ignoreCase = true) ||
            errorMessage.contains("unique", ignoreCase = true)) {
            return false
        }
        
        // Default: not recoverable
        return false
    }
}

