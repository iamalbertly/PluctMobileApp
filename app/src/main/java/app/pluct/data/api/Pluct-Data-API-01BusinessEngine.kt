package app.pluct.data.api

import app.pluct.core.error.ErrorCenter
import app.pluct.core.error.ErrorEnvelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Data-API-01BusinessEngine - Business Engine API client
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@Singleton
class PluctBusinessEngineClient @Inject constructor(
    private val errorCenter: ErrorCenter
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val baseUrl = "https://pluct-business-engine.romeo-lya2.workers.dev"
    
    suspend fun checkHealth(): Result<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/health")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                Result.success(HealthResponse(
                    status = json.optString("status", "unknown"),
                    uptimeSeconds = json.optLong("uptimeSeconds", 0),
                    version = json.optString("version", "unknown")
                ))
            } else {
                val error = ErrorEnvelope(
                    code = "HEALTH_CHECK_FAILED",
                    message = "Health check failed with status: ${response.code}",
                    details = mapOf("statusCode" to response.code.toString())
                )
                errorCenter.emitError(error)
                Result.failure(Exception("Health check failed"))
            }
        } catch (e: IOException) {
            val error = ErrorEnvelope(
                code = "NETWORK_ERROR",
                message = "Network error during health check",
                details = mapOf("exception" to (e.message ?: "Unknown error"))
            )
            errorCenter.emitError(error)
            Result.failure(e)
        }
    }
    
    suspend fun getCreditBalance(jwtToken: String): Result<CreditBalanceResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/v1/credits/balance")
                .get()
                .addHeader("Authorization", "Bearer $jwtToken")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            when (response.code) {
                200 -> {
                    val json = JSONObject(responseBody)
                    Result.success(CreditBalanceResponse(
                        userId = json.optString("userId", ""),
                        balance = json.optInt("balance", 0),
                        updatedAt = json.optString("updatedAt", "")
                    ))
                }
                401 -> {
                    val error = ErrorEnvelope(
                        code = "AUTHENTICATION_FAILED",
                        message = "Invalid or expired JWT token",
                        details = mapOf("statusCode" to "401")
                    )
                    errorCenter.emitError(error)
                    Result.failure(Exception("Authentication failed"))
                }
                else -> {
                    val error = ErrorEnvelope(
                        code = "BALANCE_CHECK_FAILED",
                        message = "Balance check failed with status: ${response.code}",
                        details = mapOf("statusCode" to response.code.toString())
                    )
                    errorCenter.emitError(error)
                    Result.failure(Exception("Balance check failed"))
                }
            }
        } catch (e: IOException) {
            val error = ErrorEnvelope(
                code = "NETWORK_ERROR",
                message = "Network error during balance check",
                details = mapOf("exception" to (e.message ?: "Unknown error"))
            )
            errorCenter.emitError(error)
            Result.failure(e)
        }
    }
    
    suspend fun vendToken(jwtToken: String, clientRequestId: String? = null): Result<TokenVendingResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                if (clientRequestId != null) {
                    put("clientRequestId", clientRequestId)
                }
            }.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/v1/vend-token")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $jwtToken")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            when (response.code) {
                200 -> {
                    val json = JSONObject(responseBody)
                    Result.success(TokenVendingResponse(
                        token = json.optString("token", ""),
                        scope = json.optString("scope", ""),
                        expiresAt = json.optString("expiresAt", ""),
                        balanceAfter = json.optInt("balanceAfter", 0),
                        requestId = json.optString("requestId", "")
                    ))
                }
                401 -> {
                    val error = ErrorEnvelope(
                        code = "AUTHENTICATION_FAILED",
                        message = "Invalid or expired JWT token",
                        details = mapOf("statusCode" to "401")
                    )
                    errorCenter.emitError(error)
                    Result.failure(Exception("Authentication failed"))
                }
                402 -> {
                    val error = ErrorEnvelope(
                        code = "INSUFFICIENT_CREDITS",
                        message = "Insufficient credits for token vending",
                        details = mapOf("statusCode" to "402")
                    )
                    errorCenter.emitError(error)
                    Result.failure(Exception("Insufficient credits"))
                }
                else -> {
                    val error = ErrorEnvelope(
                        code = "TOKEN_VENDING_FAILED",
                        message = "Token vending failed with status: ${response.code}",
                        details = mapOf("statusCode" to response.code.toString())
                    )
                    errorCenter.emitError(error)
                    Result.failure(Exception("Token vending failed"))
                }
            }
        } catch (e: IOException) {
            val error = ErrorEnvelope(
                code = "NETWORK_ERROR",
                message = "Network error during token vending",
                details = mapOf("exception" to (e.message ?: "Unknown error"))
            )
            errorCenter.emitError(error)
            Result.failure(e)
        }
    }
    
    suspend fun submitTranscription(shortLivedToken: String, url: String): Result<TranscriptionResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("url", url)
            }.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/ttt/transcribe")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $shortLivedToken")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            when (response.code) {
                200 -> {
                    val json = JSONObject(responseBody)
                    Result.success(TranscriptionResponse(
                        jobId = json.optString("jobId", ""),
                        status = json.optString("status", ""),
                        estimatedTime = json.optInt("estimatedTime", 0),
                        url = json.optString("url", "")
                    ))
                }
                401 -> {
                    val error = ErrorEnvelope(
                        code = "AUTHENTICATION_FAILED",
                        message = "Invalid or expired short-lived token",
                        details = mapOf("statusCode" to "401")
                    )
                    errorCenter.emitError(error)
                    Result.failure(Exception("Authentication failed"))
                }
                else -> {
                    val error = ErrorEnvelope(
                        code = "TRANSCRIPTION_SUBMISSION_FAILED",
                        message = "Transcription submission failed with status: ${response.code}",
                        details = mapOf("statusCode" to response.code.toString())
                    )
                    errorCenter.emitError(error)
                    Result.failure(Exception("Transcription submission failed"))
                }
            }
        } catch (e: IOException) {
            val error = ErrorEnvelope(
                code = "NETWORK_ERROR",
                message = "Network error during transcription submission",
                details = mapOf("exception" to (e.message ?: "Unknown error"))
            )
            errorCenter.emitError(error)
            Result.failure(e)
        }
    }
}

data class HealthResponse(
    val status: String,
    val uptimeSeconds: Long,
    val version: String
)

data class CreditBalanceResponse(
    val userId: String,
    val balance: Int,
    val updatedAt: String
)

data class TokenVendingResponse(
    val token: String,
    val scope: String,
    val expiresAt: String,
    val balanceAfter: Int,
    val requestId: String
)

data class TranscriptionResponse(
    val jobId: String,
    val status: String,
    val estimatedTime: Int,
    val url: String
)
