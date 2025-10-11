package app.pluct.data.provider

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pluct-HuggingFace-Provider-Coordinator - Simplified coordinator for Hugging Face provider
 */
class PluctHuggingFaceProviderCoordinator {
    companion object {
        private const val TAG = "PluctHuggingFaceProviderCoordinator"
        private const val MAX_POLL_ATTEMPTS = 90
        private const val POLL_INTERVAL_MS = 2000L
    }
    
    private val apiClient = PluctHuggingFaceApiClient()
    
    suspend fun checkHealth(): Boolean {
        return apiClient.checkHealth()
    }
    
    suspend fun startTranscription(videoUrl: String): PluctHuggingFaceApiClient.TranscriptionResponse? {
        return apiClient.startTranscription(videoUrl)
    }
    
    suspend fun pollTranscriptionStatus(jobId: String): PluctHuggingFaceApiClient.TranscriptionResponse? {
        return apiClient.pollTranscriptionStatus(jobId)
    }
    
    suspend fun getTranscriptContent(transcriptUrl: String): String? {
        return apiClient.getTranscriptContent(transcriptUrl)
    }
    
    suspend fun transcribeVideo(videoUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting complete transcription workflow for: $videoUrl")
                
                val startResponse = startTranscription(videoUrl)
                if (startResponse == null) {
                    Log.e(TAG, "Failed to start transcription")
                    return@withContext null
                }
                
                val jobId = startResponse.id ?: startResponse.job_id
                if (jobId == null) {
                    Log.e(TAG, "No job ID returned from transcription start")
                    return@withContext null
                }
                
                Log.d(TAG, "Transcription started with job ID: $jobId")
                
                repeat(MAX_POLL_ATTEMPTS) { attempt ->
                    Log.d(TAG, "Polling attempt ${attempt + 1}/$MAX_POLL_ATTEMPTS")
                    
                    val statusResponse = pollTranscriptionStatus(jobId)
                    if (statusResponse == null) {
                        Log.e(TAG, "Failed to get status for job: $jobId")
                        return@withContext null
                    }
                    
                    Log.d(TAG, "Status: ${statusResponse.status}, Queue: ${statusResponse.queue_position}, ETA: ${statusResponse.estimated_wait_seconds}s")
                    
                    when (statusResponse.status) {
                        "COMPLETE" -> {
                            Log.d(TAG, "Transcription completed successfully")
                            val transcriptUrl = statusResponse.transcript_url
                            if (transcriptUrl != null) {
                                return@withContext getTranscriptContent(transcriptUrl)
                            } else {
                                Log.e(TAG, "No transcript URL in completion response")
                                return@withContext null
                            }
                        }
                        "FAILED" -> {
                            Log.e(TAG, "Transcription failed: ${statusResponse.message}")
                            return@withContext null
                        }
                        else -> {
                            kotlinx.coroutines.delay(POLL_INTERVAL_MS)
                        }
                    }
                }
                
                Log.e(TAG, "Transcription timed out after $MAX_POLL_ATTEMPTS attempts")
                null
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in transcription workflow: ${e.message}", e)
                null
            }
        }
    }
}
