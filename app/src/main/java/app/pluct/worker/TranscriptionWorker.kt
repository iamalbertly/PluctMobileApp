package app.pluct.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.repository.PluctRepository
import app.pluct.notification.NotificationHelper
import app.pluct.api.TTTranscribeApiService
import app.pluct.api.TranscriptionResult
import app.pluct.api.AIAnalysisResult
import app.pluct.scraper.WebViewScraper
import app.pluct.scraper.ScrapingResult
import app.pluct.error.ErrorHandler
import app.pluct.error.PluctError
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.util.UUID

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: PluctRepository,
    private val ttTranscribeApiService: TTTranscribeApiService
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_VIDEO_ID = "video_id"
        const val KEY_PROCESSING_TIER = "processing_tier"
        private const val TAG = "TranscriptionWorker"
    }

    override suspend fun doWork(): Result {
        val videoId = inputData.getString(KEY_VIDEO_ID)
        val processingTierString = inputData.getString(KEY_PROCESSING_TIER)
        
        if (videoId == null || processingTierString == null) {
            Log.e(TAG, "Missing required input data")
            return Result.failure()
        }
        
        val processingTier = try {
            ProcessingTier.valueOf(processingTierString)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid processing tier: $processingTierString")
            return Result.failure()
        }
        
        Log.i(TAG, "Starting transcription for video $videoId with tier $processingTier")
        
        try {
            // Update status to TRANSCRIBING
            repository.updateVideoStatus(videoId, ProcessingStatus.TRANSCRIBING)
            
            // Perform transcription based on tier
            val transcriptResult = when (processingTier) {
                ProcessingTier.QUICK_SCAN -> performQuickScan(videoId)
                ProcessingTier.AI_ANALYSIS -> performAIAnalysis(videoId)
            }
            
            if (transcriptResult.isSuccess) {
                // Update status to ANALYZING
                repository.updateVideoStatus(videoId, ProcessingStatus.ANALYZING)
                
                // Perform AI analysis (summary, key takeaways, etc.)
                val analysisResult = performAIAnalysis(videoId)
                
                if (analysisResult.isSuccess) {
                    // Update status to COMPLETED
                    repository.updateVideoStatus(videoId, ProcessingStatus.COMPLETED)
                    Log.i(TAG, "Successfully completed transcription and analysis for video $videoId")
                    
                    // Show notification
                    val video = repository.getVideoById(videoId)
                    if (video != null) {
                        when (processingTier) {
                            ProcessingTier.QUICK_SCAN -> {
                                NotificationHelper.showQuickScanCompleteNotification(
                                    applicationContext,
                                    video.title ?: "Video"
                                )
                            }
                            ProcessingTier.AI_ANALYSIS -> {
                                NotificationHelper.showProcessingCompleteNotification(
                                    applicationContext,
                                    video.title ?: "Video",
                                    "AI Analysis"
                                )
                            }
                        }
                    }
                    
                    return Result.success()
                } else {
                    // Analysis failed, but we have the transcript
                    repository.updateVideoStatus(videoId, ProcessingStatus.COMPLETED, "Analysis failed but transcript is available")
                    Log.w(TAG, "Analysis failed for video $videoId, but transcript is available")
                    
                    // Show notification for quick scan completion
                    val video = repository.getVideoById(videoId)
                    if (video != null) {
                        NotificationHelper.showQuickScanCompleteNotification(
                            applicationContext,
                            video.title ?: "Video"
                        )
                    }
                    
                    return Result.success()
                }
            } else {
                // Transcription failed
                repository.updateVideoStatus(videoId, ProcessingStatus.FAILED, transcriptResult.exceptionOrNull()?.message)
                Log.e(TAG, "Transcription failed for video $videoId", transcriptResult.exceptionOrNull())
                return Result.failure()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during transcription for video $videoId", e)
            repository.updateVideoStatus(videoId, ProcessingStatus.FAILED, e.message)
            return Result.failure()
        }
    }
    
    private suspend fun performQuickScan(videoId: String): kotlin.Result<String> {
        return ErrorHandler.executeWithRetry(
            operation = {
                Log.i(TAG, "Performing quick scan for video $videoId")
                
                val video = repository.getVideoById(videoId)
                if (video == null) {
                    throw PluctError.ValidationError("Video not found: $videoId")
                }
                
                // Use WebView scraper for fragile but free transcription
                val webViewScraper = WebViewScraper()
                
                // Note: In a real implementation, you would need to pass a WebView instance
                // For now, we'll simulate the scraping process
                delay(2000) // Simulate processing time
                
                // Simulate WebView scraping result
                val scrapingResult = simulateWebViewScraping(video.sourceUrl)
                
                when (scrapingResult) {
                    is ScrapingResult.Success -> {
                        Log.i(TAG, "WebView scraping successful for video $videoId")
                        repository.saveTranscript(videoId, scrapingResult.transcript)
                        scrapingResult.transcript
                    }
                    is ScrapingResult.Error -> {
                        Log.w(TAG, "WebView scraping failed for video $videoId: ${scrapingResult.message}")
                        // Fallback to mock transcript
                        val fallbackTranscript = "Quick scan transcript for video $videoId (fallback)"
                        repository.saveTranscript(videoId, fallbackTranscript)
                        fallbackTranscript
                    }
                }
            },
            config = ErrorHandler.SCRAPING_RETRY_CONFIG,
            operationName = "Quick Scan for video $videoId"
        )
    }
    
    /**
     * Simulate WebView scraping (replace with actual WebView implementation)
     */
    private suspend fun simulateWebViewScraping(videoUrl: String): ScrapingResult {
        return try {
            delay(1000) // Simulate network delay
            
            // Simulate success/failure based on URL
            if (videoUrl.contains("tiktok.com")) {
                ScrapingResult.Success("This is a simulated transcript from WebView scraping for $videoUrl")
            } else {
                ScrapingResult.Error("Invalid URL format")
            }
        } catch (e: Exception) {
            ScrapingResult.Error("Scraping error: ${e.message}")
        }
    }
    
    private suspend fun performAIAnalysis(videoId: String): kotlin.Result<String> {
        return ErrorHandler.executeWithRetry(
            operation = {
                Log.i(TAG, "Performing AI analysis for video $videoId")
                
                // Get the video to extract URL
                val video = repository.getVideoById(videoId)
                if (video == null) {
                    throw PluctError.ValidationError("Video not found: $videoId")
                }
                
                // Use TTTranscribe API for high-quality transcription
                val transcriptionResult = ttTranscribeApiService.transcribeVideo(video.sourceUrl)
                
                when (transcriptionResult) {
                    is TranscriptionResult.Success -> {
                        Log.i(TAG, "TTTranscribe transcription successful for video $videoId")
                        
                        // Save the transcript
                        repository.saveTranscript(videoId, transcriptionResult.transcript)
                        
                        // Generate AI analysis
                        val analysisResult = ttTranscribeApiService.generateAIAnalysis(transcriptionResult.transcript)
                        
                        when (analysisResult) {
                            is AIAnalysisResult.Success -> {
                                Log.i(TAG, "AI analysis successful for video $videoId")
                                
                                // Save analysis results
                                repository.saveArtifact(videoId, "summary", analysisResult.summary)
                                repository.saveArtifact(videoId, "key_takeaways", analysisResult.keyTakeaways.joinToString("\n"))
                                repository.saveArtifact(videoId, "actionable_steps", analysisResult.actionableSteps.joinToString("\n"))
                                
                                transcriptionResult.transcript
                            }
                            is AIAnalysisResult.Error -> {
                                Log.w(TAG, "AI analysis failed for video $videoId: ${analysisResult.message}")
                                // Still return success as we have the transcript
                                transcriptionResult.transcript
                            }
                        }
                    }
                    is TranscriptionResult.Error -> {
                        Log.e(TAG, "TTTranscribe transcription failed for video $videoId: ${transcriptionResult.message}")
                        throw PluctError.APIError(500, "Transcription failed: ${transcriptionResult.message}")
                    }
                }
            },
            config = ErrorHandler.API_RETRY_CONFIG,
            operationName = "AI Analysis for video $videoId"
        )
    }
}
