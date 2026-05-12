package app.pluct

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import app.pluct.ui.readiness.PluctUIReadiness01Kind
import app.pluct.ui.readiness.PluctUIReadiness01Resolve
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dagger.hilt.android.AndroidEntryPoint
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.PluctCoreAPIDetailedError
import app.pluct.services.PluctCoreUserIdentification
import app.pluct.services.PluctCoreValidationInputSanitizer
import javax.inject.Inject
import app.pluct.ui.theme.PluctTheme
import app.pluct.ui.screens.PluctMainContent

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
import app.pluct.data.entity.LogLevel
import app.pluct.data.entity.QueueReason
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
import app.pluct.ui.components.PluctHomeShellTopBar
import app.pluct.ui.navigation.PluctUIMainShellBottomBar
import app.pluct.ui.navigation.PluctUIMainShellTab
import app.pluct.ui.screens.PluctUIScreen02LibraryTab01Screen
import app.pluct.ui.screens.PluctUIScreen03SettingsTab01Screen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag

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
    private val themeModeState = mutableStateOf("dark")
    
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
                Log.i("MainActivity", "Setting prefilled URL from onCreate intent: $url")
                prefilledUrlState.value = null
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
                Log.i("MainActivity", "Prefilled URL from new intent: $url")
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
                        prefilledUrlState.value = null
                        delay(100)
                        prefilledUrlState.value = url
                        Log.i("MainActivity", "Prefilled URL state set after delay: $url")
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
