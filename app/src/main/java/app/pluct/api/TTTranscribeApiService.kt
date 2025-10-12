package app.pluct.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * TTTranscribe API Service for high-quality transcription
 * This service integrates with the TTTranscribe API for AI Analysis tier
 */
class TTTranscribeApiService {
    companion object {
        private const val TAG = "TTTranscribeApiService"
        private const val BASE_URL = "https://api.tttranscribe.com"
        private const val API_KEY = "your_api_key_here" // TODO: Move to secure storage
        private const val TIMEOUT_SECONDS = 30L
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    /**
     * Transcribe video using TTTranscribe API
     * @param videoUrl The TikTok video URL to transcribe
     * @return TranscriptionResult with transcript and metadata
     */
    suspend fun transcribeVideo(videoUrl: String): TranscriptionResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Starting TTTranscribe API call for: $videoUrl")
                
                val requestBody = JSONObject().apply {
                    put("url", videoUrl)
                    put("language", "en")
                    put("format", "text")
                    put("include_timestamps", true)
                    put("include_speaker_detection", true)
                }.toString()
                
                val request = Request.Builder()
                    .url("$BASE_URL/v1/transcribe")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.i(TAG, "TTTranscribe API call successful")
                    
                    val result = parseTranscriptionResponse(responseBody ?: "")
                    when (result) {
                        is TranscriptionResult.Success -> Log.d(TAG, "Transcription completed: ${result.transcript.length} characters")
                        is TranscriptionResult.Error -> Log.e(TAG, "Transcription failed: ${result.message}")
                    }
                    result
                } else {
                    Log.e(TAG, "TTTranscribe API call failed: ${response.code} ${response.message}")
                    TranscriptionResult.Error("API call failed: ${response.code}")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error during TTTranscribe API call", e)
                TranscriptionResult.Error("Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during TTTranscribe API call", e)
                TranscriptionResult.Error("Unexpected error: ${e.message}")
            }
        }
    }
    
    /**
     * Generate AI analysis from transcript
     * @param transcript The transcript text to analyze
     * @return AIAnalysisResult with summary, key takeaways, and actionable steps
     */
    suspend fun generateAIAnalysis(transcript: String): AIAnalysisResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Starting AI analysis for transcript: ${transcript.length} characters")
                
                val requestBody = JSONObject().apply {
                    put("transcript", transcript)
                    put("analysis_type", "comprehensive")
                    put("include_summary", true)
                    put("include_key_takeaways", true)
                    put("include_actionable_steps", true)
                }.toString()
                
                val request = Request.Builder()
                    .url("$BASE_URL/v1/analyze")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.i(TAG, "AI analysis API call successful")
                    
                    val result = parseAIAnalysisResponse(responseBody ?: "")
                    when (result) {
                        is AIAnalysisResult.Success -> Log.d(TAG, "AI analysis completed: ${result.summary.length} characters")
                        is AIAnalysisResult.Error -> Log.e(TAG, "AI analysis failed: ${result.message}")
                    }
                    result
                } else {
                    Log.e(TAG, "AI analysis API call failed: ${response.code} ${response.message}")
                    AIAnalysisResult.Error("API call failed: ${response.code}")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error during AI analysis API call", e)
                AIAnalysisResult.Error("Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during AI analysis API call", e)
                AIAnalysisResult.Error("Unexpected error: ${e.message}")
            }
        }
    }
    
    private fun parseTranscriptionResponse(responseBody: String): TranscriptionResult {
        return try {
            val json = JSONObject(responseBody)
            val transcript = json.optString("transcript", "")
            val confidence = json.optDouble("confidence", 0.0)
            val duration = json.optLong("duration", 0L)
            val language = json.optString("language", "en")
            
            TranscriptionResult.Success(
                transcript = transcript,
                confidence = confidence,
                duration = duration,
                language = language
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing transcription response", e)
            TranscriptionResult.Error("Failed to parse response: ${e.message}")
        }
    }
    
    private fun parseAIAnalysisResponse(responseBody: String): AIAnalysisResult {
        return try {
            val json = JSONObject(responseBody)
            val summary = json.optString("summary", "")
            val keyTakeaways = json.optJSONArray("key_takeaways")?.let { array ->
                (0 until array.length()).map { array.getString(it) }
            } ?: emptyList()
            val actionableSteps = json.optJSONArray("actionable_steps")?.let { array ->
                (0 until array.length()).map { array.getString(it) }
            } ?: emptyList()
            
            AIAnalysisResult.Success(
                summary = summary,
                keyTakeaways = keyTakeaways,
                actionableSteps = actionableSteps
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing AI analysis response", e)
            AIAnalysisResult.Error("Failed to parse response: ${e.message}")
        }
    }
}

/**
 * Result of transcription API call
 */
sealed class TranscriptionResult {
    data class Success(
        val transcript: String,
        val confidence: Double,
        val duration: Long,
        val language: String
    ) : TranscriptionResult()
    
    data class Error(val message: String) : TranscriptionResult()
}

/**
 * Result of AI analysis API call
 */
sealed class AIAnalysisResult {
    data class Success(
        val summary: String,
        val keyTakeaways: List<String>,
        val actionableSteps: List<String>
    ) : AIAnalysisResult()
    
    data class Error(val message: String) : AIAnalysisResult()
}
