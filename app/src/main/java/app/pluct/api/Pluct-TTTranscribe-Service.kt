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
     * Updated to use proper Business Engine gateway flow
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
            
            // Stage 1: VEND TOKEN
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
            Log.d(TAG, "Successfully obtained token from Business Engine")
            
            // Stage 2: CALL TTTRANSCRIBE PROXY
            statusTracker.updateProgress(statusId, 40, "Calling TTTranscribe proxy...")
            val transcribeResponse = PluctErrorHandler.executeWithRetry(
                operation = {
                    apiService.transcribeViaBusinessEngine(
                        authorization = "Bearer $token",
                        request = BusinessEngineTranscribeRequest(url = videoUrl)
                    )
                },
                config = PluctErrorHandler.API_RETRY_CONFIG,
                operationName = "Business Engine /ttt/transcribe"
            ).getOrThrow()
            
            if (!transcribeResponse.isSuccessful || transcribeResponse.body() == null) {
                val code = transcribeResponse.code()
                val err = transcribeResponse.errorBody()?.string()
                statusTracker.markFailed(statusId, "TTTranscribe proxy call failed: $code")
                return@withContext TTTranscribeResult.Error("TTTranscribe proxy failed: $code - ${err ?: "no body"}")
            }
            
            val requestId = transcribeResponse.body()!!.request_id
            Log.d(TAG, "Successfully submitted transcription request: $requestId")
            
            // Stage 3: POLL FOR COMPLETION
            statusTracker.updateProgress(statusId, 60, "Polling for completion...")
            val transcript = pollForCompletion(token, requestId, statusId)
            
            if (transcript != null) {
                statusTracker.markCompleted(statusId, "Transcription completed successfully")
                Log.d(TAG, "Transcription completed successfully")
                
                TTTranscribeResult.Success(
                    transcript = transcript,
                    language = "en", // Default language
                    duration = 0.0, // Duration not available from status endpoint
                    requestId = requestId,
                    videoId = "unknown" // Video ID not available from status endpoint
                )
            } else {
                statusTracker.markFailed(statusId, "Transcription polling failed")
                TTTranscribeResult.Error("Transcription polling failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in TTTranscribe transcription: ${e.message}", e)
            statusTracker.markFailed(statusId, "Transcription failed: ${e.message}")
            TTTranscribeResult.Error("Transcription failed: ${e.message}")
        }
    }
    
    /**
     * Poll for transcription completion using Business Engine status endpoint
     */
    private suspend fun pollForCompletion(token: String, requestId: String, statusId: String): String? {
        var attempts = 0
        val maxAttempts = 30 // 5 minutes with 10-second intervals
        
        while (attempts < maxAttempts) {
            try {
                val statusResponse = apiService.checkTranscriptionStatus(
                    authorization = "Bearer $token",
                    requestId = requestId
                )
                
                if (statusResponse.isSuccessful && statusResponse.body() != null) {
                    val statusBody = statusResponse.body()!!
                    val phase = statusBody.phase
                    val progress = statusBody.percent ?: 0
                    
                    statusTracker.updateProgress(statusId, 60 + (progress * 0.4).toInt(), "Processing: $phase")
                    
                    when (phase) {
                        "COMPLETED" -> {
                            val transcript = statusBody.transcript ?: ""
                            if (transcript.isNotEmpty()) {
                                return transcript
                            }
                        }
                        "FAILED" -> {
                            Log.e(TAG, "Transcription failed: ${statusBody.note}")
                            return null
                        }
                        else -> {
                            // Still processing, continue polling
                            kotlinx.coroutines.delay(10000) // Wait 10 seconds
                            attempts++
                        }
                    }
                } else {
                    Log.e(TAG, "Status check failed: ${statusResponse.code()}")
                    return null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Status check error: ${e.message}")
                return null
            }
        }
        
        Log.e(TAG, "Transcription polling timed out after $maxAttempts attempts")
        return null
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
