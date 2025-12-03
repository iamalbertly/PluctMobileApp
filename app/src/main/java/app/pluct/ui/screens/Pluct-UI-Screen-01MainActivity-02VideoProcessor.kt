package app.pluct.ui.screens

import android.util.Log
import android.content.ClipData
import android.content.ClipboardManager
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.MetadataResponse
import app.pluct.core.error.PluctCoreError03UserMessageFormatter

/**
 * Pluct-UI-Screen-01MainActivity-02VideoProcessor - Video processing logic
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Single source of truth for video processing operations
 */
object PluctUIScreen01MainActivityVideoProcessor {
    
    /**
     * Load initial data from API
     */
    suspend fun loadInitialData(
        apiService: PluctCoreAPIUnifiedService,
        userIdentification: app.pluct.services.PluctCoreUserIdentification,
        onDataLoaded: (Int, Int) -> Unit
    ) {
        try {
            Log.d("VideoProcessor", "Loading initial data...")
            
            // Get credit balance
            val balanceResult = apiService.checkUserBalance()
            balanceResult.fold(
                onSuccess = { balance ->
                    Log.d("VideoProcessor", "REAL credit balance loaded: ${balance.balance} for user: ${userIdentification.userId}")
                    onDataLoaded(balance.balance, 3) // Default free uses
                },
                onFailure = { error ->
                    Log.e("VideoProcessor", "Failed to load credit balance: ${error.message}")
                    onDataLoaded(0, 3) // Fallback values
                }
            )
        } catch (e: Exception) {
            Log.e("VideoProcessor", "Error loading initial data: ${e.message}")
            onDataLoaded(0, 3) // Fallback values
        }
    }
    
    /**
     * Process video with the selected tier
     */
    suspend fun processVideo(
        apiService: PluctCoreAPIUnifiedService,
        url: String,
        tier: ProcessingTier,
        currentBalance: Int,
        currentFreeUses: Int,
        videoRepository: PluctVideoRepository,
        clipboardManager: ClipboardManager,
        onResult: (Boolean, Int, Int, String?, Throwable?) -> Unit
    ) {
        try {
            Log.d("VideoProcessor", "Processing video: $url with tier: $tier")
            
            // Check if user has enough credits/free uses
            val hasEnoughCredits = when (tier) {
                ProcessingTier.EXTRACT_SCRIPT -> currentFreeUses > 0 || currentBalance >= 1
                ProcessingTier.GENERATE_INSIGHTS -> currentBalance >= 2
                else -> false // Other tiers not supported yet
            }
            
            if (!hasEnoughCredits) {
                val technicalMsg = when (tier) {
                    ProcessingTier.EXTRACT_SCRIPT -> "Insufficient credits. You need 1 credit or a free use remaining."
                    ProcessingTier.GENERATE_INSIGHTS -> "Insufficient credits. You need 2 credits for AI Insights."
                    else -> "Insufficient credits for this tier."
                }
                Log.w("VideoProcessor", "Insufficient credits for tier: $tier")
                val userMessage = PluctCoreError03UserMessageFormatter.formatUserMessage(
                    error = null,
                    technicalMessage = technicalMsg,
                    errorCode = "INSUFFICIENT_CREDITS",
                    httpStatus = 402,
                    context = "video processing"
                )
                
                // Create a detailed error for the handler
                val error = app.pluct.services.PluctCoreAPIDetailedError(
                    userMessage = userMessage.message,
                    technicalDetails = app.pluct.services.TechnicalErrorDetails(
                        serviceName = "VideoProcessor",
                        operation = "Check Credits",
                        endpoint = "local",
                        requestMethod = "CHECK",
                        requestUrl = "local",
                        responseStatusCode = 402,
                        errorCode = "INSUFFICIENT_CREDITS"
                    )
                )
                
                onResult(false, currentBalance, currentFreeUses, userMessage.message, error)
                return
            }
            
            // Check for existing processing video to prevent duplicates
            val existingProcessing = videoRepository.getProcessingVideoByUrl(url)
            val urlHistoryItem = if (existingProcessing != null) {
                Log.d("VideoProcessor", "Found existing processing video for URL: $url, updating instead of creating duplicate")
                existingProcessing.copy(
                    status = ProcessingStatus.PROCESSING,
                    progress = 0,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                // Check for any existing video with this URL
                val existingVideo = videoRepository.getVideoByUrl(url)
                if (existingVideo != null) {
                    Log.d("VideoProcessor", "Found existing video for URL: $url, updating status to PROCESSING")
                    existingVideo.copy(
                        status = ProcessingStatus.PROCESSING,
                        progress = 0,
                        timestamp = System.currentTimeMillis()
                    )
                } else {
                    // Create new video item
                    val videoId = System.currentTimeMillis().toString()
                    VideoItem(
                        id = videoId,
                        url = url,
                        title = "", // Not storing full video data
                        thumbnailUrl = "",
                        author = "",
                        duration = 0L,
                        status = ProcessingStatus.PROCESSING,
                        progress = 0,
                        transcript = null, // Not storing transcript in database
                        timestamp = System.currentTimeMillis(),
                        tier = tier,
                        createdAt = System.currentTimeMillis()
                    )
                }
            }
            
            // Store URL history for dropdown (minimal data only) - use REPLACE to update existing
            val insertResult = videoRepository.insertVideo(urlHistoryItem)
            if (insertResult.isFailure) {
                val dbError = insertResult.exceptionOrNull()
                Log.w("VideoProcessor", "Failed to save URL history (non-critical): ${dbError?.message}")
                // Don't fail the entire operation if history save fails
            } else {
                Log.d("VideoProcessor", "URL history saved/updated for: $url")
            }
            
            // Fetch metadata to populate title and author
            val metadataResult = apiService.getMetadata(url)
            var currentVideoItem = urlHistoryItem
            
            if (metadataResult.isSuccess) {
                val metadata = metadataResult.getOrNull()
                if (metadata != null) {
                    Log.d("VideoProcessor", "Metadata fetched: ${metadata.title}")
                    currentVideoItem = currentVideoItem.copy(
                        title = metadata.title ?: "",
                        author = metadata.author ?: ""
                    )
                    videoRepository.updateVideo(currentVideoItem)
                }
            } else {
                Log.w("VideoProcessor", "Failed to fetch metadata: ${metadataResult.exceptionOrNull()?.message}")
            }
            
            // Use complete processTikTokVideo flow which handles metadata, vending, submission, and polling
            // This ensures the transcript is retrieved and can be saved
            Log.d("VideoProcessor", "Starting complete transcription flow with processTikTokVideo...")
            val transcriptionResult = apiService.processTikTokVideo(url)
            
            transcriptionResult.fold(
                onSuccess = { statusResponse ->
                    Log.d("VideoProcessor", "Transcription completed successfully")
                    
                    // Update video item with completed status and transcript
                    val completedItem = currentVideoItem.copy(
                        status = ProcessingStatus.COMPLETED,
                        progress = 100,
                        transcript = statusResponse.transcript,
                        duration = (statusResponse.duration ?: 0).toLong()
                    )
                    videoRepository.updateVideo(completedItem)
                    
                    // AUTO-COPY TO CLIPBOARD (Simplicity)
                    statusResponse.transcript?.let { transcript ->
                        try {
                            val clipData = ClipData.newPlainText("Pluct Transcript", transcript)
                            clipboardManager.setPrimaryClip(clipData)
                            Log.d("VideoProcessor", "✅ Transcript auto-copied to clipboard (${transcript.length} chars)")
                        } catch (e: Exception) {
                            Log.w("VideoProcessor", "Failed to auto-copy to clipboard: ${e.message}")
                        }
                    }
                    
                    // Update credits based on tier
                    val newBalance = when (tier) {
                        ProcessingTier.EXTRACT_SCRIPT -> if (currentFreeUses > 0) currentBalance else currentBalance - 1
                        ProcessingTier.GENERATE_INSIGHTS -> currentBalance - 2
                        else -> currentBalance
                    }
                    val newFreeUses = if (tier == ProcessingTier.EXTRACT_SCRIPT && currentFreeUses > 0) currentFreeUses - 1 else currentFreeUses
                    
                    Log.d("VideoProcessor", "Transcript saved: ${statusResponse.transcript?.length ?: 0} characters")
                    
                    // Check for low balance warning
                    var successMessage: String? = null
                    if (newBalance <= 2 && newBalance > 0) {
                        successMessage = "Transcription complete. Warning: You only have $newBalance credits left."
                    }
                    
                    onResult(true, newBalance, newFreeUses, successMessage, null)
                },
                onFailure = { error ->
                    Log.e("VideoProcessor", "Transcription failed: ${error.message}")
                    
                    // Update video item with failure status
                    videoRepository.updateVideo(currentVideoItem.copy(
                        status = ProcessingStatus.FAILED,
                        failureReason = "Transcription failed: ${error.message}"
                    ))
                    
                    val isAuth = error.message?.contains("auth", ignoreCase = true) == true || error.message?.contains("401") == true
                    val costHint = if (tier == ProcessingTier.EXTRACT_SCRIPT) {
                        if (currentFreeUses > 0) "This attempt uses a free credit (remaining: $currentFreeUses)." else "This attempt costs 1 credit (balance: $currentBalance)."
                    } else {
                        "This attempt costs 2 credits (balance: $currentBalance)."
                    }
                    val friendly = when {
                        isAuth -> "We couldn’t authenticate with the transcription service. $costHint Please retry."
                        else -> null
                    }

                    val userMessage = PluctCoreError03UserMessageFormatter.formatUserMessage(
                        error = error,
                        technicalMessage = error.message,
                        errorCode = "TRANSCRIPTION_FAILED",
                        context = "complete transcription flow"
                    )
                    onResult(false, currentBalance, currentFreeUses, friendly ?: userMessage.message, error)
                }
            )
        } catch (e: Exception) {
            Log.e("VideoProcessor", "Error processing video: ${e.message}", e)
            val userMessage = PluctCoreError03UserMessageFormatter.formatUserMessage(
                error = e,
                technicalMessage = e.message,
                errorCode = "PROCESSING_ERROR",
                context = "video processing"
            )
            onResult(false, currentBalance, currentFreeUses, userMessage.message, e)
        }
    }
}






