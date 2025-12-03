package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.VideoItem
import app.pluct.ui.components.PluctOfflineBadge

/**
 * Pluct-UI-Screen-01VideoDetail-02Component-02Header
 * Simplified header for Video Details screen showing only essential information
 * Follows naming convention: [Project]-[Module]-[Feature]-[SubFeature]-[Sequence][Responsibility]
 * Displays: Title, Author, Offline Badge (no technical details)
 */
@Composable
fun PluctVideoDetailHeader(
    video: VideoItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Title (large, bold, prominent)
        Text(
            text = video.title.ifBlank { "Untitled Video" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Author (secondary, if available)
        if (video.author.isNotBlank()) {
            Text(
                text = "by ${video.author}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Offline Badge (shows if transcript is cached)
        PluctOfflineBadge(video = video)
    }
}
