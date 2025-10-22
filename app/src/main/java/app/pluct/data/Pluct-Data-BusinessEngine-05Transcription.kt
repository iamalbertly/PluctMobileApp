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
 * Pluct-Data-BusinessEngine-05Transcription - Transcription functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation][CoreResponsibility]
 */
class PluctBusinessEngineTranscription(
    private val baseUrl: String,
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "PluctBusinessEngineTranscription"
    }

    /**
     * Start transcription job
     */
    suspend fun transcribe(videoUrl: String, token: String): BusinessEngineTranscriptionResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🎬 Starting transcription for: $videoUrl")
                val requestBody = JSONObject().apply {
                    put("url", videoUrl)
                }.toString()

                val request = Request.Builder()
                    .url("$baseUrl/ttt/transcribe")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val jobId = json.optString("jobId", "")
                    val status = json.optString("status", "")
                    val estimatedTime = json.optInt("estimatedTime", 0)
                    
                    PluctLogger.logBusinessEngineCall("transcribe", true, 0, mapOf(
                        "jobId" to jobId,
                        "status" to status
                    ))

                    BusinessEngineTranscriptionResult(
                        jobId = jobId,
                        status = status,
                        estimatedTime = estimatedTime,
                        responseTime = 0
                    )
                } else {
                    PluctLogger.logBusinessEngineCall("transcribe", false, 0, mapOf(
                        "error" to "HTTP ${response.code}",
                        "body" to responseBody
                    ))
                    
                    BusinessEngineTranscriptionResult(
                        jobId = "",
                        status = "error",
                        estimatedTime = 0,
                        responseTime = 0,
                        error = "HTTP ${response.code}: $responseBody"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Transcription start failed", e)
                PluctLogger.logError("Transcription start failed: ${e.message}")
                
                BusinessEngineTranscriptionResult(
                    jobId = "",
                    status = "error",
                    estimatedTime = 0,
                    responseTime = 0,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    /**
     * Check transcription status
     */
    suspend fun checkStatus(jobId: String, token: String): BusinessEngineTranscriptionStatusResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📊 Checking transcription status for job: $jobId")
                val request = Request.Builder()
                    .url("$baseUrl/ttt/status/$jobId")
                    .get()
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val status = json.optString("status", "")
                    val progress = json.optInt("progress", 0)
                    val transcript = json.optString("transcript", "")
                    val confidence = json.optDouble("confidence", 0.0)
                    val language = json.optString("language", "")
                    val duration = json.optInt("duration", 0)
                    
                    PluctLogger.logBusinessEngineCall("check_status", true, 0, mapOf(
                        "jobId" to jobId,
                        "status" to status,
                        "progress" to progress
                    ))

                    BusinessEngineTranscriptionStatusResult(
                        jobId = jobId,
                        status = status,
                        progress = progress,
                        transcript = transcript,
                        confidence = confidence,
                        language = language,
                        duration = duration,
                        responseTime = 0
                    )
                } else {
                    PluctLogger.logBusinessEngineCall("check_status", false, 0, mapOf(
                        "error" to "HTTP ${response.code}",
                        "body" to responseBody
                    ))
                    
                    BusinessEngineTranscriptionStatusResult(
                        jobId = jobId,
                        status = "error",
                        progress = 0,
                        transcript = "",
                        confidence = 0.0,
                        language = "",
                        duration = 0,
                        responseTime = 0,
                        error = "HTTP ${response.code}: $responseBody"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Status check failed", e)
                PluctLogger.logError("Status check failed: ${e.message}")
                
                BusinessEngineTranscriptionStatusResult(
                    jobId = jobId,
                    status = "error",
                    progress = 0,
                    transcript = "",
                    confidence = 0.0,
                    language = "",
                    duration = 0,
                    responseTime = 0,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}

data class BusinessEngineTranscriptionResult(
    val jobId: String,
    val status: String,
    val estimatedTime: Int,
    val responseTime: Long,
    val error: String? = null
)

data class BusinessEngineTranscriptionStatusResult(
    val jobId: String,
    val status: String,
    val progress: Int,
    val transcript: String,
    val confidence: Double,
    val language: String,
    val duration: Int,
    val responseTime: Long,
    val error: String? = null
)
