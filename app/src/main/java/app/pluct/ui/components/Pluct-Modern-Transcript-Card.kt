package app.pluct.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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

/**
 * Modern, sleek transcript card with swipe functionality
 * Refactored to use focused components following naming convention
 */
@Composable
fun PluctModernTranscriptCard(
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
        
        // Main card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header with status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PluctAnimatedStatusIndicator(video.status)
                        Text(
                            text = (video.title ?: "Untitled").take(30) + if ((video.title?.length ?: 0) > 30) "..." else "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    PluctStatusChip(video.status)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Creator and tier info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "@${video.author ?: "unknown"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // TODO: Add tier information when available
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description
                Text(
                    text = (video.description ?: "").take(100) + if ((video.description?.length ?: 0) > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Footer with timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently", // TODO: Add proper timestamp when available
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (video.status == ProcessingStatus.COMPLETED) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        PluctDeleteConfirmationDialog(
            videoTitle = video.title ?: "Untitled",
            onDismiss = { showDeleteDialog = false },
            onConfirm = onDelete
        )
    }
}