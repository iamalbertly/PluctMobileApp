package app.pluct.core.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

/**
 * Pluct-Core-Permission-02Launcher-01Helper - ActivityResultLauncher helper for permissions
 * Follows naming convention: [Project]-[Core]-[Permission]-[Launcher]-[Sequence][Helper]
 * 5 scope layers: Project, Core, Permission, Launcher, Sequence, Helper
 * 
 * Provides modern ActivityResultLauncher-based permission request handling
 * to replace deprecated onRequestPermissionsResult/onActivityResult methods
 */
class PluctCorePermission02Launcher01Helper(
    private val activity: ComponentActivity
) {
    private val TAG = "PermissionLauncher"
    
    // Notification permission launcher
    private var notificationPermissionLauncher: ActivityResultLauncher<String>? = null
    private var notificationPermissionCallback: ((Boolean) -> Unit)? = null
    
    // Overlay permission launcher
    private var overlayPermissionLauncher: ActivityResultLauncher<Intent>? = null
    private var overlayPermissionCallback: ((Boolean) -> Unit)? = null
    
    /**
     * Initialize permission launchers
     * Must be called from Activity.onCreate before setContent
     */
    fun initialize() {
        // Notification permission launcher
        notificationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            PluctCorePermission01Manager.invalidateCache()
            Log.d(TAG, "Notification permission result: $isGranted")
            notificationPermissionCallback?.invoke(isGranted)
            notificationPermissionCallback = null
        }
        
        // Overlay permission launcher
        overlayPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            PluctCorePermission01Manager.invalidateCache()
            val isGranted = PluctCorePermission01Manager.hasOverlayPermission(activity)
            Log.d(TAG, "Overlay permission result: $isGranted")
            overlayPermissionCallback?.invoke(isGranted)
            overlayPermissionCallback = null
        }
    }
    
    /**
     * Request notification permission using ActivityResultLauncher
     * @param callback Called with true if granted, false otherwise
     */
    fun requestNotificationPermission(callback: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Android 12 and below: permission always granted
            callback(true)
            return
        }
        
        if (PluctCorePermission01Manager.hasNotificationPermission(activity)) {
            callback(true)
            return
        }
        
        notificationPermissionCallback = callback
        notificationPermissionLauncher?.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
    
    /**
     * Request overlay permission using ActivityResultLauncher
     * @param callback Called with true if granted, false otherwise
     */
    fun requestOverlayPermission(callback: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Android 5 and below: permission always granted
            callback(true)
            return
        }
        
        if (PluctCorePermission01Manager.hasOverlayPermission(activity)) {
            callback(true)
            return
        }
        
        overlayPermissionCallback = callback
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        overlayPermissionLauncher?.launch(intent)
    }
}
