package app.pluct.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import android.util.Log
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.ui.components.PluctDebugLogViewer
import app.pluct.ui.components.PluctHeaderWithRefreshableBalance
import app.pluct.ui.components.PluctUIComponent03CaptureCard
import app.pluct.ui.components.PluctUIComponent08InlineHint01Card
import app.pluct.data.preferences.PluctUserPreferencesInlineHint
// UX FIX: Removed PluctErrorLogSection import - error logs consolidated in Settings
import app.pluct.ui.screens.PluctQueueSection
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.LogLevel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Pluct-UI-Screen-01HomeScreen - Main home screen with video list and capture interface.
 */
@Composable
fun PluctHomeScreen(
    creditBalance: Int,
    freeUsesRemaining: Int,
    videos: List<VideoItem>,
    isLoading: Boolean,
    errorMessage: String?,
    userName: String,
    onRequestCredits: (String) -> Unit,
    onTierSubmit: (String, ProcessingTier) -> Unit,
    onRetryVideo: (VideoItem) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    onVideoClick: (VideoItem) -> Unit = {},
    prefilledUrl: String? = null,
    apiService: PluctCoreAPIUnifiedService? = null,
    onRefreshCreditBalance: () -> Unit = {},
    snackbarHostState: SnackbarHostState,
    videoRepository: PluctVideoRepository? = null,
    ctaHelperMessage: String? = null,
    debugLogManager: app.pluct.core.debug.PluctCoreDebug01LogManager? = null,
    onQueueForLater: ((String, app.pluct.data.entity.QueueReason) -> Unit)? = null,
    isLoadingCreditBalance: Boolean = false,
    onShowTutorial: (() -> Unit)? = null,
    onThemeModeChange: ((String) -> Unit)? = null
) {
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDebugLogs by remember { mutableStateOf(false) }

    var effectivePrefilledUrl by remember(prefilledUrl) { mutableStateOf(prefilledUrl) }

    val uniqueVideos = remember(videos) {
        dedupeVideos(videos)
    }

    val debugInfo by (apiService?.transcriptionDebugFlow ?: MutableStateFlow(null)).collectAsState()
    val debugLogs by (debugLogManager?.getRecentLogs(100) ?: MutableStateFlow(emptyList())).collectAsState(initial = emptyList())
    val errorLogs = remember(debugLogs) {
        debugLogs.filter { it.level == LogLevel.ERROR }.take(3)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            PluctHeaderWithRefreshableBalance(
                creditBalance = creditBalance,
                isCreditBalanceLoading = isLoading,
                creditBalanceError = errorMessage,
                onRefreshCreditBalance = onRefreshCreditBalance,
                onSettingsClick = { showSettingsDialog = true }
            )
        }
    ) { paddingValues ->
        HomeContent(
            paddingValues = paddingValues,
            freeUsesRemaining = freeUsesRemaining,
            creditBalance = creditBalance,
            uniqueVideos = uniqueVideos,
            onTierSubmit = onTierSubmit,
            onRetryVideo = onRetryVideo,
            onDeleteVideo = onDeleteVideo,
            onVideoClick = onVideoClick,
            prefilledUrl = effectivePrefilledUrl,
            onDemoLinkClick = {
                effectivePrefilledUrl = "https://vm.tiktok.com/ZMDRUGT2P/" // Demo Link
            },
            apiService = apiService,
            snackbarHostState = snackbarHostState,
            videoRepository = videoRepository,
            debugInfo = debugInfo,
            ctaHelperMessage = ctaHelperMessage,
            onRequestCreditsClick = { showSettingsDialog = true },
            debugLogManager = debugLogManager,
            onViewDebugLogs = { showDebugLogs = true },
            // UX FIX: errorLogs parameter removed from HomeContent - errors consolidated in Settings
            // errorLogs still calculated above for Settings badge count
            onQueueForLater = onQueueForLater,
            isLoadingCreditBalance = isLoadingCreditBalance,
            onShowTutorial = onShowTutorial
        )
    }

    if (showSettingsDialog) {
        PluctUIScreen01HomeScreen04Settings01Dialog(
            userName = userName,
            creditBalance = creditBalance,
            debugLogCount = debugLogs.size,
            errorLogCount = errorLogs.size, // UX FIX: Pass error count for badge display
            onDismiss = { showSettingsDialog = false },
            onRequestCredits = { requestText ->
                onRequestCredits(requestText)
                showSettingsDialog = false
            },
            onViewDebugLogs = {
                showDebugLogs = true
            },
            permissionLauncherHelper = null, // Settings dialog can use fallback method
            onThemeModeChange = onThemeModeChange
        )
    }
    
    if (showDebugLogs && debugLogManager != null) {
        PluctDebugLogViewer(
            logs = debugLogs,
            debugLogManager = debugLogManager,
            onDismiss = { showDebugLogs = false }
        )
    }
}

@Composable
private fun HomeContent(
    paddingValues: PaddingValues,
    freeUsesRemaining: Int,
    creditBalance: Int,
    uniqueVideos: List<VideoItem>,
    onTierSubmit: (String, ProcessingTier) -> Unit,
    onRetryVideo: (VideoItem) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    prefilledUrl: String?,
    apiService: PluctCoreAPIUnifiedService?,
    snackbarHostState: SnackbarHostState,
    videoRepository: PluctVideoRepository?,
    debugInfo: app.pluct.services.TranscriptionDebugInfo?,
    ctaHelperMessage: String?,
    onRequestCreditsClick: () -> Unit,
    onDemoLinkClick: () -> Unit = {},
    debugLogManager: app.pluct.core.debug.PluctCoreDebug01LogManager? = null,
    onViewDebugLogs: () -> Unit = {},
    // UX FIX: errorLogs parameter removed - errors consolidated in Settings, not displayed in HomeContent
    onQueueForLater: ((String, app.pluct.data.entity.QueueReason) -> Unit)? = null,
    isLoadingCreditBalance: Boolean = false,
    onShowTutorial: (() -> Unit)? = null
) {
    // Filter queued videos
    val queuedVideos = uniqueVideos.filter { it.status == ProcessingStatus.QUEUED }
    
    // Check if inline hint should be shown
    val context = LocalContext.current
    val showInlineHint = remember { 
        PluctUserPreferencesInlineHint.getInlineHintEnabled(context) 
    }
    var inlineHintVisible by remember { mutableStateOf(showInlineHint) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Inline hint for users who skipped tutorial
        if (inlineHintVisible) {
            item {
                PluctUIComponent08InlineHint01Card(
                    onShowTutorial = {
                        Log.d("HomeScreen", "Inline hint: Show tutorial clicked")
                        onShowTutorial?.invoke()
                    },
                    onDismiss = {
                        PluctUserPreferencesInlineHint.setInlineHintEnabled(context, false)
                        inlineHintVisible = false
                        Log.d("HomeScreen", "Inline hint dismissed")
                    }
                )
            }
        }
        
        item {
            PluctUIComponent03CaptureCard(
                freeUsesRemaining = freeUsesRemaining,
                creditBalance = creditBalance,
                onTierSubmit = onTierSubmit,
                preFilledUrl = prefilledUrl,
                apiService = apiService,
                videoRepository = videoRepository,
                ctaHelperMessage = ctaHelperMessage,
                onRequestCredits = onRequestCreditsClick,
                debugLogManager = debugLogManager,
                onViewInLogs = onViewDebugLogs,
                onQueueForLater = onQueueForLater,
                isLoadingCreditBalance = isLoadingCreditBalance
            )
        }
        
        // UX FIX: Removed duplicate error log section from home screen
        // Errors are now consolidated in the Debug Logs viewer accessible from Settings
        // This follows single source of truth principle and reduces UI clutter
        
        // Queue Section
        if (queuedVideos.isNotEmpty()) {
            item {
                PluctQueueSection(
                    queuedVideos = queuedVideos,
                    onRetryVideo = onRetryVideo,
                    onDeleteVideo = onDeleteVideo
                )
            }
        }

        if (uniqueVideos.isNotEmpty()) {
            item {
                PluctTimeSavedCard(videos = uniqueVideos)
            }
            items(uniqueVideos) { video ->
                PluctVideoItemCard(
                    video = video,
                    onClick = { onVideoClick(video) },
                    onRetry = { onRetryVideo(video) },
                    onDelete = { onDeleteVideo(video) },
                    debugInfo = if (video.status == app.pluct.data.entity.ProcessingStatus.PROCESSING && debugInfo?.url == video.url) debugInfo else null,
                    snackbarHostState = snackbarHostState
                )
            }
        } else {
            item {
                PluctEmptyStateMessage(onDemoLinkClick = onDemoLinkClick)
            }
        }
    }
}

// SettingsDialog extracted to Pluct-UI-Screen-01HomeScreen-04Settings-01Dialog.kt
// Removed duplicate implementation - using PluctUIScreen01HomeScreen04Settings01Dialog instead

private fun dedupeVideos(videos: List<VideoItem>): List<VideoItem> {
    return videos
        .groupBy { it.url }
        .mapNotNull { (_, group) ->
            group.maxByOrNull { it.timestamp }
        }
        .sortedByDescending { it.timestamp }
}


