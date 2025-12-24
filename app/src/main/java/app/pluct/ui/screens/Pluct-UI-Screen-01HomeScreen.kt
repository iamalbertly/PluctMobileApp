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
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.ui.components.PluctHeaderWithRefreshableBalance
import app.pluct.ui.components.PluctUIComponent03CaptureCard
import app.pluct.ui.components.PluctDebugLogViewer
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.flow.MutableStateFlow

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
    debugLogManager: app.pluct.core.debug.PluctCoreDebug01LogManager? = null
) {
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDebugLogs by remember { mutableStateOf(false) }

    var effectivePrefilledUrl by remember(prefilledUrl) { mutableStateOf(prefilledUrl) }

    val uniqueVideos = remember(videos) {
        val urlMap = mutableMapOf<String, VideoItem>()
        videos.forEach { video ->
            val existing = urlMap[video.url]
            if (existing == null || video.timestamp > existing.timestamp) {
                urlMap[video.url] = video
            }
        }
        urlMap.values.sortedByDescending { it.timestamp }
    }

    val debugInfo by (apiService?.transcriptionDebugFlow ?: MutableStateFlow(null)).collectAsState()
    val debugLogs by (debugLogManager?.getRecentLogs(100) ?: MutableStateFlow(emptyList())).collectAsState(initial = emptyList())

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
            onViewDebugLogs = { showDebugLogs = true }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            userName = userName,
            creditBalance = creditBalance,
            debugLogCount = debugLogs.size,
            onDismiss = { showSettingsDialog = false },
            onRequestCredits = { requestText ->
                onRequestCredits(requestText)
                showSettingsDialog = false
            },
            onViewDebugLogs = {
                showDebugLogs = true
            }
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
    onViewDebugLogs: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                onViewInLogs = onViewDebugLogs
            )
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
                    debugInfo = if (video.status == app.pluct.data.entity.ProcessingStatus.PROCESSING && debugInfo?.url == video.url) debugInfo else null
                )
            }
        } else {
            item {
                PluctEmptyStateMessage(onDemoLinkClick = onDemoLinkClick)
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    userName: String,
    creditBalance: Int,
    debugLogCount: Int,
    onDismiss: () -> Unit,
    onRequestCredits: (String) -> Unit,
    onViewDebugLogs: () -> Unit
) {
    var referenceText by remember { mutableStateOf("") }
    var isRequesting by remember { mutableStateOf(false) }
    var isCreditRequestInFlight by remember { mutableStateOf(false) }
    var creditRequestStatus by remember { mutableStateOf<String?>(null) }
    var requestId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Settings & Credits",
                modifier = Modifier.semantics {
                    contentDescription = "Settings and Credits"
                    testTag = "settings_dialog_title"
                }
            )
        },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Settings content"
                        testTag = "settings_dialog_content"
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Info Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Signed in as: $userName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Current Balance: $creditBalance Credits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                androidx.compose.material3.Divider()

                // Request Credits Section
                if (!isRequesting) {
                    androidx.compose.material3.Button(
                        onClick = { isRequesting = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add More Credits")
                    }
                    
                    // Debug Logs Button
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            onDismiss()
                            onViewDebugLogs()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Debug Logs ($debugLogCount)")
                    }
                    
                    Text(
                        text = "Tap refresh on header to update balance.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isCreditRequestInFlight) {
                            // Loading state
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Sending request...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            val status = creditRequestStatus
                            if (status != null) {
                                // Success/Error state
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (status.contains("sent", ignoreCase = true) || 
                                                             status.contains("received", ignoreCase = true)) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.errorContainer
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = status,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    requestId?.let { id ->
                                        Text(
                                            text = "Request ID: ${id.take(8)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            } else {
                                // Request form
                                Text(
                                    text = "To add credits, send mobile money and paste your bank/SMS confirmation message below.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                androidx.compose.material3.OutlinedTextField(
                                    value = referenceText,
                                    onValueChange = { referenceText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Paste confirmation message") },
                                    maxLines = 4,
                                    minLines = 2,
                                    enabled = !isCreditRequestInFlight
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isRequesting) {
                if (creditRequestStatus != null) {
                    // Show OK button after status is shown
                    androidx.compose.material3.Button(
                        onClick = {
                            isRequesting = false
                            referenceText = ""
                            creditRequestStatus = null
                            requestId = null
                            isCreditRequestInFlight = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OK")
                    }
                } else if (!isCreditRequestInFlight) {
                    // Submit button
                    androidx.compose.material3.Button(
                        onClick = {
                            if (referenceText.isNotBlank()) {
                                isCreditRequestInFlight = true
                                onRequestCredits(referenceText)
                                creditRequestStatus = "Request sent. We'll verify your payment and apply credits."
                                requestId = "credit_req_${System.currentTimeMillis()}"
                                scope.launch {
                                    delay(2000)
                                    isCreditRequestInFlight = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = referenceText.isNotBlank() && !isCreditRequestInFlight
                    ) {
                        Text("Submit Request")
                    }
                }
            } else {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics {
                        contentDescription = "Close settings"
                        testTag = "settings_dialog_close"
                    }
                ) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (isRequesting) {
                TextButton(onClick = { isRequesting = false }) {
                    Text("Cancel")
                }
            }
        },
        modifier = Modifier.semantics {
            contentDescription = "Settings dialog"
            testTag = "settings_dialog"
        }
    )
}


