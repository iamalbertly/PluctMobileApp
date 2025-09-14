package app.pluct.ui.screens

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pluct.ui.components.SimpleNetworkStatus
import app.pluct.ui.components.SimpleBottomNavigation
import app.pluct.ui.screens.ingest.IngestErrorView
import app.pluct.ui.screens.ingest.IngestPendingView
import app.pluct.ui.screens.ingest.IngestReadyView
import app.pluct.ui.screens.ingest.NeedsTranscriptView
import app.pluct.ui.screens.ingest.TranscriptSuccessView
import app.pluct.ui.utils.NetworkHandler
import app.pluct.ui.utils.WebTranscriptResultHandler
import app.pluct.utils.Constants
import app.pluct.viewmodel.IngestState
import app.pluct.viewmodel.IngestViewModel
import app.pluct.web.WebTranscriptActivity
import kotlinx.coroutines.delay

/**
 * Main screen for the transcript ingestion process with enhanced network handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngestScreen(
    url: String,
    onNavigateBack: () -> Unit,
    viewModel: IngestViewModel = hiltViewModel()
) {
    
    // Context needs to be defined before using it in helper functions
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Track processing state and timeout
    var isProcessing by remember { mutableStateOf(false) }
    var processingStartTime by remember { mutableStateOf(0L) }
    var hasShownError by remember { mutableStateOf(false) }
    var showNetworkStatus by remember { mutableStateOf(false) }
    var networkQuality by remember { mutableStateOf(app.pluct.utils.NetworkUtils.NetworkQuality.UNRELIABLE) }
    
    /**
     * Helper function to launch WebTranscriptActivity
     */
    fun launchWebActivity(
        videoId: String?,
        processedUrl: String?,
        launcher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>
    ) {
        WebTranscriptResultHandler.launchWebActivity(
            context = context,
            videoId = videoId,
            processedUrl = processedUrl,
            launcher = launcher
        ) {
            isProcessing = true
            processingStartTime = System.currentTimeMillis()
            hasShownError = false
            // Set the provider being used
            val provider = app.pluct.ui.utils.ProviderSettings.getSelectedProvider(context)
            viewModel.setProviderUsed(provider.name)
        }
    }
    
    /**
     * Enhanced network connectivity check with quality assessment
     */
    fun checkNetworkConnectivity(): Boolean {
        val result = NetworkHandler.checkNetworkConnectivity(context)
        showNetworkStatus = result.showNetworkStatus
        networkQuality = result.networkQuality
        return result.isConnected
    }
    
    // Activity result launcher for WebTranscriptActivity
    val webTranscriptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isProcessing = false
        WebTranscriptResultHandler.handleWebTranscriptResult(
            context = context,
            resultCode = result.resultCode,
            data = result.data,
            viewModel = viewModel,
            onErrorShown = { hasShownError = true }
        )
    }
    
    // Monitor state changes for auto-launching WebTranscriptActivity
    LaunchedEffect(uiState.state, uiState.videoId) {
        Log.d("PluctIngest", "State changed to: ${uiState.state}, videoId: ${uiState.videoId}, hasLaunchedWebActivity: ${uiState.hasLaunchedWebActivity}")
        
        // Check network connectivity first with enhanced detection
        if (!checkNetworkConnectivity()) {
            // Save the URL for later processing when internet returns
            viewModel.saveUrlForLaterProcessing(url)
            return@LaunchedEffect
        }
        
        // Immediately launch WebTranscriptActivity when we need a transcript
        if (uiState.state == IngestState.NEEDS_TRANSCRIPT && 
            uiState.videoId != null && 
            uiState.processedUrl != null &&
            !uiState.hasLaunchedWebActivity) {
            
            Log.d("PluctIngest", "Auto-launching WebTranscriptActivity immediately")
            viewModel.markWebActivityLaunched()
            
            // Launch using helper function
            launchWebActivity(uiState.videoId, uiState.processedUrl, webTranscriptLauncher)
        }
    }
    
    // Enhanced timeout detection for processing with network-aware timeouts
    // Only timeout if WebView automation is not progressing
    LaunchedEffect(isProcessing, uiState.hasLaunchedWebActivity) {
        if (isProcessing && uiState.hasLaunchedWebActivity) {
            // Use adaptive timeout based on network quality
            val timeout = NetworkHandler.getAdaptiveTimeout(networkQuality)
            
            Log.d("PluctIngest", "Starting timeout timer for ${timeout}ms - WebView automation in progress")
            
            delay(timeout)
            if (isProcessing && uiState.hasLaunchedWebActivity) {
                Log.w("PluctIngest", "Processing timeout detected after ${timeout}ms - WebView automation may have stalled")
                isProcessing = false
                viewModel.resetWebActivityLaunch()
                hasShownError = true
                // Add a timeout error to the UI state
                viewModel.handleWebTranscriptResult(
                    android.app.Activity.RESULT_CANCELED,
                    android.content.Intent().apply {
                        putExtra(WebTranscriptActivity.EXTRA_ERROR_CODE, "timeout")
                        putExtra(WebTranscriptActivity.EXTRA_ERROR_MESSAGE, "WebView automation timed out. The transcript service may be slow or unavailable.")
                    }
                )
            }
        }
    }
    
    // Auto-navigate back on error after user has seen it
    LaunchedEffect(hasShownError, uiState.error, uiState.webErrorCode) {
        if (hasShownError && (uiState.error != null || uiState.webErrorCode != null)) {
            delay(5000) // Wait 5 seconds for user to see error
            if (hasShownError) {
                Log.d("PluctIngest", "Auto-navigating back after error display")
                onNavigateBack()
            }
        }
    }
    
    // Auto-mark error as shown when any error appears (covers invalid URL case)
    LaunchedEffect(uiState.error) {
        if (uiState.error != null && !hasShownError) {
            hasShownError = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Process Video") },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d("IngestScreen", "Back button clicked - navigating back")
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            SimpleBottomNavigation(
                onHomeClick = onNavigateBack,
                onSettingsClick = {
                    // Navigate to settings - we'll need to pass navController or use a different approach
                    // For now, just navigate back to home where user can access settings
                    onNavigateBack()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Show simple network status only if there are connectivity issues
            if (showNetworkStatus) {
                SimpleNetworkStatus(
                    onRetry = {
                        showNetworkStatus = false
                        if (uiState.state == IngestState.NEEDS_TRANSCRIPT) {
                            viewModel.resetWebActivityLaunch()
                        }
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            when (uiState.state) {
                IngestState.PENDING -> {
                    IngestPendingView(url = url)
                }
                
                IngestState.NEEDS_TRANSCRIPT -> {
                    NeedsTranscriptView(
                        url = url,
                        uiState = uiState,
                        onSaveTranscript = { text, language ->
                            viewModel.saveTranscript(text, language, setStateToReady = true)
                        },
                        onLaunchWebTranscript = {
                            launchWebActivity(uiState.videoId, uiState.processedUrl, webTranscriptLauncher)
                        },
                        onNavigateBack = onNavigateBack
                    )
                }
                
                IngestState.TRANSCRIPT_SUCCESS -> {
                    TranscriptSuccessView(
                        uiState = uiState,
                        onGenerateValueProposition = {
                            viewModel.generateValueProposition()
                        },
                        onNavigateBack = onNavigateBack,
                        onTryAnotherProvider = {
                            viewModel.tryAnotherProvider(context)
                        }
                    )
                }
                
                IngestState.READY -> {
                    IngestReadyView(
                        uiState = uiState,
                        onNavigateBack = onNavigateBack
                    )
                }
            }
            
            // Error handling - always show errors regardless of state
            IngestErrorView(
                uiState = uiState,
                onClearError = { 
                    viewModel.clearError()
                    hasShownError = false
                },
                onRetryWebTranscript = {
                    launchWebActivity(uiState.videoId, uiState.processedUrl, webTranscriptLauncher)
                }
            )
        }
    }
}
