package app.pluct.core.checks

import android.util.Log
import app.pluct.services.PluctCoreAPIDetailedError
import app.pluct.shared.PluctApiError
import app.pluct.shared.PluctRetryPolicy

/**
 * Pluct-Core-Checks-01RetryabilityDecider - Centralized retry decision logic
 * Follows naming convention: [Project]-[Scope]-[Module]-[CoreResponsibility]
 * 
 * TECH DEBT FIX #1: Single source of truth for retry decisions
 * Consolidates retry logic that was duplicated across:
 * - PluctCoreAPIHTTPClient02Implementation
 * - PluctCoreAPIUnifiedService01Main
 * - PluctCoreRetryUnifiedHandler
 * - PluctCoreErrorUserMessageFormatter03
 */
object PluctCoreChecks01RetryabilityDecider {
    private const val TAG = "RetryabilityDecider"

    /**
     * Determine if an error is retryable based on type, message, and HTTP status code
     */
    fun isErrorRetryable(error: Throwable?): Boolean {
        if (error == null) return false

        // Check if it's a detailed API error (has HTTP status code)
        if (error is PluctCoreAPIDetailedError) {
            return isDetailedErrorRetryable(error)
        }

        // Check generic error message patterns
        val message = error.message?.lowercase() ?: ""
        return isMessageRetryable(message) || isExceptionTypeRetryable(error.javaClass.simpleName)
    }

    /**
     * Determine if a detailed API error is retryable
     */
    private fun isDetailedErrorRetryable(error: PluctCoreAPIDetailedError): Boolean {
        val statusCode = error.technicalDetails.responseStatusCode
        val message = error.userMessage.lowercase()

        // Network-level errors (retryable)
        if (message.contains("timeout") || message.contains("connection") ||
            message.contains("network") || message.contains("temporarily") ||
            message.contains("unavailable")) {
            Log.d(TAG, "Retryable: Network-level error")
            return true
        }

        // Server errors (retryable)
        if (statusCode >= 500 || statusCode == 429 || statusCode == 408) {
            Log.d(TAG, "Retryable: Server error (status=$statusCode)")
            return true
        }

        // Client errors (non-retryable except 408, 429)
        if (statusCode in 400..499) {
            Log.d(TAG, "Non-retryable: Client error (status=$statusCode)")
            return false
        }

        return PluctRetryPolicy.isRetryable(
            PluctApiError(
                statusCode = statusCode.takeIf { it > 0 },
                message = error.userMessage,
                exceptionType = error.javaClass.simpleName,
                retryableHint = error.isRetryable
            )
        )
    }

    /**
     * Check if error message indicates a retryable condition
     */
    private fun isMessageRetryable(message: String): Boolean {
        return PluctRetryPolicy.isRetryable(PluctApiError(message = message))
    }

    /**
     * Check if exception type indicates a retryable condition
     */
    private fun isExceptionTypeRetryable(exceptionType: String): Boolean {
        return PluctRetryPolicy.isRetryable(PluctApiError(exceptionType = exceptionType))
    }

    /**
     * Determine if a failure should count toward circuit breaker threshold
     * Not all failures should trigger circuit breaker (e.g., client validation errors)
     */
    fun shouldCountForCircuitBreaker(error: Throwable?): Boolean {
        if (error is PluctCoreAPIDetailedError) {
            val statusCode = error.technicalDetails.responseStatusCode
            // Don't count 4xx errors (except 408, 429) as circuit breaker failures
            if (statusCode in 400..499 && statusCode != 408 && statusCode != 429) {
                Log.d(TAG, "Circuit breaker: Not counting client error (status=$statusCode)")
                return false
            }
        }
        // Count all retryable errors toward circuit breaker
        return if (error is PluctCoreAPIDetailedError) {
            PluctRetryPolicy.shouldCountForCircuitBreaker(
                PluctApiError(
                    statusCode = error.technicalDetails.responseStatusCode.takeIf { it > 0 },
                    message = error.userMessage,
                    exceptionType = error.javaClass.simpleName,
                    retryableHint = error.isRetryable
                )
            )
        } else {
            isErrorRetryable(error)
        }
    }

    /**
     * Calculate backoff delay for retry attempt
     * Uses exponential backoff: 1s, 2s, 4s, etc., up to 30s
     */
    fun calculateRetryDelayMs(attemptNumber: Int, baseDelayMs: Long = 1000): Long {
        return PluctRetryPolicy.calculateRetryDelayMs(attemptNumber, baseDelayMs)
    }
}
