package app.pluct.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pluct-UI-Transcript-01List - Core transcript list component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Focused on displaying video transcripts in a modern, scrollable list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctTranscriptList(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onRemoveVideo: (VideoItem) -> Unit,
    onRetryVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<VideoItem?>(null) }
    
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transcript_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(videos) { video ->
            PluctTranscriptCard(
                video = video,
                onClick = { onVideoClick(video) },
                onRemove = { 
                    videoToDelete = video
                    showDeleteDialog = true
                },
                onRetry = { onRetryVideo(video) }
            )
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && videoToDelete != null) {
        PluctDeleteConfirmationDialog(
            videoTitle = videoToDelete!!.title ?: "Untitled Video",
            onDismiss = { 
                showDeleteDialog = false
                videoToDelete = null
            },
            onConfirm = {
                videoToDelete?.let { video ->
                    onRemoveVideo(video)
                }
                showDeleteDialog = false
                videoToDelete = null
            }
        )
    }
}

/**
 * Individual transcript card component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctTranscriptCard(
    video: VideoItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("transcript_card_${video.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and status
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatDate(video.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                PluctStatusPill(status = video.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Transcript preview
            if (video.transcript.isNotEmpty()) {
                Text(
                    text = video.transcript,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = getStatusMessage(video.status),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (video.status == ProcessingStatus.FAILED) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.testTag("retry_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry")
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag("remove_button")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove video",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Status pill component
 */
@Composable
fun PluctStatusPill(
    status: ProcessingStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, text) = when (status) {
        ProcessingStatus.COMPLETED -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Completed"
        )
        ProcessingStatus.PROCESSING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Processing"
        )
        ProcessingStatus.FAILED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Failed"
        )
        ProcessingStatus.PENDING -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Pending"
        )
    }
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .testTag("status_pill_${status.name.lowercase()}"),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Delete confirmation dialog
 */
@Composable
fun PluctDeleteConfirmationDialog(
    videoTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Video")
        },
        text = {
            Text("Are you sure you want to delete \"$videoTitle\"? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Format date for display
 */
private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(date)
}

/**
 * Get status message for display
 */
private fun getStatusMessage(status: ProcessingStatus): String {
    return when (status) {
        ProcessingStatus.COMPLETED -> "Transcript ready"
        ProcessingStatus.PROCESSING -> "Processing video..."
        ProcessingStatus.FAILED -> "Processing failed"
        ProcessingStatus.PENDING -> "Waiting to process..."
    }
}
