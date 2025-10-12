package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.pluct.MainActivity
import app.pluct.data.entity.ProcessingTier
import app.pluct.ui.components.CaptureInsightSheet
import app.pluct.ui.components.EmptyStateView
import app.pluct.ui.components.ModernBottomNavigation
import app.pluct.ui.screens.components.*
import app.pluct.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val focusRequester = remember { FocusRequester() }
    
    // Handle capture request from MainActivity
    LaunchedEffect(Unit) {
        if (context is MainActivity) {
            // Check the current intent
            val intent = context.intent
            android.util.Log.d("HomeScreen", "Initial intent check: action=${intent?.action}")
            
            // Also check for CAPTURE_INSIGHT intent periodically
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
                kotlinx.coroutines.delay(500) // Wait 500ms between checks
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
                            viewModel.createVideoWithTier(captureRequest.url, tier)
                            // Show toast based on tier
                            val message = when (tier) {
                                ProcessingTier.QUICK_SCAN -> "✅ Queued for Quick Scan. We'll notify you shortly."
                                ProcessingTier.AI_ANALYSIS -> "✨ Queued for AI Analysis. We'll notify you when your insights are ready."
                            }
                            // Show toast message
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        },
            viewModel = viewModel
        )
    }
    
    // Debug logging for UI state
    LaunchedEffect(uiState.captureRequest) {
        android.util.Log.d("HomeScreen", "UI State changed - captureRequest: ${uiState.captureRequest}")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pluct - TikTok Transcripts") }
            )
        },
        bottomBar = {
            ModernBottomNavigation(navController = navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Manual URL Input Section
            ManualUrlInput(
                navController = navController,
                focusRequester = focusRequester
            )
            
            // Recent Transcripts Section
            RecentTranscriptsSection(
                videos = videos,
                navController = navController,
                viewModel = viewModel
            )
            
            // Content
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (videos.isEmpty()) {
                EmptyStateView()
            }
        }
    }
    
    // Auto-focus the URL input field
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}