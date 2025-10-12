package app.pluct.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct TTTranscribe Service - Handles TTTranscribe API calls with proper authentication
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctTTTranscribeService @Inject constructor(
    private val apiService: PluctCoreApiService,
    private val authenticator: PluctTTTranscribeAuthenticator
) {
    companion object {
        private const val TAG = "PluctTTTranscribeService"
    }

    /**
     * Transcribe a TikTok video using TTTranscribe API
     */
    suspend fun transcribeVideo(videoUrl: String): TTTranscribeResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting TTTranscribe transcription for: $videoUrl")
            
            val request = TTTranscribeRequest(url = videoUrl)
            val method = "POST"
            val path = "/api/transcribe"
            val body = """{"url":"$videoUrl"}"""
            
            val authHeaders = authenticator.createAuthHeaders(method, path, body)
            
            Log.d(TAG, "Making TTTranscribe API call with auth headers")
            val response = apiService.transcribeWithTTTranscribe(
                apiKey = authHeaders["X-API-Key"]!!,
                timestamp = authHeaders["X-Timestamp"]!!,
                signature = authHeaders["X-Signature"]!!,
                request = request
            )
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    Log.d(TAG, "TTTranscribe transcription successful")
                    Log.d(TAG, "Transcript length: ${responseBody.transcript.length} characters")
                    Log.d(TAG, "Duration: ${responseBody.duration_sec} seconds")
                    Log.d(TAG, "Language: ${responseBody.lang}")
                    
                    TTTranscribeResult.Success(
                        transcript = responseBody.transcript,
                        language = responseBody.lang,
                        duration = responseBody.duration_sec,
                        requestId = responseBody.request_id,
                        videoId = responseBody.source.video_id
                    )
                } else {
                    Log.e(TAG, "TTTranscribe response body is null")
                    TTTranscribeResult.Error("Response body is null")
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "TTTranscribe API call failed: ${response.code()} - $errorBody")
                TTTranscribeResult.Error("API call failed: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in TTTranscribe transcription: ${e.message}", e)
            TTTranscribeResult.Error("Transcription failed: ${e.message}")
        }
    }

    /**
     * Test TTTranscribe API connectivity
     */
    suspend fun testConnectivity(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing TTTranscribe API connectivity")
            
            // Test with a simple TikTok URL
            val testUrl = "https://vm.tiktok.com/ZMAPTWV7o/"
            val result = transcribeVideo(testUrl)
            
            when (result) {
                is TTTranscribeResult.Success -> {
                    Log.d(TAG, "TTTranscribe connectivity test successful")
                    true
                }
                is TTTranscribeResult.Error -> {
                    Log.e(TAG, "TTTranscribe connectivity test failed: ${result.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTTranscribe connectivity test error: ${e.message}", e)
            false
        }
    }
}

/**
 * TTTranscribe result types
 */
sealed class TTTranscribeResult {
    data class Success(
        val transcript: String,
        val language: String,
        val duration: Double,
        val requestId: String,
        val videoId: String
    ) : TTTranscribeResult()
    
    data class Error(val message: String) : TTTranscribeResult()
}
