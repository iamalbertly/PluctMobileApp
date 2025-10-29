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
            Log.d(TAG, "🚀 API Request: $method $fullUrl")
            Log.d(TAG, "📦 Payload: ${payload?.toString() ?: "null"}")
            Log.d(TAG, "🔑 Auth Token: ${authToken?.take(20)}...")
            
            val url = URL(fullUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            try {
                connection.requestMethod = method
                connection.connectTimeout = TIMEOUT_MS.toInt()
                connection.readTimeout = TIMEOUT_MS.toInt()
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("User-Agent", "PluctMobile/1.0")
                connection.setRequestProperty("Accept", "application/json")
                
                if (authToken != null) {
                    connection.setRequestProperty("Authorization", "Bearer $authToken")
                    Log.d(TAG, "🔐 Authorization header set")
                }
                
                if (payload != null && method in listOf("POST", "PUT", "PATCH")) {
                    connection.doOutput = true
                    val jsonPayload = json.encodeToString(JsonObject.serializer(), JsonObject(payload.mapValues { JsonPrimitive(it.value.toString()) }))
                    Log.d(TAG, "📤 Sending JSON payload: $jsonPayload")
                    OutputStreamWriter(connection.outputStream).use { it.write(jsonPayload) }
                }
                
                val responseCode = connection.responseCode
                Log.d(TAG, "📊 Response Code: $responseCode")
                
                val responseBody = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                }
                
                Log.d(TAG, "📥 Response Body: $responseBody")
                
                if (responseCode !in 200..299) {
                    Log.e(TAG, "❌ HTTP Error $responseCode: $responseBody")
                    throw Exception("HTTP $responseCode: $responseBody")
                }
                
                val result = parseResponse(endpoint, responseBody)
                Log.d(TAG, "✅ Request successful")
                result
            } catch (e: Exception) {
                Log.e(TAG, "❌ Request failed: ${e.message}", e)
                throw e
            } finally {
                connection.disconnect()
            }
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
