package app.pluct.api

import android.util.Log
import app.pluct.status.PluctStatusTrackingManager
import app.pluct.data.entity.ProcessingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.pluct.error.PluctErrorHandler
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct TTTranscribe Service - Handles TTTranscribe API calls with proper authentication and status tracking
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctTTTranscribeService @Inject constructor(
    private val apiService: PluctCoreApiService,
    private val authenticator: PluctTTTranscribeAuthenticator,
    private val statusTracker: PluctStatusTrackingManager
) {
    companion object {
        private const val TAG = "PluctTTTranscribeService"
    }

    /**
     * Transcribe a TikTok video using TTTranscribe API with status tracking
     */
    suspend fun transcribeVideo(videoUrl: String): TTTranscribeResult = withContext(Dispatchers.IO) {
        val statusId = "tttranscribe-${System.currentTimeMillis()}"
        
        try {
            Log.d(TAG, "Starting TTTranscribe transcription for: $videoUrl")
            
            // Update status to TRANSCRIBING
            statusTracker.updateStatus(
                id = statusId,
                title = "TTTranscribe Processing",
                description = "Transcribing video with TTTranscribe API",
                status = ProcessingStatus.TRANSCRIBING,
                progress = 10
            )
            
            val request = TTTranscribeRequest(url = videoUrl)
            
            statusTracker.updateProgress(statusId, 20, "Vending access token...")
            val vendTokenResponse = PluctErrorHandler.executeWithRetry(
                operation = {
                    apiService.vendToken(VendTokenRequest(userId = "mobile"))
                },
                config = PluctErrorHandler.API_RETRY_CONFIG,
                operationName = "Business Engine /vend-token"
            ).getOrThrow()
            if (!vendTokenResponse.isSuccessful || vendTokenResponse.body() == null) {
                val code = vendTokenResponse.code()
                val err = vendTokenResponse.errorBody()?.string()
                statusTracker.markFailed(statusId, "Token vending failed: $code")
                return@withContext TTTranscribeResult.Error("vend-token failed: $code - ${err ?: "no body"}")
            }
            val token = vendTokenResponse.body()!!.token
            
            statusTracker.updateProgress(statusId, 40, "Calling Pluct proxy for transcription...")
            Log.d(TAG, "Calling Pluct proxy with Bearer token")
            val responseResult = PluctErrorHandler.executeWithRetry(
                operation = {
                    apiService.transcribeViaPluctProxy(
                        authorization = "Bearer $token",
                        request = request
                    )
                },
                config = PluctErrorHandler.API_RETRY_CONFIG,
                operationName = "Pluct Proxy /ttt/transcribe"
            )
            val response = responseResult.getOrThrow()
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    statusTracker.updateProgress(statusId, 80, "Processing transcript...")
                    
                    Log.d(TAG, "TTTranscribe transcription successful")
                    Log.d(TAG, "Transcript length: ${responseBody.transcript.length} characters")
                    Log.d(TAG, "Duration: ${responseBody.duration_sec} seconds")
                    Log.d(TAG, "Language: ${responseBody.lang}")
                    
                    statusTracker.markCompleted(statusId, "Transcription completed successfully")
                    
                    TTTranscribeResult.Success(
                        transcript = responseBody.transcript,
                        language = responseBody.lang,
                        duration = responseBody.duration_sec,
                        requestId = responseBody.request_id,
                        videoId = responseBody.source.video_id
                    )
                } else {
                    Log.e(TAG, "TTTranscribe response body is null")
                    statusTracker.markFailed(statusId, "Response body is null")
                    TTTranscribeResult.Error("Response body is null")
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "TTTranscribe API call failed: ${response.code()} - $errorBody")
                statusTracker.markFailed(statusId, "API call failed: ${response.code()} - $errorBody")
                TTTranscribeResult.Error("API call failed: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in TTTranscribe transcription: ${e.message}", e)
            statusTracker.markFailed(statusId, "Transcription failed: ${e.message}")
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
