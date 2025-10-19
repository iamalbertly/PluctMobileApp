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
 * Enhanced video list with modern design and better organization
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Composable
fun PluctEnhancedVideoList(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onVideoDelete: (VideoItem) -> Unit,
    onVideoRetry: (VideoItem) -> Unit,
    onVideoArchive: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) {
        // Enhanced empty state
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(40.dp)
            ) {
                // Animated icon
                Text(
                    text = "🎬",
                    style = MaterialTheme.typography.displayLarge
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No transcripts yet",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Share a TikTok video to get started with instant summaries",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Feature highlights
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "✨ What you can do:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "• Get instant video summaries",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Extract key insights and quotes",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Save time on long-form content",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    } else {
        // Enhanced video list with sections
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Recent transcripts header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transcripts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${videos.size} ${if (videos.size == 1) "transcript" else "transcripts"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Video items
            videos.forEach { video ->
                PluctEnhancedTranscriptCard(
                    video = video,
                    onClick = { onVideoClick(video) },
                    onDelete = { onVideoDelete(video) },
                    onRetry = { onVideoRetry(video) },
                    onArchive = { onVideoArchive(video) }
                )
            }
            
            // Bottom spacing
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
