package app.pluct.data.service

import android.util.Log
import app.pluct.data.provider.PluctHuggingFaceProviderCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling Hugging Face transcription requests
 * This service provides a direct API-based approach instead of WebView automation
 */
@Singleton
class HuggingFaceTranscriptionService @Inject constructor(
    private val provider: PluctHuggingFaceProviderCoordinator
) {
    companion object {
        private const val TAG = "HuggingFaceService"
    }
    
    /**
     * Transcribe a video URL using the Hugging Face API
     */
    suspend fun transcribeVideo(
        videoUrl: String,
        onProgress: (String) -> Unit = {},
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Starting Hugging Face transcription for: $videoUrl")
                onProgress("Checking service health...")
                
                // Check if service is healthy
                val isHealthy = provider.checkHealth()
                if (!isHealthy) {
                    Log.e(TAG, "Hugging Face service is not healthy")
                    onError("service_unavailable")
                    return@launch
                }
                
                onProgress("Service is healthy, starting transcription...")
                
                // Start transcription
                val startResponse = provider.startTranscription(videoUrl)
                if (startResponse == null) {
                    Log.e(TAG, "Failed to start transcription")
                    onError("transcription_start_failed")
                    return@launch
                }
                
                val jobId = startResponse.id ?: startResponse.job_id
                if (jobId == null) {
                    Log.e(TAG, "No job ID returned from transcription start")
                    onError("no_job_id")
                    return@launch
                }
                
                Log.d(TAG, "Transcription started with job ID: $jobId")
                onProgress("Transcription started, polling for completion...")
                
                // Poll for completion
                var attempt = 0
                val maxAttempts = 90 // 3 minutes with 2-second intervals
                
                while (attempt < maxAttempts) {
                    attempt++
                    Log.d(TAG, "Polling attempt $attempt/$maxAttempts")
                    
                    val statusResponse = provider.pollTranscriptionStatus(jobId)
                    if (statusResponse == null) {
                        Log.e(TAG, "Failed to get status for job: $jobId")
                        onError("status_poll_failed")
                        return@launch
                    }
                    
                    val queuePosition = statusResponse.queue_position ?: 0
                    val estimatedWait = statusResponse.estimated_wait_seconds ?: 0
                    
                    Log.d(TAG, "Status: ${statusResponse.status}, Queue: $queuePosition, ETA: ${estimatedWait}s")
                    
                    when (statusResponse.status) {
                        "COMPLETE" -> {
                            Log.d(TAG, "Transcription completed successfully")
                            onProgress("Transcription completed, retrieving transcript...")
                            
                            val transcriptUrl = statusResponse.transcript_url
                            if (transcriptUrl != null) {
                                val transcriptContent = provider.getTranscriptContent(transcriptUrl)
                                if (transcriptContent != null) {
                                    Log.d(TAG, "Transcript retrieved successfully, length: ${transcriptContent.length}")
                                    onSuccess(transcriptContent)
                                    return@launch
                                } else {
                                    Log.e(TAG, "Failed to retrieve transcript content")
                                    onError("transcript_retrieval_failed")
                                    return@launch
                                }
                            } else {
                                Log.e(TAG, "No transcript URL in completion response")
                                onError("no_transcript_url")
                                return@launch
                            }
                        }
                        "FAILED" -> {
                            Log.e(TAG, "Transcription failed: ${statusResponse.message}")
                            onError("transcription_failed: ${statusResponse.message}")
                            return@launch
                        }
                        else -> {
                            // Still processing, show progress
                            val progressMessage = if (queuePosition > 0) {
                                "In queue (position: $queuePosition, ETA: ${estimatedWait}s)"
                            } else {
                                "Processing transcription..."
                            }
                            onProgress(progressMessage)
                            
                            // Wait before next poll
                            kotlinx.coroutines.delay(2000)
                        }
                    }
                }
                
                Log.e(TAG, "Transcription timed out after $maxAttempts attempts")
                onError("transcription_timeout")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in transcription service: ${e.message}", e)
                onError("service_error: ${e.message}")
            }
        }
    }
    
    /**
     * Check if the Hugging Face service is available
     */
    suspend fun isServiceAvailable(): Boolean {
        return try {
            provider.checkHealth()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking service availability: ${e.message}", e)
            false
        }
    }
}

