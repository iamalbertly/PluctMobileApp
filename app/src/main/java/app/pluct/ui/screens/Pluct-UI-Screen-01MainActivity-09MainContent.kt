package app.pluct.ui.screens

// SIZE-EXEMPT: Main home/library/settings Compose shell; incremental split tracked in context.md.

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.pluct.data.entity.LogLevel
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.QueueReason
import app.pluct.data.entity.VideoItem
import app.pluct.data.preferences.PluctUserPreferences
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.PluctCoreUserIdentification
import app.pluct.services.PluctCoreValidationInputSanitizer
import app.pluct.services.PluctQueueManager
import app.pluct.shared.PluctClientPolicyModels
import app.pluct.ui.components.PluctDebugLogViewer
import app.pluct.ui.components.PluctHomeShellTopBar
import app.pluct.ui.components.PluctUIComponent05Notification01SnackbarManager
import app.pluct.ui.components.PluctUIComponent07Onboarding01Tutorial01Flow
import app.pluct.ui.components.PluctUIComponent09ContextualPermission01Dialog
import app.pluct.ui.navigation.PluctUIMainShellBottomBar
import app.pluct.ui.navigation.PluctUIMainShellTab
import app.pluct.ui.readiness.PluctUIReadiness01Kind
import app.pluct.ui.readiness.PluctUIReadiness01Resolve
import app.pluct.core.permission.PluctCorePermission02Launcher01Helper
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
/**
 * Main content composable - extracted for better separation of concerns
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    @Suppress("UNUSED_PARAMETER") isLoadingCreditBalance: Boolean = false,
    onLoadingCreditBalanceChange: (Boolean) -> Unit = {},
    permissionLauncherHelper: PluctCorePermission02Launcher01Helper? = null,
    onThemeModeChange: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    fun openUpgradePolicyUrl() {
        val prefs = context.getSharedPreferences("pluct_user_preferences", Context.MODE_PRIVATE)
        val snapshot = prefs.getString("client_policy_snapshot", "") ?: ""
        val policyUrl = PluctClientPolicyModels.updateUrl(snapshot)
            ?: "https://play.google.com/store/apps/details?id=app.pluct"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(policyUrl)))
    }
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    
    // State management - no optimistic fake balance (Speed & Trust)
    var creditBalance by remember { mutableStateOf(0) }
    var freeUsesRemaining by remember { mutableStateOf(0) }
    var balanceKnown by remember { mutableStateOf(false) }
    var balanceLoadFailed by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var hasLoadedBalanceOnce by remember { mutableStateOf(false) }
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
    var hardUpdateRequired by remember { mutableStateOf(false) }
    var softUpdateAvailable by remember { mutableStateOf(false) }
    var policyMessage by remember { mutableStateOf("Update Pluct to continue.") }
    
    // Track queued and processing videos for notifications
    val queuedVideos = videoRepository.getVideosByStatus(ProcessingStatus.QUEUED)
        .collectAsState(initial = emptyList())
    val processingVideos = videoRepository.getVideosByStatus(ProcessingStatus.PROCESSING)
        .collectAsState(initial = emptyList())
    val queuedCount = queuedVideos.value.size
    val processingCount = processingVideos.value.size

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    try {
                        apiService.onAppForegroundedForDiagnostics()
                    } catch (e: Exception) {
                        Log.w("MainActivity", "foreground_diag ${e.message}")
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
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
        balanceKnown = balanceKnown,
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
            prefilledUrl = null
            delay(50)
            prefilledUrl = prefilledUrlExternal
            onPrefilledUrlConsumed()
        }
    }
    
    // Load videos from database using Flow
    val videos by videoRepository.getAllVideos().collectAsState(initial = emptyList())
    val debugLogsMain by debugLogManager.getRecentLogs(100).collectAsState(initial = emptyList())
    val errorLogCountMain = remember(debugLogsMain) {
        debugLogsMain.count { it.level == LogLevel.ERROR }
    }
    var showDebugLogViewerMain by remember { mutableStateOf(false) }
    var mainShellTab by remember { mutableStateOf(PluctUIMainShellTab.HOME) }
    val transcriptionDebugInfo by apiService.transcriptionDebugFlow.collectAsState(initial = null)
    
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

        val policySnap = context.getSharedPreferences("pluct_user_preferences", Context.MODE_PRIVATE)
            .getString("client_policy_snapshot", "") ?: ""
        if (PluctCoreAPIUnifiedService.isPolicyBlockingTranscribe(policySnap)) {
            ctaHelperMessage = "Update Pluct in Play Store, then open again."
        }
        apiService.refreshClientPolicy(force = false)
            .onSuccess { raw ->
                hardUpdateRequired = PluctClientPolicyModels.isHardUpdateRequiredByCode(raw, app.pluct.BuildConfig.VERSION_CODE)
                softUpdateAvailable = PluctClientPolicyModels.isSoftUpdateAvailableByCode(raw, app.pluct.BuildConfig.VERSION_CODE)
                policyMessage = PluctClientPolicyModels.updateMessage(raw, hardUpdateRequired, softUpdateAvailable)
                val updateCta = PluctClientPolicyModels.ctaMessage(hardUpdateRequired, softUpdateAvailable)
                if (hardUpdateRequired && updateCta != null) {
                    ctaHelperMessage = updateCta
                } else if (softUpdateAvailable && ctaHelperMessage.isNullOrBlank()) {
                    ctaHelperMessage = updateCta
                }
            }
            .onFailure { Log.w("MainActivity", "startup_policy_check_failed ${it.message}") }
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
    
    LaunchedEffect(Unit) {
        isLoading = true
        onLoadingCreditBalanceChange(true)
        balanceKnown = false
        balanceLoadFailed = false
        Log.i("MainActivity", "Truth-first balance load started (no optimistic credits)")

        scope.launch {
            try {
                fetchCreditBalance()
                balanceKnown = true
                balanceLoadFailed = false
                hasLoadedBalanceOnce = true
                Log.i(
                    "MainActivity",
                    "Balance fetch completed: $creditBalance credits, $freeUsesRemaining free uses; hasLoadedBalanceOnce=true"
                )

                if (creditBalance < 1 && freeUsesRemaining < 1) {
                    Log.d("MainActivity", "No credits available, attempting token vend in background")
                    vendTokenWithBalanceUpdate(reason = "app_launch")
                } else {
                    hasVendTokenAttempted = true
                    ctaHelperMessage = if (creditBalance > 0) null else "Add credits to unlock transcription"
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Background balance fetch failed: ${e.message}", e)
                Log.i("MainActivity", "Balance fetch ended with error (truth-first, no optimistic credits): ${e.message}")
                balanceKnown = true
                balanceLoadFailed = true
                hasLoadedBalanceOnce = true
                creditBalance = 0
                freeUsesRemaining = 0
                errorMessage = "Could not verify credits"
                ctaHelperMessage = "Add credits to unlock transcription"
            } finally {
                isLoading = false
                onLoadingCreditBalanceChange(false)
            }

            val feedback = PluctUserPreferences.getAndClearIntentFeedback(context)
            feedback?.let {
                if (it.isError) {
                    PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                        scope, snackbarHostState, it.message
                    )
                } else {
                    val shouldSuppressToast = !prefilledUrl.isNullOrBlank() &&
                        balanceKnown &&
                        (creditBalance >= 1 || freeUsesRemaining > 0)

                    if (!shouldSuppressToast) {
                        PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                            scope, snackbarHostState, it.message
                        )
                    }
                }
            }
        }
    }
    
    // Refresh credit balance function
    val refreshCreditBalance: () -> Unit = {
        scope.launch {
            isLoading = true
            onLoadingCreditBalanceChange(true)
            errorMessage = null
            balanceLoadFailed = false
            try {
                fetchCreditBalance()
                hasLoadedBalanceOnce = true
                balanceKnown = true
                balanceLoadFailed = false

                if (!hasVendTokenAttempted || creditBalance < 1) {
                    vendTokenWithBalanceUpdate(reason = if (creditBalance < 1) "refresh_no_credits" else "manual_refresh")
                } else {
                    ctaHelperMessage = if (creditBalance > 0) null else "Add credits to unlock transcription"
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to refresh balance: ${e.message}", e)
                errorMessage = "Failed to refresh balance"
                currentError = e
                hasLoadedBalanceOnce = true
                balanceKnown = true
                balanceLoadFailed = true
                creditBalance = 0
                freeUsesRemaining = 0
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

    val healthMap by apiService.healthStatus.collectAsState(initial = emptyMap())
    val readinessKind = remember(balanceKnown, balanceLoadFailed, creditBalance, freeUsesRemaining, healthMap) {
        PluctUIReadiness01Resolve.resolve(
            context,
            balanceKnown,
            balanceLoadFailed,
            creditBalance,
            freeUsesRemaining,
            healthMap["ttt"],
            healthMap["api"]
        )
    }

    val openWirelessSettings: () -> Unit = {
        try {
            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Log.w("MainActivity", "Could not open wireless settings")
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

    val rawOnTierSubmit = remember(creditBalance, freeUsesRemaining) {
        eventHandlers.createOnTierSubmit(creditBalance, freeUsesRemaining)
    }
    val rawOnRetryVideo = remember(creditBalance, freeUsesRemaining) {
        eventHandlers.createOnRetryVideo(creditBalance, freeUsesRemaining)
    }

    val onTierSubmit = remember(readinessKind, rawOnTierSubmit, scope, snackbarHostState, hardUpdateRequired) {
        { url: String, tier: app.pluct.data.entity.ProcessingTier ->
            if (hardUpdateRequired) {
                openUpgradePolicyUrl()
                PluctUIComponent05Notification01SnackbarManager.showInfoAsync(
                    scope,
                    snackbarHostState,
                    "Update Pluct to continue."
                )
            } else if (readinessKind != PluctUIReadiness01Kind.READY) {
                PluctUIComponent05Notification01SnackbarManager.showInfoAsync(
                    scope,
                    snackbarHostState,
                    "Not ready â€” check the bar above."
                )
            } else {
                rawOnTierSubmit(url, tier)
            }
        }
    }

    val onRetryVideo = remember(readinessKind, rawOnRetryVideo, scope, snackbarHostState) {
        { video: VideoItem ->
            if (readinessKind != PluctUIReadiness01Kind.READY) {
                PluctUIComponent05Notification01SnackbarManager.showInfoAsync(
                    scope,
                    snackbarHostState,
                    "Not ready â€” check the bar above."
                )
            } else {
                rawOnRetryVideo(video)
            }
        }
    }
    
    val onDeleteVideo = remember {
        eventHandlers.createOnDeleteVideo()
    }

    val onQueueForLaterFromError = remember(currentErrorUrl) {
        eventHandlers.createOnQueueForLater(currentErrorUrl)
    }

    val onQueueForLaterForCapture: (String, QueueReason) -> Unit = remember(queueManager, snackbarHostState) {
        { url: String, reason: QueueReason ->
            scope.launch {
                val result = queueManager.queueVideo(
                    url = url,
                    tier = ProcessingTier.EXTRACT_SCRIPT,
                    reason = reason
                )
                if (result.isSuccess) {
                    PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                        scope,
                        snackbarHostState,
                        "Saved. We'll continue when possible."
                    )
                } else {
                    PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                        scope,
                        snackbarHostState,
                        "Could not save. Try again."
                    )
                }
            }
            Unit
        }
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
            onUpgradeClick = { openUpgradePolicyUrl() }
        )
    } else {
        val onRequestCreditsAction = PluctUIScreen01MainActivity07CreditRequestHandler.createOnRequestCredits(
            userIdentification = userIdentification,
            debugLogManager = debugLogManager,
            snackbarHostState = snackbarHostState
        )
        val sendDiagnostic: () -> Unit = {
            scope.launch {
                val breakdown = debugLogManager.formatErrorCategorySummary().orEmpty()
                val text = app.pluct.core.debug.PluctCoreDebug02DiagnosticShare01Builder.buildText(debugLogsMain, breakdown)
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Pluct diagnostic")
                    putExtra(android.content.Intent.EXTRA_TEXT, text)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = android.content.Intent.createChooser(send, "Send diagnostic")
                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(chooser)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Could not start share intent for diagnostic", e)
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                when (mainShellTab) {
                    PluctUIMainShellTab.HOME -> PluctHomeShellTopBar(
                        onSettingsClick = { mainShellTab = PluctUIMainShellTab.SETTINGS },
                        creditBalance = creditBalance,
                        waitingCount = queuedCount
                    )
                    PluctUIMainShellTab.LIBRARY -> CenterAlignedTopAppBar(
                        title = { Text("Library") }
                    )
                    PluctUIMainShellTab.SETTINGS -> CenterAlignedTopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = { mainShellTab = PluctUIMainShellTab.HOME },
                                modifier = Modifier.semantics {
                                    contentDescription = "Back to home"
                                    testTag = "settings_top_bar_back"
                                }
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null)
                            }
                        },
                        title = { Text("Settings") }
                    )
                }
            },
            bottomBar = {
                PluctUIMainShellBottomBar(
                    selected = mainShellTab,
                    onSelect = { mainShellTab = it }
                )
            }
        ) { padding ->
            val homeCtaMessage = when {
                hardUpdateRequired -> PluctClientPolicyModels.HARD_UPDATE_CTA
                softUpdateAvailable && ctaHelperMessage.isNullOrBlank() -> PluctClientPolicyModels.SOFT_UPDATE_CTA
                queuedCount > 0 && creditBalance < 1 && freeUsesRemaining < 1 ->
                    "$queuedCount waiting. Add balance and we'll continue."
                queuedCount > 0 -> "$queuedCount waiting. We'll continue when ready."
                else -> ctaHelperMessage
            }
            when (mainShellTab) {
                PluctUIMainShellTab.HOME -> PluctHomeScreen(
                    creditBalance = creditBalance,
                    freeUsesRemaining = freeUsesRemaining,
                    readinessKind = readinessKind,
                    onReadinessRetryBalance = refreshCreditBalance,
                    onReadinessOpenNetwork = openWirelessSettings,
                    videos = videos,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    userName = userName,
                    onRequestCredits = onRequestCreditsAction,
                    onTierSubmit = onTierSubmit,
                    onRetryVideo = onRetryVideo,
                    onDeleteVideo = onDeleteVideo,
                    onVideoClick = { video -> selectedVideo = video },
                    prefilledUrl = prefilledUrl,
                    apiService = apiService,
                    onRefreshCreditBalance = refreshCreditBalance,
                    snackbarHostState = snackbarHostState,
                    videoRepository = videoRepository,
                    ctaHelperMessage = homeCtaMessage,
                    debugLogManager = debugLogManager,
                    onQueueForLater = onQueueForLaterForCapture,
                    isLoadingCreditBalance = isLoading || !balanceKnown,
                    onShowTutorial = {
                        showOnboardingTutorial = true
                        Log.d("MainActivity", "Tutorial reopened from inline hint")
                    },
                    onThemeModeChange = onThemeModeChange,
                    useInnerScaffold = false,
                    innerContentPadding = padding,
                    permissionLauncherHelper = permissionLauncherHelper,
                    onViewDebugLogs = { showDebugLogViewerMain = true }
                )
                PluctUIMainShellTab.LIBRARY -> PluctUIScreen02LibraryTab01Screen(
                    paddingValues = padding,
                    videos = videos,
                    onVideoClick = { video -> selectedVideo = video },
                    onRetryVideo = onRetryVideo,
                    onDeleteVideo = onDeleteVideo,
                    snackbarHostState = snackbarHostState,
                    debugInfo = transcriptionDebugInfo
                )
                PluctUIMainShellTab.SETTINGS -> PluctUIScreen03SettingsTab01Screen(
                    paddingValues = padding,
                    userName = userName,
                    creditBalance = creditBalance,
                    debugLogCount = debugLogsMain.size,
                    errorLogCount = errorLogCountMain,
                    onRequestCredits = onRequestCreditsAction,
                    onViewDebugLogs = { showDebugLogViewerMain = true },
                    onSendDiagnostic = sendDiagnostic,
                    permissionLauncherHelper = permissionLauncherHelper,
                    onThemeModeChange = onThemeModeChange
                )
            }
        }

        if (hardUpdateRequired) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Update Pluct") },
                text = { Text(policyMessage) },
                confirmButton = {
                    Button(onClick = { openUpgradePolicyUrl() }) {
                        Text("Update")
                    }
                }
            )
        }

        if (showDebugLogViewerMain) {
            PluctDebugLogViewer(
                logs = debugLogsMain,
                debugLogManager = debugLogManager,
                onDismiss = { showDebugLogViewerMain = false }
            )
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
            currentError = null
            currentErrorUrl = null
            openUpgradePolicyUrl()
            PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                scope, snackbarHostState, "Opening update / credits help"
            )
        },
        onRetry = {
            currentError = null
            currentErrorUrl = null
            // Retry logic would depend on context, for now just dismiss
        },
        url = currentErrorUrl,
        queuedCount = queuedCount,
        onQueueForLater = onQueueForLaterFromError
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

