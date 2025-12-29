package app.pluct

import android.content.Intent
import android.os.Bundle
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.PluctCoreAPIDetailedError
import app.pluct.services.PluctCoreUserIdentification
import app.pluct.services.PluctCoreValidationInputSanitizer
import javax.inject.Inject
import app.pluct.ui.theme.PluctTheme
import app.pluct.ui.screens.PluctHomeScreen
import app.pluct.ui.screens.PluctUIScreen01MainActivityIntentHandler
import app.pluct.ui.screens.PluctUIScreen01MainActivityTranscriptionOrchestrator
import app.pluct.ui.screens.PluctUIScreen01MainActivityIntentHandlerQueueManager
import app.pluct.ui.screens.PluctVideoDetailScreen
import app.pluct.ui.components.PluctUIComponent05Notification01SnackbarManager
import app.pluct.ui.components.PluctDebugLogViewer
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.preferences.PluctUserPreferences
import app.pluct.notification.PluctQueueNotificationManager
import app.pluct.core.network.PluctNetworkConnectivityChecker
import app.pluct.services.PluctQueueManager
import app.pluct.services.background.status.PluctStatusResumer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pluct-Main-01Activity - Simplified main activity for core UI testing
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@AndroidEntryPoint
class PluctUIScreen01MainActivity : ComponentActivity() {
    
    @Inject lateinit var apiService: PluctCoreAPIUnifiedService
    @Inject lateinit var userIdentification: PluctCoreUserIdentification
    @Inject lateinit var videoRepository: app.pluct.data.repository.PluctVideoRepository
    @Inject lateinit var validator: PluctCoreValidationInputSanitizer
    @Inject lateinit var debugLogManager: app.pluct.core.debug.PluctCoreDebug01LogManager
    @Inject lateinit var queueManager: app.pluct.services.PluctQueueManager

    // Drives recomposition when new intents provide a prefilled URL.
    private val prefilledUrlState = mutableStateOf<String?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize debug log manager (cleanup old logs on startup)
        debugLogManager.initialize()
        super.onCreate(savedInstanceState)
        
        // Handle notification navigation
        if (intent.getStringExtra("action") == "view_transcript") {
            val url = intent.getStringExtra("url")
            val transcript = intent.getStringExtra("transcript")
            if (url != null) {
                // Navigate to video detail - set prefilled URL to trigger navigation
                prefilledUrlState.value = url
                // Store transcript for display if needed
                if (transcript != null) {
                    // Could store in preferences or pass via state
                    PluctUserPreferences.setPrefilledUrl(this, url)
                }
            }
        }
        
        // Handle TikTok intent
        PluctUIScreen01MainActivityIntentHandler.handleTikTokIntent(intent, this, validator)

        // Seed any prefilled URL immediately so Compose can render it without waiting for effects.
        if (prefilledUrlState.value == null) {
            val url = PluctUserPreferences.getAndClearPrefilledUrl(this)
            if (url != null) {
                // Check if processing is active and queue if needed
                lifecycleScope.launch {
                    val wasQueued = PluctUIScreen01MainActivityIntentHandlerQueueManager.queueIntentIfProcessing(
                        url = url,
                        context = this@PluctUIScreen01MainActivity,
                        videoRepository = videoRepository,
                        queueManager = queueManager
                    )
                    if (!wasQueued) {
                        // Not queued, can proceed with auto-submit
                        prefilledUrlState.value = url
                    } else {
                        // Queued, don't set prefilled URL (will process later)
                        Log.d("MainActivity", "Intent queued due to active processing")
                    }
                }
            }
        }
        
        setContent {
            PluctTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PluctMainContent(
                        apiService = apiService,
                        userIdentification = userIdentification,
                        videoRepository = videoRepository,
                        prefilledUrlExternal = prefilledUrlState.value,
                        onPrefilledUrlConsumed = { prefilledUrlState.value = null },
                        debugLogManager = debugLogManager,
                        queueManager = queueManager
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the intent so getIntent() returns the new one
        intent?.let { 
            Log.d("MainActivity", "onNewIntent received: ${it.action}")
            PluctUIScreen01MainActivityIntentHandler.handleTikTokIntent(it, this, validator)
            // Check for prefilled URL immediately after handling intent
            val url = PluctUserPreferences.getAndClearPrefilledUrl(this)
            if (url != null) {
                Log.d("MainActivity", "Prefilled URL from new intent: $url")
                // Check if processing is active and queue if needed
                lifecycleScope.launch {
                    val wasQueued = PluctUIScreen01MainActivityIntentHandlerQueueManager.queueIntentIfProcessing(
                        url = url,
                        context = this@PluctUIScreen01MainActivity,
                        videoRepository = videoRepository,
                        queueManager = queueManager
                    )
                    if (!wasQueued) {
                        // Not queued, can proceed with auto-submit
                        prefilledUrlState.value = url
                    } else {
                        // Queued, don't set prefilled URL (will process later)
                        Log.d("MainActivity", "Intent queued due to active processing")
                    }
                }
            }
        }
    }
}

/**
 * Main content composable - extracted for better separation of concerns
 */
@Composable
fun PluctMainContent(
    apiService: PluctCoreAPIUnifiedService,
    userIdentification: PluctCoreUserIdentification,
    videoRepository: app.pluct.data.repository.PluctVideoRepository,
    prefilledUrlExternal: String?,
    onPrefilledUrlConsumed: () -> Unit = {},
    debugLogManager: app.pluct.core.debug.PluctCoreDebug01LogManager,
    queueManager: app.pluct.services.PluctQueueManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    
    // State management
    var creditBalance by remember { mutableStateOf(0) }
    var freeUsesRemaining by remember { mutableStateOf(3) }
    var isLoading by remember { mutableStateOf(true) } // Start with loading = true to show loading indicator
    var hasLoadedBalanceOnce by remember { mutableStateOf(false) } // Track if we've loaded balance at least once
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentError by remember { mutableStateOf<Throwable?>(null) }
    var currentErrorUrl by remember { mutableStateOf<String?>(null) } // Track URL for error context
    var showWelcomeDialog by remember { mutableStateOf(false) }
    var prefilledUrl by remember { mutableStateOf<String?>(prefilledUrlExternal) }
    val userName = remember { userIdentification.userId }
    var creditRequestLog by remember { mutableStateOf<String?>(null) }
    var ctaHelperMessage by remember { mutableStateOf<String?>(null) }
    var hasVendTokenAttempted by remember { mutableStateOf(false) }
    
    // Track queued and processing videos for notifications
    val queuedVideos = videoRepository.getVideosByStatus(ProcessingStatus.QUEUED)
        .collectAsState(initial = emptyList())
    val processingVideos = videoRepository.getVideosByStatus(ProcessingStatus.PROCESSING)
        .collectAsState(initial = emptyList())
    val queuedCount = queuedVideos.value.size
    val processingCount = processingVideos.value.size
    
    // Resume incomplete transcriptions on app start
    LaunchedEffect(Unit) {
        scope.launch {
            val resumer = PluctStatusResumer(videoRepository, apiService, context)
            resumer.resumeIncompleteTranscriptions()
        }
    }
    
    // Monitor and update progress for processing videos when app is in foreground
    LaunchedEffect(processingVideos.value) {
        if (processingVideos.value.isNotEmpty()) {
            scope.launch {
                processingVideos.value.forEach { video ->
                    if (video.jobId != null && video.jobId.isNotBlank()) {
                        // Poll status for this video periodically and update progress
                        launch {
                            var shouldContinue = true
                            while (shouldContinue) {
                                try {
                                    // Get service token for status check
                                    val vendResult = apiService.vendToken("status_check_${System.currentTimeMillis()}")
                                    if (vendResult.isSuccess) {
                                        val serviceToken = vendResult.getOrNull()?.token ?: vendResult.getOrNull()?.serviceToken
                                        if (serviceToken != null) {
                                            val statusResult = apiService.checkTranscriptionStatus(video.jobId, serviceToken)
                                            if (statusResult.isSuccess) {
                                                val status = statusResult.getOrNull()!!
                                                val progress = status.progress ?: video.progress
                                                val transcript = status.transcript ?: status.result?.transcription
                                                
                                                // Update video in database
                                                val updatedVideo = when {
                                                    status.status == "completed" && transcript != null -> {
                                                        video.copy(
                                                            status = ProcessingStatus.COMPLETED,
                                                            progress = 100,
                                                            transcript = transcript
                                                        )
                                                    }
                                                    status.status == "failed" -> {
                                                        video.copy(
                                                            status = ProcessingStatus.FAILED,
                                                            failureReason = status.error ?: "Transcription failed"
                                                        )
                                                    }
                                                    else -> {
                                                        video.copy(progress = progress)
                                                    }
                                                }
                                                videoRepository.insertVideo(updatedVideo)
                                                Log.d("MainActivity", "Updated progress for video ${video.url}: $progress%")
                                                
                                                // Stop polling if completed or failed
                                                if (status.status == "completed" || status.status == "failed") {
                                                    shouldContinue = false
                                                    break
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w("MainActivity", "Failed to check status for video ${video.url}: ${e.message}")
                                }
                                
                                // Poll every 5 seconds
                                delay(5000)
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Update queue notification when counts change
    LaunchedEffect(queuedCount, processingCount) {
        val queueReasons = queuedVideos.value.associate { it.url to it.queueReason }
        PluctQueueNotificationManager.updateQueueNotification(
            context = context,
            queuedCount = queuedCount,
            processingCount = processingCount,
            queueReasons = queueReasons
        )
    }
    
    // Auto-retry: Process queued videos when credits become available
    LaunchedEffect(creditBalance) {
        if (creditBalance > 0 && queuedCount > 0) {
            scope.launch {
                val processedCount = queueManager.processQueuedVideos(
                    apiService = apiService,
                    currentBalance = creditBalance,
                    currentFreeUses = freeUsesRemaining,
                    isNetworkAvailable = PluctNetworkConnectivityChecker.isNetworkAvailable(context),
                    onProcess = { video ->
                        // Process the video
                        PluctUIScreen01MainActivityTranscriptionOrchestrator.processVideo(
                            apiService, video.url, video.tier, creditBalance, freeUsesRemaining,
                            videoRepository, clipboardManager, debugLogManager, context
                        ) { success, newBalance, newFreeUses, errorMessage, error ->
                            creditBalance = newBalance
                            freeUsesRemaining = newFreeUses
                            if (success) {
                                PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                                    scope, snackbarHostState, "Processing queued video..."
                                )
                            }
                        }
                    }
                )
                if (processedCount > 0) {
                    Log.d("MainActivity", "Auto-processed $processedCount queued video(s) after credits added")
                }
            }
        }
    }
    
    // Auto-retry: Process queued videos when network becomes available
    LaunchedEffect(Unit) {
        var wasNetworkAvailable = PluctNetworkConnectivityChecker.isNetworkAvailable(context)
        
        while (true) {
            delay(2000) // Check every 2 seconds
            val isNetworkAvailable = PluctNetworkConnectivityChecker.isNetworkAvailable(context)
            
            if (isNetworkAvailable && !wasNetworkAvailable && queuedCount > 0) {
                // Network just became available, process queued videos
                scope.launch {
                    val processedCount = queueManager.processQueuedVideos(
                        apiService = apiService,
                        currentBalance = creditBalance,
                        currentFreeUses = freeUsesRemaining,
                        isNetworkAvailable = true,
                        onProcess = { video ->
                            // Process the video
                            PluctUIScreen01MainActivityTranscriptionOrchestrator.processVideo(
                                apiService, video.url, video.tier, creditBalance, freeUsesRemaining,
                                videoRepository, clipboardManager, debugLogManager, context
                            ) { success, newBalance, newFreeUses, errorMessage, error ->
                                creditBalance = newBalance
                                freeUsesRemaining = newFreeUses
                                if (success) {
                                    PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                                        scope, snackbarHostState, "Processing queued video..."
                                    )
                                }
                            }
                        }
                    )
                    if (processedCount > 0) {
                        Log.d("MainActivity", "Auto-processed $processedCount queued video(s) after network restored")
                        PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                            scope, snackbarHostState, "Processing $processedCount queued video(s)..."
                        )
                    }
                }
            }
            
            wasNetworkAvailable = isNetworkAvailable
        }
    }

    suspend fun fetchCreditBalance() {
        PluctUIScreen01MainActivityTranscriptionOrchestrator.loadInitialData(apiService, userIdentification, debugLogManager) { balance, freeUses ->
            creditBalance = balance
            freeUsesRemaining = freeUses
        }
    }

    suspend fun vendTokenWithBalanceUpdate(reason: String) {
        // Skip vending when free uses are available or balance is zero to avoid noisy 402s
        if (freeUsesRemaining > 0) {
            hasVendTokenAttempted = true
            debugLogManager.logInfo(
                category = "CREDIT_CHECK",
                operation = "vendToken",
                message = "Skipping vendToken; free uses available",
                details = "Balance=$creditBalance FreeUses=$freeUsesRemaining Reason=$reason"
            )
            return
        }
        if (creditBalance <= 0) {
            hasVendTokenAttempted = true
            debugLogManager.logWarning(
                category = "CREDIT_CHECK",
                operation = "vendToken",
                message = "Skipping vendToken; no credits available (would trigger 402)",
                details = "Balance=$creditBalance FreeUses=$freeUsesRemaining Reason=$reason"
            )
            ctaHelperMessage = "Add credits to unlock transcription"
            return
        }

        // Avoid spamming vend-token when we already have credits; reduces 429 rate limits.
        if (creditBalance > 0 && freeUsesRemaining > 0 && reason == "app_launch") {
            hasVendTokenAttempted = true
            debugLogManager.logInfo(
                category = "CREDIT_CHECK",
                operation = "vendToken",
                message = "Skipping vendToken; credits already available",
                details = "Balance=$creditBalance FreeUses=$freeUsesRemaining"
            )
            return
        }

        hasVendTokenAttempted = true
        debugLogManager.logInfo(
            category = "CREDIT_CHECK",
            operation = "vendToken",
            message = "Vend token requested",
            details = "Reason: $reason",
            requestUrl = "https://pluct-business-engine.romeo-lya2.workers.dev/v1/vend-token",
            requestMethod = "POST",
            requestPayload = """{"userId":"${userIdentification.userId}"}"""
        )

        val vendResult = apiService.vendToken()
        vendResult.fold(
            onSuccess = { vend ->
                creditBalance = maxOf(creditBalance, vend.balanceAfter)
                freeUsesRemaining = maxOf(freeUsesRemaining, vend.balanceAfter)
                ctaHelperMessage = if (vend.balanceAfter >= 1) {
                    "You have ${vend.balanceAfter} free credits"
                } else {
                    "Add credits to unlock transcription"
                }
                debugLogManager.logInfo(
                    category = "CREDIT_CHECK",
                    operation = "vendToken",
                    message = "Vend token succeeded",
                    details = "Balance after vend: ${vend.balanceAfter}; Reason: $reason",
                    requestUrl = "https://pluct-business-engine.romeo-lya2.workers.dev/v1/vend-token",
                    requestMethod = "POST"
                )

                // Refresh balance to show updated gem counter immediately
                val balanceResult = apiService.checkUserBalance()
                balanceResult.onSuccess { balance ->
                    val totalCredits = maxOf(0, balance.main + balance.bonus)
                    creditBalance = totalCredits
                    freeUsesRemaining = maxOf(freeUsesRemaining, totalCredits)
                }
            },
            onFailure = { error ->
                val detailed = error as? PluctCoreAPIDetailedError
                val statusCode = detailed?.technicalDetails?.responseStatusCode

                if (detailed != null) {
                    debugLogManager.logAPIError(detailed, "CREDIT_CHECK")
                } else {
                    debugLogManager.logError(
                        category = "CREDIT_CHECK",
                        operation = "vendToken",
                        message = error.message ?: "Vend token failed",
                        exception = error,
                        requestUrl = "https://pluct-business-engine.romeo-lya2.workers.dev/v1/vend-token"
                    )
                }

                if (statusCode == 402) {
                    ctaHelperMessage = "Add credits to unlock transcription"
                    PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                        scope,
                        snackbarHostState,
                        "No credits available.",
                        actionLabel = "Request credits"
                    )
                }
            }
        )
    }

    // Keep local state in sync when a new intent provides a prefilled URL.
    LaunchedEffect(prefilledUrlExternal) {
        if (!prefilledUrlExternal.isNullOrBlank()) {
            prefilledUrl = prefilledUrlExternal
            onPrefilledUrlConsumed()
        }
    }
    
    // Load videos from database using Flow
    val videos by videoRepository.getAllVideos().collectAsState(initial = emptyList())
    
    // Load initial data and check for prefilled URL
    LaunchedEffect(Unit) {
        // Check for prefilled URL from intent
        val url = PluctUserPreferences.getAndClearPrefilledUrl(context)
        if (url != null) {
            Log.d("MainActivity", "Found prefilled URL: $url")
            prefilledUrl = url
        }
        
        // Check for first-time user
        if (PluctUserPreferences.isFirstTimeUser(context)) {
            showWelcomeDialog = true
        }
        
        // Always bootstrap balance, then vend a token only if we need credits.
        isLoading = true
        errorMessage = null
        try {
            fetchCreditBalance()
            hasLoadedBalanceOnce = true // Mark that we've loaded balance at least once
            if (creditBalance < 1 && freeUsesRemaining < 1) {
                vendTokenWithBalanceUpdate(reason = "app_launch")
            } else {
                hasVendTokenAttempted = true
                ctaHelperMessage = if (creditBalance > 0) null else "Add credits to unlock transcription"
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to load initial balance: ${e.message}", e)
            errorMessage = "Failed to load balance"
            hasLoadedBalanceOnce = true // Mark as loaded even on error to show 0 instead of loading
        } finally {
            isLoading = false
        }

        // Show any stored intent feedback to the user
        // Suppress duplicate toast when auto-submitting (if prefilled URL exists and credits available)
        val feedback = PluctUserPreferences.getAndClearIntentFeedback(context)
        feedback?.let {
            if (it.isError) {
                PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                    scope, snackbarHostState, it.message
                )
            } else {
                // Suppress "TikTok video link received!" toast when URL is prefilled and credits available
                // (auto-submit will happen, so no need for duplicate notification)
                val shouldSuppressToast = !prefilledUrl.isNullOrBlank() && 
                                         (creditBalance >= 1 || freeUsesRemaining > 0)
                
                if (!shouldSuppressToast) {
                    PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                        scope, snackbarHostState, it.message
                    )
                }
                // Otherwise silent - auto-submit will handle it
            }
        }
    }
    
    // Refresh credit balance function
    val refreshCreditBalance: () -> Unit = {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                fetchCreditBalance()
                hasLoadedBalanceOnce = true // Mark that we've loaded balance

                // Vend token if we have not already attempted or user balance is empty to surface welcome bonus.
                if (!hasVendTokenAttempted || creditBalance < 1) {
                    vendTokenWithBalanceUpdate(reason = if (creditBalance < 1) "refresh_no_credits" else "manual_refresh")
                } else {
                    ctaHelperMessage = if (creditBalance > 0) null else "Add credits to unlock transcription"
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to refresh balance: ${e.message}", e)
                errorMessage = "Failed to refresh balance"
                currentError = e
                hasLoadedBalanceOnce = true // Mark as loaded even on error
                PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                    scope,
                    snackbarHostState,
                    "Could not refresh credits. Check connection and retry."
                )
            } finally {
                isLoading = false
            }
        }
    }

    // Auto-dismiss welcome gate so the capture card stays reachable during tests and first run.
    LaunchedEffect(showWelcomeDialog) {
        if (showWelcomeDialog) {
            delay(3000)
            if (showWelcomeDialog) {
                PluctUserPreferences.markUserAsReturning(context)
                showWelcomeDialog = false
                refreshCreditBalance()
            }
        }
    }
    
    // Handle video processing
    val onTierSubmit: (String, ProcessingTier) -> Unit = { url: String, tier: ProcessingTier ->
        scope.launch {
            PluctUIScreen01MainActivityTranscriptionOrchestrator.processVideo(apiService, url, tier, creditBalance, freeUsesRemaining, videoRepository, clipboardManager, debugLogManager, context) { success, newBalance, newFreeUses, errorMessage, error ->
                if (success) {
                    creditBalance = newBalance
                    freeUsesRemaining = newFreeUses
                    currentErrorUrl = null // Clear error URL on success
                    // Show success message (or warning if provided)
                    val msg = errorMessage ?: "Transcription started successfully"
                    PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                        scope, snackbarHostState, msg
                    )
                } else {
                    // Show error message via snackbar if it's not a handled error type
                    // If it is a handled error (like 402/429), the ErrorHandler will show a dialog
                    if (error != null) {
                        currentError = error
                        currentErrorUrl = url // Track URL for error context
                    } else {
                        currentErrorUrl = null
                        val errorMsg = errorMessage ?: "Failed to process video. Please try again."
                        PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                            scope, snackbarHostState, errorMsg
                        )
                    }
                    Log.e("MainActivity", "Video processing failed: ${errorMessage ?: error?.message}")
                }
            }
        }
    }
    
    // Handle video retry
    val onRetryVideo: (VideoItem) -> Unit = { video ->
        scope.launch {
            PluctUIScreen01MainActivityTranscriptionOrchestrator.processVideo(apiService, video.url, video.tier, creditBalance, freeUsesRemaining, videoRepository, clipboardManager, debugLogManager, context) { success, newBalance, newFreeUses, errorMessage, error ->
                if (success) {
                    creditBalance = newBalance
                    freeUsesRemaining = newFreeUses
                    PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                        scope, snackbarHostState, "Retry started successfully"
                    )
                } else {
                    if (error != null) {
                        currentError = error
                    } else {
                        val errorMsg = errorMessage ?: "Failed to retry video. Please try again."
                        PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                            scope, snackbarHostState, errorMsg
                        )
                    }
                    Log.e("MainActivity", "Video retry failed: ${errorMessage ?: error?.message}")
                }
            }
        }
    }
    
    // Handle video deletion
    val onDeleteVideo: (VideoItem) -> Unit = { video ->
        scope.launch {
            videoRepository.deleteVideo(video)
            Log.d("MainActivity", "Video deleted: ${video.id}")
        }
    }
    
    // Navigation State
    var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }
    
    // Main UI
    if (selectedVideo != null) {
        PluctVideoDetailScreen(
            video = selectedVideo!!,
            onBackClick = { selectedVideo = null },
            onUpgradeClick = { /* Handle upgrade */ }
        )
    } else {
    // Handle queue for later (from pre-validation prompt) - defined before use
    val onQueueForLaterWithReason: (String, app.pluct.data.entity.QueueReason) -> Unit = { url, reason ->
        scope.launch {
            val tier = ProcessingTier.EXTRACT_SCRIPT
            val result = queueManager.queueVideo(
                url = url,
                tier = tier,
                reason = reason
            )
            if (result.isSuccess) {
                PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                    scope, snackbarHostState, "Saved! Will process when ready."
                )
            } else {
                PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                    scope, snackbarHostState, "Failed to save video. Please try again."
                )
            }
        }
    }
    
        PluctHomeScreen(
            creditBalance = creditBalance,
            freeUsesRemaining = freeUsesRemaining,
            videos = videos,
            isLoading = isLoading,
            errorMessage = errorMessage,
            userName = userName,
            onRequestCredits = { confirmation ->
                val requestId = "credit_req_${System.currentTimeMillis()}"
                val userId = userIdentification.userId
                val timestamp = System.currentTimeMillis()
                
                // Log BEFORE request
                debugLogManager.logInfo(
                    category = "CREDIT_REQUEST",
                    operation = "requestCredits",
                    message = "Credit request initiated",
                    details = buildString {
                        appendLine("Request ID: $requestId")
                        appendLine("User ID: $userId")
                        appendLine("Confirmation Text: $confirmation")
                        appendLine("Timestamp: $timestamp")
                    },
                    requestUrl = "https://pluct-business-engine.romeo-lya2.workers.dev/v1/user/balance",
                    requestMethod = "POST",
                    requestPayload = buildString {
                        appendLine("{")
                        appendLine("  \"userId\": \"$userId\",")
                        appendLine("  \"confirmation\": \"$confirmation\",")
                        appendLine("  \"clientRequestId\": \"$requestId\",")
                        appendLine("  \"timestamp\": $timestamp")
                        appendLine("}")
                    }
                )
                
                // Record the manual request for validation
                creditRequestLog = confirmation
                
                // Log success (since this is a manual confirmation flow)
                scope.launch {
                    delay(100) // Small delay to ensure log is written
                    debugLogManager.logInfo(
                        category = "CREDIT_REQUEST",
                        operation = "requestCredits",
                        message = "Credit request acknowledged",
                        details = buildString {
                            appendLine("Request ID: $requestId")
                            appendLine("Status: Acknowledged (manual processing)")
                            appendLine("User will be notified when credits are applied")
                            appendLine()
                            appendLine("Response:")
                            appendLine("{")
                            appendLine("  \"status\": \"acknowledged\",")
                            appendLine("  \"message\": \"Request received for manual processing\",")
                            appendLine("  \"requestId\": \"$requestId\"")
                            appendLine("}")
                        },
                        requestUrl = "https://pluct-business-engine.romeo-lya2.workers.dev/v1/user/balance",
                        requestMethod = "POST",
                        requestPayload = buildString {
                            appendLine("{")
                            appendLine("  \"userId\": \"$userId\",")
                            appendLine("  \"confirmation\": \"$confirmation\",")
                            appendLine("  \"clientRequestId\": \"$requestId\"")
                            appendLine("}")
                        }
                    )
                    
                    PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                        scope,
                        snackbarHostState,
                        "Request sent (ID: ${requestId.take(8)}). We'll verify your payment and apply credits."
                    )
                }
            },
            onTierSubmit = onTierSubmit,
            onRetryVideo = onRetryVideo,
            onDeleteVideo = onDeleteVideo,
            onVideoClick = { video -> selectedVideo = video },
            prefilledUrl = prefilledUrl,
            apiService = apiService,
            onRefreshCreditBalance = refreshCreditBalance,
            snackbarHostState = snackbarHostState,
            videoRepository = videoRepository,
            ctaHelperMessage = ctaHelperMessage,
            debugLogManager = debugLogManager,
            onQueueForLater = onQueueForLaterWithReason
        )
    }
    
    // Handle queue for later (from error handler)
    val onQueueForLater: () -> Unit = {
        scope.launch {
            val url = currentErrorUrl
            if (url != null) {
                // Determine tier from context (default to EXTRACT_SCRIPT)
                val tier = ProcessingTier.EXTRACT_SCRIPT // Could be enhanced to track tier
                val result = queueManager.queueVideo(
                    url = url,
                    tier = tier,
                    reason = app.pluct.data.entity.QueueReason.INSUFFICIENT_CREDITS
                )
                if (result.isSuccess) {
                    PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                        scope, snackbarHostState, "Video saved! Will process when credits are added."
                    )
                } else {
                    PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                        scope, snackbarHostState, "Failed to save video. Please try again."
                    )
                }
            }
            currentError = null
            currentErrorUrl = null
        }
    }
    
    // Unified Error Handler
    app.pluct.ui.error.PluctBusinessEngineErrorHandler(
        error = currentError,
        onDismiss = { 
            currentError = null
            currentErrorUrl = null
        },
        onPurchaseCredits = {
            // TODO: Navigate to purchase screen
            currentError = null
            currentErrorUrl = null
            PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                scope, snackbarHostState, "Contact support@pluct.app to upgrade"
            )
        },
        onRetry = {
            currentError = null
            currentErrorUrl = null
            // Retry logic would depend on context, for now just dismiss
        },
        url = currentErrorUrl,
        queuedCount = queuedCount,
        onQueueForLater = onQueueForLater
    )
    
    // Welcome Dialog
    if (showWelcomeDialog) {
        app.pluct.ui.components.PluctWelcomeDialog(
            onDismiss = { showWelcomeDialog = false },
            onGetStarted = {
                PluctUserPreferences.markUserAsReturning(context)
                showWelcomeDialog = false
                // Refresh balance to show free credits
                refreshCreditBalance()
            }
        )
    }
}
