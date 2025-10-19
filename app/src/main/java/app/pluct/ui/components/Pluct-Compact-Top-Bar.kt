package app.pluct.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Compact top bar with merged Pluct title + credits
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctCompactTopBar(
    credits: Int,
    onRefreshCredits: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    SmallTopAppBar(
        title = { 
            Text("Pluct") 
        },
        actions = {
            AssistChip(
                onClick = onRefreshCredits,
                label = { Text("$credits credits") },
                leadingIcon = { 
                    Icon(Icons.Outlined.Diamond, contentDescription = null) 
                },
                modifier = Modifier.testTag("credits_chip")
            )
            IconButton(onClick = onSettings) { 
                Icon(Icons.Outlined.Settings, contentDescription = "Settings") 
            }
        },
        modifier = modifier.testTag("top_bar")
    )
}
