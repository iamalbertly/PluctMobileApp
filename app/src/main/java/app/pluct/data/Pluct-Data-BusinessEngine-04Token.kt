package app.pluct.data

import android.util.Log
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import org.json.JSONObject
import app.pluct.core.log.PluctLogger

/**
 * Pluct-Data-BusinessEngine-04Token - Token vending functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation][CoreResponsibility]
 */
class PluctBusinessEngineToken(
    private val baseUrl: String,
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "PluctBusinessEngineToken"
    }

    /**
     * Vend a short-lived token for transcription
     */
    suspend fun vendToken(userJwt: String, clientRequestId: String = ""): BusinessEngineTokenResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🎫 Vending token...")
                val requestBody = JSONObject().apply {
                    if (clientRequestId.isNotEmpty()) {
                        put("clientRequestId", clientRequestId)
                    }
                }.toString()

                val request = Request.Builder()
                    .url("$baseUrl/v1/vend-token")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $userJwt")
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val token = json.optString("token", "")
                    val scope = json.optString("scope", "")
                    val expiresAt = json.optString("expiresAt", "")
                    val balanceAfter = json.optInt("balanceAfter", 0)
                    val requestId = json.optString("requestId", "")
                    
                    PluctLogger.logBusinessEngineCall("vend_token", true, 0, mapOf(
                        "scope" to scope,
                        "balanceAfter" to balanceAfter
                    ))

                    BusinessEngineTokenResult(
                        token = token,
                        scope = scope,
                        expiresAt = expiresAt,
                        balanceAfter = balanceAfter,
                        requestId = requestId,
                        responseTime = 0
                    )
                } else {
                    PluctLogger.logBusinessEngineCall("vend_token", false, 0, mapOf(
                        "error" to "HTTP ${response.code}",
                        "body" to responseBody
                    ))
                    
                    BusinessEngineTokenResult(
                        token = "",
                        scope = "",
                        expiresAt = "",
                        balanceAfter = 0,
                        requestId = "",
                        responseTime = 0,
                        error = "HTTP ${response.code}: $responseBody"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Token vending failed", e)
                PluctLogger.logError("Token vending failed: ${e.message}")
                
                BusinessEngineTokenResult(
                    token = "",
                    scope = "",
                    expiresAt = "",
                    balanceAfter = 0,
                    requestId = "",
                    responseTime = 0,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}

data class BusinessEngineTokenResult(
    val token: String,
    val scope: String,
    val expiresAt: String,
    val balanceAfter: Int,
    val requestId: String,
    val responseTime: Long,
    val error: String? = null
)
