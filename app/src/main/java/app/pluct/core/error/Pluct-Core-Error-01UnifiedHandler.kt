package app.pluct.core.error

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.random.Random
import app.pluct.utils.Constants
import app.pluct.data.EngineError

/**
 * Pluct-Core-Error-01UnifiedHandler - Single source of truth for all error handling
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Consolidates UI error handling, retry logic, and Business Engine error mapping
 */
object PluctUnifiedErrorHandler {
    
    private const val TAG = "PluctUnifiedErrorHandler"
    private const val MAX_RETRIES = 3
    private const val BASE_DELAY_MS = 1000L
    private const val MAX_DELAY_MS = 30000L
    
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
        NETWORK, WEBVIEW, TRANSCRIPT, USER_INPUT, SYSTEM, BUSINESS_ENGINE, UNKNOWN
    }
    
    /**
     * Retry configuration for different error types
     */
    data class RetryConfig(
        val maxRetries: Int = MAX_RETRIES,
        val baseDelayMs: Long = BASE_DELAY_MS,
        val maxDelayMs: Long = MAX_DELAY_MS,
        val jitter: Boolean = true
    )
    
    /**
     * Comprehensive error information
     */
    data class ErrorInfo(
        val code: String,
        val message: String,
        val severity: ErrorSeverity,
        val category: ErrorCategory,
        val userMessage: String,
        val canRetry: Boolean = true,
        val canUseManual: Boolean = true,
        val canReturn: Boolean = true,
        val isRetryable: Boolean = true
    )
    
    /**
     * Get error information for a given error code
     */
    fun getErrorInfo(errorCode: String, customMessage: String? = null): ErrorInfo {
        return when (errorCode) {
            "timeout", "processing_timeout" -> ErrorInfo(
                code = errorCode,
                message = "Processing timeout occurred",
                severity = ErrorSeverity.WARNING,
                category = ErrorCategory.TRANSCRIPT,
                userMessage = Constants.ErrorMessages.TIMEOUT_ERROR,
                canRetry = true,
                canUseManual = true,
                canReturn = true,
                isRetryable = true
            )
            "network_error", "webview_error" -> ErrorInfo(
                code = errorCode,
                message = "Network connectivity issue",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.NETWORK,
                userMessage = Constants.ErrorMessages.NETWORK_ERROR,
                canRetry = true,
                canUseManual = true,
                canReturn = true,
                isRetryable = true
            )
            "service_unavailable" -> ErrorInfo(
                code = errorCode,
                message = "Transcript service unavailable",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.TRANSCRIPT,
                userMessage = Constants.ErrorMessages.SERVICE_UNAVAILABLE,
                canRetry = true,
                canUseManual = true,
                canReturn = true,
                isRetryable = true
            )
            "invalid_url" -> ErrorInfo(
                code = errorCode,
                message = "Invalid URL format provided",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.USER_INPUT,
                userMessage = Constants.ErrorMessages.INVALID_URL,
                canRetry = false,
                canUseManual = true,
                canReturn = true,
                isRetryable = false
            )
            "invalid_data" -> ErrorInfo(
                code = errorCode,
                message = "No valid TikTok data found",
                severity = ErrorSeverity.WARNING,
                category = ErrorCategory.TRANSCRIPT,
                userMessage = Constants.ErrorMessages.INVALID_DATA,
                canRetry = true,
                canUseManual = true,
                canReturn = true,
                isRetryable = true
            )
            "no_subtitles" -> ErrorInfo(
                code = errorCode,
                message = "Subtitles not available for video",
                severity = ErrorSeverity.WARNING,
                category = ErrorCategory.TRANSCRIPT,
                userMessage = Constants.ErrorMessages.NO_SUBTITLES,
                canRetry = false,
                canUseManual = true,
                canReturn = true,
                isRetryable = false
            )
            "generic_error" -> ErrorInfo(
                code = errorCode,
                message = "Generic processing error",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.TRANSCRIPT,
                userMessage = Constants.ErrorMessages.GENERIC_ERROR,
                canRetry = true,
                canUseManual = true,
                canReturn = true,
                isRetryable = true
            )
            "webview_crash" -> ErrorInfo(
                code = errorCode,
                message = "WebView crashed",
                severity = ErrorSeverity.CRITICAL,
                category = ErrorCategory.WEBVIEW,
                userMessage = "A browser error occurred. This may be due to memory issues or network problems.",
                canRetry = true,
                canUseManual = true,
                canReturn = true,
                isRetryable = true
            )
            else -> ErrorInfo(
                code = errorCode,
                message = customMessage ?: "Unknown error occurred",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.UNKNOWN,
                userMessage = customMessage ?: Constants.ErrorMessages.UNKNOWN_ERROR,
                canRetry = true,
                canUseManual = true,
                canReturn = true,
                isRetryable = true
            )
        }
    }
    
    /**
     * Handle Business Engine errors
     */
    fun handleEngineError(error: EngineError): ErrorInfo {
        return when (error) {
            is EngineError.Network -> ErrorInfo(
                code = "ENGINE_NETWORK",
                message = "Business Engine network error",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.BUSINESS_ENGINE,
                userMessage = "Network connection failed. Please check your internet connection.",
                canRetry = true,
                isRetryable = true
            )
            is EngineError.Auth -> ErrorInfo(
                code = "ENGINE_AUTH",
                message = "Business Engine authentication failed",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.BUSINESS_ENGINE,
                userMessage = "Authentication failed. Please try again.",
                canRetry = true,
                isRetryable = true
            )
            is EngineError.InsufficientCredits -> ErrorInfo(
                code = "ENGINE_CREDITS",
                message = "Insufficient credits for operation",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.BUSINESS_ENGINE,
                userMessage = "Insufficient credits. Please add more credits to continue.",
                canRetry = false,
                isRetryable = false
            )
            is EngineError.RateLimited -> ErrorInfo(
                code = "ENGINE_RATE_LIMIT",
                message = "Rate limit exceeded",
                severity = ErrorSeverity.WARNING,
                category = ErrorCategory.BUSINESS_ENGINE,
                userMessage = "Rate limit exceeded. Please wait a moment and try again.",
                canRetry = true,
                isRetryable = true
            )
            is EngineError.InvalidUrl -> ErrorInfo(
                code = "ENGINE_INVALID_URL",
                message = "Invalid URL format",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.USER_INPUT,
                userMessage = "Invalid URL format. Please check the URL and try again.",
                canRetry = false,
                isRetryable = false
            )
            is EngineError.Upstream -> ErrorInfo(
                code = "ENGINE_UPSTREAM",
                message = "Upstream service error",
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.BUSINESS_ENGINE,
                userMessage = "Server error (${error.code}): ${error.errorMessage ?: "Unknown error"}",
                canRetry = error.code in 500..599,
                isRetryable = error.code in 500..599
            )
            is EngineError.Unexpected -> ErrorInfo(
                code = "ENGINE_UNEXPECTED",
                message = "Unexpected Business Engine error",
                severity = ErrorSeverity.CRITICAL,
                category = ErrorCategory.BUSINESS_ENGINE,
                userMessage = "Unexpected error: ${error.errorCause.message}",
                canRetry = true,
                isRetryable = true
            )
        }
    }
    
    /**
     * Execute a suspend function with retry logic
     */
    suspend fun <T> withRetry(
        config: RetryConfig = RetryConfig(),
        operation: suspend () -> T
    ): T {
        var lastException: Exception? = null
        
        repeat(config.maxRetries) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")
                
                if (attempt < config.maxRetries - 1) {
                    val delay = calculateDelay(attempt, config)
                    Log.d(TAG, "Retrying in ${delay}ms...")
                    delay(delay)
                }
            }
        }
        
        throw lastException ?: Exception("All retry attempts failed")
    }
    
    /**
     * Calculate delay with exponential backoff and jitter
     */
    private fun calculateDelay(attempt: Int, config: RetryConfig): Long {
        val exponentialDelay = config.baseDelayMs * (1L shl attempt)
        val cappedDelay = minOf(exponentialDelay, config.maxDelayMs)
        
        return if (config.jitter) {
            val jitterRange = (cappedDelay * 0.1).toLong()
            cappedDelay + Random.nextLong(-jitterRange, jitterRange)
        } else {
            cappedDelay
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
    
    /**
     * Get user-friendly error message for any throwable
     */
    fun getUserFriendlyMessage(error: Throwable): String {
        return when (error) {
            is EngineError -> handleEngineError(error).userMessage
            is java.net.UnknownHostException -> "No internet connection. Please check your network settings."
            is java.net.SocketTimeoutException -> "Request timed out. Please try again."
            is java.net.ConnectException -> "Cannot connect to server. Please try again later."
            else -> "An unexpected error occurred: ${error.message ?: "Unknown error"}"
        }
    }
}
