/**
 * Pluct-Transcription-01Service-00Orchestrator - Main transcription service orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

package app.pluct.services

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingStatus
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Transcription-01Service-00Orchestrator - Main transcription service orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@Singleton
class PluctTranscriptionServiceOrchestrator @Inject constructor(
    private val coreService: PluctTranscriptionServiceCore,
    private val monitoringService: PluctTranscriptionServiceMonitoring
) {
    
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
            Log.d("PluctTranscriptionServiceOrchestrator", "Handling TikTok intent")
            
            val url = extractUrlFromIntent(intent)
            if (url == null) {
                return@withContext Result.failure(Exception("No TikTok URL found in intent"))
            }
            
            Log.d("PluctTranscriptionServiceOrchestrator", "Extracted URL from intent: $url")
            
            // Process the URL
            processTikTokUrl(url, source = "intent")
        } catch (e: Exception) {
            Log.e("PluctTranscriptionServiceOrchestrator", "Failed to handle TikTok intent", e)
            Result.failure(e)
        }
    }
    
    /**
     * Handle manual URL input
     */
    suspend fun handleManualUrlInput(url: String): Result<VideoItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("PluctTranscriptionServiceOrchestrator", "Handling manual URL input: $url")
            
            // Validate URL format
            if (!isValidTikTokUrl(url)) {
                return@withContext Result.failure(Exception("Invalid TikTok URL format"))
            }
            
            // Process the URL
            processTikTokUrl(url, source = "manual")
        } catch (e: Exception) {
            Log.e("PluctTranscriptionServiceOrchestrator", "Failed to handle manual URL input", e)
            Result.failure(e)
        }
    }
    
    /**
     * Process TikTok URL - main entry point for ViewModel
     */
    suspend fun processTikTokUrl(url: String, source: String): Result<VideoItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("PluctTranscriptionServiceOrchestrator", "Processing TikTok URL: $url (source: $source)")
            
            _transcriptionState.value = TranscriptionState.Processing
            
            // Step 1: Extract metadata
            Log.d("PluctTranscriptionServiceOrchestrator", "Step 1: Extracting metadata")
            val metadataResult = coreService.extractMetadata(url)
            if (metadataResult.isFailure) {
                Log.w("PluctTranscriptionServiceOrchestrator", "Metadata extraction failed, using fallback")
            }
            
            val metadata = metadataResult.getOrNull()
            Log.d("PluctTranscriptionServiceOrchestrator", "Metadata extracted: $metadata")
            
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
                sourceUrl = url
            )
            
            // Step 3: Add to videos list
            val currentVideos = _videos.value.toMutableList()
            currentVideos.add(0, videoItem) // Add to beginning
            _videos.value = currentVideos
            
            Log.d("PluctTranscriptionServiceOrchestrator", "Video item created and added to list")
            
            // Step 4: Start transcription process
            Log.d("PluctTranscriptionServiceOrchestrator", "Step 4: Starting transcription process")
            startTranscriptionProcess(videoItem)
            
            _transcriptionState.value = TranscriptionState.Success(videoItem)
            Result.success(videoItem)
            
        } catch (e: Exception) {
            Log.e("PluctTranscriptionServiceOrchestrator", "Failed to process TikTok URL", e)
            _transcriptionState.value = TranscriptionState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    /**
     * Start the transcription process
     */
    private fun startTranscriptionProcess(videoItem: VideoItem) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("PluctTranscriptionServiceOrchestrator", "🎯 Starting REAL transcription for video: ${videoItem.id}")
                
                // Update status to processing
                updateVideoStatus(videoItem.id, ProcessingStatus.PROCESSING)
                
                // Start monitoring
                monitoringService.startMonitoring(videoItem.id)
                
                // Process with core service
                val result = coreService.processTranscription(videoItem)
                
                if (result.isSuccess) {
                    Log.d("PluctTranscriptionServiceOrchestrator", "✅ Transcription completed successfully")
                    updateVideoStatus(videoItem.id, ProcessingStatus.COMPLETED)
                    updateVideoTranscript(videoItem.id, result.getOrNull()?.transcript ?: "")
                } else {
                    Log.e("PluctTranscriptionServiceOrchestrator", "❌ Transcription failed")
                    updateVideoStatus(videoItem.id, ProcessingStatus.FAILED)
                }
                
                // Stop monitoring
                monitoringService.stopMonitoring(videoItem.id)
                
            } catch (e: Exception) {
                Log.e("PluctTranscriptionServiceOrchestrator", "❌ Transcription process failed", e)
                updateVideoStatus(videoItem.id, ProcessingStatus.FAILED)
                _transcriptionState.value = TranscriptionState.Error(e.message ?: "Transcription failed")
            }
        }
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
            Log.d("PluctTranscriptionServiceOrchestrator", "Updated video $videoId status to $status")
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
            Log.d("PluctTranscriptionServiceOrchestrator", "Updated video $videoId with transcript (${transcript.length} chars)")
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
}
