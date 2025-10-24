package app.pluct.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem
import kotlinx.coroutines.launch

/**
 * Pluct-UI-Screen-05TranscriptList - Modern transcript list with swipe controls
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */

@Composable
fun PluctTranscriptList(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onRetryVideo: (VideoItem) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("transcript_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = videos,
            key = { it.id }
        ) { video ->
            PluctTranscriptCard(
                video = video,
                onVideoClick = { onVideoClick(video) },
                onRetry = { onRetryVideo(video) },
                onDelete = { onDeleteVideo(video) },
                modifier = Modifier.testTag("transcript_card_${video.id}")
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctTranscriptCard(
    video: VideoItem,
    onVideoClick: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var isSwipeRevealed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transcript_card_wrapper")
    ) {
        // Swipe actions (behind the card)
        if (isSwipeRevealed) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .background(
                        when (video.status) {
                            ProcessingStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Retry button (for failed videos)
                if (video.status == ProcessingStatus.FAILED) {
                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier.testTag("retry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        // Main card
        Card(
            onClick = onVideoClick,
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = offsetX.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX < -100f) {
                                    // Reveal swipe actions
                                    isSwipeRevealed = true
                                    offsetX = -120f
                                } else {
                                    // Hide swipe actions
                                    isSwipeRevealed = false
                                    offsetX = 0f
                                }
                            }
                        }
                    ) { _, dragAmount ->
                        if (dragAmount.x < 0) { // Only allow left swipe
                            offsetX = (offsetX + dragAmount.x).coerceAtLeast(-150f)
                        }
                    }
                }
                .testTag("transcript_card"),
            colors = CardDefaults.cardColors(
                containerColor = when (video.status) {
                    ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.surface
                    ProcessingStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    ProcessingStatus.PROCESSING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon
                Icon(
                    imageVector = when (video.status) {
                        ProcessingStatus.COMPLETED -> Icons.Default.CheckCircle
                        ProcessingStatus.FAILED -> Icons.Default.Error
                        ProcessingStatus.PROCESSING -> Icons.Default.PlayArrow
                        ProcessingStatus.QUEUED -> Icons.Default.HourglassEmpty
                        else -> Icons.Default.QuestionMark
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("status_icon"),
                    tint = when (video.status) {
                        ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error
                        ProcessingStatus.PROCESSING -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Video title
                    Text(
                        text = if (video.title.isNotBlank() && video.title != "TikTok Video") {
                            video.title
                        } else {
                            "TikTok Video"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("video_title")
                    )
                    
                    // Creator name
                    if (video.author.isNotBlank() && video.author != "Unknown Author") {
                        Text(
                            text = "by ${video.author}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("video_author")
                        )
                    }
                    
                    // Status and progress
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = when (video.status) {
                                ProcessingStatus.QUEUED -> "Queued"
                                ProcessingStatus.PROCESSING -> "Processing"
                                ProcessingStatus.COMPLETED -> "Completed"
                                ProcessingStatus.FAILED -> "Failed"
                                else -> "Unknown"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (video.status) {
                                ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                                ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error
                                ProcessingStatus.PROCESSING -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.testTag("status_text")
                        )
                        
                        if (video.status == ProcessingStatus.PROCESSING) {
                            Text(
                                text = "${video.progress}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.testTag("progress_text")
                            )
                        }
                    }
                    
                    // Transcript preview
                    if (video.transcript != null && video.transcript.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = video.transcript,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("transcript_preview")
                        )
                    }
                }
                
                // Arrow icon
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    modifier = Modifier
                        .size(20.dp)
                        .testTag("arrow_icon"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PluctDeleteConfirmationDialog(
    isVisible: Boolean,
    videoTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Delete Transcript",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"$videoTitle\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirm,
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("Cancel")
                }
            },
            modifier = modifier.testTag("delete_confirmation_dialog")
        )
    }
}
