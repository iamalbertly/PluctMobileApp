package app.pluct.ui.screens

import androidx.compose.runtime.*
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.repository.PluctVideoRepository

/**
 * Pluct-UI-Screen-01MainActivity-03StateManager
 * Follows naming convention: [Project]-[UI]-[Screen]-[MainActivity]-[StateManager]
 * 5 scope layers: Project, UI, Screen, MainActivity, StateManager
 * Manages state for MainActivity content
 */
@Composable
fun PluctUIScreen01MainActivity03StateManager(
    prefilledUrlExternal: String?,
    videoRepository: PluctVideoRepository,
    onStateReady: (
        creditBalance: MutableState<Int>,
        freeUsesRemaining: MutableState<Int>,
        isLoading: MutableState<Boolean>,
        hasLoadedBalanceOnce: MutableState<Boolean>,
        errorMessage: MutableState<String?>,
        currentError: MutableState<Throwable?>,
        currentErrorUrl: MutableState<String?>,
        showWelcomeDialog: MutableState<Boolean>,
        prefilledUrl: MutableState<String?>,
        creditRequestLog: MutableState<String?>,
        ctaHelperMessage: MutableState<String?>,
        hasVendTokenAttempted: MutableState<Boolean>,
        queuedCount: Int,
        processingCount: Int
    ) -> Unit
) {
    // State management
    var creditBalance by remember { mutableStateOf(0) }
    var freeUsesRemaining by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var hasLoadedBalanceOnce by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentError by remember { mutableStateOf<Throwable?>(null) }
    var currentErrorUrl by remember { mutableStateOf<String?>(null) }
    var showWelcomeDialog by remember { mutableStateOf(false) }
    var prefilledUrl by remember { mutableStateOf<String?>(prefilledUrlExternal) }
    var creditRequestLog by remember { mutableStateOf<String?>(null) }
    var ctaHelperMessage by remember { mutableStateOf<String?>(null) }
    var hasVendTokenAttempted by remember { mutableStateOf(false) }
    
    // Track queued and processing videos for notifications
    val queuedVideos = videoRepository.getVideosByStatus(ProcessingStatus.QUEUED)
        .collectAsState(initial = emptyList())
    val processingVideos = videoRepository.getVideosByStatus(ProcessingStatus.PROCESSING)
        .collectAsState(initial = emptyList())
    val queuedCount = queuedVideos.value.size
    val processingCount = processingVideos.value.size
    
    // Keep local state in sync when a new intent provides a prefilled URL
    LaunchedEffect(prefilledUrlExternal) {
        if (!prefilledUrlExternal.isNullOrBlank()) {
            prefilledUrl = prefilledUrlExternal
        }
    }
    
    onStateReady(
        remember { mutableStateOf(creditBalance) }.apply { value = creditBalance },
        remember { mutableStateOf(freeUsesRemaining) }.apply { value = freeUsesRemaining },
        remember { mutableStateOf(isLoading) }.apply { value = isLoading },
        remember { mutableStateOf(hasLoadedBalanceOnce) }.apply { value = hasLoadedBalanceOnce },
        remember { mutableStateOf(errorMessage) }.apply { value = errorMessage },
        remember { mutableStateOf(currentError) }.apply { value = currentError },
        remember { mutableStateOf(currentErrorUrl) }.apply { value = currentErrorUrl },
        remember { mutableStateOf(showWelcomeDialog) }.apply { value = showWelcomeDialog },
        remember { mutableStateOf(prefilledUrl) }.apply { value = prefilledUrl },
        remember { mutableStateOf(creditRequestLog) }.apply { value = creditRequestLog },
        remember { mutableStateOf(ctaHelperMessage) }.apply { value = ctaHelperMessage },
        remember { mutableStateOf(hasVendTokenAttempted) }.apply { value = hasVendTokenAttempted },
        queuedCount,
        processingCount
    )
}
