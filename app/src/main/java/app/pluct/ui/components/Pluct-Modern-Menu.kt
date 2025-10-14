package app.pluct.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Modern 3-dot menu for Pluct app
 * Replaces the redundant header bar with a sleek floating menu
 */
@Composable
fun PluctModernMenu(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
    ) {
        // Floating action button for menu
        FloatingActionButton(
            onClick = { showMenu = !showMenu },
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.TopEnd),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                imageVector = if (showMenu) Icons.Default.Close else Icons.Default.MoreVert,
                contentDescription = if (showMenu) "Close menu" else "Open menu",
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Dropdown menu
        if (showMenu) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MenuItem(
                        icon = Icons.Default.Search,
                        text = "Search",
                        onClick = {
                            onSearchClick()
                            showMenu = false
                        }
                    )
                    
                    MenuItem(
                        icon = Icons.Default.Settings,
                        text = "Settings",
                        onClick = {
                            onSettingsClick()
                            showMenu = false
                        }
                    )
                    
                    MenuItem(
                        icon = Icons.Default.Help,
                        text = "Help",
                        onClick = {
                            onHelpClick()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
