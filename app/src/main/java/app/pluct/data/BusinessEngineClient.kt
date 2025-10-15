package app.pluct.data

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.UUID

/**
 * Centralized Business Engine Client
 * Single point of communication with the Business Engine backend
 */
class BusinessEngineClient(
    private val baseUrl: String = "https://pluct-business-engine.romeo-lya2.workers.dev"
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val correlationId = UUID.randomUUID().toString()
    
    companion object {
        private const val TAG = "BusinessEngineClient"
    }

    /**
     * Check Business Engine health
     */
    suspend fun health(): Health {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/health")
                .addHeader("User-Agent", "Pluct-Mobile-App/1.0")
                .addHeader("Accept", "application/json")
                .addHeader("X-Correlation-ID", correlationId)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val isHealthy = response.isSuccessful
            
            Log.d(TAG, "Health check: $isHealthy (${response.code})")
            response.close()
            
            Health(isHealthy)
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed: ${e.message}")
            throw EngineError.Network
        }
    }

    /**
     * Get user credit balance
     */
    suspend fun balance(): Balance {
        return try {
            val requestBody = JSONObject().apply {
                put("userId", "mobile")
            }
            
            val request = Request.Builder()
                .url("$baseUrl/user/balance")
                .addHeader("User-Agent", "Pluct-Mobile-App/1.0")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Correlation-ID", correlationId)
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            
            when (response.code) {
                200 -> {
                    val responseBody = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseBody)
                    val balance = jsonResponse.optInt("balance", 0)
                    response.close()
                    Balance(balance)
                }
                404 -> {
                    response.close()
                    Balance(0) // User doesn't exist, no credits
                }
                else -> {
                    response.close()
                    throw EngineError.Upstream(response.code, "Failed to get balance")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Balance check failed: ${e.message}")
            throw EngineError.Network
        }
    }

    /**
     * Vend authentication token
     */
    suspend fun vendToken(): VendResult {
        return try {
            val requestBody = JSONObject().apply {
                put("userId", "mobile")
            }
            
            val request = Request.Builder()
                .url("$baseUrl/vend-token")
                .addHeader("User-Agent", "Pluct-Mobile-App/1.0")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Correlation-ID", correlationId)
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            
            when (response.code) {
                200 -> {
                    val responseBody = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseBody)
                    val token = jsonResponse.getString("token")
                    val scope = jsonResponse.optString("scope", "default")
                    val expiresAt = jsonResponse.optLong("expiresAt", System.currentTimeMillis() + 3600000)
                    val balanceAfter = jsonResponse.optInt("balanceAfter", 0)
                    
                    response.close()
                    VendResult(token, scope, expiresAt, balanceAfter)
                }
                402 -> {
                    response.close()
                    throw EngineError.InsufficientCredits
                }
                429 -> {
                    response.close()
                    throw EngineError.RateLimited
                }
                else -> {
                    response.close()
                    throw EngineError.Upstream(response.code, "Token vending failed")
                }
            }
        } catch (e: Exception) {
            when (e) {
                is EngineError -> throw e
                else -> {
                    Log.e(TAG, "Token vending failed: ${e.message}")
                    throw EngineError.Network
                }
            }
        }
    }

    /**
     * Start transcription process
     */
    suspend fun transcribe(url: String, token: String): String {
        return try {
            val requestBody = JSONObject().apply {
                put("url", url)
            }
            
            val request = Request.Builder()
                .url("$baseUrl/ttt/transcribe")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("User-Agent", "Pluct-Mobile-App/1.0")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Correlation-ID", correlationId)
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            
            when (response.code) {
                200 -> {
                    val responseBody = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseBody)
                    val requestId = jsonResponse.getString("request_id")
                    response.close()
                    requestId
                }
                400 -> {
                    response.close()
                    throw EngineError.InvalidUrl
                }
                401 -> {
                    response.close()
                    throw EngineError.Auth
                }
                else -> {
                    response.close()
                    throw EngineError.Upstream(response.code, "Transcription failed")
                }
            }
        } catch (e: Exception) {
            when (e) {
                is EngineError -> throw e
                else -> {
                    Log.e(TAG, "Transcription failed: ${e.message}")
                    throw EngineError.Network
                }
            }
        }
    }

    /**
     * Poll transcription status
     */
    fun pollStatus(requestId: String): Flow<Status> = flow {
        var attempt = 0
        var backoffMs = 1000L
        
        while (true) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/ttt/status/$requestId")
                    .addHeader("User-Agent", "Pluct-Mobile-App/1.0")
                    .addHeader("Accept", "application/json")
                    .addHeader("X-Correlation-ID", correlationId)
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()
                
                when (response.code) {
                    200 -> {
                        val responseBody = response.body?.string() ?: ""
                        val jsonResponse = JSONObject(responseBody)
                        val phase = jsonResponse.getString("phase")
                        val percent = jsonResponse.optInt("percent", 0)
                        val note = jsonResponse.optString("note", "")
                        val text = jsonResponse.optString("text", null)
                        
                        val status = Status(phase, percent, note, text)
                        emit(status)
                        
                        if (phase == "COMPLETED" || phase == "FAILED") {
                            response.close()
                            break
                        }
                    }
                    else -> {
                        response.close()
                        throw EngineError.Upstream(response.code, "Status check failed")
                    }
                }
                
                response.close()
                delay(backoffMs)
                
                // Exponential backoff on errors
                attempt++
                if (attempt > 3) {
                    backoffMs = minOf(backoffMs * 2, 10000L)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Status polling failed: ${e.message}")
                delay(backoffMs)
                attempt++
                if (attempt > 5) {
                    emit(Status("FAILED", 0, "Polling failed", null))
                    break
                }
            }
        }
    }

    /**
     * Fetch video metadata
     */
    suspend fun fetchMetadata(url: String): Metadata {
        return try {
            val requestBody = JSONObject().apply {
                put("url", url)
            }
            
            val request = Request.Builder()
                .url("$baseUrl/metadata")
                .addHeader("User-Agent", "Pluct-Mobile-App/1.0")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Correlation-ID", correlationId)
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            
            when (response.code) {
                200 -> {
                    val responseBody = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseBody)
                    val title = jsonResponse.optString("title", "")
                    val description = jsonResponse.optString("description", "")
                    val thumbnail = jsonResponse.optString("thumbnail", "")
                    
                    response.close()
                    Metadata(title, description, thumbnail)
                }
                400 -> {
                    response.close()
                    throw EngineError.InvalidUrl
                }
                else -> {
                    response.close()
                    throw EngineError.Upstream(response.code, "Metadata fetch failed")
                }
            }
        } catch (e: Exception) {
            when (e) {
                is EngineError -> throw e
                else -> {
                    Log.e(TAG, "Metadata fetch failed: ${e.message}")
                    throw EngineError.Network
                }
            }
        }
    }
}

// Data classes
data class Health(val isHealthy: Boolean)
data class Balance(val balance: Int)
data class VendResult(
    val token: String,
    val scope: String,
    val expiresAt: Long,
    val balanceAfter: Int
)
data class Status(
    val phase: String,
    val percent: Int,
    val note: String,
    val text: String?
)
data class Metadata(
    val title: String,
    val description: String,
    val thumbnail: String
)

// Error model
sealed class EngineError : Throwable() {
    data object Network : EngineError()
    data object Auth : EngineError()
    data object InsufficientCredits : EngineError()
    data object RateLimited : EngineError()
    data class Upstream(val code: Int, val errorMessage: String?) : EngineError()
    data object InvalidUrl : EngineError()
    data class Unexpected(val errorCause: Throwable) : EngineError()
}
