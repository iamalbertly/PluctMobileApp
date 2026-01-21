package app.pluct.ui.components

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import app.pluct.core.permission.PluctCorePermission01Manager
import app.pluct.data.preferences.PluctUserPreferences

/**
 * Pluct-UI-Component-05Notification-03Overlay-01Service - Overlay notification service
 * Follows naming convention: [Project]-[UI]-[Component]-[Notification]-[Overlay]-[Sequence][Service]
 * 6 scope layers: Project, UI, Component, Notification, Overlay, Sequence, Service
 * 
 * Provides floating overlay window to show transcription status when app is in background.
 * Only shows when overlay permission is granted and user preference is enabled.
 */
class PluctUIComponent05Notification03Overlay01Service : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var statusText: String = "Processing..."
    private var progress: Int = 0
    
    companion object {
        private const val TAG = "OverlayService"
        const val ACTION_SHOW_OVERLAY = "app.pluct.SHOW_OVERLAY"
        const val ACTION_UPDATE_STATUS = "app.pluct.UPDATE_STATUS"
        const val ACTION_DISMISS_OVERLAY = "app.pluct.DISMISS_OVERLAY"
        
        const val EXTRA_STATUS_TEXT = "status_text"
        const val EXTRA_PROGRESS = "progress"
        
        private var instance: PluctUIComponent05Notification03Overlay01Service? = null
        
        /**
         * Check if overlay should be shown
         * Returns true if permission granted and user preference enabled
         */
        fun shouldShowOverlay(context: Context): Boolean {
            // Check permission
            if (!PluctCorePermission01Manager.hasOverlayPermission(context)) {
                Log.d(TAG, "Overlay permission not granted")
                return false
            }
            
            // Check user preference
            val prefs = PluctUserPreferences(context)
            val overlayEnabled = prefs.getOverlayNotificationsEnabled()
            
            if (!overlayEnabled) {
                Log.d(TAG, "Overlay notifications disabled in settings")
                return false
            }
            
            return true
        }
        
        /**
         * Start overlay service with status
         */
        fun startOverlay(context: Context, statusText: String, progress: Int = 0) {
            if (!shouldShowOverlay(context)) {
                Log.d(TAG, "Overlay should not be shown, skipping")
                return
            }
            
            val intent = Intent(context, PluctUIComponent05Notification03Overlay01Service::class.java).apply {
                action = ACTION_SHOW_OVERLAY
                putExtra(EXTRA_STATUS_TEXT, statusText)
                putExtra(EXTRA_PROGRESS, progress)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * Update overlay status
         */
        fun updateOverlay(context: Context, statusText: String, progress: Int = 0) {
            val intent = Intent(context, PluctUIComponent05Notification03Overlay01Service::class.java).apply {
                action = ACTION_UPDATE_STATUS
                putExtra(EXTRA_STATUS_TEXT, statusText)
                putExtra(EXTRA_PROGRESS, progress)
            }
            context.startService(intent)
        }
        
        /**
         * Dismiss overlay
         */
        fun dismissOverlay(context: Context) {
            val intent = Intent(context, PluctUIComponent05Notification03Overlay01Service::class.java).apply {
                action = ACTION_DISMISS_OVERLAY
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Log.d(TAG, "Overlay service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                val statusText = intent.getStringExtra(EXTRA_STATUS_TEXT) ?: "Processing..."
                val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                showOverlay(statusText, progress)
            }
            ACTION_UPDATE_STATUS -> {
                val statusText = intent.getStringExtra(EXTRA_STATUS_TEXT) ?: "Processing..."
                val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                updateStatus(statusText, progress)
            }
            ACTION_DISMISS_OVERLAY -> {
                dismissOverlay()
                stopSelf()
            }
        }
        return START_NOT_STICKY // Don't restart if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        dismissOverlay()
        instance = null
        Log.d(TAG, "Overlay service destroyed")
    }
    
    /**
     * Show overlay window
     */
    private fun showOverlay(statusText: String, progress: Int) {
        // Check permission again
        if (!PluctCorePermission01Manager.hasOverlayPermission(this)) {
            Log.w(TAG, "Overlay permission not granted, cannot show overlay")
            stopSelf()
            return
        }
        
        // Don't show if app is in foreground (use toast instead)
        if (isAppInForeground()) {
            Log.d(TAG, "App is in foreground, skipping overlay")
            stopSelf()
            return
        }
        
        // Don't show on lock screen or during phone call
        if (shouldNotShowOverlay()) {
            Log.d(TAG, "Should not show overlay (lock screen or phone call)")
            stopSelf()
            return
        }
        
        // Remove existing overlay if any
        dismissOverlay()
        
        this.statusText = statusText
        this.progress = progress
        
        try {
            // Create overlay view using standard Android Views
            val overlayLayout = FrameLayout(this).apply {
                setPadding(32, 32, 32, 32)
                setBackgroundColor(0xE0000000.toInt()) // Semi-transparent black
                alpha = 0.9f
            }
            
            // Add progress indicator
            val progressBar = ProgressBar(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    64,
                    64,
                    Gravity.CENTER
                )
            }
            overlayLayout.addView(progressBar)
            
            // Add status text
            val statusTextView = TextView(this).apply {
                text = statusText
                textSize = 12f
                setTextColor(0xFFFFFFFF.toInt())
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                ).apply {
                    topMargin = 80
                }
            }
            overlayLayout.addView(statusTextView)
            
            overlayView = overlayLayout
            
            // Create window parameters
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 20
                y = 100
            }
            
            windowManager?.addView(overlayView, params)
            Log.d(TAG, "Overlay shown: $statusText")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay: ${e.message}", e)
            stopSelf()
        }
    }
    
    /**
     * Update overlay status
     */
    private fun updateStatus(statusText: String, progress: Int) {
        this.statusText = statusText
        this.progress = progress
        
        overlayView?.let { view ->
            if (view is FrameLayout) {
                // Find and update status text
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    if (child is TextView) {
                        child.text = statusText
                        break
                    }
                }
            }
        }
    }
    
    /**
     * Dismiss overlay
     */
    private fun dismissOverlay() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
                Log.d(TAG, "Overlay dismissed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dismiss overlay: ${e.message}", e)
            }
        }
        overlayView = null
    }
    
    /**
     * TECH DEBT #3: Check if app is in foreground using safe approach
     * getRunningTasks is deprecated since Android 5.0 (API 21)
     * For overlay service, we default to false (show overlay) if check fails
     */
    private fun isAppInForeground(): Boolean {
        // For overlay service context, we cannot reliably detect foreground state
        // without ProcessLifecycleOwner dependency. Default to false (background)
        // which means overlays will be shown - safer behavior for user awareness
        Log.d(TAG, "Overlay service foreground check: assuming background (will show overlay)")
        return false
    }
    
    /**
     * Check if overlay should not be shown (lock screen, phone call, etc.)
     */
    private fun shouldNotShowOverlay(): Boolean {
        try {
            // Check for keyguard (lock screen)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
            if (keyguardManager?.isKeyguardLocked == true) {
                Log.d(TAG, "Keyguard locked, not showing overlay")
                return true
            }
            
            // Check for phone call state (requires READ_PHONE_STATE permission)
            // For now, we'll skip this check to avoid requiring additional permission
            // The overlay is small and non-intrusive, so it's acceptable during calls
            
            return false
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check overlay conditions: ${e.message}")
            return false
        }
    }
}

