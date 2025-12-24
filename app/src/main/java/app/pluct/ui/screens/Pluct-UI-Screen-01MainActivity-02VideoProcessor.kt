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
        debugLogManager: app.pluct.core.debug.PluctCoreDebug01LogManager?,
        onDataLoaded: (Int, Int) -> Unit
    ) {
        try {
            Log.d("VideoProcessor", "Loading initial data...")
            debugLogManager?.logInfo(
                category = "CREDIT_CHECK",
                operation = "loadInitialData",
                message = "Starting credit balance load",
                details = "User: ${userIdentification.userId}"
            )
            
            // Get credit balance - sum main and bonus credits for unified display
            val balanceResult = apiService.checkUserBalance()
            balanceResult.fold(
                onSuccess = { balance ->
                    // Calculate total available credits (main + bonus), ensure non-negative
                    val totalCredits = maxOf(0, balance.main + balance.bonus)
                    Log.d("VideoProcessor", "Credit balance loaded: $totalCredits total (${balance.main} main + ${balance.bonus} bonus) for user: ${userIdentification.userId}")
                    debugLogManager?.logInfo(
                        category = "CREDIT_CHECK",
                        operation = "loadInitialData",
                        message = "Credit balance loaded",
                        details = "Total=$totalCredits; Main=${balance.main}; Bonus=${balance.bonus}; User=${userIdentification.userId}",
                        requestUrl = "https://pluct-business-engine.romeo-lya2.workers.dev/v1/credits/balance",
                        requestMethod = "GET"
                    )
                    
                    // Log if credits are invalid/negative with full API details
                    if (balance.main < 0 || balance.bonus < 0) {
                        debugLogManager?.logWarning(
                            category = "CREDIT_CHECK",
                            operation = "loadInitialData",
                            message = "Received negative credit values from API",
                            details = buildString {
                                appendLine("USER CONTEXT:")
                                appendLine("  User ID: ${userIdentification.userId}")
                                appendLine()
                                appendLine("API RESPONSE:")
                                appendLine("  Main Credits: ${balance.main}")
                                appendLine("  Bonus Credits: ${balance.bonus}")
                                appendLine("  Total (calculated): $totalCredits")
                                appendLine("  Available Credits: ${balance.availableCredits}")
                                appendLine("  Held Credits: ${balance.heldCredits}")
                                appendLine("  Pending Jobs: ${balance.pendingJobs}")
                                appendLine("  Updated At: ${balance.updatedAt}")
                                appendLine()
                                appendLine("REQUEST DETAILS:")
                                appendLine("  Endpoint: /v1/credits/balance")
                                appendLine("  Method: GET")
                                appendLine("  Service: Business Engine")
                                appendLine("  Base URL: https://pluct-business-engine.romeo-lya2.workers.dev")
                                appendLine()
                                appendLine("EXPECTED BEHAVIOR:")
                                appendLine("  Both main and bonus credits should be >= 0")
                                appendLine("  Negative values indicate API error or data corruption")
                            }
                        )
                    }
                    
                    onDataLoaded(totalCredits, 3) // Default free uses
                },
                onFailure = { error ->
                    Log.e("VideoProcessor", "Failed to load credit balance: ${error.message}")
                    
                    // Log credit check failure to debug system with full details
                    debugLogManager?.logError(
                        category = "CREDIT_CHECK",
                        operation = "checkUserBalance",
                        message = "Failed to load credit balance from Business Engine",
                        exception = error,
                        requestUrl = "Business Engine API: /credits/balance",
                        requestPayload = buildString {
                            appendLine("REQUEST:")
                            appendLine("  Method: GET")
                            appendLine("  Endpoint: /v1/credits/balance")
                            appendLine("  User ID: ${userIdentification.userId}")
                            appendLine("  Base URL: https://pluct-business-engine.romeo-lya2.workers.dev")
                        },
                        responseBody = buildString {
                            appendLine("ERROR DETAILS:")
                            appendLine("  Error Type: ${error::class.simpleName}")
                            appendLine("  Error Message: ${error.message}")
                            if (error is app.pluct.services.PluctCoreAPIDetailedError) {
                                appendLine()
                                appendLine("DETAILED ERROR INFO:")
                                appendLine(error.getDetailedMessage())
                            }
                        }
                    )
                    
                    onDataLoaded(0, 3) // Fallback to 0 credits, not -1
                }
            )
        } catch (e: Exception) {
            Log.e("VideoProcessor", "Error loading initial data: ${e.message}")
            
            // Log exception to debug system with full context
            debugLogManager?.logError(
                category = "CREDIT_CHECK",
                operation = "loadInitialData",
                message = "Exception during credit balance load",
                exception = e,
                requestUrl = "Business Engine API: /credits/balance",
                requestPayload = buildString {
                    appendLine("CONTEXT:")
                    appendLine("  User ID: ${userIdentification.userId}")
                    appendLine("  Operation: Initial data load")
                    appendLine("  Endpoint: /v1/credits/balance")
                    appendLine()
                    appendLine("STACK TRACE:")
                    appendLine(e.stackTraceToString())
                }
            )
            
            onDataLoaded(0, 3) // Fallback to 0 credits, not -1
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
        debugLogManager: app.pluct.core.debug.PluctCoreDebug01LogManager?,
        onResult: (Boolean, Int, Int, String?, Throwable?) -> Unit
    ) {
        try {
            Log.d("VideoProcessor", "Processing video: $url with tier: $tier")
            debugLogManager?.logInfo(
                category = "TRANSCRIPTION",
                operation = "processVideo_start",
                message = "Starting transcription flow",
                details = buildString {
                    appendLine("URL: $url")
                    appendLine("Tier: $tier")
                    appendLine("Balance: $currentBalance")
                    appendLine("Free uses: $currentFreeUses")
                }
            )
            
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
            
            // Fetch metadata to populate title and author (fast-fail to avoid blocking UX on slow /meta)
            val metadataResult = apiService.getMetadata(url, timeoutMs = 8000L)
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
                    
                    // CRITICAL: Validate transcript exists before saving
                    val transcriptText = statusResponse.transcript?.takeIf { it.isNotBlank() }
                        ?: statusResponse.result?.transcription?.takeIf { it.isNotBlank() }
                        ?: null
                    
                    if (transcriptText == null) {
                        Log.e("VideoProcessor", "WARNING: Transcript is null or empty in response")
                        debugLogManager?.logWarning(
                            category = "TRANSCRIPTION",
                            operation = "processVideo_missing_transcript",
                            message = "Transcription completed but transcript field is empty",
                            details = "Response structure: transcript=${statusResponse.transcript != null}, result.transcription=${statusResponse.result?.transcription != null}"
                        )
                    }
                    
                    // Update video item with completed status and transcript
                    val completedItem = currentVideoItem.copy(
                        status = ProcessingStatus.COMPLETED,
                        progress = 100,
                        transcript = transcriptText, // Use validated transcript
                        duration = (statusResponse.duration ?: statusResponse.result?.duration ?: 0).toLong()
                    )
                    
                    // Use insertVideo to ensure transcript is saved (handles both insert and update)
                    val updateResult = videoRepository.insertVideo(completedItem)
                    if (updateResult.isFailure) {
                        Log.e("VideoProcessor", "Failed to save transcript: ${updateResult.exceptionOrNull()?.message}")
                    } else {
                        Log.d("VideoProcessor", "Transcript saved successfully: ${transcriptText?.length ?: 0} chars")
                    }
                    
                    // AUTO-COPY TO CLIPBOARD (Simplicity)
                    transcriptText?.let { transcript ->
                        try {
                            val clipData = ClipData.newPlainText("Pluct Transcript", transcript)
                            clipboardManager.setPrimaryClip(clipData)
                            Log.d("VideoProcessor", "Transcript auto-copied to clipboard (${transcript.length} chars)")
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
                    
                    Log.d("VideoProcessor", "Transcript saved: ${transcriptText?.length ?: 0} characters")
                    debugLogManager?.logInfo(
                        category = "TRANSCRIPTION",
                        operation = "processVideo_complete",
                        message = "Transcription completed",
                        details = buildString {
                            appendLine("URL: $url")
                            appendLine("Tier: $tier")
                            appendLine("Transcript chars: ${transcriptText?.length ?: 0}")
                            appendLine("New balance: $newBalance")
                            appendLine("Free uses remaining: $newFreeUses")
                        }
                    )
                    
                    // Check for low balance warning
                    var successMessage: String? = null
                    if (newBalance <= 2 && newBalance > 0) {
                        successMessage = "Transcription complete. Warning: You only have $newBalance credits left."
                    }
                    
                    onResult(true, newBalance, newFreeUses, successMessage, null)
                },
                onFailure = { error ->
                    Log.e("VideoProcessor", "Transcription failed: ${error.message}")
                    
                    // Log transcription failure to debug system with full details
                    debugLogManager?.logError(
                        category = "TRANSCRIPTION",
                        operation = "processTikTokVideo",
                        message = "Transcription failed for URL: $url",
                        exception = error,
                        requestUrl = url,
                        requestPayload = buildString {
                            appendLine("REQUEST CONTEXT:")
                            appendLine("  Video URL: $url")
                            appendLine("  Processing Tier: $tier")
                            appendLine("  Current Balance: $currentBalance")
                            appendLine("  Free Uses Remaining: $currentFreeUses")
                            appendLine()
                            appendLine("FLOW DETAILS:")
                            appendLine("  Expected Flow: Metadata → Vend Token → Submit → Poll → Complete")
                            appendLine("  Cost: ${if (tier == ProcessingTier.EXTRACT_SCRIPT) "1 credit" else "2 credits"}")
                        },
                        responseBody = buildString {
                            appendLine("ERROR DETAILS:")
                            appendLine("  Error Type: ${error::class.simpleName}")
                            appendLine("  Error Message: ${error.message}")
                            if (error is app.pluct.services.PluctCoreAPIDetailedError) {
                                appendLine()
                                appendLine("DETAILED API ERROR:")
                                appendLine(error.getDetailedMessage())
                            }
                            appendLine()
                            appendLine("STACK TRACE:")
                            appendLine(error.stackTraceToString())
                        }
                    )
                    
                    // Update video item with failure status
                    videoRepository.updateVideo(currentVideoItem.copy(
                        status = ProcessingStatus.FAILED,
                        failureReason = "Transcription failed: ${error.message}"
                    ))
                    
                    val isAuth = error.message?.contains("auth", ignoreCase = true) == true || 
                                 error.message?.contains("401") == true ||
                                 error.message?.contains("token_expired", ignoreCase = true) == true ||
                                 error.message?.contains("session has expired", ignoreCase = true) == true
                    
                    val friendly = when {
                        isAuth -> "Authentication expired. We'll refresh and retry automatically."
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
