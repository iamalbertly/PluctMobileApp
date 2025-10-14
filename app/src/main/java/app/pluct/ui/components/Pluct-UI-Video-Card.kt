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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pluct-UI-Video-Card - Unified video card component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Consolidates all video card related components
 */
@Composable
fun PluctUIVideoCard(
    video: VideoItem,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title ?: "Untitled Video",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (!video.author.isNullOrEmpty()) {
                        Text(
                            text = "by ${video.author}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                onMenuClick?.let { menuClick ->
                    IconButton(onClick = menuClick) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status and progress
            PluctVideoCardStatus(video = video)
            
            if (!video.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Processing tier info
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tier: ${video.processingTier.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PluctVideoCardStatus(video: VideoItem) {
    val status = video.status ?: ProcessingStatus.PENDING
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = when (status) {
                        ProcessingStatus.COMPLETED -> Color.Green
                        ProcessingStatus.FAILED -> Color.Red
                        ProcessingStatus.TRANSCRIBING -> Color.Blue
                        ProcessingStatus.ANALYZING -> Color(0xFFFF9800)
                        else -> Color.Gray
                    },
                    shape = RoundedCornerShape(4.dp)
                )
        )
        
        // Status text
        Text(
            text = when (status) {
                ProcessingStatus.COMPLETED -> "Completed"
                ProcessingStatus.FAILED -> "Failed"
                ProcessingStatus.TRANSCRIBING -> "Transcribing"
                ProcessingStatus.ANALYZING -> "Analyzing"
                else -> "Pending"
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = when (status) {
                ProcessingStatus.COMPLETED -> Color.Green
                ProcessingStatus.FAILED -> Color.Red
                ProcessingStatus.TRANSCRIBING -> Color.Blue
                ProcessingStatus.ANALYZING -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        
        // Progress indicator for active statuses
        if (status == ProcessingStatus.TRANSCRIBING || status == ProcessingStatus.ANALYZING) {
            Spacer(modifier = Modifier.weight(1f))
            LinearProgressIndicator(
                progress = 0.5f, // This would come from video progress
                modifier = Modifier.width(60.dp)
            )
        }
    }
}

@Composable
fun PluctVideoCardDeleteDialog(
    video: VideoItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Delete Video")
        },
        text = {
            Text(text = "Are you sure you want to delete this video? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

