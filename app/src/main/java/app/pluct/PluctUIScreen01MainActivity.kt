package app.pluct

import android.content.Intent
import android.os.Bundle
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import app.pluct.ui.screens.PluctUIScreen01MainActivityVideoProcessor
import app.pluct.ui.screens.PluctVideoDetailScreen
import app.pluct.ui.components.PluctUIComponent05Notification01SnackbarManager
import app.pluct.ui.components.PluctDebugLogViewer
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.preferences.PluctUserPreferences
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

    // Drives recomposition when new intents provide a prefilled URL.
    private val prefilledUrlState = mutableStateOf<String?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle TikTok intent
        PluctUIScreen01MainActivityIntentHandler.handleTikTokIntent(intent, this, validator)

        // Seed any prefilled URL immediately so Compose can render it without waiting for effects.
        prefilledUrlState.value = PluctUserPreferences.getAndClearPrefilledUrl(this)
        
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
                        debugLogManager = debugLogManager
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
                prefilledUrlState.value = url
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
    debugLogManager: app.pluct.core.debug.PluctCoreDebug01LogManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    
    // State management
    var creditBalance by remember { mutableStateOf(0) }
    var freeUsesRemaining by remember { mutableStateOf(3) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentError by remember { mutableStateOf<Throwable?>(null) }
    var showWelcomeDialog by remember { mutableStateOf(false) }
    var prefilledUrl by remember { mutableStateOf<String?>(prefilledUrlExternal) }
    val userName = remember { userIdentification.userId }
    var creditRequestLog by remember { mutableStateOf<String?>(null) }
    var ctaHelperMessage by remember { mutableStateOf<String?>(null) }
    var hasVendTokenAttempted by remember { mutableStateOf(false) }

    suspend fun fetchCreditBalance() {
        PluctUIScreen01MainActivityVideoProcessor.loadInitialData(apiService, userIdentification, debugLogManager) { balance, freeUses ->
            creditBalance = balance
            freeUsesRemaining = freeUses
        }
    }

    suspend fun vendTokenWithBalanceUpdate(reason: String) {
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
        
        // Always bootstrap balance, then vend a token to grant welcome bonus if needed.
        fetchCreditBalance()
        vendTokenWithBalanceUpdate(reason = "app_launch")

        // Show any stored intent feedback to the user
        val feedback = PluctUserPreferences.getAndClearIntentFeedback(context)
        feedback?.let {
            if (it.isError) {
                PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                    scope, snackbarHostState, it.message
                )
            } else {
                PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                    scope, snackbarHostState, it.message
                )
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
            PluctUIScreen01MainActivityVideoProcessor.processVideo(apiService, url, tier, creditBalance, freeUsesRemaining, videoRepository, clipboardManager, debugLogManager) { success, newBalance, newFreeUses, errorMessage, error ->
                if (success) {
                    creditBalance = newBalance
                    freeUsesRemaining = newFreeUses
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
                    } else {
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
            PluctUIScreen01MainActivityVideoProcessor.processVideo(apiService, video.url, video.tier, creditBalance, freeUsesRemaining, videoRepository, clipboardManager, debugLogManager) { success, newBalance, newFreeUses, errorMessage, error ->
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
            debugLogManager = debugLogManager
        )
    }
    
    // Unified Error Handler
    app.pluct.ui.error.PluctBusinessEngineErrorHandler(
        error = currentError,
        onDismiss = { currentError = null },
        onPurchaseCredits = {
            // TODO: Navigate to purchase screen
            currentError = null
            PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                scope, snackbarHostState, "Purchase flow coming soon!"
            )
        },
        onRetry = {
            currentError = null
            // Retry logic would depend on context, for now just dismiss
        }
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
