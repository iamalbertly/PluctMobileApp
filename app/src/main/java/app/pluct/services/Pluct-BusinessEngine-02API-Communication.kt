package app.pluct.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-BusinessEngine-02API-Communication - Complete API communication service
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation][CoreResponsibility]
 * Implements the complete communication journey to Business Engine
 */
@Singleton
class PluctBusinessEngineAPICommunication @Inject constructor() {
    
    companion object {
        private const val BASE_URL = "https://pluct-business-engine.romeo-lya2.workers.dev"
        private const val JWT_SECRET = "prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e"
        private const val TAG = "PluctBusinessEngineAPI"
    }
    
    data class HealthCheckResponse(
        val status: String,
        val uptimeSeconds: Long,
        val version: String,
        val connectivity: Map<String, String>,
        val configuration: Map<String, Boolean>
    )
    
    data class BalanceResponse(
        val userId: String,
        val balance: Int,
        val updatedAt: String
    )
    
    data class TokenVendingResponse(
        val token: String,
        val scope: String,
        val expiresAt: String,
        val balanceAfter: Int,
        val requestId: String?
    )
    
    data class MetadataResponse(
        val url: String,
        val title: String,
        val description: String,
        val author: String,
        val duration: Int,
        val thumbnail: String,
        val cached: Boolean,
        val timestamp: String
    )
    
    data class TranscriptionResponse(
        val jobId: String,
        val status: String,
        val estimatedTime: Int,
        val url: String
    )
    
    data class StatusResponse(
        val jobId: String,
        val status: String,
        val progress: Int,
        val transcript: String?,
        val confidence: Double?,
        val language: String?,
        val duration: Int?
    )
    
    /**
     * Stage 1: Health Check & System Status
     */
    suspend fun performHealthCheck(): Result<HealthCheckResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Stage 1: Performing health check...")
            
            val url = URL("$BASE_URL/health")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Health check response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val healthResponse = HealthCheckResponse(
                    status = json.optString("status", "unknown"),
                    uptimeSeconds = json.optLong("uptimeSeconds", 0),
                    version = json.optString("version", "unknown"),
                    connectivity = extractMap(json.optJSONObject("connectivity")),
                    configuration = extractBooleanMap(json.optJSONObject("configuration"))
                )
                
                Log.d(TAG, "✅ Health check successful: ${healthResponse.status}")
                Result.success(healthResponse)
            } else {
                val errorMessage = "Health check failed with status: $responseCode"
                Log.e(TAG, "❌ $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Health check failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stage 2: User Authentication & Balance Check (Modern)
     */
    suspend fun checkUserBalance(userId: String): Result<BalanceResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Stage 2: Checking user balance for $userId...")
            
            val jwtToken = generateUserJWT(userId)
            val url = URL("$BASE_URL/v1/credits/balance")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $jwtToken")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Balance check response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val balanceResponse = BalanceResponse(
                    userId = json.optString("userId", userId),
                    balance = json.optInt("balance", 0),
                    updatedAt = json.optString("updatedAt", "")
                )
                
                Log.d(TAG, "✅ Balance check successful: ${balanceResponse.balance} credits")
                Result.success(balanceResponse)
            } else {
                val errorMessage = "Balance check failed with status: $responseCode"
                Log.e(TAG, "❌ $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Balance check failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stage 3: Token Vending (Credit Deduction)
     */
    suspend fun vendToken(userId: String, clientRequestId: String? = null): Result<TokenVendingResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Stage 3: Vending token for $userId...")
            
            val jwtToken = generateUserJWT(userId)
            val url = URL("$BASE_URL/v1/vend-token")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $jwtToken")
            connection.setRequestProperty("Content-Type", "application/json")
            if (clientRequestId != null) {
                connection.setRequestProperty("X-Client-Request-Id", clientRequestId)
            }
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val requestBody = JSONObject().apply {
                if (clientRequestId != null) {
                    put("clientRequestId", clientRequestId)
                }
            }
            
            connection.outputStream.use { outputStream ->
                outputStream.write(requestBody.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Token vending response code: $responseCode")
            
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    
                    val tokenResponse = TokenVendingResponse(
                        token = json.optString("token", ""),
                        scope = json.optString("scope", ""),
                        expiresAt = json.optString("expiresAt", ""),
                        balanceAfter = json.optInt("balanceAfter", 0),
                        requestId = json.optString("requestId", null)
                    )
                    
                    Log.d(TAG, "✅ Token vending successful: ${tokenResponse.balanceAfter} credits remaining")
                    Result.success(tokenResponse)
                }
                HttpURLConnection.HTTP_PAYMENT_REQUIRED -> {
                    val errorMessage = "Insufficient credits (402)"
                    Log.e(TAG, "❌ $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    val errorMessage = "Authentication failed (401)"
                    Log.e(TAG, "❌ $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
                else -> {
                    val errorMessage = "Token vending failed with status: $responseCode"
                    Log.e(TAG, "❌ $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Token vending failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stage 4: Metadata Resolution
     */
    suspend fun fetchMetadata(tiktokUrl: String): Result<MetadataResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Stage 4: Fetching metadata for $tiktokUrl...")
            
            val encodedUrl = java.net.URLEncoder.encode(tiktokUrl, "UTF-8")
            val url = URL("$BASE_URL/meta?url=$encodedUrl")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Metadata fetch response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val metadataResponse = MetadataResponse(
                    url = json.optString("url", tiktokUrl),
                    title = json.optString("title", "Unknown Title"),
                    description = json.optString("description", ""),
                    author = json.optString("author", "Unknown Author"),
                    duration = json.optInt("duration", 0),
                    thumbnail = json.optString("thumbnail", ""),
                    cached = json.optBoolean("cached", false),
                    timestamp = json.optString("timestamp", "")
                )
                
                Log.d(TAG, "✅ Metadata fetch successful: ${metadataResponse.title}")
                Result.success(metadataResponse)
            } else {
                val errorMessage = "Metadata fetch failed with status: $responseCode"
                Log.e(TAG, "❌ $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Metadata fetch failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stage 5: Transcription Request
     */
    suspend fun startTranscription(shortLivedToken: String, tiktokUrl: String): Result<TranscriptionResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Stage 5: Starting transcription for $tiktokUrl...")
            
            val url = URL("$BASE_URL/ttt/transcribe")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $shortLivedToken")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            val requestBody = JSONObject().apply {
                put("url", tiktokUrl)
            }
            
            connection.outputStream.use { outputStream ->
                outputStream.write(requestBody.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Transcription start response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val transcriptionResponse = TranscriptionResponse(
                    jobId = json.optString("jobId", ""),
                    status = json.optString("status", "unknown"),
                    estimatedTime = json.optInt("estimatedTime", 0),
                    url = json.optString("url", tiktokUrl)
                )
                
                Log.d(TAG, "✅ Transcription started: Job ${transcriptionResponse.jobId}")
                Result.success(transcriptionResponse)
            } else {
                val errorMessage = "Transcription start failed with status: $responseCode"
                Log.e(TAG, "❌ $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Transcription start failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stage 6: Status Check
     */
    suspend fun checkTranscriptionStatus(shortLivedToken: String, jobId: String): Result<StatusResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Stage 6: Checking transcription status for job $jobId...")
            
            val url = URL("$BASE_URL/ttt/status/$jobId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $shortLivedToken")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Status check response code: $responseCode")
            
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    
                    val statusResponse = StatusResponse(
                        jobId = json.optString("jobId", jobId),
                        status = json.optString("status", "unknown"),
                        progress = json.optInt("progress", 0),
                        transcript = json.optString("transcript", null),
                        confidence = json.optDouble("confidence", 0.0),
                        language = json.optString("language", null),
                        duration = if (json.has("duration")) json.optInt("duration", 0) else null
                    )
                    
                    Log.d(TAG, "✅ Status check successful: ${statusResponse.status} (${statusResponse.progress}%)")
                    Result.success(statusResponse)
                }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    val errorMessage = "Job not found (404)"
                    Log.e(TAG, "❌ $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
                else -> {
                    val errorMessage = "Status check failed with status: $responseCode"
                    Log.e(TAG, "❌ $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Status check failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate JWT token for authentication
     */
    private fun generateUserJWT(userId: String): String {
        val now = System.currentTimeMillis() / 1000
        val payload = mapOf(
            "sub" to userId,
            "scope" to "ttt:transcribe",
            "iat" to now,
            "exp" to (now + 900) // 15 minutes max
        )
        
        // Simple JWT implementation (in production, use a proper JWT library)
        val header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}"
        val payloadJson = JSONObject(payload).toString()
        
        val headerEncoded = android.util.Base64.encodeToString(header.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)
        val payloadEncoded = android.util.Base64.encodeToString(payloadJson.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)
        
        val signature = android.util.Base64.encodeToString(
            javax.crypto.Mac.getInstance("HmacSHA256").apply {
                init(javax.crypto.spec.SecretKeySpec(JWT_SECRET.toByteArray(), "HmacSHA256"))
            }.doFinal("$headerEncoded.$payloadEncoded".toByteArray()),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING
        )
        
        return "$headerEncoded.$payloadEncoded.$signature"
    }
    
    private fun extractMap(jsonObject: org.json.JSONObject?): Map<String, String> {
        if (jsonObject == null) return emptyMap()
        val map = mutableMapOf<String, String>()
        jsonObject.keys().forEach { key ->
            map[key] = jsonObject.optString(key, "")
        }
        return map
    }
    
    private fun extractBooleanMap(jsonObject: org.json.JSONObject?): Map<String, Boolean> {
        if (jsonObject == null) return emptyMap()
        val map = mutableMapOf<String, Boolean>()
        jsonObject.keys().forEach { key ->
            map[key] = jsonObject.optBoolean(key, false)
        }
        return map
    }
}
