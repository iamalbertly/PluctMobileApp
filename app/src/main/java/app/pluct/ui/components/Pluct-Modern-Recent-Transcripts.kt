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
 * Modern Recent Transcripts section with vertical scrolling and video management
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctModernRecentTranscripts(
    videos: List<VideoItem>,
    onSeeAll: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onRemoveVideo: (VideoItem) -> Unit,
    onRetryVideo: (VideoItem) -> Unit,
    onArchiveVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<VideoItem?>(null) }
    val maxVisibleItems = 3
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("modern_recent_transcripts"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with expand/collapse functionality
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transcripts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Count badge
                Surface(
                    modifier = Modifier.testTag("count_badge"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${videos.count { it.status == ProcessingStatus.PENDING }} Pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Status summary chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusSummaryChip(
                        count = videos.count { it.status == ProcessingStatus.COMPLETED },
                        label = "Done",
                        color = Color(0xFF4CAF50)
                    )
                    StatusSummaryChip(
                        count = videos.count { it.status == ProcessingStatus.TRANSCRIBING || it.status == ProcessingStatus.ANALYZING },
                        label = "Processing",
                        color = Color(0xFF2196F3)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("expand_button")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Video list with animation
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transcripts_vertical_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(videos) { video ->
                        PluctSwipeableTranscriptCard(
                            video = video,
                            onDelete = { 
                                android.util.Log.i("PluctModernRecentTranscripts", "🎯 DELETE REQUESTED for video: ${video.title}")
                                videoToDelete = video
                                showDeleteDialog = true
                            },
                            onRetry = { 
                                android.util.Log.i("PluctModernRecentTranscripts", "🎯 RETRY REQUESTED for video: ${video.title}")
                                onRetryVideo(video)
                            },
                            onArchive = { 
                                android.util.Log.i("PluctModernRecentTranscripts", "🎯 ARCHIVE REQUESTED for video: ${video.title}")
                                onArchiveVideo(video)
                            },
                            onOpen = { 
                                android.util.Log.i("PluctModernRecentTranscripts", "🎯 OPEN REQUESTED for video: ${video.title}")
                                onVideoClick(video)
                            }
                        )
                    }
                }
            }
            
            // Collapsed view - show only first few items
            if (!expanded && videos.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transcripts_collapsed_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(videos.take(maxVisibleItems)) { video ->
                        PluctSwipeableTranscriptCard(
                            video = video,
                            onDelete = { 
                                android.util.Log.i("PluctModernRecentTranscripts", "🎯 DELETE REQUESTED for video: ${video.title}")
                                videoToDelete = video
                                showDeleteDialog = true
                            },
                            onRetry = { 
                                android.util.Log.i("PluctModernRecentTranscripts", "🎯 RETRY REQUESTED for video: ${video.title}")
                                onRetryVideo(video)
                            },
                            onArchive = { 
                                android.util.Log.i("PluctModernRecentTranscripts", "🎯 ARCHIVE REQUESTED for video: ${video.title}")
                                onArchiveVideo(video)
                            },
                            onOpen = { 
                                android.util.Log.i("PluctModernRecentTranscripts", "🎯 OPEN REQUESTED for video: ${video.title}")
                                onVideoClick(video)
                            }
                        )
                    }
                    
                    if (videos.size > maxVisibleItems) {
                        item {
                            TextButton(
                                onClick = onSeeAll,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("see_all_button")
                            ) {
                                Text("View ${videos.size - maxVisibleItems} more...")
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
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
                    android.util.Log.i("PluctModernRecentTranscripts", "🎯 DELETE CONFIRMED for video: ${video.title}")
                    onRemoveVideo(video)
                }
                showDeleteDialog = false
                videoToDelete = null
            }
        )
    }
}

@Composable
private fun StatusSummaryChip(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (count > 0) {
        Surface(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .testTag("status_summary_${label.lowercase()}"),
            color = color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernTranscriptCard(
    video: VideoItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRemoveDialog by remember { mutableStateOf(false) }
    
    Card(
        onClick = {
            android.util.Log.i("ModernTranscriptCard", "🎯 TRANSCRIPT CARD CLICKED: ${video.title}")
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag("modern_transcript_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = "Video",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Video details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title ?: "TikTok Video",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "@${video.author ?: "user"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PluctStatusPill(
                        status = video.status,
                        modifier = Modifier.testTag("status_pill")
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = formatVideoDate(video.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Remove button for failed or stuck videos
                if (video.status == ProcessingStatus.FAILED || 
                    (video.status == ProcessingStatus.PENDING && isVideoStuck(video))) {
                    IconButton(
                        onClick = { showRemoveDialog = true },
                        modifier = Modifier.testTag("remove_button")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove video",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // More options button
                IconButton(
                    onClick = { /* TODO: Show more options */ },
                    modifier = Modifier.testTag("more_options_button")
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
    
    // Remove confirmation dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Video") },
            text = { Text("Are you sure you want to remove this video from your transcripts?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove()
                        showRemoveDialog = false
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactTranscriptCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = {
            android.util.Log.i("CompactTranscriptCard", "🎯 COMPACT CARD CLICKED: ${video.title}")
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag("compact_transcript_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(getStatusColor(video.status))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title ?: "TikTok Video",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "@${video.author ?: "user"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            PluctStatusPill(
                status = video.status,
                modifier = Modifier.testTag("compact_status_pill")
            )
        }
    }
}

private fun formatVideoDate(date: Long?): String {
    if (date == null) return "Unknown"
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(Date(date))
}

private fun isVideoStuck(video: VideoItem): Boolean {
    // Consider a video stuck if it's been pending for more than 10 minutes
    val tenMinutesAgo = System.currentTimeMillis() - (10 * 60 * 1000)
    return video.createdAt != null && video.createdAt < tenMinutesAgo
}

private fun getStatusColor(status: ProcessingStatus): Color {
    return when (status) {
        ProcessingStatus.COMPLETED -> Color(0xFF4CAF50)
        ProcessingStatus.TRANSCRIBING, ProcessingStatus.ANALYZING -> Color(0xFF2196F3)
        ProcessingStatus.PENDING -> Color(0xFFFF9800)
        ProcessingStatus.FAILED -> Color(0xFFF44336)
    }
}
