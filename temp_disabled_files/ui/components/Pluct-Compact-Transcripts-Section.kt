package app.pluct.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
 * Compact transcripts section with horizontal scrolling for space efficiency
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Composable
fun PluctCompactTranscriptsSection(
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transcripts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${videos.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            // Horizontal scrolling sections for each status
            if (processingVideos.isNotEmpty()) {
                PluctCompactStatusRow(
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
                PluctCompactStatusRow(
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
                PluctCompactStatusRow(
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
                PluctCompactStatusRow(
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
}

@Composable
private fun PluctCompactStatusRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onVideoDelete: (VideoItem) -> Unit,
    onVideoRetry: (VideoItem) -> Unit,
    onVideoArchive: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${videos.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Horizontal scrolling video cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(videos) { video ->
                PluctCompactTranscriptCard(
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

@Composable
private fun PluctCompactTranscriptCard(
    video: VideoItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = video.title?.take(20) ?: "Untitled",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (video.status) {
                        ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ProcessingStatus.TRANSCRIBING, ProcessingStatus.ANALYZING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    }
                ) {
                    Text(
                        text = when (video.status) {
                            ProcessingStatus.COMPLETED -> "✓"
                            ProcessingStatus.FAILED -> "✗"
                            ProcessingStatus.TRANSCRIBING -> "⏳"
                            ProcessingStatus.ANALYZING -> "🔍"
                            else -> "⏸"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            // Creator info
            Text(
                text = "@${video.author ?: "unknown"}.take(15)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Actions row
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp)
                    )
                }
                
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
                
                IconButton(
                    onClick = onArchive,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Archive",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
