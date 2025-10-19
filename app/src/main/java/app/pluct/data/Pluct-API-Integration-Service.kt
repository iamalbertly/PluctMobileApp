package app.pluct.data

import android.util.Log
import app.pluct.auth.PluctAuthJWTGenerator
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
 * Pluct-API-Integration-Service - Complete API integration with Business Engine and TTTranscribe
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluctAPIIntegrationService @Inject constructor() {
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(PluctNetworkHttp01Logger())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val baseUrl = "https://pluct-business-engine.romeo-lya2.workers.dev"
    
    data class APIResult<T>(
        val success: Boolean,
        val data: T?,
        val error: String? = null,
        val statusCode: Int? = null
    )
    
    data class HealthStatus(
        val isHealthy: Boolean,
        val status: String,
        val uptimeSeconds: Long?,
        val version: String?,
        val connectivity: Map<String, String>?
    )
    
    data class CreditBalance(
        val balance: Int,
        val userId: String,
        val updatedAt: String?
    )
    
    data class TokenVendResult(
        val token: String,
        val expiresAt: String,
        val balanceAfter: Int,
        val requestId: String
    )
    
    data class TranscriptionJob(
        val jobId: String,
        val status: String,
        val estimatedTime: Int?,
        val url: String
    )
    
    data class TranscriptionResult(
        val jobId: String,
        val status: String,
        val transcript: String?,
        val confidence: Double?,
        val language: String?,
        val duration: Int?
    )
    
    /**
     * Step 1: Health Check - Verify Business Engine is operational
     */
    suspend fun checkHealth(): APIResult<HealthStatus> = withContext(Dispatchers.IO) {
        try {
            Log.i("PluctAPIIntegrationService", "🎯 STEP 1: Checking Business Engine health...")
            
            val request = Request.Builder()
                .url("$baseUrl/health")
                .get()
                .addHeader("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.i("PluctAPIIntegrationService", "🎯 Health API Response: $responseBody")
            
            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val status = json.optString("status", "unknown")
                val uptimeSeconds = json.optLong("uptimeSeconds", 0)
                val version = json.optString("version", "unknown")
                
                val connectivity = json.optJSONObject("connectivity")?.let { conn ->
                    mapOf(
                        "d1" to conn.optString("d1", "unknown"),
                        "kv" to conn.optString("kv", "unknown"),
                        "ttt" to conn.optString("ttt", "unknown"),
                        "circuitBreaker" to conn.optString("circuitBreaker", "unknown")
                    )
                }
                
                val isHealthy = status == "ok" && 
                    connectivity?.get("ttt") == "healthy" &&
                    connectivity?.get("circuitBreaker") == "closed"
                
                Log.i("PluctAPIIntegrationService", "✅ Health check result: $status")
                Log.i("PluctAPIIntegrationService", "✅ TTT Status: ${connectivity?.get("ttt")}")
                Log.i("PluctAPIIntegrationService", "✅ Circuit Breaker: ${connectivity?.get("circuitBreaker")}")
                
                APIResult(
                    success = true,
                    data = HealthStatus(
                        isHealthy = isHealthy,
                        status = status,
                        uptimeSeconds = uptimeSeconds,
                        version = version,
                        connectivity = connectivity
                    )
                )
            } else {
                Log.w("PluctAPIIntegrationService", "⚠️ Health check failed: ${response.code}")
                APIResult(
                    success = false,
                    data = null,
                    error = "HTTP ${response.code}: $responseBody",
                    statusCode = response.code
                )
            }
        } catch (e: Exception) {
            Log.e("PluctAPIIntegrationService", "❌ Health check failed: ${e.message}", e)
            APIResult(
                success = false,
                data = null,
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Step 2: Generate JWT Token for authentication
     */
    fun generateJWT(): String {
        Log.i("PluctAPIIntegrationService", "🎯 STEP 2: Generating JWT token...")
        val jwt = PluctAuthJWTGenerator.generateUserJWT("mobile")
        Log.i("PluctAPIIntegrationService", "✅ JWT token generated")
        return jwt
    }
    
    /**
     * Step 3: Check Credit Balance
     */
    suspend fun getCreditBalance(userJwt: String): APIResult<CreditBalance> = withContext(Dispatchers.IO) {
        try {
            Log.i("PluctAPIIntegrationService", "🎯 STEP 3: Checking credit balance...")
            
            val request = Request.Builder()
                .url("$baseUrl/v1/credits/balance")
                .get()
                .addHeader("Authorization", "Bearer $userJwt")
                .addHeader("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.i("PluctAPIIntegrationService", "🎯 Balance API Response: $responseBody")
            
            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val balance = json.optInt("balance", 0)
                val userId = json.optString("userId", "mobile")
                val updatedAt = json.optString("updatedAt", "")
                
                Log.i("PluctAPIIntegrationService", "✅ Balance retrieved: $balance")
                
                APIResult(
                    success = true,
                    data = CreditBalance(
                        balance = balance,
                        userId = userId,
                        updatedAt = updatedAt
                    )
                )
            } else {
                Log.w("PluctAPIIntegrationService", "⚠️ Balance API failed: ${response.code}")
                APIResult(
                    success = false,
                    data = null,
                    error = "HTTP ${response.code}: $responseBody",
                    statusCode = response.code
                )
            }
        } catch (e: Exception) {
            Log.e("PluctAPIIntegrationService", "❌ Balance check failed: ${e.message}", e)
            APIResult(
                success = false,
                data = null,
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Step 4: Vend Token for transcription
     */
    suspend fun vendToken(userJwt: String, clientRequestId: String? = null): APIResult<TokenVendResult> = withContext(Dispatchers.IO) {
        try {
            val requestId = clientRequestId ?: "req_${System.currentTimeMillis()}"
            Log.i("PluctAPIIntegrationService", "🎯 STEP 4: Vending token with request ID: $requestId")
            
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
            
            Log.i("PluctAPIIntegrationService", "🎯 Token vend response: $responseBody")
            
            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val token = json.optString("token", "")
                val expiresAt = json.optString("expiresAt", "")
                val balanceAfter = json.optInt("balanceAfter", 0)
                val returnedRequestId = json.optString("requestId", requestId)
                
                Log.i("PluctAPIIntegrationService", "✅ Token vended successfully")
                Log.i("PluctAPIIntegrationService", "✅ Balance after: $balanceAfter")
                Log.i("PluctAPIIntegrationService", "✅ Token expires: $expiresAt")
                
                APIResult(
                    success = true,
                    data = TokenVendResult(
                        token = token,
                        expiresAt = expiresAt,
                        balanceAfter = balanceAfter,
                        requestId = returnedRequestId
                    )
                )
            } else {
                val errorMessage = when (response.code) {
                    401 -> "Authentication failed - invalid JWT token"
                    402 -> "Insufficient credits"
                    403 -> "Missing required scope"
                    429 -> "Rate limit exceeded"
                    else -> "HTTP ${response.code}: $responseBody"
                }
                
                Log.e("PluctAPIIntegrationService", "❌ Token vend failed: $errorMessage")
                
                APIResult(
                    success = false,
                    data = null,
                    error = errorMessage,
                    statusCode = response.code
                )
            }
        } catch (e: Exception) {
            Log.e("PluctAPIIntegrationService", "❌ Token vend exception: ${e.message}", e)
            APIResult(
                success = false,
                data = null,
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Step 5: Start Transcription Job
     */
    suspend fun startTranscription(shortLivedToken: String, videoUrl: String): APIResult<TranscriptionJob> = withContext(Dispatchers.IO) {
        try {
            Log.i("PluctAPIIntegrationService", "🎯 STEP 5: Starting transcription for: $videoUrl")
            
            val requestBody = JSONObject().apply {
                put("url", videoUrl)
            }.toString()
            
            val request = Request.Builder()
                .url("$baseUrl/ttt/transcribe")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $shortLivedToken")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.i("PluctAPIIntegrationService", "🎯 Transcription start response: $responseBody")
            
            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val jobId = json.optString("jobId", "")
                val status = json.optString("status", "processing")
                val estimatedTime = json.optInt("estimatedTime", 30)
                
                Log.i("PluctAPIIntegrationService", "✅ Transcription started - Job ID: $jobId")
                Log.i("PluctAPIIntegrationService", "✅ Status: $status, Estimated time: ${estimatedTime}s")
                
                APIResult(
                    success = true,
                    data = TranscriptionJob(
                        jobId = jobId,
                        status = status,
                        estimatedTime = estimatedTime,
                        url = videoUrl
                    )
                )
            } else {
                val errorMessage = "HTTP ${response.code}: $responseBody"
                Log.e("PluctAPIIntegrationService", "❌ Transcription start failed: $errorMessage")
                
                APIResult(
                    success = false,
                    data = null,
                    error = errorMessage,
                    statusCode = response.code
                )
            }
        } catch (e: Exception) {
            Log.e("PluctAPIIntegrationService", "❌ Transcription start exception: ${e.message}", e)
            APIResult(
                success = false,
                data = null,
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Step 6: Check Transcription Status
     */
    suspend fun checkTranscriptionStatus(shortLivedToken: String, jobId: String): APIResult<TranscriptionResult> = withContext(Dispatchers.IO) {
        try {
            Log.i("PluctAPIIntegrationService", "🎯 STEP 6: Checking transcription status for job: $jobId")
            
            val request = Request.Builder()
                .url("$baseUrl/ttt/status/$jobId")
                .get()
                .addHeader("Authorization", "Bearer $shortLivedToken")
                .addHeader("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.i("PluctAPIIntegrationService", "🎯 Transcription status response: $responseBody")
            
            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val status = json.optString("status", "unknown")
                val transcript = json.optString("transcript", "")
                val confidence = json.optDouble("confidence", 0.0)
                val language = json.optString("language", "en")
                val duration = json.optInt("duration", 0)
                
                Log.i("PluctAPIIntegrationService", "✅ Status check - Status: $status")
                if (status == "completed" && transcript.isNotEmpty()) {
                    Log.i("PluctAPIIntegrationService", "✅ Transcription completed successfully")
                    Log.i("PluctAPIIntegrationService", "✅ Transcript length: ${transcript.length} chars")
                    Log.i("PluctAPIIntegrationService", "✅ Confidence: $confidence, Language: $language")
                }
                
                APIResult(
                    success = true,
                    data = TranscriptionResult(
                        jobId = jobId,
                        status = status,
                        transcript = transcript.takeIf { it.isNotEmpty() },
                        confidence = confidence.takeIf { it > 0.0 },
                        language = language.takeIf { it.isNotEmpty() },
                        duration = duration.takeIf { it > 0 }
                    )
                )
            } else {
                val errorMessage = "HTTP ${response.code}: $responseBody"
                Log.e("PluctAPIIntegrationService", "❌ Status check failed: $errorMessage")
                
                APIResult(
                    success = false,
                    data = null,
                    error = errorMessage,
                    statusCode = response.code
                )
            }
        } catch (e: Exception) {
            Log.e("PluctAPIIntegrationService", "❌ Status check exception: ${e.message}", e)
            APIResult(
                success = false,
                data = null,
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Complete End-to-End Flow
     */
    suspend fun processVideoEndToEnd(videoUrl: String, clientRequestId: String? = null): APIResult<TranscriptionResult> = withContext(Dispatchers.IO) {
        try {
            Log.i("PluctAPIIntegrationService", "🎯 STARTING COMPLETE END-TO-END FLOW")
            
            // Step 1: Health Check
            val healthResult = checkHealth()
            if (!healthResult.success || !healthResult.data?.isHealthy!!) {
                return@withContext APIResult(false, null, "Health check failed: ${healthResult.error}")
            }
            
            // Step 2: Generate JWT
            val userJwt = generateJWT()
            
            // Step 3: Check Balance
            val balanceResult = getCreditBalance(userJwt)
            if (!balanceResult.success || balanceResult.data?.balance!! <= 0) {
                return@withContext APIResult(false, null, "Insufficient credits: ${balanceResult.data?.balance}")
            }
            
            // Step 4: Vend Token
            val tokenResult = vendToken(userJwt, clientRequestId)
            if (!tokenResult.success) {
                return@withContext APIResult(false, null, "Token vending failed: ${tokenResult.error}")
            }
            
            // Step 5: Start Transcription
            val transcriptionResult = startTranscription(tokenResult.data!!.token, videoUrl)
            if (!transcriptionResult.success) {
                return@withContext APIResult(false, null, "Transcription start failed: ${transcriptionResult.error}")
            }
            
            // Step 6: Poll for Completion
            var attempts = 0
            val maxAttempts = 30 // 5 minutes max
            
            while (attempts < maxAttempts) {
                kotlinx.coroutines.delay(10000) // Wait 10 seconds between checks
                attempts++
                
                val statusResult = checkTranscriptionStatus(tokenResult.data!!.token, transcriptionResult.data!!.jobId)
                
                if (statusResult.success && statusResult.data?.status == "completed") {
                    Log.i("PluctAPIIntegrationService", "🎯 COMPLETE END-TO-END FLOW SUCCESSFUL")
                    return@withContext statusResult
                } else if (statusResult.data?.status == "failed") {
                    return@withContext APIResult(false, null, "Transcription failed: ${statusResult.error}")
                }
            }
            
            APIResult(false, null, "Transcription timeout after $maxAttempts attempts")
            
        } catch (e: Exception) {
            Log.e("PluctAPIIntegrationService", "❌ End-to-end flow failed: ${e.message}", e)
            APIResult(false, null, e.message ?: "Unknown error")
        }
    }
}
