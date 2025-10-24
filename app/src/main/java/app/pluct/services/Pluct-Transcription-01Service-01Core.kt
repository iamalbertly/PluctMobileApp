/**
 * Pluct-Transcription-01Service-01Core - Core transcription service functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

package app.pluct.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Transcription-01Service-01Core - Core transcription service functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@Singleton
class PluctTranscriptionServiceCore @Inject constructor(
    private val metadataExtractor: PluctMetadataExtractor,
    private val businessEngineService: PluctBusinessEngineService
) {
    
    /**
     * Extract metadata from TikTok URL
     */
    suspend fun extractMetadata(url: String): Result<PluctMetadataExtractor.TikTokMetadata> = withContext(Dispatchers.IO) {
        try {
            Log.d("PluctTranscriptionServiceCore", "Extracting metadata from URL: $url")
            val result = metadataExtractor.extractMetadata(url)
            if (result.isSuccess) {
                Log.d("PluctTranscriptionServiceCore", "Metadata extracted successfully")
                Result.success(result.getOrNull()!!)
            } else {
                Log.e("PluctTranscriptionServiceCore", "Failed to extract metadata")
                Result.failure(result.exceptionOrNull()!!)
            }
        } catch (e: Exception) {
            Log.e("PluctTranscriptionServiceCore", "Failed to extract metadata", e)
            Result.failure(e)
        }
    }
    
    /**
     * Process transcription for a video item
     */
    suspend fun processTranscription(videoItem: VideoItem): Result<TranscriptionResult> = withContext(Dispatchers.IO) {
        try {
            Log.d("PluctTranscriptionServiceCore", "🎯 Starting REAL transcription for video: ${videoItem.id}")
            
            // Step 1: Get credit balance from Business Engine
            Log.d("PluctTranscriptionServiceCore", "Step 1: Checking credit balance with Business Engine")
            val balanceResult = businessEngineService.getCreditBalance()
            if (balanceResult.isFailure) {
                val error = balanceResult.exceptionOrNull()?.message ?: "Unknown error"
                Log.e("PluctTranscriptionServiceCore", "❌ Credit balance check failed: $error")
                throw Exception("Failed to get credit balance: $error")
            }
            
            val balance = balanceResult.getOrNull()?.balance ?: 0
            Log.d("PluctTranscriptionServiceCore", "✅ Credit balance: $balance")
            
            if (balance <= 0) {
                Log.e("PluctTranscriptionServiceCore", "❌ Insufficient credits: $balance")
                throw Exception("Insufficient credits: $balance")
            }
            
            // Step 2: Vend token from Business Engine
            Log.d("PluctTranscriptionServiceCore", "Step 2: Vending token from Business Engine")
            val tokenResult = businessEngineService.vendToken()
            if (tokenResult.isFailure) {
                val error = tokenResult.exceptionOrNull()?.message ?: "Unknown error"
                Log.e("PluctTranscriptionServiceCore", "❌ Token vending failed: $error")
                throw Exception("Failed to vend token: $error")
            }
            
            val vendResponse = tokenResult.getOrNull()!!
            val token = vendResponse.token
            val balanceAfter = vendResponse.balanceAfter
            Log.d("PluctTranscriptionServiceCore", "✅ Token vended successfully, balance after: $balanceAfter")
            
            // Step 3: Submit transcription job to Business Engine
            Log.d("PluctTranscriptionServiceCore", "Step 3: Submitting transcription job to Business Engine")
            val jobResult = businessEngineService.submitTranscriptionJob(videoItem.url, token)
            if (jobResult.isFailure) {
                val error = jobResult.exceptionOrNull()?.message ?: "Unknown error"
                Log.e("PluctTranscriptionServiceCore", "❌ Job submission failed: $error")
                throw Exception("Failed to submit transcription job: $error")
            }
            
            val jobResponse = jobResult.getOrNull()!!
            val jobId = jobResponse.jobId
            val estimatedTime = jobResponse.estimatedTime
            Log.d("PluctTranscriptionServiceCore", "✅ Transcription job submitted: $jobId (estimated: ${estimatedTime}s)")
            
            // Step 4: Monitor job progress with Business Engine
            Log.d("PluctTranscriptionServiceCore", "Step 4: Monitoring job progress with Business Engine")
            val transcript = monitorTranscriptionJob(jobId, token)
            
            Log.d("PluctTranscriptionServiceCore", "✅ Transcription completed successfully!")
            Log.d("PluctTranscriptionServiceCore", "📝 Transcript length: ${transcript.length} chars")
            
            Result.success(TranscriptionResult(transcript, 0.95, "en", 30))
            
        } catch (e: Exception) {
            Log.e("PluctTranscriptionServiceCore", "❌ Transcription process failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Monitor transcription job progress
     */
    private suspend fun monitorTranscriptionJob(jobId: String, token: String): String {
        val maxAttempts = 160 // 160 seconds timeout
        var attempts = 0
        
        Log.d("PluctTranscriptionServiceCore", "🔄 Starting job monitoring for $jobId (max ${maxAttempts}s)")
        
        while (attempts < maxAttempts) {
            try {
                Log.d("PluctTranscriptionServiceCore", "🔍 Checking job status with Business Engine (attempt ${attempts + 1}/$maxAttempts)")
                
                val statusResult = businessEngineService.getJobStatus(jobId, token)
                if (statusResult.isFailure) {
                    val error = statusResult.exceptionOrNull()?.message ?: "Unknown error"
                    Log.w("PluctTranscriptionServiceCore", "⚠️ Failed to get job status: $error")
                    delay(3000)
                    attempts++
                    continue
                }
                
                val status = statusResult.getOrNull()
                val progress = status?.progress ?: 0
                Log.d("PluctTranscriptionServiceCore", "📊 Job status: ${status?.status} (progress: $progress%)")
                
                when (status?.status) {
                    "completed" -> {
                        val transcript = status.transcript ?: ""
                        val confidence = status.confidence ?: 0.0
                        val language = status.language ?: "unknown"
                        val duration = status.duration ?: 0
                        
                        Log.d("PluctTranscriptionServiceCore", "✅ Transcription completed successfully!")
                        Log.d("PluctTranscriptionServiceCore", "📝 Transcript length: ${transcript.length} chars")
                        Log.d("PluctTranscriptionServiceCore", "🎯 Confidence: $confidence, Language: $language, Duration: ${duration}s")
                        
                        return transcript
                    }
                    "failed" -> {
                        Log.e("PluctTranscriptionServiceCore", "❌ Transcription job failed")
                        throw Exception("Transcription job failed")
                    }
                    "queued" -> {
                        Log.d("PluctTranscriptionServiceCore", "⏳ Job queued, waiting...")
                        delay(3000)
                        attempts++
                    }
                    "processing" -> {
                        Log.d("PluctTranscriptionServiceCore", "🔄 Job processing (${progress}%), waiting...")
                        delay(3000)
                        attempts++
                    }
                    else -> {
                        Log.d("PluctTranscriptionServiceCore", "⏳ Job status: ${status?.status}, waiting...")
                        delay(3000)
                        attempts++
                    }
                }
                
            } catch (e: Exception) {
                Log.e("PluctTranscriptionServiceCore", "❌ Error monitoring job", e)
                delay(3000)
                attempts++
            }
        }
        
        // Timeout reached
        Log.e("PluctTranscriptionServiceCore", "⏰ Transcription timed out after $maxAttempts attempts (${maxAttempts * 3}s)")
        throw Exception("Transcription timed out after ${maxAttempts * 3} seconds")
    }
    
    /**
     * Transcription result data class
     */
    data class TranscriptionResult(
        val transcript: String,
        val confidence: Double,
        val language: String,
        val duration: Int
    )
}
