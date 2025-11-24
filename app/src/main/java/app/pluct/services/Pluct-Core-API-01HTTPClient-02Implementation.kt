package app.pluct.services

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

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
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { it.readText() }
                }
                
                Log.d(TAG, "   Body: $responseBody")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                
                if (responseCode !in 200..299) {
                    // Parse error response for detailed information
                    val errorInfo = parseErrorResponse(responseCode, responseMessage, responseBody, endpoint)
                    Log.e(TAG, "❌ HTTP Error Details:")
                    Log.e(TAG, "   Status: $responseCode $responseMessage")
                    Log.e(TAG, "   Error Code: ${errorInfo.errorCode}")
                    Log.e(TAG, "   Message: ${errorInfo.errorMessage}")
                    Log.e(TAG, "   Upstream Status: ${errorInfo.upstreamStatus}")
                    Log.e(TAG, "   Upstream Response: ${errorInfo.upstreamResponse}")
                    Log.e(TAG, "   Full Body: $responseBody")
                    
                    throw Exception(errorInfo.toDetailedMessage())
                }
                
                val result = parseResponse(endpoint, responseBody)
                Log.d(TAG, "✅ Request successful [$requestId]")
                result
            } catch (e: Exception) {
                Log.e(TAG, "❌ Request failed [$requestId]: ${e.message}", e)
                throw e
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
                endpoint.contains("/credits/balance") -> json.decodeFromString<CreditBalanceResponse>(responseBody)
                endpoint.contains("/vend-token") -> json.decodeFromString<VendTokenResponse>(responseBody)
                endpoint.contains("/meta") -> json.decodeFromString<MetadataResponse>(responseBody)
                endpoint.contains("/ttt/transcribe") -> json.decodeFromString<TranscriptionResponse>(responseBody)
                endpoint.contains("/ttt/status") -> json.decodeFromString<TranscriptionStatusResponse>(responseBody)
                else -> responseBody
            }
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse response: ${e.message}")
            Result.failure(e)
        }
    }
}
