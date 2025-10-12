package app.pluct.data.manager

import android.content.Context
import android.util.Log
import app.pluct.data.processor.UrlProcessor
import app.pluct.data.processor.UrlProcessingResult
import app.pluct.data.service.HuggingFaceTranscriptionService
import app.pluct.viewmodel.ValuePropositionGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Consolidated transcription manager for Pluct
 * Single source of truth for all transcription operations
 */
class PluctTranscriptionManager(
    private val context: Context,
    private val urlProcessor: UrlProcessor = UrlProcessor(),
    private val huggingFaceService: HuggingFaceTranscriptionService,
    private val valuePropositionGenerator: ValuePropositionGenerator = ValuePropositionGenerator
) {
    companion object {
        private const val TAG = "PluctTranscriptionManager"
        private const val TRANSCRIPTS_DIR = "transcripts"
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
                        normalizedUrl = result.normalizedUrl,
                        displayHost = urlProcessor.extractHostFromUrl(result.normalizedUrl)
                    )
                }
                is UrlProcessingResult.Invalid -> {
                    TranscriptProcessingResult.ValidationError(result.message, result.errorCode)
                }
                is UrlProcessingResult.Error -> {
                    TranscriptProcessingResult.ProcessingError(result.message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing URL for transcript: ${e.message}", e)
            TranscriptProcessingResult.ProcessingError("Failed to process URL: ${e.message}")
        }
    }
    
    /**
     * Execute transcription with different providers
     */
    suspend fun executeTranscription(
        videoUrl: String,
        onProgress: (String) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        return try {
            Log.d(TAG, "Attempting transcription for URL: $videoUrl")
            
            if (!huggingFaceService.isServiceAvailable()) {
                Log.w(TAG, "Hugging Face service not available")
                return false
            }
            
            var success = false
            var errorMessage = ""
            
            huggingFaceService.transcribeVideo(
                videoUrl = videoUrl,
                onProgress = onProgress,
                onSuccess = { transcript ->
                    Log.d(TAG, "Transcription successful")
                    onSuccess(transcript)
                    success = true
                },
                onError = { error ->
                    Log.w(TAG, "Transcription failed: $error")
                    errorMessage = error
                }
            )
            
            var attempts = 0
            while (!success && attempts < 30) {
                kotlinx.coroutines.delay(2000)
                attempts++
            }
            
            if (!success) {
                onError(errorMessage)
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing transcription: ${e.message}", e)
            onError("Transcription failed: ${e.message}")
            false
        }
    }
    
    /**
     * Save transcript to file
     */
    suspend fun saveTranscriptToFile(
        transcript: String,
        videoUrl: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val transcriptsDir = File(context.filesDir, TRANSCRIPTS_DIR)
                if (!transcriptsDir.exists()) {
                    transcriptsDir.mkdirs()
                }
                
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "transcript_${timestamp}.txt"
                val file = File(transcriptsDir, fileName)
                
                file.writeText(transcript)
                
                Log.d(TAG, "Transcript saved to: ${file.absolutePath}")
                onSuccess(file.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving transcript: ${e.message}", e)
                onError("Failed to save transcript: ${e.message}")
            }
        }
    }
    
    /**
     * Generate value proposition from transcript
     */
    suspend fun generateValueProposition(transcript: String): String {
        return try {
            valuePropositionGenerator.generateValueProposition(transcript)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating value proposition: ${e.message}", e)
            "Unable to generate value proposition"
        }
    }
}

/**
 * Result of transcript processing
 */
sealed class TranscriptProcessingResult {
    data class ReadyForExtraction(
        val processedUrl: String,
        val normalizedUrl: String,
        val displayHost: String
    ) : TranscriptProcessingResult()
    
    data class ValidationError(
        val message: String,
        val errorCode: String
    ) : TranscriptProcessingResult()
    
    data class ProcessingError(
        val message: String
    ) : TranscriptProcessingResult()
}
