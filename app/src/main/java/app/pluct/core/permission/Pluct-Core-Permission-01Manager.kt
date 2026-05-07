package app.pluct.core.permission

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Pluct-Core-Permission-01Manager - Centralized permission management
 * Follows naming convention: [Project]-[Core]-[Permission]-[Sequence][Manager]
 * 5 scope layers: Project, Core, Permission, Sequence, Manager
 * 
 * Provides centralized permission checking, requesting, and state management
 * with caching to avoid repeated system calls.
 */
object PluctCorePermission01Manager {
    private const val TAG = "PermissionManager"
    
    // Permission state cache to avoid repeated system calls
    @Volatile
    private var notificationPermissionCached: Boolean? = null
    
    @Volatile
    private var overlayPermissionCached: Boolean? = null
    
    @Volatile
    private var batteryOptimizationCached: Boolean? = null
    
    @Volatile
    private var lastPermissionCheckTimestamp: Long = 0
    
    private const val CACHE_VALIDITY_MS = 5000L // Cache valid for 5 seconds
    
    /**
     * Check if notification permission is granted
     * For Android 13+ (API 33+), checks POST_NOTIFICATIONS permission
     * For older versions, always returns true (permission not required)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        // Invalidate cache if stale
        val now = System.currentTimeMillis()
        if (now - lastPermissionCheckTimestamp > CACHE_VALIDITY_MS) {
            notificationPermissionCached = null
        }
        
        // Return cached value if available
        notificationPermissionCached?.let { return it }
        
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires POST_NOTIFICATIONS permission
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 and below: notification permission always granted
            // But we should still check if notification channels are enabled
            true
        }
        val appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val hasPermission = runtimeGranted && appNotificationsEnabled
        
        // Cache the result
        notificationPermissionCached = hasPermission
        lastPermissionCheckTimestamp = now
        
        Log.d(TAG, "Notification permission check: $hasPermission")
        return hasPermission
    }
    
    /**
     * Check if overlay/draw-over-apps permission is granted
     * Uses AppOpsManager to check SYSTEM_ALERT_WINDOW permission
     */
    fun hasOverlayPermission(context: Context): Boolean {
        // Invalidate cache if stale
        val now = System.currentTimeMillis()
        if (now - lastPermissionCheckTimestamp > CACHE_VALIDITY_MS) {
            overlayPermissionCached = null
        }
        
        // Return cached value if available
        overlayPermissionCached?.let { return it }
        
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            val mode = appOpsManager?.checkOpNoThrow(
                AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } else {
            // Android 5 and below: overlay permission always granted
            true
        }
        
        // Cache the result
        overlayPermissionCached = hasPermission
        lastPermissionCheckTimestamp = now
        
        Log.d(TAG, "Overlay permission check: $hasPermission")
        return hasPermission
    }
    
    /**
     * UX IMPROVEMENT #1: Check if app is exempt from battery optimization
     * Battery optimization can kill background WorkManager jobs, causing transcriptions to fail silently
     * when the app is backgrounded. This violates Trust (users expect background processing) and
     * Customer (poor experience when transcriptions don't complete).
     * Returns true if exempt (optimized), false if not exempt (may be killed)
     */
    fun isBatteryOptimizationExempt(context: Context): Boolean {
        // Invalidate cache if stale
        val now = System.currentTimeMillis()
        if (now - lastPermissionCheckTimestamp > CACHE_VALIDITY_MS) {
            batteryOptimizationCached = null
        }
        
        // Return cached value if available
        batteryOptimizationCached?.let { return it }
        
        val isExempt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            // Android 5 and below: no battery optimization restrictions
            true
        }
        
        // Cache the result
        batteryOptimizationCached = isExempt
        lastPermissionCheckTimestamp = now
        
        Log.d(TAG, "Battery optimization exempt check: $isExempt")
        return isExempt
    }
    
    /**
     * UX IMPROVEMENT #1: Open battery optimization settings
     * Guides user to exempt app from battery optimization for reliable background processing
     */
    fun openBatteryOptimizationSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to app settings if direct request fails
                Log.w(TAG, "Failed to open battery optimization request, opening app settings: ${e.message}")
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
    
    // Deprecated request methods removed - use PluctCorePermission02Launcher01Helper instead
    
    /**
     * Open system settings for notification permission
     */
    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    /**
     * Open system settings for overlay permission
     */
    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    /**
     * Check if permission was permanently denied (user selected "Don't ask again")
     * For notification permission (Android 13+)
     */
    fun isNotificationPermissionPermanentlyDenied(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false // Permission not required on older versions
        }
        
        return !hasNotificationPermission(activity) &&
               !ActivityCompat.shouldShowRequestPermissionRationale(
                   activity,
                   android.Manifest.permission.POST_NOTIFICATIONS
               )
    }
    
    /**
     * Check if overlay permission was permanently denied
     * Note: Overlay permission doesn't have "Don't ask again" option,
     * but we can check if user has explicitly denied it
     */
    fun isOverlayPermissionPermanentlyDenied(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false // Permission not required on older versions
        }
        
        return !hasOverlayPermission(context)
    }
    
    /**
     * Invalidate permission cache
     * Call this when permissions may have changed (e.g., after permission request)
     */
    fun invalidateCache() {
        notificationPermissionCached = null
        overlayPermissionCached = null
        batteryOptimizationCached = null
        lastPermissionCheckTimestamp = 0
        Log.d(TAG, "Permission cache invalidated")
    }
    
    // Deprecated handler methods removed - use PluctCorePermission02Launcher01Helper instead
    
}

// Request codes for permission requests
const val REQUEST_CODE_NOTIFICATION = 1001
const val REQUEST_CODE_OVERLAY = 1002
