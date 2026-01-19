package app.pluct.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.runtime.State
import kotlinx.coroutines.*
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.api.PluctCoreAPITranscriptionResult01Extractor
import app.pluct.services.background.status.PluctStatusResumer
import app.pluct.core.network.PluctNetworkConnectivityChecker
import app.pluct.services.PluctQueueManager
import app.pluct.notification.PluctQueueNotificationManager
import app.pluct.ui.screens.PluctUIScreen01MainActivityTranscriptionOrchestrator
import app.pluct.ui.components.PluctUIComponent05Notification01SnackbarManager
import app.pluct.data.preferences.PluctUserPreferences
import app.pluct.data.preferences.PluctUserPreferencesInlineHint

/**
 * Pluct-UI-Screen-01MainActivity-04EffectsHandler
 * Follows naming convention: [Project]-[UI]-[Screen]-[MainActivity]-[EffectsHandler]
 * 5 scope layers: Project, UI, Screen, MainActivity, EffectsHandler
 * Handles all LaunchedEffects for MainActivity
 */
@Composable
fun PluctUIScreen01MainActivity04EffectsHandler(
    scope: CoroutineScope,
    apiService: PluctCoreAPIUnifiedService,
    videoRepository: PluctVideoRepository,
    context: Context,
    queuedVideos: State<List<app.pluct.data.entity.VideoItem>>,
    processingVideos: State<List<app.pluct.data.entity.VideoItem>>,
    queuedCount: Int,
    processingCount: Int,
    creditBalance: Int,
    freeUsesRemaining: Int,
    queueManager: PluctQueueManager,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
    clipboardManager: android.content.ClipboardManager,
    debugLogManager: app.pluct.core.debug.PluctCoreDebug01LogManager,
    validator: app.pluct.services.PluctCoreValidationInputSanitizer,
    onBalanceUpdate: (Int, Int) -> Unit,
    onQueueProcessed: (Int) -> Unit
) {
    // Resume incomplete transcriptions on app start
    LaunchedEffect(Unit) {
        scope.launch {
            val resumer = PluctStatusResumer(videoRepository, apiService, context)
            resumer.resumeIncompleteTranscriptions()
        }
    }
    
    // UX IMPROVEMENT #1 & #5: Monitor and update progress with optimized polling
    // Technical Debt #1: Uses extracted ProgressPoller helper for cleaner code
    LaunchedEffect(processingVideos.value) {
        if (processingVideos.value.isNotEmpty()) {
            scope.launch {
                processingVideos.value.forEach { video ->
                    launch {
                        PluctUIScreen01MainActivity04EffectsHandler02ProgressPoller.pollTranscriptionProgress(
                            video = video,
                            apiService = apiService,
                            videoRepository = videoRepository,
                            context = context,
                            onFirstTranscriptCompleted = {
                                // Show celebration for first transcript milestone
                                scope.launch {
                                    // Small delay to let transcript appear first
                                    delay(500)
                                    
                                    Log.d("EffectsHandler", "MILESTONE: first_transcript completed for user")
                                    Log.d("EffectsHandler", "MILESTONE: Showing celebration and triggering balance refresh")
                                    
                                    // UX IMPROVEMENT: Show Duolingo-style celebration toast
                                    PluctUIComponent05Notification01SnackbarManager.showCelebrationAsync(
                                        scope, snackbarHostState,
                                        PluctUIComponent05Notification01SnackbarManager.CelebrationMilestone.FIRST_TRANSCRIPT
                                    )
                                    
                                    // Trigger balance refresh to surface potential milestone bonus
                                    onBalanceUpdate(creditBalance, freeUsesRemaining)
                                    
                                    // Disable inline hint if it was showing
                                    PluctUserPreferencesInlineHint.setInlineHintEnabled(context, false)
                                    Log.d("EffectsHandler", "MILESTONE: Inline hint disabled after first transcript")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Update queue notification when counts change
    LaunchedEffect(queuedCount, processingCount) {
        val queueReasons = queuedVideos.value.associate { it.url to it.queueReason }
        PluctQueueNotificationManager.updateQueueNotification(
            context = context,
            queuedCount = queuedCount,
            processingCount = processingCount,
            queueReasons = queueReasons
        )
    }
    
    // Auto-retry: Process queued videos when credits become available
    LaunchedEffect(creditBalance) {
        if (creditBalance > 0 && queuedCount > 0) {
            scope.launch {
                val processedCount = queueManager.processQueuedVideos(
                    apiService = apiService,
                    currentBalance = creditBalance,
                    currentFreeUses = freeUsesRemaining,
                    isNetworkAvailable = PluctNetworkConnectivityChecker.isNetworkAvailable(context),
                    onProcess = { video ->
                        PluctUIScreen01MainActivityTranscriptionOrchestrator.processVideo(
                            apiService, video.url, video.tier, creditBalance, freeUsesRemaining,
                            videoRepository, clipboardManager, debugLogManager, context,
                            validator
                        ) { success, newBalance, newFreeUses, _, _ ->
                            onBalanceUpdate(newBalance, newFreeUses)
                            if (success) {
                                PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                                    scope, snackbarHostState, "Processing queued video..."
                                )
                            }
                        }
                    }
                )
                if (processedCount > 0) {
                    Log.d("MainActivity", "Auto-processed $processedCount queued video(s) after credits added")
                    onQueueProcessed(processedCount)
                }
            }
        }
    }
    
    // Auto-retry: Process queued videos when network becomes available
    LaunchedEffect(Unit) {
        var wasNetworkAvailable = PluctNetworkConnectivityChecker.isNetworkAvailable(context)
        
        while (isActive) {
            delay(2000)
            val isNetworkAvailable = PluctNetworkConnectivityChecker.isNetworkAvailable(context)
            
            if (isNetworkAvailable && !wasNetworkAvailable && queuedCount > 0) {
                scope.launch {
                    val processedCount = queueManager.processQueuedVideos(
                        apiService = apiService,
                        currentBalance = creditBalance,
                        currentFreeUses = freeUsesRemaining,
                        isNetworkAvailable = true,
                        onProcess = { video ->
                            PluctUIScreen01MainActivityTranscriptionOrchestrator.processVideo(
                                apiService, video.url, video.tier, creditBalance, freeUsesRemaining,
                                videoRepository, clipboardManager, debugLogManager, context,
                                validator
                            ) { success, newBalance, newFreeUses, _, _ ->
                                onBalanceUpdate(newBalance, newFreeUses)
                                if (success) {
                                    PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                                        scope, snackbarHostState, "Processing queued video..."
                                    )
                                }
                            }
                        }
                    )
                    if (processedCount > 0) {
                        Log.d("MainActivity", "Auto-processed $processedCount queued video(s) after network restored")
                        PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                            scope, snackbarHostState, "Processing $processedCount queued video(s)..."
                        )
                        onQueueProcessed(processedCount)
                    }
                }
            }
            
            wasNetworkAvailable = isNetworkAvailable
        }
    }
}
