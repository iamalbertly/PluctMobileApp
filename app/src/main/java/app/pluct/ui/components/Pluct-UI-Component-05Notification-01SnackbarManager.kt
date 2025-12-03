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
}

