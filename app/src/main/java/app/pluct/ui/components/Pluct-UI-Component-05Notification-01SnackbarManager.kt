package app.pluct.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Pluct-UI-Component-05Notification-01SnackbarManager - Centralized snackbar notification management
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Single source of truth for snackbar notifications
 */
object PluctUIComponent05Notification01SnackbarManager {
    
    private const val TAG = "SnackbarManager"
    
    /**
     * Show success notification
     */
    suspend fun showSuccess(
        snackbarHostState: SnackbarHostState,
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ): SnackbarResult {
        return try {
            snackbarHostState.showSnackbar(
                message = message,
                duration = duration,
                withDismissAction = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show success snackbar: ${e.message}", e)
            SnackbarResult.Dismissed
        }
    }
    
    /**
     * Show error notification
     */
    suspend fun showError(
        snackbarHostState: SnackbarHostState,
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Long,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ): SnackbarResult {
        return try {
            snackbarHostState.showSnackbar(
                message = message,
                duration = duration,
                withDismissAction = true,
                actionLabel = actionLabel
            ).also { result ->
                if (result == SnackbarResult.ActionPerformed && onAction != null) {
                    onAction()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show error snackbar: ${e.message}", e)
            SnackbarResult.Dismissed
        }
    }
    
    /**
     * Show info notification
     */
    suspend fun showInfo(
        snackbarHostState: SnackbarHostState,
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ): SnackbarResult {
        return try {
            snackbarHostState.showSnackbar(
                message = message,
                duration = duration,
                withDismissAction = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show info snackbar: ${e.message}", e)
            SnackbarResult.Dismissed
        }
    }
    
    /**
     * Show notification from coroutine scope (non-suspending wrapper)
     */
    fun showSuccessAsync(
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        scope.launch {
            showSuccess(snackbarHostState, message, duration)
        }
    }
    
    /**
     * Show error notification from coroutine scope (non-suspending wrapper)
     */
    fun showErrorAsync(
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Long,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        scope.launch {
            showError(snackbarHostState, message, duration, actionLabel, onAction)
        }
    }
    
    /**
     * Show info notification from coroutine scope (non-suspending wrapper)
     */
    fun showInfoAsync(
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        scope.launch {
            showInfo(snackbarHostState, message, duration)
        }
    }

    /**
     * UX IMPROVEMENT: Show celebration notification for milestone achievements
     * Duolingo-style celebratory messages
     */
    suspend fun showCelebration(
        snackbarHostState: SnackbarHostState,
        milestone: CelebrationMilestone,
        duration: SnackbarDuration = SnackbarDuration.Long
    ): SnackbarResult {
        val message = when (milestone) {
            CelebrationMilestone.FIRST_TRANSCRIPT -> "You did it! Your first transcript is ready!"
            CelebrationMilestone.FIVE_TRANSCRIPTS -> "You're on a roll! 5 transcripts completed!"
            CelebrationMilestone.TEN_TRANSCRIPTS -> "Unstoppable! 10 transcripts and counting!"
            CelebrationMilestone.QUICK_TRANSCRIPT -> "Speed demon! That was fast!"
            CelebrationMilestone.CREDITS_SAVED -> "Smart move! You saved credits!"
        }
        return try {
            snackbarHostState.showSnackbar(
                message = message,
                duration = duration,
                withDismissAction = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show celebration snackbar: ${e.message}", e)
            SnackbarResult.Dismissed
        }
    }

    /**
     * Show celebration notification from coroutine scope (non-suspending wrapper)
     */
    fun showCelebrationAsync(
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        milestone: CelebrationMilestone,
        duration: SnackbarDuration = SnackbarDuration.Long
    ) {
        scope.launch {
            showCelebration(snackbarHostState, milestone, duration)
        }
    }

    /**
     * Celebration milestone types for gamification
     */
    enum class CelebrationMilestone {
        FIRST_TRANSCRIPT,
        FIVE_TRANSCRIPTS,
        TEN_TRANSCRIPTS,
        QUICK_TRANSCRIPT,
        CREDITS_SAVED
    }
}

