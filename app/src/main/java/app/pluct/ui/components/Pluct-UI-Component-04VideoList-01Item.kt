package app.pluct.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.ProcessingStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pluct-UI-Component-04VideoList-01Item - Single video item component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single source of truth for video item display
 */
@Composable
fun PluctVideoListItem(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .semantics { 
                contentDescription = "Video item: ${video.title}"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(getStatusColor(video.status))
                    .semantics { 
                        contentDescription = "Status: ${video.status}"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getStatusIcon(video.status),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { 
                        contentDescription = "Video title: ${video.title}"
                    }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatDate(video.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { 
                        contentDescription = "Created: ${formatDate(video.createdAt)}"
                    }
                )
                
                if (video.status == ProcessingStatus.COMPLETED && !video.transcript.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Transcript available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { 
                            contentDescription = "Transcript available"
                        }
                    )
                }
            }
            
            // Action Button
            IconButton(
                onClick = onClick,
                modifier = Modifier.semantics { 
                    contentDescription = "View video details"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View details"
                )
            }
        }
    }
}

/**
 * Get status color based on processing status
 */
private fun getStatusColor(status: ProcessingStatus): Color {
    return when (status) {
        ProcessingStatus.COMPLETED -> Color(0xFF4CAF50)
        ProcessingStatus.PROCESSING -> Color(0xFF2196F3)
        ProcessingStatus.FAILED -> Color(0xFFF44336)
        ProcessingStatus.QUEUED -> Color(0xFFFF9800)
        ProcessingStatus.UNKNOWN -> Color(0xFF9E9E9E)
    }
}

/**
 * Get status icon based on processing status
 */
private fun getStatusIcon(status: ProcessingStatus): ImageVector {
    return when (status) {
        ProcessingStatus.COMPLETED -> Icons.Default.CheckCircle
        ProcessingStatus.PROCESSING -> Icons.Default.Refresh
        ProcessingStatus.FAILED -> Icons.Default.Error
        ProcessingStatus.QUEUED -> Icons.Default.Schedule
        ProcessingStatus.UNKNOWN -> Icons.Default.Help
    }
}

/**
 * Format timestamp to readable date
 */
private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
