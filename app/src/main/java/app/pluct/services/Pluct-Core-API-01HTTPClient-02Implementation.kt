package app.pluct.services

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
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
 * Pluct-Core-API-01HTTPClient - HTTP client implementation
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single source of truth for HTTP operations
 */
class PluctCoreAPIHTTPClientImpl(
    private val logger: PluctCoreLoggingStructuredLogger,
    private val validator: PluctCoreValidationInputSanitizer,
    private val userIdentification: PluctCoreUserIdentification
) {
    
    companion object {
        private const val TAG = "PluctCoreAPIHTTPClient"
        private const val BASE_URL = "https://pluct-business-engine.romeo-lya2.workers.dev"
        private const val TIMEOUT_MS = 30000L
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun executeRequest(
        method: String,
        endpoint: String,
        payload: Map<String, Any>? = null,
        authToken: String? = null
    ): Result<Any> {
        return withContext(Dispatchers.IO) {
            val fullUrl = "$BASE_URL$endpoint"
            val requestId = "req_${System.currentTimeMillis()}"
            
            // Detailed request logging
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🚀 API REQUEST [$requestId]")
            Log.d(TAG, "   Method: $method")
            Log.d(TAG, "   URL: $fullUrl")
            Log.d(TAG, "   Endpoint: $endpoint")
            if (payload != null) {
                Log.d(TAG, "   Payload: $payload")
            }
            if (authToken != null) {
                Log.d(TAG, "   Auth Token: ${authToken.take(30)}...${authToken.takeLast(10)}")
                Log.d(TAG, "   Token Length: ${authToken.length}")
            }
            
            val url = URL(fullUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            try {
                connection.requestMethod = method
                connection.connectTimeout = TIMEOUT_MS.toInt()
                connection.readTimeout = TIMEOUT_MS.toInt()
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("User-Agent", "PluctMobile/1.0")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("X-Request-ID", requestId)
                
                if (authToken != null) {
                    connection.setRequestProperty("Authorization", "Bearer $authToken")
                    Log.d(TAG, "   ✅ Authorization header set")
                }
                
                if (payload != null && method in listOf("POST", "PUT", "PATCH")) {
                    connection.doOutput = true
                    val jsonPayload = json.encodeToString(JsonObject.serializer(), JsonObject(payload.mapValues { JsonPrimitive(it.value.toString()) }))
                    Log.d(TAG, "   📤 JSON Payload: $jsonPayload")
                    OutputStreamWriter(connection.outputStream).use { it.write(jsonPayload) }
                }
                
                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage ?: ""
                
                // Log response headers
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "📥 API RESPONSE [$requestId]")
                Log.d(TAG, "   Status: $responseCode $responseMessage")
                connection.headerFields.forEach { (key, values) ->
                    if (key != null) {
                        Log.d(TAG, "   Header: $key = ${values.joinToString(", ")}")
                    }
                }
                
                val responseBody = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                } else {
                    // Read error response body
                    val errorStream = connection.errorStream
                    val errorBody = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    } else {
                        "No error body available"
                    }
                    
                    Log.e(TAG, "❌ API ERROR [$requestId]")
                    Log.e(TAG, "   Status: $responseCode $responseMessage")
                    Log.e(TAG, "   Error Body: ${errorBody.take(500)}")
                    
                    val errorInfo = parseErrorResponse(responseCode, responseMessage, errorBody, endpoint)
                    throw Exception("HTTP $responseCode: ${errorInfo.errorMessage}")
                }
                
                Log.d(TAG, "   Response Body: ${responseBody.take(200)}${if (responseBody.length > 200) "..." else ""}")
                
                val parsedResult = parseResponse(endpoint, responseBody)
                if (parsedResult.isSuccess) {
                    Log.d(TAG, "   ✅ Request succeeded [$requestId]")
                } else {
                    Log.e(TAG, "   ❌ Parse failed [$requestId]: ${parsedResult.exceptionOrNull()?.message}")
                }
                
                parsedResult
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "❌ Request timeout [$requestId]: ${e.message}")
                Result.failure(Exception("Request timeout: ${e.message}"))
            } catch (e: java.net.UnknownHostException) {
                Log.e(TAG, "❌ Host not found [$requestId]: ${e.message}")
                Result.failure(Exception("Host not found: ${e.message}"))
            } catch (e: java.net.ConnectException) {
                Log.e(TAG, "❌ Connection failed [$requestId]: ${e.message}")
                Result.failure(Exception("Connection failed: ${e.message}"))
            } catch (e: javax.net.ssl.SSLException) {
                Log.e(TAG, "❌ SSL error [$requestId]: ${e.message}")
                Result.failure(Exception("SSL error: ${e.message}"))
            } catch (e: java.io.IOException) {
                Log.e(TAG, "❌ IO error [$requestId]: ${e.message}")
                if (e.message?.contains("ECONNRESET") == true || e.message?.contains("Connection reset") == true) {
                    Result.failure(Exception("Connection reset by peer: ${e.message}"))
                } else {
                    Result.failure(Exception("IO error: ${e.message}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Request failed [$requestId]: ${e.message}", e)
                Result.failure(e)
            } finally {
                connection.disconnect()
            }
        }
    }
    
    /**
     * Parse error response to extract detailed information
     */
    private fun parseErrorResponse(
        statusCode: Int,
        statusMessage: String,
        responseBody: String,
        endpoint: String
    ): ErrorInfo {
        return try {
            val jsonResponse = json.decodeFromString<JsonObject>(responseBody)
            
            // Extract error code and message
            val errorCode = jsonResponse["code"]?.toString()?.trim('"') ?: "unknown_error"
            val errorMessage = jsonResponse["message"]?.toString()?.trim('"') ?: statusMessage
            
            // Extract upstream error details (for proxy errors like 502)
            val details = jsonResponse["details"] as? JsonObject
            val upstreamStatus = details?.get("upstreamStatus")?.toString()?.trim('"')
            val upstreamResponse = details?.get("upstreamResponse")?.toString()?.trim('"')
            val upstreamError = details?.get("error")?.toString()?.trim('"')
            
            ErrorInfo(
                statusCode = statusCode,
                errorCode = errorCode,
                errorMessage = errorMessage,
                upstreamStatus = upstreamStatus,
                upstreamResponse = upstreamResponse,
                upstreamError = upstreamError,
                fullResponse = responseBody,
                endpoint = endpoint
            )
        } catch (e: Exception) {
            // Fallback if JSON parsing fails
            ErrorInfo(
                statusCode = statusCode,
                errorCode = "parse_error",
                errorMessage = statusMessage,
                upstreamStatus = null,
                upstreamResponse = null,
                upstreamError = null,
                fullResponse = responseBody,
                endpoint = endpoint
            )
        }
    }
    
    private fun parseResponse(endpoint: String, responseBody: String): Result<Any> {
        return try {
            val result = when {
                endpoint.contains("/credits/balance") -> {
                    try {
                        json.decodeFromString<CreditBalanceResponse>(responseBody)
                    } catch (e: Exception) {
                        throw createDetailedParseError(
                            service = "Business Engine (Cloudflare Workers)",
                            endpoint = endpoint,
                            operation = "Credit Balance Check",
                            expectedFormat = "CreditBalanceResponse { userId: String, balance: Int, updatedAt: String }",
                            actualResponse = responseBody,
                            error = e
                        )
                    }
                }
                endpoint.contains("/vend-token") -> {
                    try {
                        json.decodeFromString<VendTokenResponse>(responseBody)
                    } catch (e: Exception) {
                        throw createDetailedParseError(
                            service = "Business Engine (Cloudflare Workers)",
                            endpoint = endpoint,
                            operation = "Token Vending",
                            expectedFormat = "VendTokenResponse { token: String, expiresIn: Int, balanceAfter: Int, requestId: String }",
                            actualResponse = responseBody,
                            error = e
                        )
                    }
                }
                endpoint.contains("/meta") -> {
                    try {
                        json.decodeFromString<MetadataResponse>(responseBody)
                    } catch (e: Exception) {
                        throw createDetailedParseError(
                            service = "Business Engine (Cloudflare Workers)",
                            endpoint = endpoint,
                            operation = "Metadata Extraction",
                            expectedFormat = "MetadataResponse { url: String, title: String, description: String, author: String, duration: Int, ... }",
                            actualResponse = responseBody,
                            error = e
                        )
                    }
                }
                endpoint.contains("/ttt/transcribe") -> {
                    try {
                        json.decodeFromString<TranscriptionResponse>(responseBody)
                    } catch (e: Exception) {
                        throw createDetailedParseError(
                            service = "TTTranscribe Service (via Business Engine)",
                            endpoint = endpoint,
                            operation = "Submit Transcription Job",
                            expectedFormat = "TranscriptionResponse { jobId: String, status: String, estimatedTime: Int? (optional), url: String }",
                            actualResponse = responseBody,
                            error = e
                        )
                    }
                }
                endpoint.contains("/ttt/status") -> {
                    try {
                        json.decodeFromString<TranscriptionStatusResponse>(responseBody)
                    } catch (e: Exception) {
                        throw createDetailedParseError(
                            service = "TTTranscribe Service (via Business Engine)",
                            endpoint = endpoint,
                            operation = "Check Transcription Status",
                            expectedFormat = "TranscriptionStatusResponse { jobId: String, status: String, progress: Int, transcript: String?, ... }",
                            actualResponse = responseBody,
                            error = e
                        )
                    }
                }
                else -> responseBody
            }
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse response: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Create detailed parse error with service identification and expected vs actual
     */
    private fun createDetailedParseError(
        service: String,
        endpoint: String,
        operation: String,
        expectedFormat: String,
        actualResponse: String,
        error: Exception
    ): Exception {
        val errorMessage = buildString {
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("❌ API RESPONSE PARSING ERROR")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Service: $service")
            appendLine("Endpoint: $endpoint")
            appendLine("Operation: $operation")
            appendLine("")
            appendLine("Expected Response Format:")
            appendLine("  $expectedFormat")
            appendLine("")
            appendLine("Actual Response Received:")
            appendLine("  ${actualResponse.take(500)}${if (actualResponse.length > 500) "..." else ""}")
            appendLine("")
            appendLine("Parse Error:")
            appendLine("  ${error.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
        
        Log.e(TAG, errorMessage)
        return Exception(errorMessage)
    }
}
