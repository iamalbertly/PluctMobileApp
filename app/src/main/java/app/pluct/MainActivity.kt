package app.pluct

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.pluct.core.error.ErrorEnvelope
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingStatus
import app.pluct.ui.error.PluctErrorBanner
import app.pluct.ui.screens.PluctHomeScreen
import app.pluct.ui.screens.PluctCaptureSheet
import app.pluct.ui.screens.PluctSettingsScreen
import app.pluct.ui.screens.PluctQuickScanSheet
import app.pluct.ui.screens.PluctProcessingOverlay
import app.pluct.data.entity.ProcessingTier
import app.pluct.ui.theme.PluctTheme
import app.pluct.viewmodel.PluctMainViewModelOrchestrator

/**
 * Pluct-Main-01Activity - Full-featured main activity with proper state management
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            PluctTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PluctMainContent()
                }
            }
        }
    }
}

@Composable
fun PluctMainContent(
    viewModel: PluctMainViewModelOrchestrator = viewModel()
) {
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val currentError by viewModel.currentError.collectAsStateWithLifecycle()
    val creditBalance by viewModel.creditBalance.collectAsStateWithLifecycle()
    val isCreditBalanceLoading by viewModel.isCreditBalanceLoading.collectAsStateWithLifecycle()
    val creditBalanceError by viewModel.creditBalanceError.collectAsStateWithLifecycle()
    
    var showCaptureSheet by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showQuickScanSheet by remember { mutableStateOf(false) }
    var showProcessingOverlay by remember { mutableStateOf(false) }
    var selectedTier by remember { mutableStateOf(ProcessingTier.STANDARD) }
    var currentProcessingVideo by remember { mutableStateOf<VideoItem?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            showSettings -> {
                PluctSettingsScreen(
                    onBackClick = { showSettings = false }
                )
            }
            else -> {
                PluctHomeScreen(
                    videos = videos,
                    onCaptureClick = { 
                        android.util.Log.d("MainActivity", "Capture button clicked")
                        showCaptureSheet = true 
                    },
                    onVideoClick = { video ->
                        android.util.Log.d("MainActivity", "Video clicked: ${video.id}")
                        currentProcessingVideo = video
                        showProcessingOverlay = true
                    },
                    onRetryVideo = { video ->
                        android.util.Log.d("MainActivity", "Retry video: ${video.id}")
                        viewModel.retryVideo(video)
                    },
                    onDeleteVideo = { video ->
                        android.util.Log.d("MainActivity", "Delete video: ${video.id}")
                        viewModel.deleteVideo(video)
                    },
                    creditBalance = creditBalance,
                    isCreditBalanceLoading = isCreditBalanceLoading,
                    creditBalanceError = creditBalanceError,
                    onRefreshCreditBalance = { 
                        viewModel.refreshCreditBalance()
                    },
                    onSettingsClick = { showSettings = true }
                )
            }
        }
        
        PluctCaptureSheet(
            isVisible = showCaptureSheet,
            onDismiss = { showCaptureSheet = false },
            onUrlSubmit = { url ->
                android.util.Log.d("MainActivity", "onUrlSubmit called with url: $url")
                showCaptureSheet = false
                showQuickScanSheet = true
            }
        )
        
        PluctQuickScanSheet(
            isVisible = showQuickScanSheet,
            onDismiss = { showQuickScanSheet = false },
            onTierSelected = { tier ->
                android.util.Log.d("MainActivity", "Tier selected: $tier")
                selectedTier = tier
                showQuickScanSheet = false
                // Start processing with selected tier
                val currentUrl = "https://vm.tiktok.com/ZMADQVF4e/" // Default test URL
                android.util.Log.d("MainActivity", "Starting processing with tier: $tier, URL: $currentUrl")
                viewModel.processVideo(currentUrl, tier)
                
                // Create a processing video item for the overlay
                val processingVideo = VideoItem(
                    id = java.util.UUID.randomUUID().toString(),
                    url = currentUrl,
                    title = "Processing...",
                    thumbnailUrl = "",
                    author = "Unknown",
                    duration = 0L,
                    status = ProcessingStatus.QUEUED,
                    progress = 0,
                    transcript = null,
                    timestamp = System.currentTimeMillis(),
                    tier = tier,
                    createdAt = System.currentTimeMillis()
                )
                currentProcessingVideo = processingVideo
                showProcessingOverlay = true
            },
            selectedTier = selectedTier
        )
        
        PluctProcessingOverlay(
            isVisible = showProcessingOverlay,
            video = currentProcessingVideo,
            onDismiss = { 
                showProcessingOverlay = false
                currentProcessingVideo = null
            }
        )
        
        // Error banner
        PluctErrorBanner(
            error = currentError,
            onDismiss = { viewModel.dismissError() }
        )
    }
}
