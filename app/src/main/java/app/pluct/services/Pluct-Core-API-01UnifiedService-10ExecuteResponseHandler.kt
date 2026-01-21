package app.pluct.services

import android.util.Log
import app.pluct.core.debug.PluctCoreDebug01LogManager
import app.pluct.core.retry.PluctCoreRetryUnifiedHandler
import app.pluct.core.checks.PluctCoreChecks01RetryabilityDecider

/**
 * Pluct-Core-API-01UnifiedService-10ExecuteResponseHandler - Handles response processing for execute method
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[ExecuteResponseHandler]
 * 5 scope layers: Project, Core, API, UnifiedService, ExecuteResponseHandler
 * Extracted from main service to keep file size under 300 lines
 */
class PluctCoreAPI01UnifiedService10ExecuteResponseHandler(
    private val debugLogManager: PluctCoreDebug01LogManager,
    private val circuitBreaker: PluctCoreAPIUnifiedServiceCircuitBreaker,
    private val metrics: PluctCoreAPIUnifiedServiceMetrics,
    private val retryHandler: PluctCoreRetryUnifiedHandler
) {
    
    fun logRequestStart(
        requestId: String,
        endpoint: String,
        method: String,
        requestUrl: String,
        payload: Map<String, Any>?
    ) {
        debugLogManager.logInfo(
            category = "API_REQUEST",
            operation = endpoint,
            message = "Request started",
            details = buildString {
                appendLine("ID: $requestId")
                appendLine("Method: $method")
                appendLine("Payload: ${payload ?: "None"}")
            },
            requestUrl = requestUrl,
            requestMethod = method,
            requestPayload = payload?.toString() ?: ""
        )
    }
    companion object {
        private const val TAG = "ExecuteResponseHandler"
    }
    
    fun <T> handleSuccess(
        result: Result<T>,
        outcome: PluctCoreRetryUnifiedHandler.RetryOutcome<T>,
        requestId: String,
        endpoint: String,
        method: String,
        requestUrl: String,
        payload: Map<String, Any>?,
        startTime: Long
    ) {
        circuitBreaker.recordSuccess()
        val duration = System.currentTimeMillis() - startTime
        debugLogManager.logInfo(
            category = "API_RESPONSE",
            operation = endpoint,
            message = "Request completed",
            details = buildString {
                appendLine("ID: $requestId")
                appendLine("Method: $method")
                appendLine("Duration: ${duration}ms")
                appendLine("Attempts: ${outcome.attempts}")
            },
            requestUrl = requestUrl,
            requestMethod = method,
            requestPayload = payload?.toString() ?: ""
        )
    }
    
    fun <T> handleFailure(
        result: Result<T>,
        outcome: PluctCoreRetryUnifiedHandler.RetryOutcome<T>,
        requestId: String,
        endpoint: String,
        requestUrl: String,
        payload: Map<String, Any>?,
        startTime: Long
    ) {
        val error = result.exceptionOrNull()
        if (shouldCountFailureForCircuitBreaker(error)) {
            circuitBreaker.recordFailure(isRetryableFailure(error))
        }
        val duration = System.currentTimeMillis() - startTime
        if (error is PluctCoreAPIDetailedError) {
            debugLogManager.logAPIError(error, "API_RESPONSE")
        } else {
            debugLogManager.logError(
                category = "API_RESPONSE",
                operation = endpoint,
                message = error?.message ?: "Request failed",
                exception = error,
                requestUrl = requestUrl,
                requestPayload = payload?.toString() ?: "",
                responseBody = buildString {
                    appendLine("Duration: ${duration}ms; Attempts: ${outcome.attempts}")
                    appendLine("Request ID: $requestId")
                    if (circuitBreaker.isOpen()) {
                        appendLine("Circuit breaker currently OPEN")
                    }
                }
            )
        }
        metrics.updateMetrics(result.isSuccess, outcome.attempts - 1)
    }
    
    private fun isRetryableFailure(error: Throwable?): Boolean {
        return retryHandler.isRetryable(error)
    }
    
    private fun shouldCountFailureForCircuitBreaker(error: Throwable?): Boolean {
        return PluctCoreChecks01RetryabilityDecider.shouldCountForCircuitBreaker(error)
    }
}
