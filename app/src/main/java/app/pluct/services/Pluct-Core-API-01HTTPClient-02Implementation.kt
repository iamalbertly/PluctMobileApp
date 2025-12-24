package app.pluct.services

import android.util.Log
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.ConnectException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.IOException
import javax.net.ssl.SSLException

/**
 * Error information data class for structured error handling
 */
data class ErrorInfo(
    val statusCode: Int,
    val errorCode: String,
    val errorMessage: String,
    val upstreamStatus: String?,
    val upstreamResponse: String?,
    val upstreamError: String?,
    val fullResponse: String,
    val endpoint: String
) {
    fun toDetailedMessage(): String {
        val parts = mutableListOf<String>()
        parts.add("HTTP $statusCode: $errorMessage")
        parts.add("Error Code: $errorCode")
        parts.add("Endpoint: $endpoint")
        
        if (upstreamStatus != null) {
            parts.add("Upstream Status: $upstreamStatus")
        }
        if (upstreamResponse != null) {
            parts.add("Upstream Response: $upstreamResponse")
        }
        if (upstreamError != null) {
            parts.add("Upstream Error: $upstreamError")
        }
        
        return parts.joinToString(" | ")
    }
    
    fun isUpstreamError(): Boolean = upstreamStatus != null || upstreamResponse != null
    
    fun isAuthenticationError(): Boolean = statusCode == 401 || errorCode.contains("unauthorized", ignoreCase = true)
}

/**
 * Pluct-Core-API-01HTTPClient-02Implementation - HTTP client implementation orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[SubScope]-[Separation]-[Responsibility]
 * Single source of truth for HTTP operations - delegates to specialized components
 */
class PluctCoreAPIHTTPClientImpl(
    private val logger: PluctCoreLoggingStructuredLogger,
    private val validator: PluctCoreValidationInputSanitizer,
    private val userIdentification: PluctCoreUserIdentification
) {
    
    companion object {
        private const val TAG = "PluctCoreAPIHTTPClient"
    }
    
    private val requestBuilder = PluctCoreAPIHTTPClientRequestBuilder()
    private val responseParser = PluctCoreAPIHTTPClientResponseParser()
    private val apiLogger = PluctCoreAPIHTTPClientLogger()
    
    private fun buildUserFacingErrorMessage(errorInfo: ErrorInfo, statusCode: Int): String {
        // Priority 1: Upstream error (most specific)
        if (errorInfo.upstreamError != null) {
            return when {
                errorInfo.upstreamError.contains("timeout", ignoreCase = true) ->
                    "The video is taking too long to process. Try a shorter video or check your connection."
                errorInfo.upstreamError.contains("invalid", ignoreCase = true) ||
                errorInfo.upstreamError.contains("format", ignoreCase = true) ->
                    "This video URL format isn't supported. Please use a standard TikTok link."
                errorInfo.upstreamError.contains("unavailable", ignoreCase = true) ||
                errorInfo.upstreamError.contains("down", ignoreCase = true) ->
                    "The transcription service is temporarily unavailable. Please try again in a few moments."
                errorInfo.upstreamError.contains("too long", ignoreCase = true) ->
                    "This video is too long to transcribe. Please try a shorter video."
                else -> "Transcription failed: ${errorInfo.upstreamError}. Please try again."
            }
        }
        
        // Priority 2: HTTP status code with context
        return when (statusCode) {
            500 -> "The transcription service encountered an error. Please try again in a moment."
            502, 503 -> "The service is temporarily unavailable. Please try again shortly."
            504 -> "The request timed out. Please check your connection and try again."
            401 -> "Authentication failed. Tap refresh session and retry."
            402 -> "Insufficient credits. Please add credits to continue."
            429 -> "Too many requests. Please wait a moment before trying again."
            else -> "Request failed ($statusCode). ${errorInfo.errorMessage}"
        }
    }
    
    suspend fun executeRequest(
        method: String,
        endpoint: String,
        payload: Map<String, Any>? = null,
        authToken: String? = null,
        timeoutOverrideMs: Long? = null
    ): Result<Any> {
        return withContext(Dispatchers.IO) {
            val requestId = "req_${System.currentTimeMillis()}"
            val timestamp = System.currentTimeMillis()
            val fullUrl = "https://pluct-business-engine.romeo-lya2.workers.dev$endpoint"
            val userId = userIdentification.userId
            
            // Log request using unified logger
            apiLogger.logRequest(requestId, timestamp, method, fullUrl, endpoint, payload, authToken)
            
            val connection = try {
                requestBuilder.buildConnection(
                    method = method,
                    endpoint = endpoint,
                    payload = payload,
                    authToken = authToken,
                    requestId = requestId,
                    userId = userId,
                    timeoutOverrideMs = timeoutOverrideMs
                )
            } catch (e: Exception) {
                apiLogger.logNetworkException(requestId, e, "Connection build failed")
                return@withContext Result.failure(e)
            }
            
            try {
                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage ?: ""
                val responseTimestamp = System.currentTimeMillis()
                val requestDuration = responseTimestamp - timestamp
                
                val responseBody = if (responseCode in 200..299) {
                    val body = requestBuilder.readResponseBody(connection)
                    apiLogger.logResponse(
                        requestId, responseTimestamp, requestDuration,
                        responseCode, responseMessage, body, connection.headerFields
                    )
                    body
                } else {
                    val errorBody = requestBuilder.readErrorBody(connection)
                    apiLogger.logError(requestId, responseCode, responseMessage, errorBody)
                    
                    // NEW: Log full error details for debugging
                    Log.e(TAG, "=== API ERROR DETAILS ===")
                    Log.e(TAG, "Request: $method $endpoint")
                    Log.e(TAG, "Status: $responseCode $responseMessage")
                    Log.e(TAG, "Error Body: $errorBody")
                    Log.e(TAG, "Request ID: $requestId")
                    Log.e(TAG, "User ID: $userId")
                    Log.e(TAG, "========================")
                    
                    val errorInfo = responseParser.parseErrorResponse(
                        responseCode, responseMessage, errorBody, endpoint
                    )
                    
                    // Log parsed error details
                    Log.e(TAG, "Parsed Error Code: ${errorInfo.errorCode}")
                    Log.e(TAG, "Parsed Error Message: ${errorInfo.errorMessage}")
                    Log.e(TAG, "Upstream Error: ${errorInfo.upstreamError ?: "none"}")
                    Log.e(TAG, "Upstream Status: ${errorInfo.upstreamStatus ?: "none"}")
                    
                    // Build user-friendly error message with actionable guidance
                    val userMessage = buildUserFacingErrorMessage(errorInfo, responseCode)
                    
                    val detailedError = PluctCoreAPIDetailedError(
                        userMessage = userMessage,
                        technicalDetails = TechnicalErrorDetails(
                            serviceName = "Business Engine (Cloudflare Workers)",
                            operation = "$method $endpoint",
                            endpoint = endpoint,
                            requestMethod = method,
                            requestUrl = fullUrl,
                            requestPayload = payload?.toString() ?: "",
                            requestHeaders = buildString {
                                append("X-Request-ID=$requestId; ")
                                append("X-User-Id=$userId; ")
                                if (authToken != null) append("Authorization: Bearer ${authToken.take(12)}...${authToken.takeLast(6)}")
                            },
                            responseStatusCode = responseCode,
                            responseStatusMessage = responseMessage,
                            responseBody = errorBody,
                            responseHeaders = connection.headerFields.entries.joinToString { "${it.key}:${it.value.joinToString()}" },
                            errorType = if (errorInfo.isAuthenticationError()) "AUTH_ERROR" else if (errorInfo.isUpstreamError()) "UPSTREAM_ERROR" else "API_ERROR",
                            errorCode = errorInfo.errorCode,
                            expectedFormat = "Refer to Business Engine contract for $endpoint",
                            timestamp = responseTimestamp
                        ),
                        isRetryable = responseCode >= 500 || responseCode == 429
                    )
                    return@withContext Result.failure(detailedError)
                }
                
                val parsedResult = responseParser.parseResponse(endpoint, responseBody)
                if (parsedResult.isSuccess) {
                    apiLogger.logSuccess(requestId)
                } else {
                    val parseError = parsedResult.exceptionOrNull()?.message ?: "Unknown parse error"
                    apiLogger.logParseError(requestId, parseError)
                }
                
                parsedResult
            } catch (e: java.net.SocketTimeoutException) {
                apiLogger.logNetworkException(requestId, e, "Request timeout")
                Result.failure(Exception("Request timeout: ${e.message}"))
            } catch (e: java.net.UnknownHostException) {
                apiLogger.logNetworkException(requestId, e, "Host not found")
                Result.failure(Exception("Host not found: ${e.message}"))
            } catch (e: java.net.ConnectException) {
                apiLogger.logNetworkException(requestId, e, "Connection failed")
                Result.failure(Exception("Connection failed: ${e.message}"))
            } catch (e: javax.net.ssl.SSLException) {
                apiLogger.logNetworkException(requestId, e, "SSL error")
                Result.failure(Exception("SSL error: ${e.message}"))
            } catch (e: java.io.IOException) {
                apiLogger.logNetworkException(requestId, e, "IO error")
                if (e.message?.contains("ECONNRESET") == true || e.message?.contains("Connection reset") == true) {
                    Result.failure(Exception("Connection reset by peer: ${e.message}"))
                } else {
                    Result.failure(Exception("IO error: ${e.message}"))
                }
            } catch (e: Exception) {
                apiLogger.logNetworkException(requestId, e, "Request failed")
                Result.failure(e)
            } finally {
                connection.disconnect()
            }
        }
    }
}
