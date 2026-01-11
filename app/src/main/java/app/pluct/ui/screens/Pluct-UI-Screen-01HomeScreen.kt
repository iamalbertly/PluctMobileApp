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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.ui.components.PluctDebugLogViewer
import app.pluct.ui.components.PluctHeaderWithRefreshableBalance
import app.pluct.ui.components.PluctUIComponent03CaptureCard
import app.pluct.ui.components.error.log.PluctErrorLogSection
import app.pluct.ui.screens.PluctQueueSection
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.LogLevel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import app.pluct.core.permission.PluctCorePermission01Manager
import app.pluct.data.preferences.PluctUserPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import android.os.Build
import android.app.Activity

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
    isLoadingCreditBalance: Boolean = false
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
            errorLogs = errorLogs,
            onQueueForLater = onQueueForLater,
            isLoadingCreditBalance = isLoadingCreditBalance
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
            },
            permissionLauncherHelper = null // Settings dialog can use fallback method
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
    errorLogs: List<app.pluct.data.entity.DebugLogEntry> = emptyList(),
    onQueueForLater: ((String, app.pluct.data.entity.QueueReason) -> Unit)? = null,
    isLoadingCreditBalance: Boolean = false
) {
    // Filter queued videos
    val queuedVideos = uniqueVideos.filter { it.status == ProcessingStatus.QUEUED }
    
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
                onViewInLogs = onViewDebugLogs,
                onQueueForLater = onQueueForLater,
                isLoadingCreditBalance = isLoadingCreditBalance
            )
        }
        
        // Error Log Section
        if (errorLogs.isNotEmpty()) {
            item {
                PluctErrorLogSection(
                    errorLogs = errorLogs,
                    onCopyError = { error ->
                        debugLogManager?.let { manager ->
                            // Copy is handled in the component
                            manager.formatLogForClipboard(error)
                        }
                    }
                )
            }
        }
        
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

@Composable
private fun SettingsDialog(
    userName: String,
    creditBalance: Int,
    debugLogCount: Int,
    onDismiss: () -> Unit,
    onRequestCredits: (String) -> Unit,
    onViewDebugLogs: () -> Unit,
    permissionLauncherHelper: app.pluct.core.permission.PluctCorePermission02Launcher01Helper? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PluctUserPreferences(context) }
    
    var referenceText by remember { mutableStateOf("") }
    var isRequesting by remember { mutableStateOf(false) }
    var isCreditRequestInFlight by remember { mutableStateOf(false) }
    var creditRequestStatus by remember { mutableStateOf<String?>(null) }
    var requestId by remember { mutableStateOf<String?>(null) }
    
    // Permission states
    var hasNotificationPermission by remember { 
        mutableStateOf(PluctCorePermission01Manager.hasNotificationPermission(context)) 
    }
    var hasOverlayPermission by remember { 
        mutableStateOf(PluctCorePermission01Manager.hasOverlayPermission(context)) 
    }
    var overlayEnabled by remember { 
        mutableStateOf(prefs.getOverlayNotificationsEnabled()) 
    }
    
    // Refresh permission states on dialog open and when permissions change
    LaunchedEffect(Unit) {
        hasNotificationPermission = PluctCorePermission01Manager.hasNotificationPermission(context)
        hasOverlayPermission = PluctCorePermission01Manager.hasOverlayPermission(context)
        overlayEnabled = prefs.getOverlayNotificationsEnabled()
    }
    
    // Refresh permission states periodically when dialog is open
    LaunchedEffect(hasNotificationPermission, hasOverlayPermission) {
        // Re-check permissions when state might have changed
        delay(500)
        hasNotificationPermission = PluctCorePermission01Manager.hasNotificationPermission(context)
        hasOverlayPermission = PluctCorePermission01Manager.hasOverlayPermission(context)
    }

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

                // Permissions Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Permissions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Notification Permission
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Notifications",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (hasNotificationPermission) "Granted" else "Required for transcription updates",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasNotificationPermission) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (!hasNotificationPermission) {
                            val isPermanentlyDenied = if (context is Activity) {
                                PluctCorePermission01Manager.isNotificationPermissionPermanentlyDenied(context)
                            } else {
                                false
                            }
                            Button(
                                onClick = {
                                    if (isPermanentlyDenied) {
                                        PluctCorePermission01Manager.openNotificationSettings(context)
                                    } else if (permissionLauncherHelper != null) {
                                        // Use ActivityResultLauncher if available
                                        permissionLauncherHelper.requestNotificationPermission { granted ->
                                            hasNotificationPermission = granted
                                        }
                                    } else if (context is Activity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        // Fallback to deprecated method
                                        @Suppress("DEPRECATION")
                                        androidx.core.app.ActivityCompat.requestPermissions(
                                            context,
                                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                            app.pluct.core.permission.REQUEST_CODE_NOTIFICATION
                                        )
                                        PluctCorePermission01Manager.invalidateCache()
                                        scope.launch {
                                            delay(500)
                                            hasNotificationPermission = PluctCorePermission01Manager.hasNotificationPermission(context)
                                        }
                                    }
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = if (isPermanentlyDenied) "Open notification settings" else "Enable notifications"
                                    testTag = "settings_enable_notifications_button"
                                }
                            ) {
                                Text(if (isPermanentlyDenied) "Open Settings" else "Enable")
                            }
                        }
                    }
                    
                    // Overlay Permission
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPicture,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Overlay Notifications",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (hasOverlayPermission) "Granted" else "Optional - shows floating status",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasOverlayPermission) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!hasOverlayPermission) {
                            Button(
                                onClick = {
                                    if (permissionLauncherHelper != null) {
                                        // Use ActivityResultLauncher if available
                                        permissionLauncherHelper.requestOverlayPermission { granted ->
                                            hasOverlayPermission = granted
                                        }
                                    } else {
                                        // Fallback to opening settings
                                        PluctCorePermission01Manager.openOverlaySettings(context)
                                        PluctCorePermission01Manager.invalidateCache()
                                        scope.launch {
                                            delay(1000)
                                            hasOverlayPermission = PluctCorePermission01Manager.hasOverlayPermission(context)
                                        }
                                    }
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "Enable overlay permission"
                                    testTag = "settings_enable_overlay_button"
                                }
                            ) {
                                Text("Enable")
                            }
                        }
                    }
                    
                    // Overlay Toggle (only if permission granted)
                    if (hasOverlayPermission) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Show overlay during transcription",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = overlayEnabled,
                                onCheckedChange = { enabled ->
                                    overlayEnabled = enabled
                                    prefs.setOverlayNotificationsEnabled(enabled)
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "Toggle overlay notifications"
                                    testTag = "settings_overlay_toggle"
                                }
                            )
                        }
                    }
                }
                
                androidx.compose.material3.Divider()

                // Request Credits Section
                if (!isRequesting) {
                    androidx.compose.material3.Button(
                        onClick = { isRequesting = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Add more credits"
                                testTag = "settings_add_credits_button"
                            }
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription = "Paste payment confirmation message"
                                        testTag = "settings_reference_input"
                                    },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Submit credits request"
                                testTag = "settings_submit_request_button"
                            },
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
                TextButton(
                    onClick = { isRequesting = false },
                    modifier = Modifier.semantics {
                        contentDescription = "Cancel credits request"
                        testTag = "settings_cancel_request_button"
                    }
                ) {
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

private fun dedupeVideos(videos: List<VideoItem>): List<VideoItem> {
    return videos
        .groupBy { it.url }
        .mapNotNull { (_, group) ->
            group.maxByOrNull { it.timestamp }
        }
        .sortedByDescending { it.timestamp }
}


