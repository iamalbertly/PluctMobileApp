package app.pluct.transcription

import android.content.Context
import android.util.Log
import app.pluct.api.PluctCoreApiService
import app.pluct.api.PluctTTTranscribeService
import app.pluct.api.TTTranscribeResult
import app.pluct.data.processor.UrlProcessor
import app.pluct.data.processor.UrlProcessingResult
import app.pluct.data.provider.PluctHuggingFaceProviderCoordinator
import app.pluct.viewmodel.ValuePropositionGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct Core Transcription Manager - Single source of truth for all transcription operations
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Consolidated from PluctTranscriptionManager.kt and HuggingFaceTranscriptionService.kt
 */
@Singleton
class PluctTranscriptionCoreManager @Inject constructor(
    private val context: Context,
    private val apiService: PluctCoreApiService,
    private val ttTranscribeService: PluctTTTranscribeService,
    private val urlProcessor: UrlProcessor,
    private val huggingFaceProvider: PluctHuggingFaceProviderCoordinator,
    private val valuePropositionGenerator: ValuePropositionGenerator
) {
    companion object {
        private const val TAG = "PluctTranscriptionCore"
        private const val TRANSCRIPTS_DIR = "transcripts"
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
     * Execute transcription with TTTranscribe (primary method)
     */
    suspend fun executeTranscriptionWithTTTranscribe(
        videoUrl: String,
        onProgress: (String) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        return try {
            Log.d(TAG, "Starting TTTranscribe transcription for: $videoUrl")
            onProgress("Connecting to TTTranscribe API...")
            
            val result = ttTranscribeService.transcribeVideo(videoUrl)
            
            when (result) {
                is TTTranscribeResult.Success -> {
                    Log.d(TAG, "TTTranscribe transcription successful")
                    onProgress("Transcription completed successfully")
                    onSuccess(result.transcript)
                    true
                }
                is TTTranscribeResult.Error -> {
                    Log.e(TAG, "TTTranscribe transcription failed: ${result.message}")
                    onError("TTTranscribe failed: ${result.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in TTTranscribe transcription: ${e.message}", e)
            onError("TTTranscribe error: ${e.message}")
            false
        }
    }

    /**
     * Execute transcription with Hugging Face provider (fallback)
     */
    suspend fun executeTranscription(
        videoUrl: String,
        onProgress: (String) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        return try {
            Log.d(TAG, "Starting transcription for URL: $videoUrl")
            
            if (!isServiceAvailable()) {
                Log.w(TAG, "Hugging Face service not available")
                onError("service_unavailable")
                return false
            }

            var success = false
            var errorMessage = ""

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    onProgress("Checking service health...")
                    
                    if (!huggingFaceProvider.checkHealth()) {
                        onError("service_unavailable")
                        return@launch
                    }
                    
                    onProgress("Service is healthy, starting transcription...")
                    
                    val startResponse = huggingFaceProvider.startTranscription(videoUrl)
                    if (startResponse == null) {
                        onError("transcription_start_failed")
                        return@launch
                    }
                    
                    val jobId = startResponse.id ?: startResponse.job_id
                    if (jobId == null) {
                        onError("no_job_id")
                        return@launch
                    }
                    
                    Log.d(TAG, "Transcription started with job ID: $jobId")
                    onProgress("Transcription started, polling for completion...")
                    
                    // Poll for completion
                    var attempt = 0
                    while (attempt < MAX_POLL_ATTEMPTS) {
                        attempt++
                        
                        val statusResponse = huggingFaceProvider.pollTranscriptionStatus(jobId)
                        if (statusResponse == null) {
                            onError("status_poll_failed")
                            return@launch
                        }
                        
                        when (statusResponse.status) {
                            "COMPLETE" -> {
                                Log.d(TAG, "Transcription completed successfully")
                                onProgress("Transcription completed, retrieving transcript...")
                                
                                val transcriptUrl = statusResponse.transcript_url
                                if (transcriptUrl != null) {
                                    val transcriptContent = huggingFaceProvider.getTranscriptContent(transcriptUrl)
                                    if (transcriptContent != null) {
                                        onSuccess(transcriptContent)
                                        success = true
                                        return@launch
                                    } else {
                                        onError("transcript_retrieval_failed")
                                        return@launch
                                    }
                                } else {
                                    onError("no_transcript_url")
                                    return@launch
                                }
                            }
                            "FAILED" -> {
                                onError("transcription_failed: ${statusResponse.message}")
                                return@launch
                            }
                            else -> {
                                val queuePosition = statusResponse.queue_position ?: 0
                                val estimatedWait = statusResponse.estimated_wait_seconds ?: 0
                                
                                val progressMessage = if (queuePosition > 0) {
                                    "In queue (position: $queuePosition, ETA: ${estimatedWait}s)"
                                } else {
                                    "Processing transcription..."
                                }
                                onProgress(progressMessage)
                                delay(2000)
                            }
                        }
                    }
                    
                    onError("transcription_timeout")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in transcription: ${e.message}", e)
                    onError("service_error: ${e.message}")
                }
            }

            // Wait for completion
            var attempts = 0
            while (!success && attempts < 30) {
                delay(2000)
                attempts++
            }

            success
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
            valuePropositionGenerator.generateValuePropositionFromTranscript(transcript)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating value proposition: ${e.message}", e)
            "Unable to generate value proposition"
        }
    }

    /**
     * Check if the transcription service is available
     */
    suspend fun isServiceAvailable(): Boolean {
        return try {
            huggingFaceProvider.checkHealth()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking service availability: ${e.message}", e)
            false
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
