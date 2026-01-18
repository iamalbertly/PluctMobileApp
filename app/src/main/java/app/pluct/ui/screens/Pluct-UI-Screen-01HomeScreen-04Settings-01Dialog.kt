package app.pluct.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.core.permission.PluctCorePermission01Manager
import app.pluct.core.permission.PluctCorePermission02Launcher01Helper
import app.pluct.data.preferences.PluctUserPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pluct-UI-Screen-01HomeScreen-04Settings-01Dialog - Settings dialog component
 * Follows naming convention: [Project]-[UI]-[Screen]-[HomeScreen]-[Settings]-[Sequence][Dialog]
 * 7 scope layers: Project, UI, Screen, HomeScreen, Settings, Sequence, Dialog
 * 
 * Extracted from Pluct-UI-Screen-01HomeScreen.kt to maintain <300 lines per file
 */
@Composable
fun PluctUIScreen01HomeScreen04Settings01Dialog(
    userName: String,
    creditBalance: Int,
    debugLogCount: Int,
    errorLogCount: Int = 0, // UX FIX: Error count for badge display
    onDismiss: () -> Unit,
    onRequestCredits: (String) -> Unit,
    onViewDebugLogs: () -> Unit,
    permissionLauncherHelper: PluctCorePermission02Launcher01Helper? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PluctUserPreferences(context) }
    
    var referenceText by remember { mutableStateOf("") }
    var isRequesting by remember { mutableStateOf(false) }
    var isCreditRequestInFlight by remember { mutableStateOf(false) }
    var creditRequestStatus by remember { mutableStateOf<String?>(null) }
    var requestId by remember { mutableStateOf<String?>(null) }
    
    // Permission states
    var hasNotificationPermission by remember { 
        mutableStateOf(PluctCorePermission01Manager.hasNotificationPermission(context)) 
    }
    var hasOverlayPermission by remember { 
        mutableStateOf(PluctCorePermission01Manager.hasOverlayPermission(context)) 
    }
    var overlayEnabled by remember { 
        mutableStateOf(prefs.getOverlayNotificationsEnabled()) 
    }
    
    // Refresh permission states
    LaunchedEffect(Unit) {
        hasNotificationPermission = PluctCorePermission01Manager.hasNotificationPermission(context)
        hasOverlayPermission = PluctCorePermission01Manager.hasOverlayPermission(context)
        overlayEnabled = prefs.getOverlayNotificationsEnabled()
    }
    
    LaunchedEffect(hasNotificationPermission, hasOverlayPermission) {
        delay(500)
        hasNotificationPermission = PluctCorePermission01Manager.hasNotificationPermission(context)
        hasOverlayPermission = PluctCorePermission01Manager.hasOverlayPermission(context)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Settings & Credits",
                modifier = Modifier.semantics {
                    contentDescription = "Settings and Credits"
                    testTag = "settings_dialog_title"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Settings content"
                        testTag = "settings_dialog_content"
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Info Section
                PluctUIScreen01HomeScreen04Settings02UserInfoSection(
                    userName = userName,
                    creditBalance = creditBalance
                )

                androidx.compose.material3.Divider()

                // Permissions Section
                PluctUIScreen01HomeScreen04Settings03PermissionsSection(
                    hasNotificationPermission = hasNotificationPermission,
                    hasOverlayPermission = hasOverlayPermission,
                    overlayEnabled = overlayEnabled,
                    permissionLauncherHelper = permissionLauncherHelper,
                    onOverlayEnabledChange = { enabled ->
                        overlayEnabled = enabled
                        prefs.setOverlayNotificationsEnabled(enabled)
                    },
                    onNotificationPermissionUpdate = { hasNotificationPermission = it },
                    onOverlayPermissionUpdate = { hasOverlayPermission = it }
                )

                androidx.compose.material3.Divider()

                // Credits Section
                PluctUIScreen01HomeScreen04Settings04CreditsSection(
                    isRequesting = isRequesting,
                    referenceText = referenceText,
                    onReferenceTextChange = { referenceText = it },
                    isCreditRequestInFlight = isCreditRequestInFlight,
                    creditRequestStatus = creditRequestStatus,
                    onRequestCredits = {
                        if (referenceText.isNotBlank()) {
                            isCreditRequestInFlight = true
                            onRequestCredits(referenceText)
                            creditRequestStatus = "Request sent. We'll verify your payment and apply credits."
                            requestId = "credit_req_${System.currentTimeMillis()}"
                            scope.launch {
                                delay(2000)
                                isCreditRequestInFlight = false
                            }
                        }
                    },
                    onToggleRequesting = { isRequesting = !isRequesting }
                )

                // Debug Logs Section
                if (debugLogCount > 0) {
                    androidx.compose.material3.Divider()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Debug",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = onViewDebugLogs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = "View debug logs"
                                    testTag = "settings_view_debug_logs_button"
                                }
                        ) {
                            // UX FIX: Show error count badge if errors exist
                            if (errorLogCount > 0) {
                                Text("View Debug Logs ($errorLogCount errors)")
                            } else {
                                Text("View Debug Logs ($debugLogCount)")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isRequesting) {
                if (creditRequestStatus != null) {
                    Button(
                        onClick = {
                            isRequesting = false
                            referenceText = ""
                            creditRequestStatus = null
                            requestId = null
                            isCreditRequestInFlight = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OK")
                    }
                } else if (!isCreditRequestInFlight) {
                    Button(
                        onClick = {
                            if (referenceText.isNotBlank()) {
                                isCreditRequestInFlight = true
                                onRequestCredits(referenceText)
                                creditRequestStatus = "Request sent. We'll verify your payment and apply credits."
                                requestId = "credit_req_${System.currentTimeMillis()}"
                                scope.launch {
                                    delay(2000)
                                    isCreditRequestInFlight = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Submit credits request"
                                testTag = "settings_submit_request_button"
                            },
                        enabled = referenceText.isNotBlank() && !isCreditRequestInFlight
                    ) {
                        Text("Submit Request")
                    }
                }
            } else {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics {
                        contentDescription = "Close settings"
                        testTag = "settings_dialog_close"
                    }
                ) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (isRequesting) {
                TextButton(
                    onClick = { isRequesting = false },
                    modifier = Modifier.semantics {
                        contentDescription = "Cancel credits request"
                        testTag = "settings_cancel_request_button"
                    }
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}
