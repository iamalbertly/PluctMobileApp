package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Pluct-UI-Header-02Settings - Settings navigation component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */

@Composable
fun PluctSettingsButton(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onSettingsClick,
        modifier = modifier
            .clip(CircleShape)
            .testTag("settings_button"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PluctSettingsIconButton(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onSettingsClick,
        modifier = modifier.testTag("settings_icon_button")
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
