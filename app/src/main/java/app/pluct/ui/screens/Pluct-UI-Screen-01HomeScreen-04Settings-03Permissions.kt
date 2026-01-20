package app.pluct.ui.screens

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import app.pluct.data.preferences.PluctUserPreferences
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.core.permission.PluctCorePermission01Manager
import app.pluct.core.permission.PluctCorePermission02Launcher01Helper
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Pluct-UI-Screen-01HomeScreen-04Settings-03Permissions - Permissions section in settings
 * Follows naming convention: [Project]-[UI]-[Screen]-[HomeScreen]-[Settings]-[Sequence][Permissions]
 * 7 scope layers: Project, UI, Screen, HomeScreen, Settings, Sequence, Permissions
 */
@Composable
fun PluctUIScreen01HomeScreen04Settings03PermissionsSection(
    hasNotificationPermission: Boolean,
    hasOverlayPermission: Boolean,
    overlayEnabled: Boolean,
    permissionLauncherHelper: PluctCorePermission02Launcher01Helper?,
    onOverlayEnabledChange: (Boolean) -> Unit,
    onNotificationPermissionUpdate: (Boolean) -> Unit,
    onOverlayPermissionUpdate: (Boolean) -> Unit,
    onThemeModeChange: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PluctUserPreferences(context) }
    var themeMode by remember { mutableStateOf(prefs.getThemeMode()) }
    
    // UX FIX: Track battery optimization status with proper refresh
    var isBatteryOptimized by remember { 
        mutableStateOf(PluctCorePermission01Manager.isBatteryOptimizationExempt(context)) 
    }
    
    // UX FIX: Refresh battery optimization status on initial load with error handling
    LaunchedEffect(Unit) {
        try {
            PluctCorePermission01Manager.invalidateCache() // Clear cache for fresh check
            isBatteryOptimized = PluctCorePermission01Manager.isBatteryOptimizationExempt(context)
        } catch (e: Exception) {
            android.util.Log.w("PluctSettings", "Failed to check battery optimization: ${e.message}")
            // Keep current state on error
        }
    }
    
    // UX FIX: Refresh battery optimization status when returning from system settings
    LaunchedEffect(hasNotificationPermission, hasOverlayPermission) {
        try {
            // Refresh after a delay to catch system setting changes when user returns
            delay(500)
            PluctCorePermission01Manager.invalidateCache() // Clear cache to force refresh
            isBatteryOptimized = PluctCorePermission01Manager.isBatteryOptimizationExempt(context)
        } catch (e: Exception) {
            android.util.Log.w("PluctSettings", "Failed to refresh battery optimization: ${e.message}")
            // Keep current state on error
        }
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Permissions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        // Notification Permission
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    modifier = Modifier.width(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (hasNotificationPermission) "Granted" else "Required",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasNotificationPermission) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
            }
            if (!hasNotificationPermission) {
                val isPermanentlyDenied = if (context is Activity) {
                    PluctCorePermission01Manager.isNotificationPermissionPermanentlyDenied(context)
                } else {
                    false
                }
                Button(
                    onClick = {
                        if (isPermanentlyDenied) {
                            PluctCorePermission01Manager.openNotificationSettings(context)
                        } else if (permissionLauncherHelper != null) {
                            permissionLauncherHelper.requestNotificationPermission { granted ->
                                onNotificationPermissionUpdate(granted)
                            }
                        } else if (context is Activity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            @Suppress("DEPRECATION")
                            androidx.core.app.ActivityCompat.requestPermissions(
                                context,
                                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                app.pluct.core.permission.REQUEST_CODE_NOTIFICATION
                            )
                            PluctCorePermission01Manager.invalidateCache()
                            scope.launch {
                                delay(500)
                                onNotificationPermissionUpdate(PluctCorePermission01Manager.hasNotificationPermission(context))
                            }
                        }
                    },
                    modifier = Modifier.semantics {
                        contentDescription = if (isPermanentlyDenied) "Open notification settings" else "Enable notifications"
                        testTag = "settings_enable_notifications_button"
                    }
                ) {
                    Text(if (isPermanentlyDenied) "Open Settings" else "Enable")
                }
            }
        }
        
        // Overlay Permission
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PictureInPicture,
                    contentDescription = "Overlay",
                    modifier = Modifier.width(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Overlay",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (hasOverlayPermission) "Granted" else "Optional",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasOverlayPermission) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!hasOverlayPermission) {
                Button(
                    onClick = {
                        if (permissionLauncherHelper != null) {
                            permissionLauncherHelper.requestOverlayPermission { granted ->
                                onOverlayPermissionUpdate(granted)
                            }
                        } else {
                            PluctCorePermission01Manager.openOverlaySettings(context)
                            PluctCorePermission01Manager.invalidateCache()
                            scope.launch {
                                delay(1000)
                                onOverlayPermissionUpdate(PluctCorePermission01Manager.hasOverlayPermission(context))
                            }
                        }
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Enable overlay permission"
                        testTag = "settings_enable_overlay_button"
                    }
                ) {
                    Text("Enable")
                }
            }
        }
        
        // Overlay Toggle (only if permission granted)
        if (hasOverlayPermission) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show overlay during transcription",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = overlayEnabled,
                    onCheckedChange = onOverlayEnabledChange,
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle overlay notifications"
                        testTag = "settings_overlay_toggle"
                    }
                )
            }
        }
        
        // UX IMPROVEMENT #3: Battery Optimization Section
        // UX FIX: Better layout with fixed button width to prevent text wrapping
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f) // Allow text to take available space
            ) {
                Icon(
                    imageVector = Icons.Default.BatterySaver,
                    contentDescription = "Battery Optimization",
                    modifier = Modifier.width(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Background Processing",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    // UX FIX: Clearer status text
                    Text(
                        text = if (isBatteryOptimized) "Enabled" else "May be restricted",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isBatteryOptimized)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
            if (!isBatteryOptimized) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        PluctCorePermission01Manager.openBatteryOptimizationSettings(context)
                        // UX FIX: Refresh status after user returns from settings
                        scope.launch {
                            try {
                                delay(1000) // Wait for settings to open
                                // Refresh multiple times to catch when user returns
                                repeat(5) {
                                    delay(2000) // Check every 2 seconds
                                    try {
                                        PluctCorePermission01Manager.invalidateCache()
                                        isBatteryOptimized = PluctCorePermission01Manager.isBatteryOptimizationExempt(context)
                                        if (isBatteryOptimized) return@repeat // Stop if enabled
                                    } catch (e: Exception) {
                                        android.util.Log.w("PluctSettings", "Error checking battery optimization: ${e.message}")
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("PluctSettings", "Error in battery optimization refresh: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier
                        .width(88.dp) // Fixed width to prevent text wrapping
                        .semantics {
                            contentDescription = "Enable background processing"
                            testTag = "settings_enable_battery_optimization_button"
                        }
                ) {
                    Text("Enable")
                }
            }
        }

        // Appearance Section - Theme Toggle
        androidx.compose.material3.Divider()

        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        // Theme Mode Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (themeMode) {
                        "dark" -> Icons.Default.DarkMode
                        "light" -> Icons.Default.LightMode
                        else -> Icons.Default.Settings
                    },
                    contentDescription = "Theme",
                    modifier = Modifier.width(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when (themeMode) {
                            "dark" -> "Dark"
                            "light" -> "Light"
                            else -> "System"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Cycle through theme modes: system -> light -> dark -> system
            Button(
                onClick = {
                    val newMode = when (themeMode) {
                        "system" -> "light"
                        "light" -> "dark"
                        "dark" -> "system"
                        else -> "system"
                    }
                    themeMode = newMode
                    prefs.setThemeMode(newMode)
                    onThemeModeChange?.invoke(newMode)
                },
                modifier = Modifier.semantics {
                    contentDescription = "Change theme"
                    testTag = "settings_theme_button"
                }
            ) {
                Text(
                    text = when (themeMode) {
                        "system" -> "Light"
                        "light" -> "Dark"
                        "dark" -> "Auto"
                        else -> "Light"
                    }
                )
            }
        }
    }
}
