package app.pluct.ui.screens

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
import app.pluct.ui.screens.ingest.IngestErrorView
import app.pluct.ui.screens.ingest.IngestPendingView
import app.pluct.ui.screens.ingest.IngestReadyView
import app.pluct.ui.screens.ingest.NeedsTranscriptView
import app.pluct.viewmodel.IngestState
import app.pluct.viewmodel.IngestViewModel
import app.pluct.web.WebTranscriptActivity

/**
 * Main screen for the transcript ingestion process
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
    
    /**
     * Helper function to launch WebTranscriptActivity
     */
    fun launchWebActivity(
        videoId: String?,
        processedUrl: String?,
        launcher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>
    ) {
        if (videoId != null && processedUrl != null) {
            Log.d("PluctIngest", "Launching WebTranscriptActivity for videoId: $videoId")
            val intent = WebTranscriptActivity.createIntent(context, videoId, processedUrl)
            launcher.launch(intent)
        } else {
            Log.e("PluctIngest", "Cannot launch WebTranscriptActivity: missing videoId or processedUrl")
        }
    }
    
    // Track if we've already attempted to launch the WebTranscriptActivity
    var hasAttemptedLaunch by remember { mutableStateOf(false) }
    var lastLaunchTime by remember { mutableStateOf(0L) }
    var launchAttemptCount by remember { mutableStateOf(0) }
    
    // Activity result launcher for WebTranscriptActivity
    val webTranscriptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("PluctIngest", "WebTranscriptActivity returned with resultCode: ${result.resultCode}")
        viewModel.handleWebTranscriptResult(result.resultCode, result.data)
        
        // Reset the attempt flag regardless of success/failure to allow retry on error
        hasAttemptedLaunch = false
        lastLaunchTime = 0L
        
        // Only reset the launch flag if the result was not successful
        // This prevents re-launching after a successful transcript extraction
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            Log.d("PluctIngest", "Resetting WebActivity launch flag due to unsuccessful result")
            viewModel.resetWebActivityLaunch()
        } else {
            Log.d("PluctIngest", "Keeping WebActivity launch flag set due to successful result")
        }
    }
    
    // Monitor state changes for auto-launching WebTranscriptActivity
    LaunchedEffect(uiState.state, uiState.videoId) {
        Log.d("PluctIngest", "State changed to: ${uiState.state}, videoId: ${uiState.videoId}, hasLaunchedWebActivity: ${uiState.hasLaunchedWebActivity}")
        
        // Only launch if we're in the right state and haven't launched yet
        if (uiState.state == IngestState.NEEDS_TRANSCRIPT && 
            !uiState.hasLaunchedWebActivity && 
            !hasAttemptedLaunch &&
            uiState.webErrorCode == null &&
            launchAttemptCount == 0 &&
            (System.currentTimeMillis() - lastLaunchTime) > 5000) { // 5 second cooldown
            
            Log.d("PluctIngest", "Auto-launching WebTranscriptActivity")
            hasAttemptedLaunch = true
            launchAttemptCount = 1
            lastLaunchTime = System.currentTimeMillis()
            viewModel.markWebActivityLaunched()
            
            // Launch using helper function
            launchWebActivity(uiState.videoId, uiState.processedUrl, webTranscriptLauncher)
        } else {
            Log.d("PluctIngest", "Skipping WebTranscriptActivity launch: state=${uiState.state}, hasLaunched=${uiState.hasLaunchedWebActivity}, hasAttempted=$hasAttemptedLaunch, errorCode=${uiState.webErrorCode}, attemptCount=$launchAttemptCount, timeSinceLast=${System.currentTimeMillis() - lastLaunchTime}ms")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Process Video") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
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
            when (uiState.state) {
                IngestState.PENDING -> {
                    IngestPendingView(url = url)
                }
                
                IngestState.NEEDS_TRANSCRIPT -> {
                    NeedsTranscriptView(
                        url = url,
                        uiState = uiState,
                        onSaveTranscript = { text, language ->
                            viewModel.saveTranscript(text, language)
                        },
                        onLaunchWebTranscript = {
                            launchWebActivity(uiState.videoId, uiState.processedUrl, webTranscriptLauncher)
                        },
                        onNavigateBack = onNavigateBack
                    )
                }
                
                IngestState.READY -> {
                    IngestReadyView(
                        uiState = uiState,
                        onNavigateBack = onNavigateBack
                    )
                }
            }
            
            // Error handling
            IngestErrorView(
                uiState = uiState,
                onClearError = { viewModel.clearError() },
                                onRetryWebTranscript = {
                    launchWebActivity(uiState.videoId, uiState.processedUrl, webTranscriptLauncher)
                }
            )
        }
    }
}
