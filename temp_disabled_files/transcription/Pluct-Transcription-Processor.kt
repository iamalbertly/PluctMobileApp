package app.pluct.transcription

import android.content.Context
import android.util.Log
import app.pluct.api.PluctCoreApiService
import app.pluct.api.PluctTTTranscribeService
import app.pluct.api.TTTranscribeResult
import app.pluct.data.processor.UrlProcessor
import app.pluct.data.processor.UrlProcessingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Transcription-Processor - Core transcription processing logic
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctTranscriptionProcessor @Inject constructor(
    private val context: Context,
    private val apiService: PluctCoreApiService,
    private val ttTranscribeService: PluctTTTranscribeService,
    private val urlProcessor: UrlProcessor
) {
    companion object {
        private const val TAG = "PluctTranscriptionProcessor"
        private const val MAX_POLL_ATTEMPTS = 90 // 3 minutes with 2-second intervals
    }

    /**
     * Process URL and prepare for transcript extraction
     */
    suspend fun processUrlForTranscript(url: String): TranscriptProcessingResult {
        return try {
            when (val result = urlProcessor.processAndValidateUrl(url)) {
                is UrlProcessingResult.Success -> {
                    TranscriptProcessingResult.ReadyForExtraction(
                        processedUrl = result.processedUrl,
                        metadata = emptyMap()
                    )
                }
                is UrlProcessingResult.Error -> {
                    TranscriptProcessingResult.Error(result.message)
                }
                else -> {
                    TranscriptProcessingResult.Error("Unknown URL processing result")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing URL: ${e.message}")
            TranscriptProcessingResult.Error("Failed to process URL: ${e.message}")
        }
    }

    /**
     * Extract transcript using TTTranscribe
     */
    suspend fun extractTranscript(url: String): TranscriptExtractionResult {
        return try {
            Log.d(TAG, "Starting transcript extraction for: $url")
            
            when (val result = ttTranscribeService.transcribeVideo(url)) {
                is TTTranscribeResult.Success -> {
                    Log.d(TAG, "Transcript extraction successful")
                    TranscriptExtractionResult.Success(
                        transcript = result.transcript,
                        language = result.language,
                        duration = result.duration,
                        requestId = result.requestId,
                        videoId = result.videoId
                    )
                }
                is TTTranscribeResult.Error -> {
                    Log.e(TAG, "Transcript extraction failed: ${result.message}")
                    TranscriptExtractionResult.Error(result.message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting transcript: ${e.message}")
            TranscriptExtractionResult.Error("Failed to extract transcript: ${e.message}")
        }
    }

    /**
     * Poll for transcription completion
     */
    suspend fun pollForCompletion(requestId: String): PollingResult {
        return try {
            var attempts = 0
            while (attempts < MAX_POLL_ATTEMPTS) {
                // Simple polling - in real implementation, this would check the actual status
                kotlinx.coroutines.delay(2000) // Wait 2 seconds
                attempts++
                
                // For now, return success after a few attempts
                if (attempts >= 3) {
                    return PollingResult.Success("Sample transcript for request $requestId")
                }
            }
            
            PollingResult.Timeout("Transcription polling timed out after $MAX_POLL_ATTEMPTS attempts")
        } catch (e: Exception) {
            Log.e(TAG, "Error polling for completion: ${e.message}")
            PollingResult.Error("Polling failed: ${e.message}")
        }
    }
}

sealed class TranscriptProcessingResult {
    data class ReadyForExtraction(
        val processedUrl: String,
        val metadata: Map<String, Any>
    ) : TranscriptProcessingResult()
    
    data class Error(val message: String) : TranscriptProcessingResult()
}

sealed class TranscriptExtractionResult {
    data class Success(
        val transcript: String,
        val language: String,
        val duration: Double,
        val requestId: String,
        val videoId: String
    ) : TranscriptExtractionResult()
    
    data class Error(val message: String) : TranscriptExtractionResult()
}

sealed class PollingResult {
    data class Success(val transcript: String) : PollingResult()
    data class Error(val message: String) : PollingResult()
    data class Timeout(val message: String) : PollingResult()
}
