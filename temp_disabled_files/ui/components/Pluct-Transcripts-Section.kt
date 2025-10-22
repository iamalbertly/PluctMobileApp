package app.pluct.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem

/**
 * Modern transcripts section with status-based organization
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Composable
fun PluctTranscriptsSection(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onVideoDelete: (VideoItem) -> Unit,
    onVideoRetry: (VideoItem) -> Unit,
    onVideoArchive: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group videos by status
    val pendingVideos = videos.filter { it.status == ProcessingStatus.PENDING }
    val processingVideos = videos.filter { it.status == ProcessingStatus.TRANSCRIBING || it.status == ProcessingStatus.ANALYZING }
    val completedVideos = videos.filter { it.status == ProcessingStatus.COMPLETED }
    val failedVideos = videos.filter { it.status == ProcessingStatus.FAILED }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Transcripts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "${videos.size} ${if (videos.size == 1) "transcript" else "transcripts"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        
        // Status-based sections
        if (processingVideos.isNotEmpty()) {
            PluctStatusSection(
                title = "Processing",
                icon = Icons.Default.Sync,
                videos = processingVideos,
                onVideoClick = onVideoClick,
                onVideoDelete = onVideoDelete,
                onVideoRetry = onVideoRetry,
                onVideoArchive = onVideoArchive
            )
        }
        
        if (completedVideos.isNotEmpty()) {
            PluctStatusSection(
                title = "Completed",
                icon = Icons.Default.CheckCircle,
                videos = completedVideos,
                onVideoClick = onVideoClick,
                onVideoDelete = onVideoDelete,
                onVideoRetry = onVideoRetry,
                onVideoArchive = onVideoArchive
            )
        }
        
        if (failedVideos.isNotEmpty()) {
            PluctStatusSection(
                title = "Failed",
                icon = Icons.Default.Error,
                videos = failedVideos,
                onVideoClick = onVideoClick,
                onVideoDelete = onVideoDelete,
                onVideoRetry = onVideoRetry,
                onVideoArchive = onVideoArchive
            )
        }
        
        if (pendingVideos.isNotEmpty()) {
            PluctStatusSection(
                title = "Pending",
                icon = Icons.Default.Schedule,
                videos = pendingVideos,
                onVideoClick = onVideoClick,
                onVideoDelete = onVideoDelete,
                onVideoRetry = onVideoRetry,
                onVideoArchive = onVideoArchive
            )
        }
    }
}

@Composable
private fun PluctStatusSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onVideoDelete: (VideoItem) -> Unit,
    onVideoRetry: (VideoItem) -> Unit,
    onVideoArchive: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${videos.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Video cards
            videos.forEach { video ->
                PluctEnhancedTranscriptCard(
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
