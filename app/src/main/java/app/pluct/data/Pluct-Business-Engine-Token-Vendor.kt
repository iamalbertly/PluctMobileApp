package app.pluct.data

import android.util.Log
import app.pluct.net.PluctNetworkHttp01Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.UUID

/**
 * Pluct-Business-Engine-Token-Vendor - Token vending for transcription
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluctBusinessEngineTokenVendor @Inject constructor() {
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(PluctNetworkHttp01Logger())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val baseUrl = "https://pluct-business-engine.romeo-lya2.workers.dev"
    
    data class TokenVendResult(
        val success: Boolean,
        val token: String?,
        val expiresAt: String?,
        val balanceAfter: Int?,
        val requestId: String?,
        val error: String? = null
    )
    
    suspend fun vendToken(userJwt: String, clientRequestId: String? = null): TokenVendResult = withContext(Dispatchers.IO) {
        try {
            val requestId = clientRequestId ?: "req_${System.currentTimeMillis()}"
            Log.i("PluctBusinessEngineTokenVendor", "🎯 Vending token with request ID: $requestId")
            
            val requestBody = JSONObject().apply {
                put("clientRequestId", requestId)
            }.toString()
            
            val request = Request.Builder()
                .url("$baseUrl/v1/vend-token")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $userJwt")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Client-Request-Id", requestId)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.i("PluctBusinessEngineTokenVendor", "🎯 Token vend response: $responseBody")
            
            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val token = json.optString("token", "")
                val expiresAt = json.optString("expiresAt", "")
                val balanceAfter = json.optInt("balanceAfter", 0)
                val returnedRequestId = json.optString("requestId", requestId)
                
                Log.i("PluctBusinessEngineTokenVendor", "✅ Token vended successfully")
                Log.i("PluctBusinessEngineTokenVendor", "✅ Balance after: $balanceAfter")
                Log.i("PluctBusinessEngineTokenVendor", "✅ Token expires: $expiresAt")
                
                TokenVendResult(
                    success = true,
                    token = token,
                    expiresAt = expiresAt,
                    balanceAfter = balanceAfter,
                    requestId = returnedRequestId
                )
            } else {
                val errorMessage = when (response.code) {
                    401 -> "Authentication failed - invalid JWT token"
                    402 -> "Insufficient credits"
                    403 -> "Missing required scope"
                    429 -> "Rate limit exceeded"
                    else -> "HTTP ${response.code}: $responseBody"
                }
                
                Log.e("PluctBusinessEngineTokenVendor", "❌ Token vend failed: $errorMessage")
                
                TokenVendResult(
                    success = false,
                    token = null,
                    expiresAt = null,
                    balanceAfter = null,
                    requestId = requestId,
                    error = errorMessage
                )
            }
        } catch (e: Exception) {
            Log.e("PluctBusinessEngineTokenVendor", "❌ Token vend exception: ${e.message}", e)
            TokenVendResult(
                success = false,
                token = null,
                expiresAt = null,
                balanceAfter = null,
                requestId = clientRequestId,
                error = e.message ?: "Unknown error"
            )
        }
    }
}
