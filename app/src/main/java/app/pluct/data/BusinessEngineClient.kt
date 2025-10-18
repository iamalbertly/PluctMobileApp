package app.pluct.data

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import app.pluct.net.HttpTelemetryInterceptor

/**
 * Centralized Business Engine Client
 * Single point of communication with the Business Engine backend
 */
class BusinessEngineClient(
    private val baseUrl: String
) {
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(HttpTelemetryInterceptor()) // <— add first to capture request bodies
        .retryOnConnectionFailure(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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
     * Get user credit balance (requires user JWT)
     */
    suspend fun balance(userJwt: String): Balance {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Checking balance with userJwt: ${userJwt.take(20)}...")
                
                val request = Request.Builder()
                    .url("$baseUrl/v1/credits/balance")
                    .addHeader("User-Agent", "Pluct-Mobile-App/1.0")
                    .addHeader("Accept", "application/json")
                    .addHeader("X-Correlation-ID", correlationId)
                    .addHeader("Authorization", "Bearer $userJwt")
                    .get()
                    .build()

                // Enhanced HTTP telemetry logging
                Log.i("PLUCT_HTTP", """{"event":"request","reqId":"balance-${System.currentTimeMillis()}","url":"$baseUrl/v1/credits/balance","method":"GET","headers":{"Authorization":"Bearer ${userJwt.take(20)}..."}}""")
                Log.d(TAG, "Making balance request to: $baseUrl/v1/credits/balance")

                val response = httpClient.newCall(request).execute()
                Log.d(TAG, "Balance response code: ${response.code}")
            
                when (response.code) {
                    200 -> {
                        val responseBody = response.body?.string() ?: ""
                        Log.d(TAG, "Balance response body: $responseBody")
                        
                        // Enhanced HTTP telemetry logging for response
                        Log.i("PLUCT_HTTP", """{"event":"response","reqId":"balance-${System.currentTimeMillis()}","url":"$baseUrl/v1/credits/balance","code":200,"body":"$responseBody"}""")
                        
                        val jsonResponse = JSONObject(responseBody)
                        val balance = jsonResponse.optInt("balance", 0)
                        response.close()
                        Log.d(TAG, "Balance retrieved successfully: $balance")
                        Balance(balance)
                    }
                    401 -> {
                        val responseBody = response.body?.string() ?: ""
                        Log.w(TAG, "Balance check failed with 401 (unauthorized): $responseBody")
                        response.close()
                        Balance(0) // Return 0 for unauthorized
                    }
                    404 -> {
                        response.close()
                        Log.d(TAG, "User doesn't exist, returning 0 balance")
                        Balance(0) // User doesn't exist, no credits
                    }
                    500 -> {
                        val responseBody = response.body?.string() ?: ""
                        Log.w(TAG, "Balance check failed with 500 (server error): $responseBody")
                        response.close()
                        // Check if it's a business logic error (BALANCE_CHECK_FAILED)
                        if (responseBody.contains("BALANCE_CHECK_FAILED")) {
                            Log.d(TAG, "User has no credits set up (expected for new users)")
                            Balance(0) // Return 0 for users without credits
                        } else {
                            Balance(0) // Return 0 for other server errors
                        }
                    }
                    else -> {
                        val responseBody = response.body?.string() ?: ""
                        Log.w(TAG, "Balance check failed with code ${response.code}: $responseBody")
                        response.close()
                        Balance(0) // Return default balance instead of throwing error
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Balance check failed: ${e.message}")
                // Return default balance instead of throwing error to prevent app crash
                Balance(0)
            }
        }
    }

    /**
     * Vend short-lived authentication token (requires user JWT)
     * Adds X-Client-Request-Id for idempotency.
     */
    suspend fun vendShortToken(userJwt: String, clientRequestId: String = UUID.randomUUID().toString()): VendResult {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = JSONObject().apply {
                    put("clientRequestId", clientRequestId)
                    put("userId", "mobile") // Include user ID for proper credit tracking
                    put("userJwt", userJwt) // Include JWT in body for Business Engine processing
                }
                
                Log.d(TAG, "Vending token with clientRequestId: $clientRequestId, userJwt: ${userJwt.take(20)}...")
                
                val request = Request.Builder()
                    .url("$baseUrl/v1/vend-token")
                    .addHeader("User-Agent", "Pluct-Mobile-App/1.0")
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Correlation-ID", correlationId)
                    .addHeader("X-Client-Request-Id", clientRequestId)
                    .addHeader("Authorization", "Bearer $userJwt")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                // Enhanced HTTP telemetry logging
                Log.i("PLUCT_HTTP", """{"event":"request","reqId":"$clientRequestId","url":"$baseUrl/v1/vend-token","method":"POST","headers":{"Authorization":"Bearer ${userJwt.take(20)}...","Content-Type":"application/json"},"body":"${requestBody.toString()}"}""")
                Log.d(TAG, "Making vend-token request to: $baseUrl/v1/vend-token")
                Log.i("TTT", "stage=VENDING_TOKEN url=- reqId=$clientRequestId msg=requesting")
                
                val response = httpClient.newCall(request).execute()
                Log.d(TAG, "Vend-token response code: ${response.code}")
                Log.i("TTT", "stage=VENDING_TOKEN url=- reqId=$clientRequestId msg=response code=${response.code}")
            
            when (response.code) {
                200 -> {
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Vend-token response body: $responseBody")
                    
                    // Enhanced HTTP telemetry logging for response
                    Log.i("PLUCT_HTTP", """{"event":"response","reqId":"$clientRequestId","url":"$baseUrl/v1/vend-token","code":200,"body":"$responseBody"}""")
                    Log.i("TTT", "stage=VENDING_TOKEN url=- reqId=$clientRequestId msg=success")
                    
                    val jsonResponse = JSONObject(responseBody)
                    val token = jsonResponse.getString("token")
                    val scope = jsonResponse.optString("scope", "default")
                    val expiresAt = jsonResponse.optLong("expiresAt", System.currentTimeMillis() + 3600000)
                    val balanceAfter = jsonResponse.optInt("balanceAfter", 0)
                    
                    Log.d(TAG, "Token vended successfully - balanceAfter: $balanceAfter, token: ${token.take(20)}...")
                    response.close()
                    VendResult(token, scope, expiresAt, balanceAfter)
                }
                401 -> {
                    response.close()
                    throw EngineError.Auth
                }
                402 -> {
                    response.close()
                    throw EngineError.InsufficientCredits
                }
                403 -> {
                    response.close()
                    throw EngineError.Upstream(403, "Forbidden/missing scope")
                }
                409 -> {
                    response.close()
                    // idempotent request in progress: small delay and retry same requestId
                    delay(800)
                    vendShortToken(userJwt, clientRequestId)
                }
                429 -> {
                    response.close()
                    delay(1500)
                    vendShortToken(userJwt, clientRequestId)
                }
                500 -> {
                    val responseBody = response.body?.string() ?: ""
                    response.close()
                    Log.w(TAG, "Vend-token failed with 500: $responseBody")
                    // Check if it's a business logic error (TOKEN_GENERATION_FAILED)
                    if (responseBody.contains("TOKEN_GENERATION_FAILED")) {
                        Log.d(TAG, "Token generation failed (likely insufficient credits)")
                        throw EngineError.InsufficientCredits
                    } else {
                        throw EngineError.Upstream(500, "Token vending failed: $responseBody")
                    }
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
    }

    /**
     * Deprecated: use vendShortToken(userJwt)
     */
    @Deprecated("Use vendShortToken(userJwt)")
    suspend fun vendToken(): VendResult {
        throw EngineError.Upstream(401, "vendToken() deprecated - supply user JWT via vendShortToken(userJwt)")
    }

    /**
     * Start transcription process
     */
    suspend fun transcribe(url: String, token: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = JSONObject().apply {
                    put("url", url)
                    put("tier", "QUICK_SCAN") // Add tier for proper processing
                }
                
                Log.d(TAG, "Starting transcription for URL: $url with token: ${token.take(20)}...")
                
                val request = Request.Builder()
                    .url("$baseUrl/ttt/transcribe")
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("User-Agent", "Pluct-Mobile-App/1.0")
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Correlation-ID", correlationId)
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                Log.d(TAG, "Making transcribe request to: $baseUrl/ttt/transcribe")
                val response = httpClient.newCall(request).execute()
                Log.d(TAG, "Transcribe response code: ${response.code}")
            
            when (response.code) {
                200 -> {
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Transcribe response body: $responseBody")
                    val jsonResponse = JSONObject(responseBody)
                    val requestId = jsonResponse.getString("request_id")
                    Log.d(TAG, "Transcription started with requestId: $requestId")
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
                429 -> {
                    response.close()
                    delay(1500)
                    transcribe(url, token)
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
                        Log.d(TAG, "Status poll response for $requestId: $responseBody")
                        val jsonResponse = JSONObject(responseBody)
                        val phase = jsonResponse.getString("phase")
                        val percent = jsonResponse.optInt("percent", 0)
                        val note = jsonResponse.optString("note", "")
                        val text = jsonResponse.optString("text", null)
                        
                        Log.d(TAG, "Status: phase=$phase, percent=$percent, note=$note, hasText=${text != null}")
                        
                        val status = Status(phase, percent, note, text)
                        emit(status)
                        
                        if (phase == "COMPLETED" || phase == "FAILED") {
                            Log.d(TAG, "Transcription $phase for requestId: $requestId")
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
            val httpUrl: HttpUrl = "$baseUrl/meta".toHttpUrl()
                .newBuilder()
                .addQueryParameter("url", url)
                .build()

            val request = Request.Builder()
                .url(httpUrl)
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
                405 -> {
                    response.close()
                    throw EngineError.Upstream(405, "Wrong method")
                }
                500 -> {
                    response.close()
                    Log.w(TAG, "Business Engine metadata service error (500) - fetchTikTokMetadata may be undefined")
                    throw EngineError.Upstream(500, "Business Engine metadata service error")
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
