package app.pluct.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp

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
    val barTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    NavigationBar(
        modifier = modifier.semantics {
            contentDescription = "Main navigation"
            testTag = "main_bottom_navigation"
        },
        containerColor = barTint,
        tonalElevation = 0.dp,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
        )
        val homeSelected = selected == PluctUIMainShellTab.HOME
        NavigationBarItem(
            selected = homeSelected,
            onClick = { onSelect(PluctUIMainShellTab.HOME) },
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Home navigation icon",
                    modifier = Modifier.semantics { testTag = "nav_home" }
                )
            },
            label = { Text("Home", maxLines = 1) },
            modifier = Modifier.semantics {
                contentDescription = if (homeSelected) {
                    "Home tab selected, Home navigation icon"
                } else {
                    "Home tab, Home navigation icon"
                }
                this.selected = homeSelected
            },
            colors = colors
        )
        val libSelected = selected == PluctUIMainShellTab.LIBRARY
        NavigationBarItem(
            selected = libSelected,
            onClick = { onSelect(PluctUIMainShellTab.LIBRARY) },
            icon = {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = "Library navigation icon",
                    modifier = Modifier.semantics { testTag = "nav_library" }
                )
            },
            label = { Text("Library", maxLines = 1) },
            modifier = Modifier.semantics {
                contentDescription = if (libSelected) {
                    "Library tab selected, Library navigation icon"
                } else {
                    "Library tab, Library navigation icon"
                }
                this.selected = libSelected
            },
            colors = colors
        )
        val setSelected = selected == PluctUIMainShellTab.SETTINGS
        NavigationBarItem(
            selected = setSelected,
            onClick = { onSelect(PluctUIMainShellTab.SETTINGS) },
            icon = {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings navigation icon",
                    modifier = Modifier.semantics { testTag = "nav_settings" }
                )
            },
            label = { Text("Settings", maxLines = 1) },
            modifier = Modifier.semantics {
                contentDescription = if (setSelected) {
                    "Settings tab selected, Settings navigation icon"
                } else {
                    "Settings tab, Settings navigation icon"
                }
                this.selected = setSelected
            },
            colors = colors
        )
    }
}
