package app.pluct.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.pluct.MainActivity
import app.pluct.data.entity.ProcessingTier
import app.pluct.ui.components.*
import app.pluct.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.zIndex
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.pluct.worker.TTTranscribeWork
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import app.pluct.R
import app.pluct.api.EngineApiProvider
import app.pluct.ui.components.PluctUIStatusDisplay
import app.pluct.ui.components.PluctStatusItem
import app.pluct.ui.components.PluctUIVideoCard
import app.pluct.ui.components.PluctHeaderCompact
import app.pluct.ui.components.PluctUnifiedInput
import app.pluct.ui.components.PluctProgressTimeline
import app.pluct.ui.components.PluctCollapsibleProcessingOverlay
import app.pluct.ui.components.PluctAnimatedProgress
import app.pluct.ui.components.PluctAnimatedCard
import app.pluct.ui.components.PluctFloatingActionButton
import app.pluct.ui.components.PluctErrorDialog
import app.pluct.ui.components.PluctErrorSnackbar
import app.pluct.ui.components.PluctCreditBalanceDisplay
import app.pluct.orchestrator.OrchestratorResult
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Handle capture request from MainActivity - check for new intents continuously
    LaunchedEffect(context) {
        if (context is MainActivity) {
            var lastProcessedUrl: String? = null
            
            while (true) {
                val currentIntent = context.intent
                android.util.Log.d("HomeScreen", "Checking intent: action=${currentIntent?.action}")
                
                if (currentIntent?.action == "app.pluct.action.CAPTURE_INSIGHT") {
                    val url = currentIntent.getStringExtra("capture_url")
                    val caption = currentIntent.getStringExtra("capture_caption")
                    android.util.Log.d("HomeScreen", "CAPTURE_INSIGHT detected: url=$url, caption=$caption")
                    
                    // Only process if this is a new URL (prevent reprocessing same URL)
                    if (url != null && url != lastProcessedUrl) {
                        android.util.Log.i("HomeScreen", "Processing capture request for URL: $url")
                        
                        // Resolve metadata first via EngineApi.meta
                        try {
                            val response = EngineApiProvider.instance.meta(url)
                            if (!response.isSuccessful || response.body() == null) {
                                android.util.Log.w("HomeScreen", "META_RESOLVE_FAILED url=$url code=${response.code()}")
                            } else {
                                val meta = response.body()!!
                                android.util.Log.i("HomeScreen", "Metadata resolved: title=${meta["title"]}, description=${meta["description"]}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("HomeScreen", "Failed to resolve metadata for $url", e)
                        }
                        
                        android.util.Log.d("HomeScreen", "Setting capture request in ViewModel...")
                        viewModel.setCaptureRequest(url, caption)
                        android.util.Log.i("HomeScreen", "Capture request set in ViewModel successfully")
                        lastProcessedUrl = url
                    } else if (url == lastProcessedUrl) {
                        android.util.Log.d("HomeScreen", "Skipping reprocessing of same URL: $url")
                    } else {
                        android.util.Log.e("HomeScreen", "ERROR: URL is null in CAPTURE_INSIGHT intent")
                    }
                }
                
                kotlinx.coroutines.delay(1000) // Check every second
            }
        }
    }
    
    
    // Debug logging for UI state
    LaunchedEffect(uiState.captureRequest) {
        android.util.Log.d("HomeScreen", "UI State changed - captureRequest: ${uiState.captureRequest}")
        if (uiState.captureRequest == null) {
            android.util.Log.w("HomeScreen", "WARNING: captureRequest is null - this might be causing the issue")
        } else {
            android.util.Log.i("HomeScreen", "Capture request is present: ${uiState.captureRequest?.url}")
        }
    }
    
    // Handle shared URLs from Android Share Intent
    LaunchedEffect(Unit) {
        if (context is MainActivity) {
            val sharedUrl = context.intent?.getStringExtra(Intent.EXTRA_TEXT)?.trim()
            if (!sharedUrl.isNullOrEmpty()) {
                android.util.Log.d("HomeScreen", "Shared URL detected: $sharedUrl")
                viewModel.updateVideoUrl(sharedUrl)
            }
        }
    }
    
    // Modern gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Scaffold(
            topBar = {
                // Compact header with integrated credit balance
                PluctHeaderCompact(
                    creditBalance = uiState.creditBalance,
                    isCreditBalanceLoading = uiState.isCreditBalanceLoading,
                    creditBalanceError = uiState.creditBalanceError,
                    onRefreshCreditBalance = { viewModel.refreshCreditBalance() }
                )
            },
            bottomBar = {
                ModernBottomNavigation(navController = navController)
            },
            floatingActionButton = {
                PluctFloatingActionButton(
                    onClick = { /* TODO: Implement add video */ }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add video"
                    )
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Unified input section
                item {
                    PluctUnifiedInput(
                        value = uiState.videoUrl,
                        onValueChange = { viewModel.updateVideoUrl(it) },
                        onProcess = { 
                            // TODO: Implement processing logic
                            viewModel.updateCurrentStage("TOKEN")
                        },
                        isProcessing = uiState.currentStage != "IDLE" && uiState.currentStage != "COMPLETE"
                    )
                }
                
                // Progress timeline (only show when processing)
                if (uiState.currentStage != "IDLE" && uiState.currentStage != "COMPLETE") {
                    item {
                        PluctProgressTimeline(
                            currentStage = uiState.currentStage,
                            progress = uiState.progress
                        )
                    }
                }
                
                // Recent videos
                if (videos.isNotEmpty()) {
                    item {
                        val recentTranscriptsCd = stringResource(R.string.cd_recent_transcripts)
                        Text(
                            text = "Recent Transcripts",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .semantics { contentDescription = recentTranscriptsCd }
                        )
                    }
                    
                    items(videos, key = { it.id }) { video ->
                        PluctUIVideoCard(
                            video = video,
                            onClick = {
                                if (video.status == app.pluct.data.entity.ProcessingStatus.COMPLETED) {
                                    navController.navigate("videoDetail/${video.id}")
                                } else {
                                    Toast.makeText(context, "Video is still processing...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                } else {
                    item {
                        PluctHomeEmptyState()
                    }
                }
            }
        }
    }
    
    // Show capture sheet if there's a capture request - render on top of everything
    uiState.captureRequest?.let { captureRequest ->
        android.util.Log.i("HomeScreen", "Displaying capture sheet for URL: ${captureRequest.url}")
        CaptureInsightSheet(
            captureRequest = captureRequest,
            onDismiss = { 
                android.util.Log.d("HomeScreen", "Capture sheet dismissed")
                viewModel.clearCaptureRequest() 
            },
            onTierSelected = { tier ->
                android.util.Log.i("HomeScreen", "Tier selected: $tier")
                try {
                    val message = when (tier) {
                        ProcessingTier.QUICK_SCAN -> "✅ Queued for Quick Scan. We'll notify you shortly."
                        ProcessingTier.AI_ANALYSIS -> "✨ Queued for AI Analysis. We'll notify you when your insights are ready."
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    
                    viewModel.createVideoWithTier(captureRequest.url, tier)
                    // Background work is already enqueued by WorkManagerUtils in createVideoWithTier
                    android.util.Log.i("HomeScreen", "Tier selection completed successfully")
                } catch (e: Exception) {
                    android.util.Log.e("HomeScreen", "Error in tier selection: ${e.message}", e)
                    Toast.makeText(context, "Error processing selection. Please try again.", Toast.LENGTH_LONG).show()
                }
            },
            viewModel = viewModel
        )
    }

    // Error handling
    uiState.processError?.let { error ->
        PluctErrorDialog(
            error = error,
            onRetry = { 
                viewModel.clearProcessError()
                // TODO: Implement retry logic
            },
            onDismiss = { viewModel.clearProcessError() },
            onViewLogs = { logId -> viewModel.openLogs(logId) },
            onReportIssue = { message, logId -> viewModel.reportIssue(message, logId) }
        )
    }
    
    // Collapsible processing overlay (only show when processing)
    if (uiState.currentStage != "IDLE" && uiState.currentStage != "COMPLETE") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .zIndex(10f),
            contentAlignment = Alignment.BottomCenter
        ) {
            PluctCollapsibleProcessingOverlay(
                currentStage = uiState.currentStage,
                progress = uiState.progress,
                onMinimize = { 
                    // TODO: Implement minimize logic
                    viewModel.updateCurrentStage("IDLE")
                },
                onCancel = { 
                    // TODO: Implement cancel logic
                    viewModel.updateCurrentStage("IDLE")
                }
            )
        }
    }
}