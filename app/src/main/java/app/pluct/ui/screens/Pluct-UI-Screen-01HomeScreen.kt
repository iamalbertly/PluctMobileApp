package app.pluct.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.util.Log
import java.util.Locale
import app.pluct.core.permission.PluctCorePermission02Launcher01Helper
import app.pluct.data.entity.LogLevel
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.QueueReason
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.TranscriptionDebugInfo
import app.pluct.ui.components.PluctDebugLogViewer
import app.pluct.ui.components.PluctHeaderWithRefreshableBalance
import app.pluct.ui.components.PluctUIComponent03CaptureCard
import app.pluct.ui.components.PluctUIComponent09Readiness01Strip
import app.pluct.ui.readiness.PluctUIReadiness01Kind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Pluct-UI-Screen-01HomeScreen - Main home screen with video list and capture interface.
 */
@Composable
fun PluctHomeScreen(
    creditBalance: Int,
    freeUsesRemaining: Int,
    readinessKind: PluctUIReadiness01Kind,
    onReadinessRetryBalance: () -> Unit,
    onReadinessOpenNetwork: () -> Unit,
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
    onQueueForLater: ((String, QueueReason) -> Unit)? = null,
    isLoadingCreditBalance: Boolean = false,
    onShowTutorial: (() -> Unit)? = null,
    onThemeModeChange: ((String) -> Unit)? = null,
    /** When false, caller owns [Scaffold] + top bar; only body is drawn (shell navigation). */
    useInnerScaffold: Boolean = true,
    innerContentPadding: PaddingValues = PaddingValues(0.dp),
    onNavigateToLibrary: () -> Unit = {},
    permissionLauncherHelper: PluctCorePermission02Launcher01Helper? = null,
    /** When null, an in-screen debug viewer is used; when set, caller owns the viewer (main shell). */
    onViewDebugLogs: (() -> Unit)? = null
) {
    var showSettingsDialog by remember { mutableStateOf(false) }
    var expandCreditsWhenOpeningSettings by remember { mutableStateOf(false) }
    var showDebugLogs by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun dismissSettingsSheet() {
        showSettingsDialog = false
        expandCreditsWhenOpeningSettings = false
    }

    val openSettingsSheet: (focusCreditsRequest: Boolean) -> Unit = { focusCredits ->
        expandCreditsWhenOpeningSettings = focusCredits
        showSettingsDialog = true
    }

    var effectivePrefilledUrl by remember(prefilledUrl) { mutableStateOf(prefilledUrl) }

    val uniqueVideos = remember(videos) {
        dedupeVideos(videos)
    }

    val debugInfo by (apiService?.transcriptionDebugFlow ?: MutableStateFlow(null)).collectAsState()
    val debugLogs by (debugLogManager?.getRecentLogs(100) ?: MutableStateFlow(emptyList())).collectAsState(initial = emptyList())
    val errorLogs = debugLogs.filter { it.level == LogLevel.ERROR }.take(3)

    val openDebugLogs: () -> Unit = onViewDebugLogs ?: { showDebugLogs = true }

    val homeBody: @Composable (PaddingValues) -> Unit = { paddingValues ->
        HomeContent(
            paddingValues = paddingValues,
            freeUsesRemaining = freeUsesRemaining,
            creditBalance = creditBalance,
            readinessKind = readinessKind,
            onReadinessRetryBalance = onReadinessRetryBalance,
            onReadinessOpenNetwork = onReadinessOpenNetwork,
            onStripAddCredits = { openSettingsSheet(true) },
            uniqueVideos = uniqueVideos,
            onTierSubmit = onTierSubmit,
            onRetryVideo = onRetryVideo,
            onDeleteVideo = onDeleteVideo,
            onVideoClick = onVideoClick,
            prefilledUrl = effectivePrefilledUrl,
            onDemoLinkClick = {
                effectivePrefilledUrl = "https://vm.tiktok.com/ZMDRUGT2P/"
            },
            apiService = apiService,
            snackbarHostState = snackbarHostState,
            videoRepository = videoRepository,
            debugInfo = debugInfo,
            ctaHelperMessage = ctaHelperMessage,
            onRequestCreditsClick = { openSettingsSheet(true) },
            debugLogManager = debugLogManager,
            onViewDebugLogs = openDebugLogs,
            onQueueForLater = onQueueForLater,
            isLoadingCreditBalance = isLoadingCreditBalance,
            onNavigateToLibrary = onNavigateToLibrary,
            onRefreshCreditBalance = onRefreshCreditBalance
        )
    }

    if (useInnerScaffold) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = {
                PluctHeaderWithRefreshableBalance(
                    creditBalance = creditBalance,
                    isCreditBalanceLoading = isLoadingCreditBalance,
                    creditBalanceError = errorMessage,
                    onRefreshCreditBalance = onRefreshCreditBalance,
                    onSettingsClick = { openSettingsSheet(false) }
                )
            }
        ) { paddingValues ->
            homeBody(paddingValues)
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerContentPadding)
        ) {
            homeBody(PaddingValues(0.dp))
        }
    }

    if (showSettingsDialog) {
        PluctUIScreen01HomeScreen04Settings01Dialog(
            userName = userName,
            creditBalance = creditBalance,
            debugLogCount = debugLogs.size,
            errorLogCount = errorLogs.size,
            onDismiss = { dismissSettingsSheet() },
            onRequestCredits = { requestText ->
                onRequestCredits(requestText)
                dismissSettingsSheet()
            },
            onViewDebugLogs = openDebugLogs,
            onSendDiagnostic = {
                scope.launch {
                    val breakdown = debugLogManager?.formatErrorCategorySummary().orEmpty()
                    val text = app.pluct.core.debug.PluctCoreDebug02DiagnosticShare01Builder.buildText(debugLogs, breakdown)
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Pluct diagnostic")
                        putExtra(android.content.Intent.EXTRA_TEXT, text)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val chooser = android.content.Intent.createChooser(send, "Send diagnostic")
                    chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(chooser)
                    } catch (_: Exception) {
                        Log.e("HomeScreen", "Could not start share intent for diagnostic")
                    }
                }
            },
            permissionLauncherHelper = permissionLauncherHelper,
            onThemeModeChange = onThemeModeChange,
            expandCreditsRequestSection = expandCreditsWhenOpeningSettings
        )
    }

    if (onViewDebugLogs == null && showDebugLogs && debugLogManager != null) {
        PluctDebugLogViewer(
            logs = debugLogs,
            debugLogManager = debugLogManager,
            onDismiss = { showDebugLogs = false }
        )
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    badge: Int?,
    onViewAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .semantics { testTag = "home_section_${title.lowercase(Locale.US)}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (badge != null && badge > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.semantics { testTag = "home_section_count_badge" }
                ) {
                    Text(
                        text = "$badge",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        TextButton(
            onClick = onViewAll,
            modifier = Modifier.semantics {
                testTag = "home_view_all_${title.lowercase()}"
                contentDescription = "View all $title"
            }
        ) {
            Text("View all", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HomeContent(
    paddingValues: PaddingValues,
    freeUsesRemaining: Int,
    creditBalance: Int,
    readinessKind: PluctUIReadiness01Kind,
    onReadinessRetryBalance: () -> Unit,
    onReadinessOpenNetwork: () -> Unit,
    onStripAddCredits: () -> Unit,
    uniqueVideos: List<VideoItem>,
    onTierSubmit: (String, ProcessingTier) -> Unit,
    onRetryVideo: (VideoItem) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    prefilledUrl: String?,
    apiService: PluctCoreAPIUnifiedService?,
    snackbarHostState: SnackbarHostState,
    videoRepository: PluctVideoRepository?,
    debugInfo: TranscriptionDebugInfo?,
    ctaHelperMessage: String?,
    onRequestCreditsClick: () -> Unit,
    onDemoLinkClick: () -> Unit = {},
    debugLogManager: app.pluct.core.debug.PluctCoreDebug01LogManager? = null,
    onViewDebugLogs: () -> Unit = {},
    onQueueForLater: ((String, QueueReason) -> Unit)? = null,
    isLoadingCreditBalance: Boolean = false,
    onNavigateToLibrary: () -> Unit = {},
    onRefreshCreditBalance: () -> Unit = {}
) {
    val activeVideosAll = remember(uniqueVideos) {
        uniqueVideos
            .filter {
                it.status == ProcessingStatus.QUEUED ||
                    it.status == ProcessingStatus.PROCESSING ||
                    it.status == ProcessingStatus.FAILED
            }
            .sortedByDescending { it.timestamp }
    }
    val activeVideos = remember(activeVideosAll) { activeVideosAll.take(2) }
    val recentVideos = remember(uniqueVideos) {
        uniqueVideos
            .filter { it.status == ProcessingStatus.COMPLETED }
            .sortedByDescending { it.timestamp }
            .take(3)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PluctUIComponent09Readiness01Strip(
                kind = readinessKind,
                onOpenMoney = onStripAddCredits,
                onOpenNetworkSettings = onReadinessOpenNetwork,
                onRetryBalance = onReadinessRetryBalance
            )
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
                isLoadingCreditBalance = isLoadingCreditBalance,
                onRefreshCreditBalance = onRefreshCreditBalance
            )
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { testTag = "home_value_promise_banner" },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .semantics { testTag = "home_value_promise_line" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "We'll get the text and clean it up for you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (activeVideos.isNotEmpty()) {
            item {
                HomeSectionHeader(
                    title = "Active",
                    badge = activeVideosAll.size,
                    onViewAll = onNavigateToLibrary
                )
            }
            items(activeVideos, key = { it.id }) { video ->
                PluctVideoItemCard(
                    video = video,
                    onClick = { onVideoClick(video) },
                    onRetry = { onRetryVideo(video) },
                    onDelete = { onDeleteVideo(video) },
                    debugInfo = if (video.status == ProcessingStatus.PROCESSING && debugInfo?.url == video.url) {
                        debugInfo
                    } else {
                        null
                    },
                    snackbarHostState = snackbarHostState,
                    onRequestCredits = onRequestCreditsClick,
                    showProcessingDebugPanel = false
                )
            }
        }

        if (recentVideos.isNotEmpty()) {
            item {
                HomeSectionHeader(
                    title = "Recent",
                    badge = null,
                    onViewAll = onNavigateToLibrary
                )
            }
            items(recentVideos, key = { it.id }) { video ->
                PluctVideoItemCard(
                    video = video,
                    onClick = { onVideoClick(video) },
                    onRetry = { onRetryVideo(video) },
                    onDelete = { onDeleteVideo(video) },
                    debugInfo = null,
                    snackbarHostState = snackbarHostState,
                    onRequestCredits = onRequestCreditsClick,
                    showProcessingDebugPanel = false
                )
            }
        }

        if (uniqueVideos.isEmpty()) {
            item {
                PluctEmptyStateMessage(onDemoLinkClick = onDemoLinkClick)
            }
        }
    }
}

private fun normalizeVideoUrl(url: String): String = url.trim().lowercase()

private fun dedupeVideos(videos: List<VideoItem>): List<VideoItem> {
    return videos
        .groupBy { normalizeVideoUrl(it.url) }
        .mapNotNull { (_, group) ->
            group.maxByOrNull { it.timestamp }
        }
        .sortedByDescending { it.timestamp }
}
