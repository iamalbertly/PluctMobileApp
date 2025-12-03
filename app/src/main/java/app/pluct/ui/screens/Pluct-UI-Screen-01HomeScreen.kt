package app.pluct.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
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
    videoRepository: PluctVideoRepository? = null
) {
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showRequestCreditsDialog by remember { mutableStateOf(false) }

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
            prefilledUrl = prefilledUrl,
            apiService = apiService,
            snackbarHostState = snackbarHostState,
            videoRepository = videoRepository,
            debugInfo = debugInfo
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            userName = userName,
            creditBalance = creditBalance,
            onDismiss = { showSettingsDialog = false },
            onRequestCredits = {
                showSettingsDialog = false
                showRequestCreditsDialog = true
            }
        )
    }

    if (showRequestCreditsDialog) {
        RequestCreditsDialog(
            userName = userName,
            onSendRequest = {
                onRequestCredits(it)
                showRequestCreditsDialog = false
            },
            onDismiss = { showRequestCreditsDialog = false }
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
    debugInfo: app.pluct.services.TranscriptionDebugInfo?
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
                videoRepository = videoRepository
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
                PluctEmptyStateMessage()
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    userName: String,
    creditBalance: Int,
    onDismiss: () -> Unit,
    onRequestCredits: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Settings",
                modifier = Modifier.semantics {
                    contentDescription = "Settings"
                    testTag = "settings_dialog_title"
                }
            )
        },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Settings configuration"
                        testTag = "settings_dialog_content"
                    }
            ) {
                Text(
                    text = "Signed in as: $userName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.AssistChip(
                        onClick = {},
                        label = { Text("Credits: $creditBalance") },
                        enabled = false
                    )
                    Text(
                        text = "Tap refresh on header to update.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.Button(onClick = onRequestCredits) {
                    Text("Request Credits")
                }
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
        modifier = Modifier.semantics {
            contentDescription = "Settings dialog"
            testTag = "settings_dialog"
        }
    )
}

@Composable
private fun RequestCreditsDialog(
    userName: String,
    onSendRequest: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var referenceText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Credits") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("To add credits, send mobile money and paste your bank/SMS confirmation message below. Our team validates and applies tokens to your account.")
                Text("Account: $userName")
                androidx.compose.material3.OutlinedTextField(
                    value = referenceText,
                    onValueChange = { referenceText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste confirmation message") },
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSendRequest(referenceText)
                },
                enabled = referenceText.isNotBlank()
            ) {
                Text("Send Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
