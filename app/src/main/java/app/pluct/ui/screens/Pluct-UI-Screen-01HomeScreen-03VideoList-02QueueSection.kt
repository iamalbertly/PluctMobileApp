package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.VideoItem
import app.pluct.ui.components.PluctQueueItemCard

/**
 * Pluct-UI-Screen-01HomeScreen-03VideoList-02QueueSection
 * Queue section component for displaying queued videos
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 */
@Composable
fun PluctQueueSection(
    queuedVideos: List<VideoItem>,
    onRetryVideo: (VideoItem) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (queuedVideos.isEmpty()) {
        return
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "📋 ${queuedVideos.size} video(s) queued",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            // Queue items
            queuedVideos.forEachIndexed { index, video ->
                PluctQueueItemCard(
                    video = video,
                    position = index + 1,
                    onRetry = { onRetryVideo(video) },
                    onRemove = { onDeleteVideo(video) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}






