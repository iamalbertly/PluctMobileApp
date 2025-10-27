package app.pluct.core.error

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

/**
 * Pluct-Error-02UnifiedHandler - Single source of truth for all error handling
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Consolidates all error handling, retry logic, and recovery mechanisms
 */
@Singleton
class PluctErrorUnifiedHandler @Inject constructor() {
    
    companion object {
        private const val TAG = "PluctErrorHandler"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 8000L
    }
    
    private val _errorState = MutableStateFlow<ErrorState>(ErrorState.None)
    val errorState: StateFlow<ErrorState> = _errorState.asStateFlow()
    
    private val _retryMetrics = MutableStateFlow(RetryMetrics())
    val retryMetrics: StateFlow<RetryMetrics> = _retryMetrics.asStateFlow()
    
    sealed class ErrorState {
        object None : ErrorState()
        data class Active(val errorInfo: ErrorInfo) : ErrorState()
    }
    
    data class ErrorInfo(
        val type: ErrorType,
        val message: String,
        val context: String,
        val timestamp: Long,
        val severity: ErrorSeverity,
        val retryable: Boolean
    )
    
    data class RetryMetrics(
        val totalAttempts: Int = 0,
        val successfulRetries: Int = 0,
        val failedRetries: Int = 0,
        val averageRetryTime: Long = 0
    )
    
    enum class ErrorType {
        NETWORK, API, VALIDATION, SYSTEM, USER
    }
    
    enum class ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    data class RetryConfig(
        val maxAttempts: Int = MAX_RETRY_ATTEMPTS,
        val baseDelayMs: Long = BASE_DELAY_MS,
        val maxDelayMs: Long = MAX_DELAY_MS,
        val exponentialBackoff: Boolean = true,
        val jitter: Boolean = true
    )
    
    data class RetryResult<T>(
        val success: Boolean,
        val result: T? = null,
        val error: Exception? = null,
        val attempts: Int,
        val totalDurationMs: Long
    )
    
    /**
     * Handle error with automatic retry logic
     */
    suspend fun <T> handleWithRetry(
        operation: suspend () -> T,
        config: RetryConfig = RetryConfig(),
        operationName: String = "operation"
    ): RetryResult<T> = withContext(Dispatchers.IO) {
        
        val startTime = System.currentTimeMillis()
        var lastException: Exception? = null
        
        repeat(config.maxAttempts) { attempt ->
            try {
                val result = operation()
                val duration = System.currentTimeMillis() - startTime
                
                updateRetryMetrics(true, duration)
                Log.d(TAG, "✅ $operationName succeeded on attempt ${attempt + 1}")
                
                return@withContext RetryResult(
                    success = true,
                    result = result,
                    attempts = attempt + 1,
                    totalDurationMs = duration
                )
                
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "❌ $operationName failed on attempt ${attempt + 1}: ${e.message}")
                
                if (attempt < config.maxAttempts - 1) {
                    val delay = calculateDelay(attempt, config)
                    Log.i(TAG, "⏳ Retrying $operationName in ${delay}ms")
                    delay(delay)
                }
            }
        }
        
        val totalDuration = System.currentTimeMillis() - startTime
        updateRetryMetrics(false, totalDuration)
        
        Log.e(TAG, "❌ $operationName failed after ${config.maxAttempts} attempts")
        return@withContext RetryResult(
            success = false,
            error = lastException,
            attempts = config.maxAttempts,
            totalDurationMs = totalDuration
        )
    }
    
    /**
     * Handle error without retry
     */
    fun handleError(
        error: Throwable,
        context: String,
        severity: ErrorSeverity = ErrorSeverity.MEDIUM,
        retryable: Boolean = false
    ) {
        val errorInfo = ErrorInfo(
            type = categorizeError(error),
            message = error.message ?: "Unknown error",
            context = context,
            timestamp = System.currentTimeMillis(),
            severity = severity,
            retryable = retryable
        )
        
        _errorState.value = ErrorState.Active(errorInfo)
        Log.e(TAG, "Error handled: ${errorInfo.message}")
    }
    
    /**
     * Clear current error state
     */
    fun clearError() {
        _errorState.value = ErrorState.None
    }
    
    /**
     * Calculate retry delay with exponential backoff and jitter
     */
    private fun calculateDelay(attempt: Int, config: RetryConfig): Long {
        val baseDelay = if (config.exponentialBackoff) {
            config.baseDelayMs * (1L shl attempt)
        } else {
            config.baseDelayMs
        }
        
        val cappedDelay = minOf(baseDelay, config.maxDelayMs)
        
        return if (config.jitter) {
            val jitter = (cappedDelay * 0.1 * Math.random()).toLong()
            cappedDelay + jitter
        } else {
            cappedDelay
        }
    }
    
    /**
     * Categorize error type
     */
    private fun categorizeError(error: Throwable): ErrorType {
        return when (error) {
            is java.net.UnknownHostException,
            is java.net.SocketTimeoutException,
            is java.io.IOException -> ErrorType.NETWORK
            is IllegalArgumentException,
            is IllegalStateException -> ErrorType.VALIDATION
            is SecurityException -> ErrorType.SYSTEM
            else -> ErrorType.API
        }
    }
    
    /**
     * Update retry metrics
     */
    private fun updateRetryMetrics(success: Boolean, duration: Long) {
        val current = _retryMetrics.value
        _retryMetrics.value = current.copy(
            totalAttempts = current.totalAttempts + 1,
            successfulRetries = if (success) current.successfulRetries + 1 else current.successfulRetries,
            failedRetries = if (!success) current.failedRetries + 1 else current.failedRetries,
            averageRetryTime = if (current.totalAttempts > 0) {
                (current.averageRetryTime + duration) / 2
            } else {
                duration
            }
        )
    }
}
