package app.pluct.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.pluct.core.debug.PluctCoreADBDetection
import app.pluct.core.network.PluctNetworkConnectivityChecker
import app.pluct.core.timing.PluctCoreTiming01Constants
import app.pluct.data.entity.QueueReason
import app.pluct.services.OperationStep
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.TranscriptionDebugInfo
import app.pluct.ui.models.data.PersistentError
import kotlinx.coroutines.delay

/**
 * Pluct-UI-Component-03CaptureCard-05State-01Manager
 * Follows naming convention: [Project]-[UI]-[Component]-[CaptureCard]-[State]-[Sequence][Manager]
 * 6 scope layers: Project, UI, Component, CaptureCard, State, Sequence, Manager
 * Single source of truth for CaptureCard state management
 */
object PluctUIComponent03CaptureCard05State01Manager {
    
    /**
     * State data class for CaptureCard
     */
    data class CaptureCardState(
        val urlText: String = "",
        val hasFocus: Boolean = false,
        val validationError: String? = null,
        val showGetCoinsDialog: Boolean = false,
        val showInsightsDialog: Boolean = false,
        val isSubmitting: Boolean = false,
        val processingMessage: String? = null,
        val persistentError: PersistentError? = null,
        val isPrefilledUrl: Boolean = false,
        val timedOutOnce: Boolean = false,
        val showQueuePrompt: Boolean = false,
        val queuePromptReason: String? = null,
        val isAutoSubmitting: Boolean = false,
        val lastStepChangeTime: Long? = null,
        val lastObservedStep: OperationStep? = null
    )
    
    /**
     * Initialize state with pre-filled URL
     */
    fun initializeState(preFilledUrl: String?): CaptureCardState {
        return CaptureCardState(
            urlText = preFilledUrl ?: "",
            isPrefilledUrl = preFilledUrl != null && preFilledUrl.isNotBlank()
        )
    }
    
    /**
     * Monitor timeout based on step changes
     * This is a helper that should be called from LaunchedEffect
     */
    suspend fun monitorTimeout(
        isSubmitting: Boolean,
        debugInfo: TranscriptionDebugInfo?,
        context: Context,
        onTimeout: (PersistentError) -> Unit,
        lastStepChangeTime: Long?,
        lastObservedStep: OperationStep?,
        updateLastStepChangeTime: (Long?) -> Unit,
        updateLastObservedStep: (OperationStep?) -> Unit
    ) {
        if (!isSubmitting) {
            updateLastStepChangeTime(null)
            updateLastObservedStep(null)
            return
        }
        
        val currentStep = debugInfo?.currentStep
        val isAdbConnected = PluctCoreADBDetection.isAdbConnected(context)
        
        // Track step changes
        if (currentStep != null && currentStep != lastObservedStep) {
            updateLastObservedStep(currentStep)
            val now = System.currentTimeMillis()
            updateLastStepChangeTime(now)
            Log.d("CaptureCard", "Step changed to: $currentStep at $now")
        }
        
        // Only timeout if no step change for threshold AND we're past initial submission
        val timeSinceLastStep = lastStepChangeTime?.let { 
            System.currentTimeMillis() - it 
        } ?: Long.MAX_VALUE
        
        // Extended timeout when ADB connected (60s) vs normal (30s)
        val timeoutThreshold = if (isAdbConnected) 60000L else 30000L
        
        if (timeSinceLastStep > timeoutThreshold && lastStepChangeTime != null) {
            // No progress for timeout threshold - show timeout
            Log.w("CaptureCard", "Timeout triggered: no step change for ${timeSinceLastStep}ms")
            onTimeout(
                PersistentError(
                    message = "Processing is taking longer than expected. Tap Retry if it seems stuck.",
                    timestamp = System.currentTimeMillis(),
                    category = "TIMEOUT"
                )
            )
        } else if (lastStepChangeTime == null) {
            // Initial submission, no step info yet - give more time
            delay(if (isAdbConnected) 30000L else 20000L)
            val currentLastStepTime = lastStepChangeTime // Re-read after delay
            if (currentLastStepTime == null) {
                // Still no step info after initial delay
                delay(PluctCoreTiming01Constants.TRANSCRIPTION_STEP_TIMEOUT_MS)
                val finalLastStepTime = lastStepChangeTime // Re-read after second delay
                if (finalLastStepTime == null) {
                    Log.w("CaptureCard", "Timeout: no step info received")
                    onTimeout(
                        PersistentError(
                            message = "Starting transcription... If this persists, check your connection.",
                            timestamp = System.currentTimeMillis(),
                            category = "TIMEOUT"
                        )
                    )
                }
            }
        } else {
            // Step info exists, monitor for changes
            delay(PluctCoreTiming01Constants.TRANSCRIPTION_STEP_CHECK_INTERVAL_MS)
        }
    }
    
    /**
     * Check if queue prompt should be shown
     */
    fun shouldShowQueuePrompt(
        urlText: String,
        creditBalance: Int,
        freeUsesRemaining: Int,
        isSubmitting: Boolean,
        context: Context
    ): Pair<Boolean, String?> {
        if (urlText.isBlank() || isSubmitting) {
            return Pair(false, null)
        }
        
        val hasNetwork = PluctNetworkConnectivityChecker.isNetworkAvailable(context)
        val hasCredits = freeUsesRemaining > 0 || creditBalance >= 1
        
        return when {
            !hasNetwork -> Pair(true, "No internet connection")
            !hasCredits -> Pair(true, "Insufficient credits (need 1 credit or free use)")
            else -> Pair(false, null)
        }
    }
}
