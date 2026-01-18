package app.pluct.ui.screens

import androidx.compose.runtime.Composable
import app.pluct.core.permission.PluctCorePermission02Launcher01Helper
import app.pluct.data.preferences.PluctUserPreferences
import android.content.Context
import androidx.compose.ui.platform.LocalContext

/**
 * Pluct-UI-Screen-01MainActivity-08Dialogs
 * Follows naming convention: [Project]-[UI]-[Screen]-[MainActivity]-[Dialogs]
 * 5 scope layers: Project, UI, Screen, MainActivity, Dialogs
 * Handles welcome and permission dialogs extracted from MainActivity
 */
object PluctUIScreen01MainActivity08Dialogs {
    
    @Composable
    fun WelcomeDialog(
        showWelcomeDialog: Boolean,
        onDismiss: () -> Unit,
        onGetStarted: () -> Unit
    ) {
        val context = LocalContext.current
        if (showWelcomeDialog) {
            app.pluct.ui.components.PluctWelcomeDialog(
                onDismiss = onDismiss,
                onGetStarted = {
                    PluctUserPreferences.markUserAsReturning(context)
                    onDismiss()
                    onGetStarted()
                }
            )
        }
    }
    
    @Composable
    fun PermissionOnboardingDialog(
        showPermissionOnboarding: Boolean,
        onDismiss: () -> Unit,
        onComplete: () -> Unit,
        permissionLauncherHelper: PluctCorePermission02Launcher01Helper?
    ) {
        if (showPermissionOnboarding) {
            app.pluct.ui.components.PluctUIComponent06Permission01Onboarding01Dialog(
                onDismiss = onDismiss,
                onComplete = onComplete,
                permissionLauncherHelper = permissionLauncherHelper
            )
        }
    }
}
