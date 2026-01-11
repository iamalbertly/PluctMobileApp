package app.pluct.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.pluct.core.permission.PluctCorePermission01Manager
import app.pluct.data.preferences.PluctUserPreferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Pluct-UI-Component-06Permission-01Onboarding-01Dialog - Permission onboarding dialog
 * Follows naming convention: [Project]-[UI]-[Component]-[Permission]-[Onboarding]-[Sequence][Dialog]
 * 6 scope layers: Project, UI, Component, Permission, Onboarding, Sequence, Dialog
 * 
 * Progressive permission onboarding dialog that guides users through enabling
 * notifications and overlay permissions with clear value propositions.
 */
@Composable
fun PluctUIComponent06Permission01Onboarding01Dialog(
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    permissionLauncherHelper: app.pluct.core.permission.PluctCorePermission02Launcher01Helper? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PluctUserPreferences(context) }
    
    // Track which permission we're currently showing
    var currentPermission by remember { mutableStateOf<PermissionType?>(null) }
    
    // Determine which permission to show first
    LaunchedEffect(Unit) {
        val hasNotificationPermission = PluctCorePermission01Manager.hasNotificationPermission(context)
        val hasOverlayPermission = PluctCorePermission01Manager.hasOverlayPermission(context)
        val hasSeenNotification = prefs.hasSeenNotificationOnboarding()
        val hasSeenOverlay = prefs.hasSeenOverlayOnboarding()
        
        // Show notification permission first if not granted and not seen
        if (!hasNotificationPermission && !hasSeenNotification) {
            currentPermission = PermissionType.NOTIFICATION
        }
        // Then show overlay permission if not granted and not seen
        else if (!hasOverlayPermission && !hasSeenOverlay) {
            currentPermission = PermissionType.OVERLAY
        }
        // All permissions handled or seen
        else {
            onComplete()
        }
    }
    
    // Show dialog for current permission
    currentPermission?.let { permission ->
        PermissionOnboardingStep(
            permissionType = permission,
            onEnable = {
                // Use ActivityResultLauncher if available, otherwise fallback to old method
                if (permissionLauncherHelper != null && context is Activity) {
                    when (permission) {
                        PermissionType.NOTIFICATION -> {
                            permissionLauncherHelper.requestNotificationPermission { granted ->
                                scope.launch {
                                    // Mark as seen
                                    prefs.markNotificationOnboardingSeen()
                                    
                                    // Move to next permission or complete
                                    if (granted) {
                                        val hasOverlayPermission = PluctCorePermission01Manager.hasOverlayPermission(context)
                                        val hasSeenOverlay = prefs.hasSeenOverlayOnboarding()
                                        
                                        if (!hasOverlayPermission && !hasSeenOverlay) {
                                            currentPermission = PermissionType.OVERLAY
                                        } else {
                                            currentPermission = null
                                            onComplete()
                                        }
                                    } else {
                                        // Permission denied, move to next or complete
                                        val hasSeenOverlay = prefs.hasSeenOverlayOnboarding()
                                        if (!hasSeenOverlay) {
                                            currentPermission = PermissionType.OVERLAY
                                        } else {
                                            currentPermission = null
                                            onComplete()
                                        }
                                    }
                                }
                            }
                        }
                        PermissionType.OVERLAY -> {
                            permissionLauncherHelper.requestOverlayPermission { granted ->
                                scope.launch {
                                    // Mark as seen
                                    prefs.markOverlayOnboardingSeen()
                                    currentPermission = null
                                    onComplete()
                                }
                            }
                        }
                    }
                } else {
                    // Fallback to old method if launcher not available
                    when (permission) {
                        PermissionType.NOTIFICATION -> {
                            if (context is Activity && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                androidx.core.app.ActivityCompat.requestPermissions(
                                    context,
                                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                    app.pluct.core.permission.REQUEST_CODE_NOTIFICATION
                                )
                                PluctCorePermission01Manager.invalidateCache()
                            }
                        }
                        PermissionType.OVERLAY -> {
                            if (context is Activity) {
                                PluctCorePermission01Manager.openOverlaySettings(context)
                                PluctCorePermission01Manager.invalidateCache()
                            }
                        }
                    }
                    
                    scope.launch {
                        delay(1000)
                        val granted = when (permission) {
                            PermissionType.NOTIFICATION -> {
                                PluctCorePermission01Manager.hasNotificationPermission(context)
                            }
                            PermissionType.OVERLAY -> {
                                PluctCorePermission01Manager.hasOverlayPermission(context)
                            }
                        }
                        
                        when (permission) {
                            PermissionType.NOTIFICATION -> prefs.markNotificationOnboardingSeen()
                            PermissionType.OVERLAY -> prefs.markOverlayOnboardingSeen()
                        }
                        
                        if (granted || permission == PermissionType.OVERLAY) {
                            val hasOverlayPermission = PluctCorePermission01Manager.hasOverlayPermission(context)
                            val hasSeenOverlay = prefs.hasSeenOverlayOnboarding()
                            
                            if (!hasOverlayPermission && !hasSeenOverlay && permission == PermissionType.NOTIFICATION) {
                                currentPermission = PermissionType.OVERLAY
                            } else {
                                currentPermission = null
                                onComplete()
                            }
                        } else {
                            if (permission == PermissionType.NOTIFICATION) {
                                val hasSeenOverlay = prefs.hasSeenOverlayOnboarding()
                                if (!hasSeenOverlay) {
                                    currentPermission = PermissionType.OVERLAY
                                } else {
                                    currentPermission = null
                                    onComplete()
                                }
                            } else {
                                currentPermission = null
                                onComplete()
                            }
                        }
                    }
                }
            },
            onSkip = {
                // Mark as seen and move to next or complete
                when (permission) {
                    PermissionType.NOTIFICATION -> {
                        prefs.markNotificationOnboardingSeen()
                        val hasSeenOverlay = prefs.hasSeenOverlayOnboarding()
                        if (!hasSeenOverlay) {
                            currentPermission = PermissionType.OVERLAY
                        } else {
                            currentPermission = null
                            onComplete()
                        }
                    }
                    PermissionType.OVERLAY -> {
                        prefs.markOverlayOnboardingSeen()
                        currentPermission = null
                        onComplete()
                    }
                }
            },
            onDismiss = {
                // Mark as seen and complete
                when (permission) {
                    PermissionType.NOTIFICATION -> prefs.markNotificationOnboardingSeen()
                    PermissionType.OVERLAY -> prefs.markOverlayOnboardingSeen()
                }
                currentPermission = null
                onDismiss()
            }
        )
    }
}

/**
 * Single permission onboarding step
 */
@Composable
private fun PermissionOnboardingStep(
    permissionType: PermissionType,
    onEnable: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, description, icon) = when (permissionType) {
        PermissionType.NOTIFICATION -> Triple(
            "Enable Notifications",
            "Get notified when your transcriptions complete, even when the app is closed.",
            "🔔"
        )
        PermissionType.OVERLAY -> Triple(
            "Enable Overlay Notifications",
            "See transcription progress in a small floating window when using other apps. You can turn this off in settings.",
            "📱"
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    contentDescription = "Permission onboarding title"
                    testTag = "permission_onboarding_title"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Permission onboarding content"
                        testTag = "permission_onboarding_content"
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon
                Text(
                    text = icon,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                // Description
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onEnable,
                modifier = Modifier.semantics {
                    contentDescription = "Enable permission"
                    testTag = "permission_onboarding_enable_button"
                }
            ) {
                Text("Enable")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.semantics {
                    contentDescription = "Skip permission"
                    testTag = "permission_onboarding_skip_button"
                }
            ) {
                Text("Skip")
            }
        },
        modifier = Modifier.semantics {
            contentDescription = "Permission onboarding dialog"
            testTag = "permission_onboarding_dialog"
        }
    )
}

/**
 * Permission type enum
 */
private enum class PermissionType {
    NOTIFICATION,
    OVERLAY
}
