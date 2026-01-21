package app.pluct.ui.components

import android.app.Activity
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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

    // UX FIX #5: Vibration pattern for completion toast - quick double-tap
    private val COMPLETION_VIBRATION_PATTERN = longArrayOf(0, 100, 100, 100)

    // Track current toast to cancel previous ones
    private var currentToast: Toast? = null
    private val handler = Handler(Looper.getMainLooper())

    /**
     * UX FIX #5: Play notification sound for transcription completion
     * Audible alert when user is not looking at app
     */
    private fun playCompletionSound(context: Context) {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, notification)
            ringtone?.play()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play completion sound: ${e.message}")
        }
    }

    /**
     * UX FIX #5: Vibrate for completion toast
     */
    private fun vibrateForCompletion(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(COMPLETION_VIBRATION_PATTERN, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(COMPLETION_VIBRATION_PATTERN, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(COMPLETION_VIBRATION_PATTERN, -1)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }
    
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
     * UX FIX #5: Now includes sound and vibration for user awareness
     * Only shows if app is in foreground
     */
    fun showTranscriptionFinished(context: Context, @Suppress("UNUSED_PARAMETER") url: String, success: Boolean) {
        val message = if (success) {
            "Transcription complete! Tap to view."
        } else {
            "Transcription failed. Please retry."
        }

        // UX FIX #5: Play sound and vibrate for completion awareness
        if (success) {
            playCompletionSound(context)
            vibrateForCompletion(context)
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
                // UX FIX #5: Use ProcessLifecycleOwner for reliable foreground detection
                try {
                    val lifecycle = androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle
                    val currentState = lifecycle.currentState
                    val isForeground = currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
                    Log.d(TAG, "ProcessLifecycleOwner foreground check: $isForeground (state=$currentState)")
                    isForeground
                } catch (e: Exception) {
                    // Fallback: assume foreground for toast display
                    Log.w(TAG, "ProcessLifecycleOwner check failed, assuming foreground: ${e.message}")
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
