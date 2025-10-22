package app.pluct.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.pluct.PluctUIMain01Activity
import app.pluct.data.entity.ProcessingTier
import app.pluct.ui.components.*
import app.pluct.viewmodel.HomeViewModel
import app.pluct.ui.error.ErrorBannerHost
import app.pluct.core.error.ErrorEnvelope
import app.pluct.viewmodel.CaptureRequest

/**
 * Pluct-UI-Screen-01Home - Main home screen with focused responsibilities
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Simplified and focused on core home screen functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctHomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Error center for modern error system
    val errorCenter = viewModel.errorCenter
    
    // Register HomeViewModel with MainActivity to handle CAPTURE_INSIGHT intents
    LaunchedEffect(Unit) {
               if (context is PluctUIMain01Activity) {
            context.setHomeViewModel(viewModel)
        }
    }
    
    // Handle error notifications based on UI state
    LaunchedEffect(uiState.error, uiState.processError, uiState.creditBalanceError) {
        when {
            uiState.error != null -> {
                errorCenter.emit(ErrorEnvelope(
                    code = "GENERAL_ERROR",
                    message = uiState.error!!,
                    source = "ui_state"
                ))
            }
            uiState.processError != null -> {
                errorCenter.emit(ErrorEnvelope(
                    code = "PROCESS_ERROR",
                    message = uiState.processError!!.reason,
                    source = "ui_state"
                ))
            }
            uiState.creditBalanceError != null -> {
                errorCenter.emit(ErrorEnvelope(
                    code = "CREDIT_ERROR",
                    message = uiState.creditBalanceError!!,
                    source = "ui_state"
                ))
            }
        }
    }
    
    // Handle shared URLs from Android Share Intent
    LaunchedEffect(Unit) {
               if (context is PluctUIMain01Activity) {
            val sharedUrl = context.intent?.getStringExtra(Intent.EXTRA_TEXT)?.trim()
            if (!sharedUrl.isNullOrEmpty()) {
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
                Column {
                    // Error banner at the top
                    ErrorBannerHost(center = errorCenter)
                    
                    // Main top bar
                    PluctCompactTopBar(
                        credits = uiState.creditBalance,
                        onRefreshCredits = { viewModel.refreshCreditBalance() },
                        navController = navController
                    )
                }
            },
            floatingActionButton = {
                PluctFloatingActionButton(
                    onClick = { 
                        viewModel.setCaptureRequest(CaptureRequest("", null))
                    }
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
                // Video list or empty state
                if (videos.isNotEmpty()) {
                    PluctTranscriptList(
                        videos = videos,
                        onVideoClick = { video ->
                            // TODO: Navigate to video details
                        },
                        onRemoveVideo = { video ->
                            viewModel.deleteVideo(video.id)
                        },
                        onRetryVideo = { video ->
                            // TODO: Implement retry functionality
                        }
                    )
                } else {
                    PluctCompactEmptyState()
                }
                
                // Test error banner system
                PluctErrorTestSection(viewModel = viewModel)
            }
        }
    }
    
    // Capture insight sheet
    uiState.captureRequest?.let { captureRequest ->
        CaptureInsightSheet(
            captureRequest = captureRequest,
            viewModel = viewModel,
            onDismiss = { viewModel.clearCaptureRequest() },
            onUrlChange = { url -> viewModel.updateCaptureRequestUrl(url) },
            onTierSelected = { tier, clientRequestId ->
                try {
                    when (tier) {
                        ProcessingTier.QUICK_SCAN -> {
                            val requestId = clientRequestId ?: java.util.UUID.randomUUID().toString()
                            viewModel.quickScan(captureRequest.url, requestId)
                        }
                        else -> {
                            viewModel.createVideoWithTier(captureRequest.url, tier, context)
                        }
                    }
                    viewModel.clearCaptureRequest()
                } catch (e: Exception) {
                    // TODO: Show error toast
                }
            }
        )
    }
}

/**
 * Error test section component
 */
@Composable
fun PluctErrorTestSection(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("error_test_section"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Error Banner Test",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        viewModel.triggerTestError("NETWORK")
                    },
                    modifier = Modifier.testTag("test_network_error")
                ) {
                    Text("Network Error")
                }
                
                Button(
                    onClick = { 
                        viewModel.triggerTestError("VALIDATION")
                    },
                    modifier = Modifier.testTag("test_validation_error")
                ) {
                    Text("Validation Error")
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        viewModel.triggerTestError("API")
                    },
                    modifier = Modifier.testTag("test_api_error")
                ) {
                    Text("API Error")
                }
                
                Button(
                    onClick = { 
                        viewModel.triggerTestError("TIMEOUT")
                    },
                    modifier = Modifier.testTag("test_timeout_error")
                ) {
                    Text("Timeout Error")
                }
            }
        }
    }
}
