package app.pluct.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.pluct.MainActivity
import app.pluct.data.entity.VideoItem
import app.pluct.ui.components.*
import app.pluct.viewmodel.HomeViewModel

/**
 * HomeScreen - Main screen for video processing
 * Refactored to use focused components following naming convention
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // State for delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<VideoItem?>(null) }
    
    // Register HomeViewModel with MainActivity to handle CAPTURE_INSIGHT intents
    LaunchedEffect(Unit) {
        if (context is MainActivity) {
            android.util.Log.d("HomeScreen", "Registering HomeViewModel with MainActivity")
            context.setHomeViewModel(viewModel)
            android.util.Log.d("HomeScreen", "HomeViewModel registered successfully")
        }
    }
    
    // Debug logging for UI state
    LaunchedEffect(uiState.captureRequest) {
        android.util.Log.d("HomeScreen", "UI State changed - captureRequest: ${uiState.captureRequest}")
        if (uiState.captureRequest == null) {
            android.util.Log.w("HomeScreen", "WARNING: captureRequest is null - this might be causing the issue")
        } else {
            android.util.Log.i("HomeScreen", "✅ Capture request is present: ${uiState.captureRequest?.url}")
            android.util.Log.i("HomeScreen", "✅ Capture request caption: ${uiState.captureRequest?.caption}")
            android.util.Log.i("HomeScreen", "✅ About to render CaptureInsightSheet component")
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
            PluctCompactTopBar(
                credits = uiState.creditBalance,
                onRefreshCredits = { viewModel.refreshCreditBalance() },
                onSettings = { /* TODO: Navigate to settings */ }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Compact URL input box
                PluctCompactUrlBox(
                    value = uiState.videoUrl,
                    onValueChange = { viewModel.updateVideoUrl(it) },
                    onProcess = { 
                        if (uiState.videoUrl.isNotEmpty()) {
                            viewModel.processVideo(uiState.videoUrl)
                        }
                    },
                    enabled = uiState.videoUrl.isNotEmpty() && uiState.creditBalance > 0
                )
                
                // Recent Transcripts section with horizontal scrolling
                if (videos.isNotEmpty()) {
                    PluctCompactRecentTranscripts(
                        sections = videos,
                        onSeeAll = { /* TODO: Navigate to full transcripts list */ },
                        onVideoClick = { video ->
                            // TODO: Navigate to video details
                        }
                    )
                } else {
                    // Compact empty state
                    PluctCompactEmptyState()
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && videoToDelete != null) {
        PluctDeleteConfirmationDialog(
            videoTitle = videoToDelete!!.title ?: "Untitled Video",
            onDismiss = { 
                showDeleteDialog = false
                videoToDelete = null
            },
            onConfirm = {
                videoToDelete?.let { video ->
                    viewModel.deleteVideo(video.id)
                }
                showDeleteDialog = false
                videoToDelete = null
            }
        )
    }
    
    // Capture insight sheet
    uiState.captureRequest?.let { captureRequest ->
        CaptureInsightSheet(
            captureRequest = captureRequest,
            viewModel = viewModel,
            onDismiss = { viewModel.clearCaptureRequest() },
            onTierSelected = { tier ->
                try {
                    viewModel.createVideoWithTier(captureRequest.url, tier, context)
                    android.util.Log.i("HomeScreen", "🎯 Tier selection completed successfully")
                    android.util.Log.i("HomeScreen", "🎯 Background work enqueued successfully")
                } catch (e: Exception) {
                    android.util.Log.e("HomeScreen", "❌ Error in createVideoWithTier: ${e.message}", e)
                    // TODO: Show error toast
                }
            }
        )
    }
}