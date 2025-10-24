package app.pluct.services

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Pluct-API-01ResponseHandler - Optimized API response handling and error recovery
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */

class PluctApiResponseHandler {
    
    /**
     * Handles API responses with automatic retry logic and error recovery
     */
    suspend fun <T> handleApiCall(
        apiCall: suspend () -> T,
        maxRetries: Int = 3,
        baseDelayMs: Long = 1000,
        maxDelayMs: Long = 10000
    ): Result<T> {
        var lastException: Exception? = null
        var currentDelay = baseDelayMs
        
        repeat(maxRetries) { attempt ->
            try {
                val result = apiCall()
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e
                
                if (!shouldRetry(e)) {
                    return Result.failure(e)
                }
                
                if (attempt < maxRetries - 1) {
                    delay(currentDelay)
                    currentDelay = minOf(currentDelay * 2, maxDelayMs)
                }
            }
        }
        
        return Result.failure(lastException ?: Exception("Unknown error"))
    }
    
    /**
     * Determines if an exception should trigger a retry
     */
    private fun shouldRetry(exception: Exception): Boolean {
        return when (exception) {
            is SocketTimeoutException -> true
            is ConnectException -> true
            is UnknownHostException -> true
            is SSLException -> true
            is IOException -> true
            is PluctApiException -> exception.isRetryable
            else -> false
        }
    }
    
    /**
     * Creates a flow with automatic retry logic for API calls
     */
    fun <T> createRetryableFlow(
        apiCall: suspend () -> T,
        maxRetries: Int = 3,
        baseDelayMs: Long = 1000
    ): Flow<T> = flow {
        emit(apiCall())
    }.retryWhen { cause, attempt ->
        if (attempt >= maxRetries) {
            false
        } else {
            delay(baseDelayMs * attempt)
            shouldRetry(cause as Exception)
        }
    }
    
    /**
     * Handles specific API error responses with appropriate recovery strategies
     */
    suspend fun handleApiError(
        error: PluctApiException,
        onRetry: (() -> Unit)? = null,
        onFallback: (() -> Unit)? = null
    ): ApiErrorHandlingResult {
        return when (error.errorType) {
            ApiErrorType.NETWORK_ERROR -> {
                if (onRetry != null) {
                    onRetry()
                    ApiErrorHandlingResult.RETRY
                } else {
                    ApiErrorHandlingResult.FAIL
                }
            }
            ApiErrorType.AUTHENTICATION_ERROR -> {
                // Handle authentication errors
                ApiErrorHandlingResult.REAUTHENTICATE
            }
            ApiErrorType.RATE_LIMIT_ERROR -> {
                // Handle rate limiting
                delay(error.retryAfterMs ?: 5000)
                if (onRetry != null) {
                    onRetry()
                    ApiErrorHandlingResult.RETRY
                } else {
                    ApiErrorHandlingResult.FAIL
                }
            }
            ApiErrorType.SERVER_ERROR -> {
                if (onFallback != null) {
                    onFallback()
                    ApiErrorHandlingResult.FALLBACK
                } else {
                    ApiErrorHandlingResult.FAIL
                }
            }
            ApiErrorType.CLIENT_ERROR -> {
                // Client errors are usually not retryable
                ApiErrorHandlingResult.FAIL
            }
        }
    }
    
    /**
     * Validates API response structure and content
     */
    fun <T> validateResponse(
        response: T,
        validator: (T) -> Boolean
    ): Result<T> {
        return if (validator(response)) {
            Result.success(response)
        } else {
            Result.failure(PluctApiException(
                errorType = ApiErrorType.CLIENT_ERROR,
                message = "Invalid response format",
                isRetryable = false
            ))
        }
    }
    
    /**
     * Implements circuit breaker pattern for API calls
     */
    class CircuitBreaker(
        private val failureThreshold: Int = 5,
        private val timeoutMs: Long = 60000
    ) {
        private var failureCount = 0
        private var lastFailureTime = 0L
        private var state = CircuitBreakerState.CLOSED
        
        suspend fun <T> execute(apiCall: suspend () -> T): Result<T> {
            when (state) {
                CircuitBreakerState.OPEN -> {
                    if (System.currentTimeMillis() - lastFailureTime > timeoutMs) {
                        state = CircuitBreakerState.HALF_OPEN
                    } else {
                        return Result.failure(PluctApiException(
                            errorType = ApiErrorType.SERVER_ERROR,
                            message = "Circuit breaker is open",
                            isRetryable = false
                        ))
                    }
                }
                CircuitBreakerState.HALF_OPEN -> {
                    // Allow one call to test if service is back
                }
                CircuitBreakerState.CLOSED -> {
                    // Normal operation
                }
            }
            
            return try {
                val result = apiCall()
                onSuccess()
                Result.success(result)
            } catch (e: Exception) {
                onFailure()
                Result.failure(e)
            }
        }
        
        private fun onSuccess() {
            failureCount = 0
            state = CircuitBreakerState.CLOSED
        }
        
        private fun onFailure() {
            failureCount++
            lastFailureTime = System.currentTimeMillis()
            
            if (failureCount >= failureThreshold) {
                state = CircuitBreakerState.OPEN
            }
        }
    }
}

/**
 * Custom API exception with enhanced error information
 */
class PluctApiException(
    val errorType: ApiErrorType,
    message: String,
    val isRetryable: Boolean = false,
    val retryAfterMs: Long? = null,
    cause: Throwable? = null
) : Exception(message, cause)

enum class ApiErrorType {
    NETWORK_ERROR,
    AUTHENTICATION_ERROR,
    RATE_LIMIT_ERROR,
    SERVER_ERROR,
    CLIENT_ERROR
}

enum class ApiErrorHandlingResult {
    RETRY,
    REAUTHENTICATE,
    FALLBACK,
    FAIL
}

enum class CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}
