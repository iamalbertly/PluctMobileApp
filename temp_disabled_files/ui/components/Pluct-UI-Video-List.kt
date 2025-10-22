package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.VideoItem

/**
 * Pluct-UI-Video-List - Video list display component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */

@Composable
fun PluctVideoList(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onVideoDelete: (VideoItem) -> Unit,
    onVideoRetry: (VideoItem) -> Unit,
    onVideoArchive: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) {
        // Empty state
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🎬",
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = "No transcripts yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Share a TikTok video to get started",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            videos.forEach { video ->
                PluctModernTranscriptCard(
                    video = video,
                    onClick = { onVideoClick(video) },
                    onDelete = { onVideoDelete(video) },
                    onRetry = { onVideoRetry(video) },
                    onArchive = { onVideoArchive(video) }
                )
            }
        }
    }
}
