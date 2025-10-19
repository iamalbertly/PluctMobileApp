package app.pluct.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.VideoItem
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.draw.rotate

/**
 * Enhanced modern transcript card with swipe functionality and rich metadata display
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Composable
fun PluctEnhancedTranscriptCard(
    video: VideoItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }
    val maxDragOffset = 200f
    
    // Animate drag offset
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "drag_offset"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(x = animatedOffset.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (dragOffset > maxDragOffset / 2) {
                            dragOffset = maxDragOffset
                        } else {
                            dragOffset = 0f
                        }
                    }
                ) { _, dragAmount ->
                    dragOffset = (dragOffset + dragAmount.x).coerceIn(0f, maxDragOffset)
                }
            }
    ) {
        // Swipe actions background
        if (dragOffset > 0) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PluctSwipeAction(
                    icon = Icons.Default.Delete,
                    color = MaterialTheme.colorScheme.error,
                    onClick = { showDeleteDialog = true }
                )
                PluctSwipeAction(
                    icon = Icons.Default.Refresh,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onRetry
                )
                PluctSwipeAction(
                    icon = Icons.Default.Archive,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onArchive
                )
            }
        }
        
        // Main card with enhanced design
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                // Header with status and tier
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PluctEnhancedStatusIndicator(video.status)
                        Column {
                            Text(
                                text = video.title?.take(40) ?: "Untitled Video",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "@${video.author ?: "unknown_creator"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    PluctEnhancedStatusChip(video.status, video.processingTier)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description with better formatting
                if (!video.description.isNullOrBlank()) {
                    Text(
                        text = video.description.take(120) + if (video.description.length > 120) "..." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Progress indicator for processing states
                if (video.status == ProcessingStatus.TRANSCRIBING || video.status == ProcessingStatus.ANALYZING) {
                    PluctProgressIndicator(video.status)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Footer with timestamp and actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(video.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (video.status == ProcessingStatus.COMPLETED) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "View Transcript",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (video.status == ProcessingStatus.FAILED) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        // Three dots menu for additional actions
                        IconButton(
                            onClick = { /* TODO: Show menu */ },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        PluctDeleteConfirmationDialog(
            videoTitle = video.title ?: "Untitled Video",
            onDismiss = { showDeleteDialog = false },
            onConfirm = onDelete
        )
    }
}

/**
 * Enhanced status indicator with better animations
 */
@Composable
private fun PluctEnhancedStatusIndicator(status: ProcessingStatus) {
    val color = when (status) {
        ProcessingStatus.PENDING -> MaterialTheme.colorScheme.outline
        ProcessingStatus.TRANSCRIBING -> MaterialTheme.colorScheme.primary
        ProcessingStatus.ANALYZING -> MaterialTheme.colorScheme.secondary
        ProcessingStatus.COMPLETED -> Color(0xFF4CAF50)
        ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error
    }
    
    val icon = when (status) {
        ProcessingStatus.PENDING -> Icons.Default.Schedule
        ProcessingStatus.TRANSCRIBING -> Icons.Default.Transcribe
        ProcessingStatus.ANALYZING -> Icons.Default.Analytics
        ProcessingStatus.COMPLETED -> Icons.Default.CheckCircle
        ProcessingStatus.FAILED -> Icons.Default.Error
    }
    
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = status.name,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Enhanced status chip with tier information
 */
@Composable
private fun PluctEnhancedStatusChip(status: ProcessingStatus, tier: ProcessingTier) {
    val (text, color) = when (status) {
        ProcessingStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.outline
        ProcessingStatus.TRANSCRIBING -> "Transcribing" to MaterialTheme.colorScheme.primary
        ProcessingStatus.ANALYZING -> "Analyzing" to MaterialTheme.colorScheme.secondary
        ProcessingStatus.COMPLETED -> "Ready" to Color(0xFF4CAF50)
        ProcessingStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
    }
    
    val tierIcon = when (tier) {
        ProcessingTier.QUICK_SCAN -> "⚡️"
        ProcessingTier.AI_ANALYSIS -> "🤖"
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = tierIcon,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Progress indicator for processing states
 */
@Composable
private fun PluctProgressIndicator(status: ProcessingStatus) {
    val progress = when (status) {
        ProcessingStatus.TRANSCRIBING -> 0.6f
        ProcessingStatus.ANALYZING -> 0.8f
        else -> 1f
    }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (status) {
                    ProcessingStatus.TRANSCRIBING -> "Transcribing video..."
                    ProcessingStatus.ANALYZING -> "Analyzing content..."
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Format timestamp for display
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> {
            val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}
