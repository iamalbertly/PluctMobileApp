package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.pluct.ui.navigation.Screen

/**
 * Compact top bar with merged Pluct title + credits + navigation
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctCompactTopBar(
    credits: Int,
    onRefreshCredits: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    SmallTopAppBar(
        title = { 
            TextButton(
                onClick = {
                    android.util.Log.i("PluctCompactTopBar", "🎯 PLUCT TITLE CLICKED - Navigating to Home")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                },
                modifier = Modifier.testTag("pluct_home_button")
            ) {
                Text(
                    text = "Pluct",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        actions = {
            // Credits with refresh button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "♦ $credits credits",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onRefreshCredits,
                    modifier = Modifier.testTag("refresh_credits")
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Refresh credits",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            // Settings button
            IconButton(
                onClick = {
                    android.util.Log.i("PluctCompactTopBar", "🎯 SETTINGS ICON CLICKED - Navigating to Settings")
                    navController.navigate(Screen.Settings.route)
                }
            ) { 
                Icon(
                    Icons.Outlined.Settings, 
                    contentDescription = "Settings",
                    modifier = Modifier.testTag("settings_button")
                ) 
            }
        },
        modifier = modifier.testTag("top_bar")
    )
}
