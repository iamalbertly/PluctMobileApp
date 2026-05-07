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
import androidx.compose.runtime.LaunchedEffect
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
import app.pluct.ui.screens.PluctUIScreen01MainActivity07CreditRequestHandler
import app.pluct.ui.screens.PluctUIScreen01MainActivity08Dialogs
import app.pluct.ui.screens.PluctVideoDetailScreen
import app.pluct.ui.components.PluctUIComponent05Notification01SnackbarManager
import app.pluct.ui.components.PluctUIComponent09ContextualPermission01Dialog
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
import app.pluct.core.permission.PluctCorePermission02Launcher01Helper
import app.pluct.ui.components.PluctUIComponent06Permission01Onboarding01Dialog
import app.pluct.ui.components.PluctUIComponent05Notification02Toast01Helper
import app.pluct.ui.components.PluctUIComponent07Onboarding01Tutorial01Flow

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
    private val themeModeState = mutableStateOf("system") // "system", "light", "dark"
    
    // Permission launcher helper
    private lateinit var permissionLauncherHelper: PluctCorePermission02Launcher01Helper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize permission launcher helper
        permissionLauncherHelper = PluctCorePermission02Launcher01Helper(this)
        permissionLauncherHelper.initialize()

        // Initialize debug log manager (cleanup old logs on startup)
        debugLogManager.initialize()

        // Load saved theme mode
        val prefs = PluctUserPreferences(this)
        themeModeState.value = prefs.getThemeMode()
        
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
            // Theme controlled by user preference
            val isDark = when (themeModeState.value) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            PluctTheme(darkTheme = isDark) {
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
                        validator = validator,
                        isLoadingCreditBalance = isLoadingCreditBalanceState.value,
                        onLoadingCreditBalanceChange = { isLoadingCreditBalanceState.value = it },
                        permissionLauncherHelper = permissionLauncherHelper,
                        onThemeModeChange = { mode -> themeModeState.value = mode }
                    )
                }
            }
        }
    }
    
    // Deprecated methods removed - using ActivityResultLauncher instead
    
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
    validator: app.pluct.services.PluctCoreValidationInputSanitizer,
    isLoadingCreditBalance: Boolean = false,
    onLoadingCreditBalanceChange: (Boolean) -> Unit = {},
    permissionLauncherHelper: PluctCorePermission02Launcher01Helper? = null,
    onThemeModeChange: ((String) -> Unit)? = null
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
    // creditRequestLog removed - no longer needed after extracting credit request handler
    var ctaHelperMessage by remember { mutableStateOf<String?>(null) }
    var hasVendTokenAttempted by remember { mutableStateOf(false) }
    var showPermissionOnboarding by remember { mutableStateOf(false) }
    var showOnboardingTutorial by remember { mutableStateOf(false) }
    
    // Track queued and processing videos for notifications
    val queuedVideos = videoRepository.getVideosByStatus(ProcessingStatus.QUEUED)
        .collectAsState(initial = emptyList())
    val processingVideos = videoRepository.getVideosByStatus(ProcessingStatus.PROCESSING)
        .collectAsState(initial = emptyList())
    val queuedCount = queuedVideos.value.size
    val processingCount = processingVideos.value.size
    
    // Use extracted effects handler
    PluctUIScreen01MainActivity04EffectsHandler(
        validator = validator,
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
    }
    
    // Check if tutorial is needed - skip permission onboarding (now contextual)
    LaunchedEffect(showWelcomeDialog) {
        if (!showWelcomeDialog) {
            // Welcome dialog dismissed, check if tutorial needed
            delay(500) // Small delay after welcome dialog
            val prefs = PluctUserPreferences(context)
            if (!prefs.hasSeenOnboardingTutorial()) {
                showOnboardingTutorial = true
            }
        }
    }
    
    // Show contextual permission request after first transcript
    var showContextualPermissionRequest by remember { mutableStateOf(false) }
    var firstTranscriptJustCompleted by remember { mutableStateOf(false) }
    
    // Listen for first transcript completion
    LaunchedEffect(Unit) {
        val prefs = PluctUserPreferences(context)
        if (prefs.isFirstTranscriptCompleted() && !prefs.hasSeenNotificationOnboarding()) {
            firstTranscriptJustCompleted = true
            Log.d("MainActivity", "First transcript completed - will show contextual permission after celebration")
        }
    }
    
    // Show permission dialog after celebration delay (3 seconds to let celebration show)
    LaunchedEffect(firstTranscriptJustCompleted) {
        if (firstTranscriptJustCompleted) {
            delay(3000) // Wait 3 seconds to let celebration toast/snackbar complete
            val prefs = PluctUserPreferences(context)
            if (!prefs.hasSeenNotificationOnboarding()) {
                showContextualPermissionRequest = true
                Log.d("MainActivity", "Showing contextual permission request after first transcript celebration")
            }
        }
    }
    
    // Optimistic balance loading: Show default immediately, fetch real balance in background
    LaunchedEffect(Unit) {
        // Set optimistic default balance immediately (non-blocking)
        creditBalance = 3
        freeUsesRemaining = 3
        hasLoadedBalanceOnce = true
        isLoading = false
        onLoadingCreditBalanceChange(false)
        
        Log.d("MainActivity", "Optimistic balance set immediately: 3 credits, 3 free uses")
        Log.d("MainActivity", "UI is now interactive - user can start exploring immediately")
        
        // Small delay to ensure UI is stable before background fetch
        delay(100)
        
        // Load real balance in background without blocking UI
        scope.launch {
            try {
                Log.d("MainActivity", "Background balance fetch started (non-blocking)")
                val startTime = System.currentTimeMillis()
                fetchCreditBalance()
                val duration = System.currentTimeMillis() - startTime
                
                Log.d("MainActivity", "Background balance fetch completed in ${duration}ms: $creditBalance credits, $freeUsesRemaining free uses")
                
                // Vend token if needed (background operation)
                if (creditBalance < 1 && freeUsesRemaining < 1) {
                    Log.d("MainActivity", "No credits available, attempting token vend in background")
                    vendTokenWithBalanceUpdate(reason = "app_launch")
                } else {
                    hasVendTokenAttempted = true
                    ctaHelperMessage = if (creditBalance > 0) null else "Add credits to unlock transcription"
                    Log.d("MainActivity", "Credits available: $creditBalance, no token vend needed")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Background balance fetch failed: ${e.message}", e)
                Log.d("MainActivity", "User still has optimistic 3 credits - app remains functional")
                // Silent failure - user already has optimistic 3 credits
                // Don't show error message for background operation
            }
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
            validator = validator,
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

    LaunchedEffect(prefilledUrl) {
        if (!prefilledUrl.isNullOrBlank() && selectedVideo != null) {
            selectedVideo = null
        }
    }
    
    // Main UI
    val currentSelectedVideo = selectedVideo
    if (currentSelectedVideo != null) {
        PluctVideoDetailScreen(
            video = currentSelectedVideo,
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
            onRequestCredits = PluctUIScreen01MainActivity07CreditRequestHandler.createOnRequestCredits(
                userIdentification = userIdentification,
                debugLogManager = debugLogManager,
                snackbarHostState = snackbarHostState
            ),
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
            isLoadingCreditBalance = isLoadingCreditBalance,
            onShowTutorial = {
                // Re-open tutorial from inline hint
                showOnboardingTutorial = true
                Log.d("MainActivity", "Tutorial reopened from inline hint")
            },
            onThemeModeChange = onThemeModeChange
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
    
    // Welcome and Permission Dialogs
    PluctUIScreen01MainActivity08Dialogs.WelcomeDialog(
        showWelcomeDialog = showWelcomeDialog,
        onDismiss = { showWelcomeDialog = false },
        onGetStarted = {
            showWelcomeDialog = false
            refreshCreditBalance()
        }
    )
    
    // Contextual permission request (after first transcript)
    if (showContextualPermissionRequest) {
        PluctUIComponent09ContextualPermission01Dialog(
            onDismiss = { 
                showContextualPermissionRequest = false
                val prefs = PluctUserPreferences(context)
                prefs.markNotificationOnboardingSeen()
            },
            onEnable = {
                permissionLauncherHelper?.requestNotificationPermission { granted ->
                    showContextualPermissionRequest = false
                    val prefs = PluctUserPreferences(context)
                    prefs.markNotificationOnboardingSeen()
                    Log.d("MainActivity", "Contextual notification permission: $granted")
                }
            }
        )
    }

    // Onboarding Tutorial Dialog - shows after permissions complete
    if (showOnboardingTutorial) {
        PluctUIComponent07Onboarding01Tutorial01Flow(
            onComplete = {
                showOnboardingTutorial = false
                Log.d("MainActivity", "Onboarding tutorial completed - user opening TikTok")
            },
            onSkip = {
                showOnboardingTutorial = false
                Log.d("MainActivity", "Onboarding tutorial skipped")
            },
            apiService = apiService,
            onBalanceRefresh = refreshCreditBalance
        )
    }
}
