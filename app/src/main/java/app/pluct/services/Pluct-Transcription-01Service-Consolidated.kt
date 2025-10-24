/**
 * Pluct-Transcription-01Service-Consolidated - Consolidated transcription service
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */
package app.pluct.services

import android.content.Intent
import android.util.Log
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import java.util.*

@Singleton
class PluctTranscriptionService @Inject constructor(
    private val businessEngineService: PluctBusinessEngineService
) {
    
    private val metadataExtractor = PluctMetadataExtractor()
    
    private val _transcriptionState = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val transcriptionState: StateFlow<TranscriptionState> = _transcriptionState.asStateFlow()
    
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()
    
    sealed class TranscriptionState {
        object Idle : TranscriptionState()
        object Processing : TranscriptionState()
        data class Success(val video: VideoItem) : TranscriptionState()
        data class Error(val message: String) : TranscriptionState()
    }
    
    /**
     * Handle TikTok intent (when user shares TikTok link)
     */
    suspend fun handleTikTokIntent(intent: Intent): Result<VideoItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("PluctTranscriptionService", "Handling TikTok intent")
            
            val url = extractUrlFromIntent(intent)
            if (url == null) {
                return@withContext Result.failure(Exception("No TikTok URL found in intent"))
            }
            
            Log.d("PluctTranscriptionService", "Extracted URL from intent: $url")
            
            // Process the URL
            processTikTokUrl(url, source = "intent")
        } catch (e: Exception) {
            Log.e("PluctTranscriptionService", "Failed to handle TikTok intent", e)
            Result.failure(e)
        }
    }
    
    /**
     * Handle manual URL input
     */
    suspend fun handleManualUrlInput(url: String, tier: ProcessingTier = ProcessingTier.STANDARD): Result<VideoItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("PluctTranscriptionService", "Handling manual URL input: $url with tier: $tier")
            
            // Validate URL format
            if (!isValidTikTokUrl(url)) {
                return@withContext Result.failure(Exception("Invalid TikTok URL format"))
            }
            
            // Process the URL
            processTikTokUrl(url, source = "manual", tier = tier)
        } catch (e: Exception) {
            Log.e("PluctTranscriptionService", "Failed to handle manual URL input", e)
            Result.failure(e)
        }
    }
    
    /**
     * Process TikTok URL - main entry point for ViewModel
     */
    suspend fun processTikTokUrl(url: String, source: String, tier: ProcessingTier = ProcessingTier.STANDARD): Result<VideoItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("PluctTranscriptionService", "Processing TikTok URL: $url (source: $source)")
            
            _transcriptionState.value = TranscriptionState.Processing
            
            // Step 1: Extract metadata
            Log.d("PluctTranscriptionService", "Step 1: Extracting metadata")
            val metadataResult = metadataExtractor.extractMetadata(url)
            if (metadataResult.isFailure) {
                Log.w("PluctTranscriptionService", "Metadata extraction failed, using fallback")
            }
            
            val metadata = metadataResult.getOrNull()
            Log.d("PluctTranscriptionService", "Metadata extracted: $metadata")
            
            // Step 2: Create video item
            val videoItem = VideoItem(
                id = UUID.randomUUID().toString(),
                url = url,
                title = metadata?.title ?: "TikTok Video",
                author = metadata?.author ?: "Unknown Author",
                description = metadata?.description ?: "",
                thumbnailUrl = metadata?.thumbnailUrl ?: "",
                duration = metadata?.duration?.toLong() ?: 0L,
                status = ProcessingStatus.QUEUED,
                progress = 0,
                transcript = null,
                timestamp = System.currentTimeMillis(),
                sourceUrl = url,
                tier = tier
            )
            
            // Step 3: Add to videos list
            val currentVideos = _videos.value.toMutableList()
            currentVideos.add(0, videoItem) // Add to beginning
            _videos.value = currentVideos
            
            Log.d("PluctTranscriptionService", "Video item created and added to list")
            
            // Step 4: Start transcription process
            Log.d("PluctTranscriptionService", "Step 4: Starting transcription process")
            startTranscriptionProcess(videoItem)
            
            _transcriptionState.value = TranscriptionState.Success(videoItem)
            Result.success(videoItem)
            
        } catch (e: Exception) {
            Log.e("PluctTranscriptionService", "Failed to process TikTok URL", e)
            _transcriptionState.value = TranscriptionState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    /**
     * Extract URL from intent
     */
    private fun extractUrlFromIntent(intent: Intent): String? {
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                extractTikTokUrlFromText(text)
            }
            Intent.ACTION_VIEW -> {
                intent.data?.toString()
            }
            else -> null
        }
    }
    
    /**
     * Extract TikTok URL from text
     */
    private fun extractTikTokUrlFromText(text: String?): String? {
        if (text == null) return null
        
        val tiktokPattern = Regex("https?://(?:vm\\.tiktok\\.com|www\\.tiktok\\.com|tiktok\\.com)/[\\w\\-]+/?")
        return tiktokPattern.find(text)?.value
    }
    
    /**
     * Validate TikTok URL
     */
    private fun isValidTikTokUrl(url: String): Boolean {
        return url.matches(Regex("https?://(?:vm\\.tiktok\\.com|www\\.tiktok\\.com|tiktok\\.com)/[\\w\\-]+/?"))
    }
    
    /**
     * Start the transcription process
     */
    private fun startTranscriptionProcess(videoItem: VideoItem) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("PluctTranscriptionService", "🎯 Starting REAL transcription for video: ${videoItem.id}")
                
                // Update status to processing
                updateVideoStatus(videoItem.id, ProcessingStatus.PROCESSING)
                
                // Step 1: Get credit balance from Business Engine
                Log.d("PluctTranscriptionService", "Step 1: Checking credit balance with Business Engine")
                val balanceResult = businessEngineService.getCreditBalance()
                if (balanceResult.isFailure) {
                    val error = balanceResult.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("PluctTranscriptionService", "❌ Credit balance check failed: $error")
                    throw Exception("Failed to get credit balance: $error")
                }
                
                val balance = balanceResult.getOrNull()?.balance ?: 0
                Log.d("PluctTranscriptionService", "✅ Credit balance: $balance")
                
                if (balance <= 0) {
                    Log.e("PluctTranscriptionService", "❌ Insufficient credits: $balance")
                    throw Exception("Insufficient credits: $balance")
                }
                
                // Step 2: Vend token from Business Engine
                Log.d("PluctTranscriptionService", "Step 2: Vending token from Business Engine")
                val tokenResult = businessEngineService.vendToken()
                if (tokenResult.isFailure) {
                    val error = tokenResult.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("PluctTranscriptionService", "❌ Token vending failed: $error")
                    throw Exception("Failed to vend token: $error")
                }
                
                val vendResponse = tokenResult.getOrNull()!!
                val token = vendResponse.token
                val balanceAfter = vendResponse.balanceAfter
                Log.d("PluctTranscriptionService", "✅ Token vended successfully, balance after: $balanceAfter")
                
                // Step 3: Submit transcription job to Business Engine
                Log.d("PluctTranscriptionService", "Step 3: Submitting transcription job to Business Engine")
                val jobResult = businessEngineService.submitTranscriptionJob(videoItem.url, token)
                if (jobResult.isFailure) {
                    val error = jobResult.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("PluctTranscriptionService", "❌ Job submission failed: $error")
                    throw Exception("Failed to submit transcription job: $error")
                }
                
                val jobResponse = jobResult.getOrNull()!!
                val jobId = jobResponse.jobId
                val estimatedTime = jobResponse.estimatedTime
                Log.d("PluctTranscriptionService", "✅ Transcription job submitted: $jobId (estimated: ${estimatedTime}s)")
                
                // Step 4: Monitor job progress with Business Engine
                Log.d("PluctTranscriptionService", "Step 4: Monitoring job progress with Business Engine")
                monitorTranscriptionJob(videoItem.id, jobId, token)
                
            } catch (e: Exception) {
                Log.e("PluctTranscriptionService", "❌ Transcription process failed", e)
                updateVideoStatus(videoItem.id, ProcessingStatus.FAILED)
                _transcriptionState.value = TranscriptionState.Error(e.message ?: "Transcription failed")
            }
        }
    }
    
    /**
     * Monitor transcription job progress
     */
    private suspend fun monitorTranscriptionJob(videoId: String, jobId: String, token: String) {
        val maxAttempts = 160 // 160 seconds timeout
        var attempts = 0
        
        Log.d("PluctTranscriptionService", "🔄 Starting job monitoring for $jobId (max ${maxAttempts}s)")
        
        while (attempts < maxAttempts) {
            try {
                Log.d("PluctTranscriptionService", "🔍 Checking job status with Business Engine (attempt ${attempts + 1}/$maxAttempts)")
                
                val statusResult = businessEngineService.getJobStatus(jobId, token)
                if (statusResult.isFailure) {
                    val error = statusResult.exceptionOrNull()?.message ?: "Unknown error"
                    Log.w("PluctTranscriptionService", "⚠️ Failed to get job status: $error")
                    delay(3000)
                    attempts++
                    continue
                }
                
                val status = statusResult.getOrNull()
                val progress = status?.progress ?: 0
                Log.d("PluctTranscriptionService", "📊 Job status: ${status?.status} (progress: $progress%)")
                
                when (status?.status) {
                    "completed" -> {
                        val transcript = status.transcript ?: ""
                        val confidence = status.confidence ?: 0.0
                        val language = status.language ?: "unknown"
                        val duration = status.duration ?: 0
                        
                        Log.d("PluctTranscriptionService", "✅ Transcription completed successfully!")
                        Log.d("PluctTranscriptionService", "📝 Transcript length: ${transcript.length} chars")
                        Log.d("PluctTranscriptionService", "🎯 Confidence: $confidence, Language: $language, Duration: ${duration}s")
                        
                        updateVideoTranscript(videoId, transcript)
                        updateVideoStatus(videoId, ProcessingStatus.COMPLETED)
                        _transcriptionState.value = TranscriptionState.Idle
                        return
                    }
                    "failed" -> {
                        Log.e("PluctTranscriptionService", "❌ Transcription job failed")
                        updateVideoStatus(videoId, ProcessingStatus.FAILED)
                        _transcriptionState.value = TranscriptionState.Error("Transcription failed")
                        return
                    }
                    "queued" -> {
                        Log.d("PluctTranscriptionService", "⏳ Job queued, waiting...")
                        delay(3000)
                        attempts++
                    }
                    "processing" -> {
                        Log.d("PluctTranscriptionService", "🔄 Job processing (${progress}%), waiting...")
                        delay(3000)
                        attempts++
                    }
                    else -> {
                        Log.d("PluctTranscriptionService", "⏳ Job status: ${status?.status}, waiting...")
                        delay(3000)
                        attempts++
                    }
                }
                
            } catch (e: Exception) {
                Log.e("PluctTranscriptionService", "❌ Error monitoring job", e)
                delay(3000)
                attempts++
            }
        }
        
        // Timeout reached
        Log.e("PluctTranscriptionService", "⏰ Transcription timed out after $maxAttempts attempts (${maxAttempts * 3}s)")
        updateVideoStatus(videoId, ProcessingStatus.FAILED)
        _transcriptionState.value = TranscriptionState.Error("Transcription timed out after ${maxAttempts * 3} seconds")
    }
    
    /**
     * Update video status
     */
    private fun updateVideoStatus(videoId: String, status: ProcessingStatus) {
        val currentVideos = _videos.value.toMutableList()
        val index = currentVideos.indexOfFirst { it.id == videoId }
        if (index != -1) {
            currentVideos[index] = currentVideos[index].copy(status = status)
            _videos.value = currentVideos
            Log.d("PluctTranscriptionService", "Updated video $videoId status to $status")
        }
    }
    
    /**
     * Update video transcript
     */
    private fun updateVideoTranscript(videoId: String, transcript: String) {
        val currentVideos = _videos.value.toMutableList()
        val index = currentVideos.indexOfFirst { it.id == videoId }
        if (index != -1) {
            currentVideos[index] = currentVideos[index].copy(transcript = transcript)
            _videos.value = currentVideos
            Log.d("PluctTranscriptionService", "Updated video $videoId with transcript (${transcript.length} chars)")
        }
    }
}
