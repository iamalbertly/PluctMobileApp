package app.pluct.ui.components

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pluct-UI-Component-05Notification-02Toast-01Helper - Toast notification helper
 * Follows naming convention: [Project]-[UI]-[Component]-[Notification]-[Toast]-[Sequence][Helper]
 * 6 scope layers: Project, UI, Component, Notification, Toast, Sequence, Helper
 * 
 * Provides thread-safe, lifecycle-aware toast notifications for transcription events.
 * Toasts are shown only when app is in foreground to avoid interrupting background processes.
 */
object PluctUIComponent05Notification02Toast01Helper {
    private const val TAG = "ToastHelper"
    private const val TOAST_DURATION_LONG_MS = 5000L // 5 seconds for long toasts
    
    // Track current toast to cancel previous ones
    private var currentToast: Toast? = null
    private val handler = Handler(Looper.getMainLooper())
    
    /**
     * Show toast notification when transcription starts
     * Only shows if app is in foreground
     */
    fun showTranscriptionStarted(context: Context, @Suppress("UNUSED_PARAMETER") url: String) {
        showToast(
            context = context,
            message = "Transcription started - Processing TikTok video...",
            duration = Toast.LENGTH_LONG
        )
    }
    
    /**
     * Show toast notification when transcription finishes
     * Only shows if app is in foreground
     */
    fun showTranscriptionFinished(context: Context, @Suppress("UNUSED_PARAMETER") url: String, success: Boolean) {
        val message = if (success) {
            "Transcription complete! Tap to view transcript."
        } else {
            "Transcription failed. Please try again."
        }
        
        showToast(
            context = context,
            message = message,
            duration = Toast.LENGTH_LONG
        )
    }
    
    /**
     * Show toast notification when permission is required
     */
    fun showPermissionRequired(context: Context, permissionType: String) {
        val message = when (permissionType) {
            "notification" -> "Notification permission required to receive transcription updates"
            "overlay" -> "Overlay permission required to show transcription status"
            else -> "Permission required for full app functionality"
        }
        
        showToast(
            context = context,
            message = message,
            duration = Toast.LENGTH_LONG
        )
    }
    
    /**
     * Show generic toast message
     * Thread-safe: ensures toast is shown on main thread
     */
    fun showToast(
        context: Context,
        message: String,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        // Check if app is in foreground
        if (!isAppInForeground(context)) {
            Log.d(TAG, "App is in background, skipping toast: $message")
            return
        }
        
        // Cancel previous toast if exists
        currentToast?.cancel()
        
        // Show toast on main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showToastOnMainThread(context, message, duration)
        } else {
            handler.post {
                showToastOnMainThread(context, message, duration)
            }
        }
    }
    
    /**
     * Show toast on main thread (internal method)
     */
    private fun showToastOnMainThread(
        context: Context,
        message: String,
        duration: Int
    ) {
        try {
            // Use application context to avoid memory leaks
            val appContext = context.applicationContext
            
            // Create toast with custom duration for long messages
            val toastDuration = if (duration == Toast.LENGTH_LONG) {
                // For long duration, we'll use LENGTH_LONG but with custom handler
                Toast.LENGTH_LONG
            } else {
                Toast.LENGTH_SHORT
            }
            
            val toast = Toast.makeText(appContext, message, toastDuration)
            
            // Position toast at bottom center
            toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
            
            // Store reference to cancel if needed
            currentToast = toast
            
            // Show toast
            toast.show()
            
            // Auto-dismiss after custom duration for long toasts
            if (duration == Toast.LENGTH_LONG) {
                handler.postDelayed({
                    currentToast?.cancel()
                    currentToast = null
                }, TOAST_DURATION_LONG_MS)
            } else {
                // Clear reference after short duration
                handler.postDelayed({
                    if (currentToast == toast) {
                        currentToast = null
                    }
                }, 2000)
            }
            
            Log.d(TAG, "Toast shown: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show toast: ${e.message}", e)
        }
    }
    
    /**
     * Check if app is in foreground
     * Improved check using Activity lifecycle state
     */
    private fun isAppInForeground(context: Context): Boolean {
        return when (context) {
            is Activity -> {
                // Check if activity is in a visible state
                val isResumed = try {
                    // Use lifecycle-aware check if available
                    if (context is androidx.lifecycle.LifecycleOwner) {
                        context.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
                    } else {
                        // Fallback to simple check
                        !context.isFinishing && !context.isDestroyed
                    }
                } catch (e: Exception) {
                    // Fallback to simple check
                    !context.isFinishing && !context.isDestroyed
                }
                isResumed
            }
            else -> {
                // For non-Activity contexts (e.g., Application context), 
                // check if any activity is in foreground
                try {
                    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                    val runningTasks = activityManager?.getRunningTasks(1)
                    val topActivity = runningTasks?.firstOrNull()?.topActivity
                    topActivity?.packageName == context.packageName
                } catch (e: Exception) {
                    // Fallback: assume foreground for Application context
                    true
                }
            }
        }
    }
    
    /**
     * Cancel current toast if any
     */
    fun cancelCurrentToast() {
        currentToast?.cancel()
        currentToast = null
    }
    
    /**
     * Show toast from coroutine scope (convenience method)
     */
    fun showToastAsync(
        scope: CoroutineScope,
        context: Context,
        message: String,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        scope.launch(Dispatchers.Main) {
            showToast(context, message, duration)
        }
    }
}
