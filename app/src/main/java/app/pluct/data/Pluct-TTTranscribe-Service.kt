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

/**
 * Pluct-TTTranscribe-Service - TTTranscribe API integration
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluctTTTranscribeService @Inject constructor() {
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(PluctNetworkHttp01Logger())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val baseUrl = "https://pluct-business-engine.romeo-lya2.workers.dev"
    
    data class TranscriptionRequest(
        val url: String,
        val jobId: String? = null,
        val status: String? = null,
        val estimatedTime: Int? = null
    )
    
    data class TranscriptionResult(
        val success: Boolean,
        val jobId: String?,
        val status: String?,
        val transcript: String?,
        val confidence: Double?,
        val language: String?,
        val duration: Int?,
        val error: String? = null
    )
    
    suspend fun startTranscription(shortLivedToken: String, videoUrl: String): TranscriptionRequest = withContext(Dispatchers.IO) {
        try {
            Log.i("PluctTTTranscribeService", "🎯 Starting transcription for: $videoUrl")
            
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
            
            Log.i("PluctTTTranscribeService", "🎯 Transcription start response: $responseBody")
            
            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val jobId = json.optString("jobId", "")
                val status = json.optString("status", "processing")
                val estimatedTime = json.optInt("estimatedTime", 30)
                
                Log.i("PluctTTTranscribeService", "✅ Transcription started - Job ID: $jobId")
                Log.i("PluctTTTranscribeService", "✅ Status: $status, Estimated time: ${estimatedTime}s")
                
                TranscriptionRequest(
                    url = videoUrl,
                    jobId = jobId,
                    status = status,
                    estimatedTime = estimatedTime
                )
            } else {
                val errorMessage = "HTTP ${response.code}: $responseBody"
                Log.e("PluctTTTranscribeService", "❌ Transcription start failed: $errorMessage")
                
                TranscriptionRequest(
                    url = videoUrl,
                    jobId = null,
                    status = "failed",
                    estimatedTime = null
                )
            }
        } catch (e: Exception) {
            Log.e("PluctTTTranscribeService", "❌ Transcription start exception: ${e.message}", e)
            TranscriptionRequest(
                url = videoUrl,
                jobId = null,
                status = "failed",
                estimatedTime = null
            )
        }
    }
    
    suspend fun checkTranscriptionStatus(shortLivedToken: String, jobId: String): TranscriptionResult = withContext(Dispatchers.IO) {
        try {
            Log.i("PluctTTTranscribeService", "🎯 Checking transcription status for job: $jobId")
            
            val request = Request.Builder()
                .url("$baseUrl/ttt/status/$jobId")
                .get()
                .addHeader("Authorization", "Bearer $shortLivedToken")
                .addHeader("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.i("PluctTTTranscribeService", "🎯 Transcription status response: $responseBody")
            
            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val status = json.optString("status", "unknown")
                val transcript = json.optString("transcript", "")
                val confidence = json.optDouble("confidence", 0.0)
                val language = json.optString("language", "en")
                val duration = json.optInt("duration", 0)
                
                Log.i("PluctTTTranscribeService", "✅ Status check - Status: $status")
                if (status == "completed" && transcript.isNotEmpty()) {
                    Log.i("PluctTTTranscribeService", "✅ Transcription completed successfully")
                    Log.i("PluctTTTranscribeService", "✅ Transcript length: ${transcript.length} chars")
                    Log.i("PluctTTTranscribeService", "✅ Confidence: $confidence, Language: $language")
                }
                
                TranscriptionResult(
                    success = status == "completed",
                    jobId = jobId,
                    status = status,
                    transcript = transcript.takeIf { it.isNotEmpty() },
                    confidence = confidence.takeIf { it > 0.0 },
                    language = language.takeIf { it.isNotEmpty() },
                    duration = duration.takeIf { it > 0 }
                )
            } else {
                val errorMessage = "HTTP ${response.code}: $responseBody"
                Log.e("PluctTTTranscribeService", "❌ Status check failed: $errorMessage")
                
                TranscriptionResult(
                    success = false,
                    jobId = jobId,
                    status = "failed",
                    transcript = null,
                    confidence = null,
                    language = null,
                    duration = null,
                    error = errorMessage
                )
            }
        } catch (e: Exception) {
            Log.e("PluctTTTranscribeService", "❌ Status check exception: ${e.message}", e)
            TranscriptionResult(
                success = false,
                jobId = jobId,
                status = "failed",
                transcript = null,
                confidence = null,
                language = null,
                duration = null,
                error = e.message ?: "Unknown error"
            )
        }
    }
}
