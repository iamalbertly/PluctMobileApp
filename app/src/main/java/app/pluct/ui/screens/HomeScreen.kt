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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Handle capture request from MainActivity
    LaunchedEffect(Unit) {
        if (context is MainActivity) {
            val intent = context.intent
            android.util.Log.d("HomeScreen", "Initial intent check: action=${intent?.action}")
            
            var attempts = 0
            while (attempts < 10) {
                val currentIntent = context.intent
                android.util.Log.d("HomeScreen", "Checking intent attempt $attempts: action=${currentIntent?.action}")
                
                if (currentIntent?.action == "app.pluct.action.CAPTURE_INSIGHT") {
                    val url = currentIntent.getStringExtra("capture_url")
                    val caption = currentIntent.getStringExtra("capture_caption")
                    android.util.Log.d("HomeScreen", "CAPTURE_INSIGHT detected: url=$url, caption=$caption")
                    android.util.Log.i("HomeScreen", "Processing capture request for URL: $url")
                    
                    if (url != null) {
                        android.util.Log.d("HomeScreen", "Setting capture request in ViewModel...")
                        viewModel.setCaptureRequest(url, caption)
                        android.util.Log.i("HomeScreen", "Capture request set in ViewModel successfully")
                        break
                    } else {
                        android.util.Log.e("HomeScreen", "ERROR: URL is null in CAPTURE_INSIGHT intent")
                    }
                }
                
                attempts++
                kotlinx.coroutines.delay(500)
            }
        }
    }
    
    // Show capture sheet if there's a capture request
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
                    android.util.Log.i("HomeScreen", "Tier selection completed successfully")
                } catch (e: Exception) {
                    android.util.Log.e("HomeScreen", "Error in tier selection: ${e.message}", e)
                    Toast.makeText(context, "Error processing selection. Please try again.", Toast.LENGTH_LONG).show()
                }
            },
            viewModel = viewModel
        )
    }
    
    // Debug logging for UI state
    LaunchedEffect(uiState.captureRequest) {
        android.util.Log.d("HomeScreen", "UI State changed - captureRequest: ${uiState.captureRequest}")
        if (uiState.captureRequest == null) {
            android.util.Log.w("HomeScreen", "WARNING: captureRequest is null - this might be causing the issue")
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
                PluctHomeTopBar(
                    title = "Pluct",
                    subtitle = "TikTok Transcripts & AI Insights",
                    onSearchClick = { /* TODO: Implement search */ },
                    onSettingsClick = { navController.navigate("profile") }
                )
            },
            bottomBar = {
                ModernBottomNavigation(navController = navController)
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { /* TODO: Implement add video */ },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
                // Welcome section
                item {
                    PluctHomeWelcomeSection()
                }
                
                // Quick actions
                item {
                    PluctHomeQuickActions()
                }
                
                // Recent videos
                if (videos.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Transcripts",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(videos, key = { it.id }) { video ->
                        ModernVideoItemCard(
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
}