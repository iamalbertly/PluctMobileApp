package app.pluct.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.sp
import app.pluct.core.permission.PluctCorePermission01Manager
import app.pluct.core.permission.PluctCorePermission02Launcher01Helper
import app.pluct.data.preferences.PluctUserPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pluct-UI-Screen-01HomeScreen-04Settings-00SharedBody
 * Single scroll body for settings — used by bottom sheet and full-screen Settings tab (Customer: one place).
 */
@Composable
fun PluctUIScreen01HomeScreen04Settings00SharedBody(
    userName: String,
    creditBalance: Int,
    debugLogCount: Int,
    errorLogCount: Int,
    onRequestCredits: (String) -> Unit,
    onViewDebugLogs: () -> Unit,
    onSendDiagnostic: () -> Unit,
    permissionLauncherHelper: PluctCorePermission02Launcher01Helper?,
    onThemeModeChange: ((String) -> Unit)?,
    showSheetTitle: Boolean,
    showCloseRow: Boolean,
    onClose: () -> Unit,
    /** When true (e.g. user hit submit with no balance), expand Credits request without an extra tap. */
    expandCreditsRequestSection: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PluctUserPreferences(context) }

    var referenceText by remember { mutableStateOf("") }
    var isRequesting by remember { mutableStateOf(false) }
    var isCreditRequestInFlight by remember { mutableStateOf(false) }
    var creditRequestStatus by remember { mutableStateOf<String?>(null) }

    var hasNotificationPermission by remember {
        mutableStateOf(PluctCorePermission01Manager.hasNotificationPermission(context))
    }
    var hasOverlayPermission by remember {
        mutableStateOf(PluctCorePermission01Manager.hasOverlayPermission(context))
    }
    var overlayEnabled by remember {
        mutableStateOf(prefs.getOverlayNotificationsEnabled())
    }

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

    LaunchedEffect(expandCreditsRequestSection) {
        if (expandCreditsRequestSection) {
            isRequesting = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .semantics {
                contentDescription = "Settings content"
                testTag = "settings_sheet_content"
            },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showSheetTitle) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { testTag = "settings_dialog_title" }
            )
        }

        PluctSettingsGroupedSection(
            sectionLabel = "Credits",
            testTagSuffix = "credits"
        ) {
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
                        scope.launch {
                            delay(2000)
                            isCreditRequestInFlight = false
                        }
                    }
                },
                onToggleRequesting = { isRequesting = !isRequesting }
            )
        }

        PluctSettingsGroupedSection(
            sectionLabel = "Account",
            testTagSuffix = "account"
        ) {
            PluctUIScreen01HomeScreen04Settings02UserInfoSection(
                userName = userName,
                creditBalance = creditBalance
            )
        }

        PluctSettingsGroupedSection(
            sectionLabel = "Preferences",
            testTagSuffix = "permissions"
        ) {
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
                onOverlayPermissionUpdate = { hasOverlayPermission = it },
                onThemeModeChange = onThemeModeChange
            )
        }

        PluctSettingsGroupedSection(
            sectionLabel = "Support",
            testTagSuffix = "support"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSendDiagnostic,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { testTag = "settings_send_diagnostic_button" }
                ) {
                    Text("Send report to support")
                }
                if (debugLogCount > 0) {
                    TextButton(
                        onClick = onViewDebugLogs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { testTag = "settings_view_debug_logs_button" }
                    ) {
                        Text(if (errorLogCount > 0) "View logs ($errorLogCount errors)" else "View logs ($debugLogCount)")
                    }
                }
            }
        }

        if (isRequesting) {
            if (creditRequestStatus != null) {
                Button(
                    onClick = {
                        isRequesting = false
                        referenceText = ""
                        creditRequestStatus = null
                        isCreditRequestInFlight = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("OK") }
            } else if (!isCreditRequestInFlight) {
                Button(
                    onClick = {
                        if (referenceText.isNotBlank()) {
                            isCreditRequestInFlight = true
                            onRequestCredits(referenceText)
                            creditRequestStatus = "Request sent. We'll verify your payment and apply credits."
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
                ) { Text("Submit Request") }
            }
        } else if (showCloseRow) {
            TextButton(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Close settings"
                        testTag = "settings_dialog_close"
                    }
            ) { Text("Close") }
        }
        if (isRequesting && creditRequestStatus == null) {
            TextButton(
                onClick = { isRequesting = false },
                modifier = Modifier.semantics { testTag = "settings_cancel_request_button" }
            ) { Text("Cancel") }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PluctSettingsGroupedSection(
    sectionLabel: String,
    testTagSuffix: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = sectionLabel.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            letterSpacing = 1.1.sp,
            modifier = Modifier
                .padding(start = 4.dp, bottom = 8.dp)
                .semantics { testTag = "settings_section_$testTagSuffix" }
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTag = "settings_group_card_$testTagSuffix" },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}
