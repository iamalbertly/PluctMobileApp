package app.pluct.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingStatus
import kotlin.math.abs

/**
 * Swipeable transcript card with reveal actions
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctSwipeableTranscriptCard(
    video: VideoItem,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onArchive: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var isRevealed by remember { mutableStateOf(false) }
    
    val maxOffset = -120.dp.value
    val animationSpec = tween<Float>(durationMillis = 300)
    
    val animatedOffset by animateFloatAsState(
        targetValue = if (isRevealed) maxOffset else 0f,
        animationSpec = animationSpec,
        finishedListener = { isRevealed = offsetX < maxOffset / 2 }
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .testTag("swipeable_transcript_card_${video.id}")
    ) {
        // Background actions (revealed when swiped)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Retry action (for failed/error states)
            if (video.status == ProcessingStatus.FAILED) {
                ActionButton(
                    icon = Icons.Default.Refresh,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onRetry,
                    testTag = "retry_action_${video.id}"
                )
            }
            
            // Archive action
            ActionButton(
                icon = Icons.Default.Archive,
                color = MaterialTheme.colorScheme.secondary,
                onClick = onArchive,
                testTag = "archive_action_${video.id}"
            )
            
            // Delete action
            ActionButton(
                icon = Icons.Default.Delete,
                color = MaterialTheme.colorScheme.error,
                onClick = onDelete,
                testTag = "delete_action_${video.id}"
            )
        }
        
        // Main card content
        Card(
            onClick = onOpen,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = animatedOffset
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            isRevealed = offsetX < maxOffset / 2
                            offsetX = if (isRevealed) maxOffset else 0f
                        }
                    ) { _, dragAmount ->
                        offsetX = (offsetX + dragAmount.x).coerceIn(maxOffset, 0f)
                    }
                }
                .testTag("transcript_card_${video.id}")
                .semantics {
                    contentDescription = "Transcript card for ${video.title ?: "TikTok Video"}. Status: ${video.status}. Tap to open."
                },
            colors = CardDefaults.cardColors(
                containerColor = when (video.status) {
                    ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.surface
                    ProcessingStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video thumbnail/icon with proper spacing
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = "Video",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Content with proper text rendering
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = video.title ?: "TikTok Video",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("video_title_${video.id}")
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = "@${video.author ?: "creator"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.testTag("video_creator_${video.id}")
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Status badge with proper spacing
                StatusBadge(
                    status = video.status,
                    modifier = Modifier.testTag("status_badge_${video.id}")
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StatusBadge(
    status: ProcessingStatus,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (status) {
        ProcessingStatus.COMPLETED -> "Done" to Color(0xFF4CAF50)
        ProcessingStatus.FAILED -> "Error" to Color(0xFFF44336)
        ProcessingStatus.TRANSCRIBING, ProcessingStatus.ANALYZING -> "Processing" to Color(0xFF2196F3)
        else -> "Pending" to Color(0xFFFF9800)
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f),
        shadowElevation = 1.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
