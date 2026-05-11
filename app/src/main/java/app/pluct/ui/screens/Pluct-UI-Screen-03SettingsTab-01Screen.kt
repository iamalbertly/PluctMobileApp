package app.pluct.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.pluct.core.permission.PluctCorePermission02Launcher01Helper

/**
 * Pluct-UI-Screen-03SettingsTab-01Screen — full-screen settings (same body as sheet; Speed: fewer taps).
 */
@Composable
fun PluctUIScreen03SettingsTab01Screen(
    paddingValues: PaddingValues,
    userName: String,
    creditBalance: Int,
    debugLogCount: Int,
    errorLogCount: Int,
    onRequestCredits: (String) -> Unit,
    onViewDebugLogs: () -> Unit,
    onSendDiagnostic: () -> Unit,
    permissionLauncherHelper: PluctCorePermission02Launcher01Helper?,
    onThemeModeChange: ((String) -> Unit)?
) {
    PluctUIScreen01HomeScreen04Settings00SharedBody(
        userName = userName,
        creditBalance = creditBalance,
        debugLogCount = debugLogCount,
        errorLogCount = errorLogCount,
        onRequestCredits = onRequestCredits,
        onViewDebugLogs = onViewDebugLogs,
        onSendDiagnostic = onSendDiagnostic,
        permissionLauncherHelper = permissionLauncherHelper,
        onThemeModeChange = onThemeModeChange,
        showSheetTitle = false,
        showCloseRow = false,
        onClose = {},
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 12.dp)
    )
}
