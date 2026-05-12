package app.pluct.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag

enum class PluctUIMainShellTab {
    HOME,
    LIBRARY,
    SETTINGS
}

@Composable
fun PluctUIMainShellBottomBar(
    selected: PluctUIMainShellTab,
    onSelect: (PluctUIMainShellTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selected == PluctUIMainShellTab.HOME,
            onClick = { onSelect(PluctUIMainShellTab.HOME) },
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.semantics { testTag = "nav_home" }
                )
            },
            label = { Text("Home") },
            modifier = Modifier.semantics { contentDescription = "Home tab" }
        )
        NavigationBarItem(
            selected = selected == PluctUIMainShellTab.LIBRARY,
            onClick = { onSelect(PluctUIMainShellTab.LIBRARY) },
            icon = {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.semantics { testTag = "nav_library" }
                )
            },
            label = { Text("Library") },
            modifier = Modifier.semantics { contentDescription = "Library tab" }
        )
        NavigationBarItem(
            selected = selected == PluctUIMainShellTab.SETTINGS,
            onClick = { onSelect(PluctUIMainShellTab.SETTINGS) },
            icon = {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.semantics { testTag = "nav_settings" }
                )
            },
            label = { Text("Settings") },
            modifier = Modifier.semantics { contentDescription = "Settings tab" }
        )
    }
}
