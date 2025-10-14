package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pluct.ui.components.NetworkStatusView
import app.pluct.ui.components.ModernBottomNavigation
import app.pluct.ui.screens.ingest.IngestErrorView
import app.pluct.ui.screens.ingest.IngestPendingView
import app.pluct.ui.screens.ingest.IngestReadyView
import app.pluct.ui.screens.ingest.NeedsTranscriptView
import app.pluct.ui.screens.ingest.TranscriptSuccessView
import app.pluct.ui.utils.NetworkHandler
import app.pluct.utils.Constants
import app.pluct.viewmodel.IngestState
import app.pluct.viewmodel.IngestUiState
import app.pluct.viewmodel.IngestViewModel
import kotlinx.coroutines.delay

/**
 * Simplified IngestScreen for Pluct
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngestScreen(
    url: String,
    onNavigateBack: () -> Unit,
    viewModel: IngestViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Track processing state
    var _isProcessing by remember { mutableStateOf(false) }
    var showNetworkStatus by remember { mutableStateOf(false) }
    
    /**
     * Enhanced network connectivity check
     */
    fun checkNetworkConnectivity(): Boolean {
        val result = NetworkHandler.checkNetworkConnectivity(context)
        showNetworkStatus = result.showNetworkStatus
        return result.isConnected
    }
    
    // Check network connectivity on composition
    LaunchedEffect(Unit) {
        if (!checkNetworkConnectivity()) {
            showNetworkStatus = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Process Video") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                }
            )
        },
        bottomBar = {
            // Simplified bottom navigation for now
            BottomAppBar {
                Text("Pluct", modifier = Modifier.padding(16.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Show network status if needed
            if (showNetworkStatus) {
                NetworkStatusView(
                    context = context,
                    onRetry = {
                        showNetworkStatus = false
                        if (uiState.state == IngestState.NEEDS_TRANSCRIPT) {
                            viewModel.resetWebActivityLaunch()
                        }
                    },
                    onManualMode = {
                        // Handle manual mode
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Main content based on state
            when (uiState.state) {
                IngestState.IDLE -> {
                    IngestReadyView(
                        uiState = uiState,
                        onStartProcessing = {
                            viewModel.startTranscriptionFlow(url)
                        }
                    )
                }
                IngestState.LOADING -> {
                    IngestPendingView(
                        uiState = uiState,
                        onCancel = {
                            viewModel.clearError()
                        }
                    )
                }
                IngestState.READY -> {
                    IngestReadyView(
                        uiState = uiState,
                        onStartProcessing = {
                            viewModel.startTranscriptionFlow(url)
                        }
                    )
                }
                IngestState.NEEDS_TRANSCRIPT -> {
                    NeedsTranscriptView(
                        uiState = uiState,
                        onStartWebTranscript = {
                            // Simplified web transcript handling
                            viewModel.markWebActivityLaunched()
                        }
                    )
                }
                IngestState.SUCCESS -> {
                    TranscriptSuccessView(
                        uiState = uiState,
                        onViewTranscript = {
                            // Handle viewing transcript
                        }
                    )
                }
                IngestState.ERROR -> {
                    IngestErrorView(
                        uiState = uiState,
                        onClearError = {
                            viewModel.clearError()
                        },
                        onRetryWebTranscript = {
                            viewModel.tryAnotherProvider()
                        }
                    )
                }
                IngestState.TRANSCRIPT_SUCCESS -> {
                    TranscriptSuccessView(
                        uiState = uiState,
                        onViewTranscript = {
                            // Handle viewing transcript
                        }
                    )
                }
                IngestState.PROCESSING -> {
                    IngestPendingView(
                        uiState = uiState,
                        onCancel = {
                            viewModel.clearError()
                        }
                    )
                }
            }
        }
    }
}