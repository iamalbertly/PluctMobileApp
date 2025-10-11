package app.pluct.ui.utils

import android.util.Log
import app.pluct.utils.Constants

/**
 * Centralized error handling for the application
 */
object ErrorHandler {
    private const val TAG = "ErrorHandler"
    
    /**
     * Error severity levels
     */
    enum class ErrorSeverity {
        INFO, WARNING, ERROR, CRITICAL
    }
    
    /**
     * Error categories
     */
    enum class ErrorCategory {
        NETWORK, WEBVIEW, TRANSCRIPT, USER_INPUT, SYSTEM, UNKNOWN
    }
    
    /**
     * Error data class
     */
    data class ErrorInfo(
        val code: String,
        val message: String,
        val severity: ErrorSeverity,
        val category: ErrorCategory,
        val userMessage: String,
        val canRetry: Boolean = true,
        val canUseManual: Boolean = true,
        val canReturn: Boolean = true
    )
    
    /**
     * Get error information for a given error code
     */
    fun getErrorInfo(errorCode: String, customMessage: String? = null): ErrorInfo {
        return when (errorCode) {
            "timeout" -> ErrorInfo(
                code = errorCode,
                message = "Processing timeout occurred",
                severity = ErrorSeverity.WARNING,
                category = ErrorCategory.TRANSCRIPT,
                userMessage = Constants.ErrorMessages.TIMEOUT_ERROR,
                canRetry = true,
                canUseManual = true,
                canReturn = true
            )
            "network_error", "webview_error" -> ErrorInfo(
                code = errorCode,
                message = "Network connectivity issue",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.NETWORK,
                userMessage = Constants.ErrorMessages.NETWORK_ERROR,
                canRetry = true,
                canUseManual = true,
                canReturn = true
            )
            "service_unavailable" -> ErrorInfo(
                code = errorCode,
                message = "Transcript service unavailable",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.TRANSCRIPT,
                userMessage = Constants.ErrorMessages.SERVICE_UNAVAILABLE,
                canRetry = true,
                canUseManual = true,
                canReturn = true
            )
            "invalid_url" -> ErrorInfo(
                code = errorCode,
                message = "Invalid URL format provided",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.USER_INPUT,
                userMessage = Constants.ErrorMessages.INVALID_URL,
                canRetry = false,
                canUseManual = true,
                canReturn = true
            )
            "invalid_data" -> ErrorInfo(
                code = errorCode,
                message = "No valid TikTok data found",
                severity = ErrorSeverity.WARNING,
                category = ErrorCategory.TRANSCRIPT,
                userMessage = Constants.ErrorMessages.INVALID_DATA,
                canRetry = true,
                canUseManual = true,
                canReturn = true
            )
            "no_subtitles" -> ErrorInfo(
                code = errorCode,
                message = "Subtitles not available for video",
                severity = ErrorSeverity.WARNING,
                category = ErrorCategory.TRANSCRIPT,
                userMessage = Constants.ErrorMessages.NO_SUBTITLES,
                canRetry = false,
                canUseManual = true,
                canReturn = true
            )
            "processing_timeout" -> ErrorInfo(
                code = errorCode,
                message = "Video processing timeout",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.TRANSCRIPT,
                userMessage = Constants.ErrorMessages.PROCESSING_TIMEOUT,
                canRetry = true,
                canUseManual = true,
                canReturn = true
            )
            "generic_error" -> ErrorInfo(
                code = errorCode,
                message = "Generic processing error",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.TRANSCRIPT,
                userMessage = Constants.ErrorMessages.GENERIC_ERROR,
                canRetry = true,
                canUseManual = true,
                canReturn = true
            )
            "red_error_text" -> ErrorInfo(
                code = errorCode,
                message = "Service error detected",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.TRANSCRIPT,
                userMessage = Constants.ErrorMessages.RED_ERROR_TEXT,
                canRetry = true,
                canUseManual = true,
                canReturn = true
            )
            "error_container" -> ErrorInfo(
                code = errorCode,
                message = "Service error container detected",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.TRANSCRIPT,
                userMessage = Constants.ErrorMessages.ERROR_CONTAINER,
                canRetry = true,
                canUseManual = true,
                canReturn = true
            )
            "webview_crash" -> ErrorInfo(
                code = errorCode,
                message = "WebView crashed",
                severity = ErrorSeverity.CRITICAL,
                category = ErrorCategory.WEBVIEW,
                userMessage = "A browser error occurred. This may be due to memory issues or network problems.",
                canRetry = true,
                canUseManual = true,
                canReturn = true
            )
            else -> ErrorInfo(
                code = errorCode,
                message = customMessage ?: "Unknown error occurred",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.UNKNOWN,
                userMessage = customMessage ?: Constants.ErrorMessages.UNKNOWN_ERROR,
                canRetry = true,
                canUseManual = true,
                canReturn = true
            )
        }
    }
    
    /**
     * Log error with appropriate severity
     */
    fun logError(errorInfo: ErrorInfo, additionalContext: String? = null) {
        val context = additionalContext?.let { " - $it" } ?: ""
        val logMessage = "${errorInfo.message}$context"
        
        when (errorInfo.severity) {
            ErrorSeverity.INFO -> Log.i(TAG, logMessage)
            ErrorSeverity.WARNING -> Log.w(TAG, logMessage)
            ErrorSeverity.ERROR -> Log.e(TAG, logMessage)
            ErrorSeverity.CRITICAL -> Log.e(TAG, "CRITICAL: $logMessage")
        }
        
        // Add to run buffer for tracking
        RunRingBuffer.addLog("", errorInfo.severity.name, logMessage, "ERROR")
    }
    
    /**
     * Handle error with logging and user feedback
     */
    fun handleError(
        errorCode: String,
        customMessage: String? = null,
        additionalContext: String? = null
    ): ErrorInfo {
        val errorInfo = getErrorInfo(errorCode, customMessage)
        logError(errorInfo, additionalContext)
        return errorInfo
    }
}
