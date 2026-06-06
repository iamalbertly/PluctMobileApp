package app.pluct.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import app.pluct.core.network.PluctNetworkConnectivityChecker
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.QueueReason
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.PluctQueueManager
import app.pluct.core.error.PluctCoreError03UserMessageFormatter

/**
 * Pluct-UI-Screen-01MainActivity-02TranscriptionOrchestrator - Transcription orchestration logic
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Single source of truth for transcription orchestration operations
 * Note: This orchestrates API calls to Business Engine, it does not process video locally
 */
object PluctUIScreen01MainActivityTranscriptionOrchestrator {
    
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
            Log.d("TranscriptionOrchestrator", "Loading initial data...")
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
                    Log.d("TranscriptionOrchestrator", "Credit balance loaded: $totalCredits total (${balance.main} main + ${balance.bonus} bonus) for user: ${userIdentification.userId}")
                    debugLogManager?.logInfo(
                        category = "CREDIT_CHECK",
                        operation = "loadInitialData",
                        message = "Credit balance loaded",
                        details = "Total=$totalCredits; Main=${balance.main}; Bonus=${balance.bonus}; User=${userIdentification.userId}",
                        requestUrl = "${app.pluct.core.api.PluctCoreAPI00Constants.BASE_URL}/v1/credits/balance",
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
                                appendLine("  Base URL: ${app.pluct.core.api.PluctCoreAPI00Constants.BASE_URL}")
                                appendLine()
                                appendLine("EXPECTED BEHAVIOR:")
                                appendLine("  Both main and bonus credits should be >= 0")
                                appendLine("  Negative values indicate API error or data corruption")
                            }
                        )
                    }
                    
                    onDataLoaded(totalCredits, maxOf(0, balance.freeUsesRemaining))
                },
                onFailure = { error ->
                    Log.e("TranscriptionOrchestrator", "Failed to load credit balance: ${error.message}")
                    
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
                            appendLine("  Base URL: ${app.pluct.core.api.PluctCoreAPI00Constants.BASE_URL}")
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
                    
                    onDataLoaded(0, 0) // Keep Business Engine as the source of truth.
                }
            )
        } catch (e: Exception) {
            Log.e("TranscriptionOrchestrator", "Error loading initial data: ${e.message}")
            
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
            
            onDataLoaded(0, 0) // Keep Business Engine as the source of truth.
        }
    }
    
    /**
     * Orchestrate transcription flow for a video URL
     * This orchestrates API calls to Business Engine and TTTranscribe - it does not process video locally
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
        context: Context?,
        validator: app.pluct.services.PluctCoreValidationInputSanitizer,
        onResult: (Boolean, Int, Int, String?, Throwable?) -> Unit
    ) {
        try {
            Log.d("TranscriptionOrchestrator", "Orchestrating transcription: $url with tier: $tier")
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
            
            // Check network connectivity first
            if (context != null && !PluctNetworkConnectivityChecker.isNetworkAvailable(context)) {
                Log.w("TranscriptionOrchestrator", "No network available, queueing video: $url")
                debugLogManager?.logInfo(
                    category = "QUEUE",
                    operation = "queueVideo_offline",
                    message = "No network available, queueing video",
                    details = "URL: $url, Reason: NO_INTERNET"
                )
                
                // Queue the video for later processing
                val queueManager = PluctQueueManager(videoRepository)
                val queueResult = queueManager.queueVideo(
                    url = url,
                    tier = tier,
                    reason = QueueReason.NO_INTERNET
                )
                
                // UX IMPROVEMENT #4: Immediate, clear feedback when video is queued
                if (queueResult.isSuccess) {
                    val userMessage = "✅ Video saved! It will automatically process when your connection is restored. You can check the Queue section below."
                    onResult(true, currentBalance, currentFreeUses, userMessage, null)
                } else {
                    val error = queueResult.exceptionOrNull() ?: Exception("Failed to queue video")
                    val userMessage = "❌ No internet connection. Unable to save video for later processing. Please check your connection and try again."
                    onResult(false, currentBalance, currentFreeUses, userMessage, error)
                }
                return
            }
            
            // Check if user has enough credits/free uses using unified validator
            // Single source of truth: InputSanitizer.validateCredits()
            val creditValidator = PluctUIScreen01MainActivityTranscriptionOrchestratorCreditValidator(validator)
            val creditValidation = creditValidator.validateCredits(
                tier = tier,
                currentBalance = currentBalance,
                currentFreeUses = currentFreeUses
            )
            
            if (!creditValidation.hasEnoughCredits) {
                onResult(false, currentBalance, currentFreeUses, creditValidation.userMessage, creditValidation.error)
                return
            }
            
            // Deduplication is handled by apiService.processTikTokVideo() via unified coordinator
            // Fetch metadata to populate title and author (fast-fail to avoid blocking UX on slow /meta)
            val metadataResult = apiService.getMetadata(url, timeoutMs = 8000L)
            var currentVideoItem: app.pluct.data.entity.VideoItem? = null
            
            // Use complete processTikTokVideo flow which handles deduplication, metadata, vending, submission, and polling
            // This ensures the transcript is retrieved and can be saved
            // Note: jobId will be available in the response and stored when transcription completes
            // Deduplication is handled internally by the unified coordinator
            Log.d("TranscriptionOrchestrator", "Starting complete transcription flow with processTikTokVideo...")
            val transcriptionResult = apiService.processTikTokVideo(url)
            
            // If metadata was fetched, update the video item (if it exists in database)
            if (metadataResult.isSuccess) {
                val metadata = metadataResult.getOrNull()
                if (metadata != null) {
                    Log.d("TranscriptionOrchestrator", "Metadata fetched: ${metadata.title}")
                    val existingVideo = videoRepository.getVideoByUrl(url)
                    if (existingVideo != null) {
                        currentVideoItem = existingVideo.copy(
                            title = metadata.title ?: "",
                            author = metadata.author ?: "",
                            thumbnailUrl = metadata.thumbnail ?: existingVideo.thumbnailUrl,
                            duration = metadata.duration.toLong(),
                            description = metadata.description
                        )
                        videoRepository.updateVideo(currentVideoItem)
                    }
                }
            } else {
                Log.w("TranscriptionOrchestrator", "Failed to fetch metadata: ${metadataResult.exceptionOrNull()?.message}")
            }
            
            // Process transcription result using extracted result processor
            val processResult = transcriptionResult.fold(
                onSuccess = { statusResponse ->
                    PluctUIScreen01MainActivityTranscriptionOrchestratorResultProcessor.processSuccess(
                        statusResponse = statusResponse,
                        url = url,
                        tier = tier,
                        currentBalance = currentBalance,
                        currentFreeUses = currentFreeUses,
                        currentVideoItem = currentVideoItem,
                        videoRepository = videoRepository,
                        clipboardManager = clipboardManager,
                        debugLogManager = debugLogManager
                    )
                },
                onFailure = { error ->
                    PluctUIScreen01MainActivityTranscriptionOrchestratorResultProcessor.processFailure(
                        error = error,
                        url = url,
                        tier = tier,
                        currentBalance = currentBalance,
                        currentFreeUses = currentFreeUses,
                        currentVideoItem = currentVideoItem,
                        videoRepository = videoRepository,
                        clipboardManager = clipboardManager,
                        debugLogManager = debugLogManager
                    )
                }
            )
            
            onResult(processResult.success, processResult.newBalance, processResult.newFreeUses, processResult.message, processResult.error)
        } catch (e: Exception) {
            // UX IMPROVEMENT #4: Catch and surface silent failures with proper error handling
            Log.e("TranscriptionOrchestrator", "Error orchestrating transcription: ${e.message}", e)
            
            // Ensure error is logged to debug system
            debugLogManager?.logError(
                category = "TRANSCRIPTION",
                operation = "processVideo_exception",
                message = "Unhandled exception during transcription orchestration",
                exception = e,
                requestUrl = url,
                requestPayload = "Exception caught in try-catch block"
            )
            
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
