package app.pluct.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.pluct.core.permission.PluctCorePermission02Launcher01Helper

/**
 * Pluct-UI-Screen-01HomeScreen-04Settings-01Dialog - Opaque settings bottom sheet (replaces AlertDialog)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctUIScreen01HomeScreen04Settings01Dialog(
    userName: String,
    creditBalance: Int,
    debugLogCount: Int,
    errorLogCount: Int = 0,
    onDismiss: () -> Unit,
    onRequestCredits: (String) -> Unit,
    onViewDebugLogs: () -> Unit,
    onSendDiagnostic: () -> Unit = {},
    permissionLauncherHelper: PluctCorePermission02Launcher01Helper? = null,
    onThemeModeChange: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { }
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
            showSheetTitle = true,
            showCloseRow = true,
            onClose = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
