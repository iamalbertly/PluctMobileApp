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
import java.util.concurrent.TimeUnit
import java.util.UUID
import app.pluct.core.log.PluctLogger
import app.pluct.core.retry.PluctRetryEngine
import app.pluct.core.error.ErrorEnvelope
import app.pluct.core.error.ErrorSeverity

/**
 * Pluct-Data-BusinessEngine-01UnifiedClient-New - Simplified unified client
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation][CoreResponsibility]
 * Uses composition with specialized components
 */
class PluctBusinessEngineUnifiedClientNew(
    private val baseUrl: String
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val healthClient = PluctBusinessEngineHealth(baseUrl, httpClient)
    private val balanceClient = PluctBusinessEngineBalance(baseUrl, httpClient)
    private val tokenClient = PluctBusinessEngineToken(baseUrl, httpClient)
    private val transcriptionClient = PluctBusinessEngineTranscription(baseUrl, httpClient)

    companion object {
        private const val TAG = "PluctBusinessEngineUnifiedClientNew"
    }

    /**
     * Check Business Engine health
     */
    suspend fun health() = healthClient.health()

    /**
     * Get user credit balance
     */
    suspend fun getCreditBalance(userJwt: String) = balanceClient.getCreditBalance(userJwt)

    /**
     * Vend a short-lived token
     */
    suspend fun vendToken(userJwt: String, clientRequestId: String = "") = 
        tokenClient.vendToken(userJwt, clientRequestId)

    /**
     * Start transcription
     */
    suspend fun transcribe(videoUrl: String, token: String) = 
        transcriptionClient.transcribe(videoUrl, token)

    /**
     * Check transcription status
     */
    suspend fun checkTranscriptionStatus(jobId: String, token: String) = 
        transcriptionClient.checkStatus(jobId, token)

    /**
     * Complete end-to-end video processing flow
     */
    suspend fun processVideoEndToEnd(
        videoUrl: String,
        userJwt: String,
        clientRequestId: String = UUID.randomUUID().toString()
    ): BusinessEngineTranscriptionResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🎯 STARTING END-TO-END FLOW for URL: $videoUrl")

                // Step 1: Health Check
                Log.d(TAG, "Step 1: Health Check")
                val healthResult = health()
                if (!healthResult.isHealthy) {
                    throw BusinessEngineException("Business Engine is not healthy: ${healthResult.status}")
                }
                Log.d(TAG, "✅ Health check passed")

                // Step 2: Check Balance
                Log.d(TAG, "Step 2: Check Balance")
                val balanceResult = getCreditBalance(userJwt)
                if (balanceResult.balance <= 0) {
                    throw BusinessEngineException("Insufficient credits: ${balanceResult.balance}")
                }
                Log.d(TAG, "✅ Balance check passed: ${balanceResult.balance} credits")

                // Step 3: Vend Token
                Log.d(TAG, "Step 3: Vend Token")
                val tokenResult = vendToken(userJwt, clientRequestId)
                Log.d(TAG, "✅ Token vended successfully")

                // Step 4: Start Transcription
                Log.d(TAG, "Step 4: Start Transcription")
                val transcriptionResult = transcribe(videoUrl, tokenResult.token)
                Log.d(TAG, "✅ Transcription started with jobId: ${transcriptionResult.jobId}")

                // Step 5: Poll for Completion
                Log.d(TAG, "Step 5: Poll for Completion")
                var attempts = 0
                val maxAttempts = 30 // 5 minutes max
                val pollInterval = 10000L // 10 seconds between checks

                while (attempts < maxAttempts) {
                    kotlinx.coroutines.delay(pollInterval)
                    attempts++

                    Log.d(TAG, "Polling attempt $attempts/$maxAttempts")
                    val statusResult = checkTranscriptionStatus(transcriptionResult.jobId, tokenResult.token)

                    if (statusResult.status == "completed") {
                        Log.d(TAG, "🎉 END-TO-END FLOW SUCCESSFUL")
                        return@withContext BusinessEngineTranscriptionResult(
                            jobId = statusResult.jobId,
                            status = statusResult.status,
                            transcript = statusResult.transcript,
                            confidence = statusResult.confidence,
                            language = statusResult.language,
                            duration = statusResult.duration,
                            responseTime = statusResult.responseTime
                        )
                    } else if (statusResult.status == "failed") {
                        throw BusinessEngineException("Transcription failed: ${statusResult.transcript}")
                    }
                }

                throw BusinessEngineException("Transcription timeout after $attempts attempts")

            } catch (e: Exception) {
                Log.e(TAG, "❌ End-to-end flow failed: ${e.message}", e)
                throw BusinessEngineException("End-to-end processing failed: ${e.message}")
            }
        }
    }

    /**
     * Generate user JWT token for authentication
     */
    fun generateUserJWT(): String {
        val now = System.currentTimeMillis() / 1000
        val payload = mapOf(
            "sub" to "mobile",
            "scope" to "ttt:transcribe",
            "iat" to now,
            "exp" to (now + 900) // 15 minutes max
        )

        // Use the same secret as the server
        val secret = "prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e"
        return com.auth0.jwt.JWT.create()
            .withPayload(payload)
            .withExpiresAt(java.util.Date(now + 900 * 1000))
            .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256(secret))
    }
}

// Data classes for Business Engine responses
data class BusinessEngineTranscriptionResult(
    val jobId: String,
    val status: String,
    val estimatedTime: Int = 0,
    val responseTime: Long = 0,
    val error: String? = null,
    val transcript: String = "",
    val confidence: Double = 0.0,
    val language: String = "",
    val duration: Int = 0
)

class BusinessEngineException(message: String) : Exception(message)
