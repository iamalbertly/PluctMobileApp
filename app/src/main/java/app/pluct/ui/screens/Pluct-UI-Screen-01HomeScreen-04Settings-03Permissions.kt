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
    onOverlayPermissionUpdate: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // UX IMPROVEMENT #3: Track battery optimization status
    var isBatteryOptimized by remember { 
        mutableStateOf(PluctCorePermission01Manager.isBatteryOptimizationExempt(context)) 
    }
    
    LaunchedEffect(Unit) {
        isBatteryOptimized = PluctCorePermission01Manager.isBatteryOptimizationExempt(context)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Text(
                        text = if (isBatteryOptimized) "Optimized" else "May be restricted",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isBatteryOptimized) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
            }
            if (!isBatteryOptimized) {
                Button(
                    onClick = {
                        PluctCorePermission01Manager.openBatteryOptimizationSettings(context)
                        scope.launch {
                            delay(1000)
                            isBatteryOptimized = PluctCorePermission01Manager.isBatteryOptimizationExempt(context)
                        }
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Enable background processing"
                        testTag = "settings_enable_battery_optimization_button"
                    }
                ) {
                    Text("Enable")
                }
            }
        }
    }
}
