package app.pluct.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.pluct.api.EngineApiProvider
import app.pluct.config.AppConfig
import app.pluct.utils.BusinessEngineHealthChecker
import app.pluct.utils.BusinessEngineCreditManager
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TTTranscribeWork(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    private val api = EngineApiProvider.instance
    private val userId = AppConfig.userId
    
    // Configure HTTP client with proper timeouts and retry logic
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private suspend fun stage(s: String, url: String, reqId: String? = null, msg: String? = null, pct: Int? = null) {
        setProgress(workDataOf("stage" to s, "percent" to (pct ?: -1)))
        Log.i("TTT", "stage=$s url=$url reqId=${reqId ?: "-"} msg=${msg ?: ""}")
    }

    override suspend fun doWork(): Result {
        val videoUrl = inputData.getString("url") ?: return Result.failure()
        
        return try {
            // Pre-flight health check
            Log.i("TTT", "stage=HEALTH_CHECK url=$videoUrl reqId=- msg=checking")
            val healthCheck = BusinessEngineHealthChecker.checkBusinessEngineHealth()
            if (!healthCheck) {
                Log.e("TTT", "Business Engine health check failed")
                BusinessEngineHealthChecker.handleTTTError("HEALTH_CHECK", "Business Engine unavailable", videoUrl)
                return Result.retry()
            }
            Log.i("TTT", "stage=HEALTH_CHECK url=$videoUrl reqId=- msg=success")
            
            // Ensure user has credits
            Log.i("TTT", "stage=CREDIT_CHECK url=$videoUrl reqId=- msg=checking")
            val creditCheck = BusinessEngineCreditManager.ensureUserWithCredits("mobile", 10)
            if (!creditCheck) {
                Log.e("TTT", "Credit check failed")
                BusinessEngineCreditManager.handleCreditError("User creation/credit check failed", "mobile")
                return Result.retry()
            }
            Log.i("TTT", "stage=CREDIT_CHECK url=$videoUrl reqId=- msg=success")
            
            // Stage 1: VEND TOKEN
            Log.i("TTT", "stage=VENDING_TOKEN url=$videoUrl reqId=- msg=requesting")
            val token = vendToken()
            if (token == null) {
                Log.e("TTT", "Token vending failed")
                BusinessEngineHealthChecker.handleTTTError("VENDING_TOKEN", "Token vending failed", videoUrl)
                return Result.retry() // Retry on token failure
            }
            Log.i("TTT", "stage=VENDING_TOKEN url=$videoUrl reqId=- msg=success")
            
            // Stage 2: CALL TTTRANSCRIBE
            Log.i("TTT", "stage=TTTRANSCRIBE_CALL url=$videoUrl reqId=- msg=requesting")
            val requestId = callTTTranscribe(token, videoUrl)
            if (requestId == null) {
                Log.e("TTT", "TTTranscribe call failed")
                BusinessEngineHealthChecker.handleTTTError("TTTRANSCRIBE_CALL", "TTTranscribe call failed", videoUrl)
                return Result.retry() // Retry on TTTranscribe failure
            }
            Log.i("TTT", "stage=TTTRANSCRIBE_CALL url=$videoUrl reqId=$requestId msg=success")
            
            // Stage 3: POLL STATUS
            Log.i("TTT", "stage=STATUS_POLLING url=$videoUrl reqId=$requestId msg=requesting")
            val transcript = pollForCompletion(token, requestId)
            if (transcript == null) {
                Log.e("TTT", "Transcription polling failed")
                BusinessEngineHealthChecker.handleTTTError("STATUS_POLLING", "Transcription polling failed", videoUrl)
                return Result.retry() // Retry on polling failure
            }
            Log.i("TTT", "stage=COMPLETED url=$videoUrl reqId=$requestId msg=success")
            
            Result.success()
        } catch (e: Exception) {
            Log.e("TTT", "Worker error: ${e.message}")
            BusinessEngineHealthChecker.handleTTTError("WORKER_ERROR", e.message ?: "Unknown error", videoUrl)
            Result.retry()
        }
    }

    private suspend fun vendToken(): String? {
        try {
            val requestBody = JSONObject().apply {
                put("userId", "mobile") // Use consistent user ID
            }
            
            val request = Request.Builder()
                .url("https://pluct-business-engine.romeo-lya2.workers.dev/vend-token")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "")
                return jsonResponse.getString("token")
            } else {
                Log.e("TTT", "Token vending failed: ${response.code}")
                return null
            }
        } catch (e: Exception) {
            Log.e("TTT", "Token vending error: ${e.message}")
            return null
        }
    }

    private suspend fun callTTTranscribe(token: String, videoUrl: String): String? {
        try {
            val requestBody = JSONObject().apply {
                put("url", videoUrl)
            }
            
            val request = Request.Builder()
                .url("https://pluct-business-engine.romeo-lya2.workers.dev/ttt/transcribe")
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "")
                return jsonResponse.getString("request_id") // Extract request_id
            } else {
                Log.e("TTT", "TTTranscribe call failed: ${response.code}")
                return null
            }
        } catch (e: Exception) {
            Log.e("TTT", "TTTranscribe error: ${e.message}")
            return null
        }
    }

    private suspend fun checkTranscriptionStatus(token: String, requestId: String): String? {
        try {
            val request = Request.Builder()
                .url("https://pluct-business-engine.romeo-lya2.workers.dev/ttt/status/$requestId")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "")
                return jsonResponse.getString("transcript") // Extract transcript
            } else {
                Log.e("TTT", "Status check failed: ${response.code}")
                return null
            }
        } catch (e: Exception) {
            Log.e("TTT", "Status check error: ${e.message}")
            return null
        }
    }

    private suspend fun pollForCompletion(token: String, requestId: String): String? {
        var attempts = 0
        val maxAttempts = 30 // 5 minutes with 10-second intervals
        
        while (attempts < maxAttempts) {
            val transcript = checkTranscriptionStatus(token, requestId)
            if (transcript != null && transcript.isNotEmpty()) {
                return transcript
            }
            
            delay(10000) // Wait 10 seconds
            attempts++
            Log.i("TTT", "stage=STATUS_POLLING reqId=$requestId attempt=$attempts")
        }
        
        return null
    }

    private fun persistResult(url: String, reqId: String, text: String) {
        // TODO: write to Room DB (Transcripts table)
        Log.i("TTT", "Persisting result for url=$url reqId=$reqId textLength=${text.length}")
    }

    companion object {
        fun input(url: String) = workDataOf("url" to url)
    }
}