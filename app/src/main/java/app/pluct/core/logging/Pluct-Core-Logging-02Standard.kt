package app.pluct.core.logging

import android.util.Log

/**
 * Pluct-Core-Logging-02Standard
 * Follows naming convention: [Project]-[Core]-[Logging]-[Sequence][Standard]
 * 5 scope layers: Project, Core, Logging, Sequence, Standard
 * 
 * Technical Debt #2: Standardized logging levels and patterns
 * Single source of truth for logging standards across the app
 */
object PluctCoreLogging02Standard {
    
    // Log level constants for consistency
    const val LEVEL_VERBOSE = Log.VERBOSE
    const val LEVEL_DEBUG = Log.DEBUG
    const val LEVEL_INFO = Log.INFO
    const val LEVEL_WARN = Log.WARN
    const val LEVEL_ERROR = Log.ERROR
    
    /**
     * Log debug message (development/diagnostic info)
     * Use for: Flow tracking, state changes, non-critical operations
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.d(tag, message, throwable)
        } else {
            Log.d(tag, message)
        }
    }
    
    /**
     * Log info message (user-visible events)
     * Use for: Successful operations, important state changes
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.i(tag, message, throwable)
        } else {
            Log.i(tag, message)
        }
    }
    
    /**
     * Log warning message (recoverable issues)
     * Use for: Retries, fallbacks, non-critical failures
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }
    
    /**
     * Log error message (failures requiring attention)
     * Use for: Exceptions, API failures, critical errors
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    /**
     * Format log message with context
     */
    fun formatMessage(operation: String, details: String? = null): String {
        return if (details != null) {
            "$operation | $details"
        } else {
            operation
        }
    }
}
