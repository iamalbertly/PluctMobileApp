package app.pluct.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.QueueReason
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.PluctCoreAPIDetailedError
import app.pluct.services.PluctQueueManager
import app.pluct.ui.components.PluctUIComponent05Notification01SnackbarManager
import app.pluct.core.debug.PluctCoreDebug01LogManager
import androidx.compose.material3.SnackbarHostState

/**
 * Pluct-UI-Screen-01MainActivity-06EventHandlers
 * Follows naming convention: [Project]-[UI]-[Screen]-[MainActivity]-[EventHandlers]
 * 5 scope layers: Project, UI, Screen, MainActivity, EventHandlers
 * Handles all event handlers for MainActivity
 */
class PluctUIScreen01MainActivity06EventHandlers(
    private val scope: CoroutineScope,
    private val apiService: PluctCoreAPIUnifiedService,
    private val videoRepository: PluctVideoRepository,
    private val clipboardManager: ClipboardManager,
    private val debugLogManager: PluctCoreDebug01LogManager,
    private val context: Context,
    private val queueManager: PluctQueueManager,
    private val snackbarHostState: SnackbarHostState,
    private val validator: app.pluct.services.PluctCoreValidationInputSanitizer,
    private val onBalanceUpdate: (Int, Int) -> Unit,
    private val onErrorUpdate: (Throwable?, String?) -> Unit
) {
    fun createOnTierSubmit(
        creditBalance: Int,
        freeUsesRemaining: Int
    ): (String, ProcessingTier) -> Unit = { url: String, tier: ProcessingTier ->
        val resultHandler = createProcessHandler(
            urlContext = url,
            successSummary = { errorMessage -> errorMessage ?: "Transcription started successfully" },
            failureSummary = "Failed to process video. Please try again."
        )

        scope.launch {
            PluctUIScreen01MainActivityTranscriptionOrchestrator.processVideo(
                apiService, url, tier, creditBalance, freeUsesRemaining,
                videoRepository, clipboardManager, debugLogManager, context,
                validator, resultHandler
            )
        }
    }

    private fun createProcessHandler(
        urlContext: String?,
        successSummary: (String?) -> String,
        failureSummary: String
    ): (Boolean, Int, Int, String?, Throwable?) -> Unit = { success, newBalance, newFreeUses, errorMessage, error ->
        if (success) {
            onBalanceUpdate(newBalance, newFreeUses)
            onErrorUpdate(null, null)
            val msg = successSummary(errorMessage)
            PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                scope, snackbarHostState, msg
            )
        } else {
            onErrorUpdate(error, urlContext)
            if (error == null) {
                val errorMsg = errorMessage ?: failureSummary
                PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                    scope, snackbarHostState, errorMsg
                )
            }
            Log.e("MainActivity", "$failureSummary ${errorMessage ?: error?.message}")
        }
    }

    /**
     * UX IMPROVEMENT #1: Add immediate feedback when queue items are retried
     * UX IMPROVEMENT #3: Add success confirmation for retry actions
     */
    fun createOnRetryVideo(
        creditBalance: Int,
        freeUsesRemaining: Int
    ): (VideoItem) -> Unit = { video ->
        // Show immediate feedback
        scope.launch {
            PluctUIComponent05Notification01SnackbarManager.showInfoAsync(
                scope, snackbarHostState, "Retrying video..."
            )
        }
        
        val resultHandler = createProcessHandler(
            urlContext = null,
            successSummary = { _ -> "Video retry started successfully" },
            failureSummary = "Failed to retry video. Please try again."
        )

        scope.launch {
            PluctUIScreen01MainActivityTranscriptionOrchestrator.processVideo(
                apiService, video.url, video.tier, creditBalance, freeUsesRemaining,
                videoRepository, clipboardManager, debugLogManager, context,
                validator, resultHandler
            )
        }
    }

    /**
     * UX IMPROVEMENT #1: Add immediate feedback when queue items are deleted
     * UX IMPROVEMENT #3: Add success confirmation for critical user actions
     */
    fun createOnDeleteVideo(): (VideoItem) -> Unit = { video ->
        scope.launch {
            val deleteResult = videoRepository.deleteVideo(video)
            deleteResult.fold(
                onSuccess = {
                    Log.d("MainActivity", "Video deleted: ${video.id}")
                    PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                        scope, snackbarHostState, "Video removed successfully"
                    )
                },
                onFailure = { error ->
                    Log.e("MainActivity", "Failed to delete video: ${error.message}", error)
                    PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                        scope, snackbarHostState, "Failed to remove video. Please try again."
                    )
                }
            )
        }
    }

    fun createOnQueueForLater(
        currentErrorUrl: String?
    ): () -> Unit = {
        scope.launch {
            val url = currentErrorUrl
            if (url != null) {
                val tier = ProcessingTier.EXTRACT_SCRIPT
                val result = queueManager.queueVideo(
                    url = url,
                    tier = tier,
                    reason = QueueReason.INSUFFICIENT_CREDITS
                )
                if (result.isSuccess) {
                    PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                        scope, snackbarHostState, "Video saved! Will process when credits are added."
                    )
                } else {
                    PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                        scope, snackbarHostState, "Failed to save video. Please try again."
                    )
                }
            }
            onErrorUpdate(null, null)
        }
    }

}
