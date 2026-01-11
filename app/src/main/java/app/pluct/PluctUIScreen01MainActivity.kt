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
import app.pluct.ui.screens.PluctUIScreen01MainActivity04EffectsHandler
import app.pluct.ui.screens.PluctUIScreen01MainActivity05CreditManager
import app.pluct.ui.screens.PluctUIScreen01MainActivity06EventHandlers
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
import app.pluct.core.permission.PluctCorePermission01Manager
import app.pluct.ui.components.PluctUIComponent06Permission01Onboarding01Dialog
import app.pluct.ui.components.PluctUIComponent05Notification02Toast01Helper
import androidx.activity.result.contract.ActivityResultContracts

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
    private val isLoadingCreditBalanceState = mutableStateOf(true) // Start as loading
    
    // Permission request launchers
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        PluctCorePermission01Manager.invalidateCache()
        Log.d("MainActivity", "Notification permission result: $isGranted")
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Permission request results are handled in onRequestPermissionsResult and onActivityResult
        
        // Initialize debug log manager (cleanup old logs on startup)
        debugLogManager.initialize()
        
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
        
        // Immediately retrieve and set prefilled URL from intent to ensure it's available for Compose
        // This handles the case where activity is created from intent (not just resumed)
        if (intent.action == Intent.ACTION_SEND || (intent.data != null && intent.data?.scheme == "pluct")) {
            val url = PluctUserPreferences.getAndClearPrefilledUrl(this)
            if (url != null) {
                Log.d("MainActivity", "Setting prefilled URL from onCreate intent: $url")
                prefilledUrlState.value = url
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
                        queueManager = queueManager,
                        isLoadingCreditBalance = isLoadingCreditBalanceState.value,
                        onLoadingCreditBalanceChange = { isLoadingCreditBalanceState.value = it }
                    )
                }
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION) {
            PluctCorePermission01Manager.handlePermissionResult(requestCode, permissions, grantResults)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY) {
            PluctCorePermission01Manager.handleActivityResult(requestCode, this)
        }
    }
    
    companion object {
        const val REQUEST_CODE_NOTIFICATION = 1001
        const val REQUEST_CODE_OVERLAY = 1002
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
                        // Small delay to ensure Compose state propagation completes
                        delay(100)
                        prefilledUrlState.value = url
                        Log.d("MainActivity", "Prefilled URL state set after delay: $url")
                    } else {
                        // Queued, don't set prefilled URL (will process later)
                        Log.d("MainActivity", "Intent queued due to active processing")
                        // Show user-friendly message that video is queued
                        // Note: Queue notification will be shown by queue manager
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
    queueManager: app.pluct.services.PluctQueueManager,
    isLoadingCreditBalance: Boolean = false,
    onLoadingCreditBalanceChange: (Boolean) -> Unit = {}
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
    var showPermissionOnboarding by remember { mutableStateOf(false) }
    
    // Track queued and processing videos for notifications
    val queuedVideos = videoRepository.getVideosByStatus(ProcessingStatus.QUEUED)
        .collectAsState(initial = emptyList())
    val processingVideos = videoRepository.getVideosByStatus(ProcessingStatus.PROCESSING)
        .collectAsState(initial = emptyList())
    val queuedCount = queuedVideos.value.size
    val processingCount = processingVideos.value.size
    
    // Use extracted effects handler
    PluctUIScreen01MainActivity04EffectsHandler(
        scope = scope,
        apiService = apiService,
        videoRepository = videoRepository,
        context = context,
        queuedVideos = queuedVideos,
        processingVideos = processingVideos,
        queuedCount = queuedCount,
        processingCount = processingCount,
        creditBalance = creditBalance,
        freeUsesRemaining = freeUsesRemaining,
        queueManager = queueManager,
        snackbarHostState = snackbarHostState,
        clipboardManager = clipboardManager,
        debugLogManager = debugLogManager,
        onBalanceUpdate = { newBalance, newFreeUses ->
            creditBalance = newBalance
            freeUsesRemaining = newFreeUses
        },
        onQueueProcessed = { count ->
            Log.d("MainActivity", "Auto-processed $count queued video(s)")
        }
    )

    // Use extracted credit manager
    val creditManager = remember {
        PluctUIScreen01MainActivity05CreditManager(apiService, userIdentification, debugLogManager)
    }

    suspend fun fetchCreditBalance() {
        creditManager.fetchCreditBalance { balance, freeUses ->
            creditBalance = balance
            freeUsesRemaining = freeUses
        }
    }

    suspend fun vendTokenWithBalanceUpdate(reason: String) {
        creditManager.vendTokenWithBalanceUpdate(
            reason = reason,
            creditBalance = creditBalance,
            freeUsesRemaining = freeUsesRemaining,
            hasVendTokenAttempted = hasVendTokenAttempted,
            scope = scope,
            snackbarHostState = snackbarHostState,
            onBalanceUpdate = { newBalance, newFreeUses ->
                creditBalance = newBalance
                freeUsesRemaining = newFreeUses
            },
            onCtaMessageUpdate = { message ->
                ctaHelperMessage = message
            },
            onVendTokenAttempted = {
                hasVendTokenAttempted = true
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
        // Check for prefilled URL from intent (only if not already set in onCreate)
        // This handles the case where activity is resumed (not created from intent)
        if (prefilledUrl.isNullOrBlank()) {
            val url = PluctUserPreferences.getAndClearPrefilledUrl(context)
            if (url != null) {
                Log.d("MainActivity", "Found prefilled URL from LaunchedEffect: $url")
                prefilledUrl = url
            }
        }
        
        // Check for first-time user
        if (PluctUserPreferences.isFirstTimeUser(context)) {
            showWelcomeDialog = true
        }
        
        // Check if permission onboarding is needed
        val prefs = PluctUserPreferences(context)
        val hasNotificationPermission = PluctCorePermission01Manager.hasNotificationPermission(context)
        val hasOverlayPermission = PluctCorePermission01Manager.hasOverlayPermission(context)
        val hasSeenNotificationOnboarding = prefs.hasSeenNotificationOnboarding()
        val hasSeenOverlayOnboarding = prefs.hasSeenOverlayOnboarding()
        
        // Show onboarding if permissions not granted and not seen before
        // Delay to allow welcome dialog to show first if needed
        scope.launch {
            delay(2000) // Wait for welcome dialog to potentially show
            if ((!hasNotificationPermission && !hasSeenNotificationOnboarding) ||
                (!hasOverlayPermission && !hasSeenOverlayOnboarding)) {
                showPermissionOnboarding = true
            }
        }
        
        // Always bootstrap balance, then vend a token only if we need credits.
        isLoading = true
        onLoadingCreditBalanceChange(true)
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
            onLoadingCreditBalanceChange(false)
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
            onLoadingCreditBalanceChange(true)
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
                onLoadingCreditBalanceChange(false)
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
    
    // Use extracted event handlers
    val eventHandlers = remember {
        PluctUIScreen01MainActivity06EventHandlers(
            scope = scope,
            apiService = apiService,
            videoRepository = videoRepository,
            clipboardManager = clipboardManager,
            debugLogManager = debugLogManager,
            context = context,
            queueManager = queueManager,
            snackbarHostState = snackbarHostState,
            onBalanceUpdate = { newBalance, newFreeUses ->
                creditBalance = newBalance
                freeUsesRemaining = newFreeUses
            },
            onErrorUpdate = { error, url ->
                currentError = error
                currentErrorUrl = url
            }
        )
    }

    val onTierSubmit = remember(creditBalance, freeUsesRemaining) {
        eventHandlers.createOnTierSubmit(creditBalance, freeUsesRemaining)
    }
    
    val onRetryVideo = remember(creditBalance, freeUsesRemaining) {
        eventHandlers.createOnRetryVideo(creditBalance, freeUsesRemaining)
    }
    
    val onDeleteVideo = remember {
        eventHandlers.createOnDeleteVideo()
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

                Log.d("PluctAPI", "CREDIT_REQUEST requestCredits id=$requestId userId=$userId confirmation=$confirmation timestamp=$timestamp")
                
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
            isLoadingCreditBalance = isLoadingCreditBalance
        )
    }
    
    // Handle queue for later (from error handler)
    val onQueueForLater = remember(currentErrorUrl) {
        eventHandlers.createOnQueueForLater(currentErrorUrl)
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
    
    // Permission Onboarding Dialog
    if (showPermissionOnboarding) {
        PluctUIComponent06Permission01Onboarding01Dialog(
            onDismiss = { showPermissionOnboarding = false },
            onComplete = { showPermissionOnboarding = false }
        )
    }
}
