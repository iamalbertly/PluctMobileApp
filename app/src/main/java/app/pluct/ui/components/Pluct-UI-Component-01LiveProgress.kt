package app.pluct.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus
import kotlinx.coroutines.delay

/**
 * Pluct-UI-Component-01LiveProgress - Real-time transcription progress component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */

@Composable
fun PluctLiveProgressIndicator(
    status: ProcessingStatus,
    progress: Int,
    currentStep: String,
    estimatedTime: Int? = null,
    modifier: Modifier = Modifier
) {
    var isAnimating by remember { mutableStateOf(true) }
    
    LaunchedEffect(status) {
        isAnimating = status == ProcessingStatus.PROCESSING
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_progress_card"),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                ProcessingStatus.PROCESSING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)
                ProcessingStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated progress circle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .testTag("progress_circle"),
                contentAlignment = Alignment.Center
            ) {
                // Background circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                )
                
                // Progress circle
                CircularProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier.size(80.dp),
                    color = when (status) {
                        ProcessingStatus.PROCESSING -> MaterialTheme.colorScheme.primary
                        ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                        ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    strokeWidth = 6.dp,
                    trackColor = Color.Transparent
                )
                
                // Progress percentage
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (status) {
                        ProcessingStatus.PROCESSING -> MaterialTheme.colorScheme.primary
                        ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                        ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.testTag("progress_percentage")
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status text with animation
            AnimatedContent(
                targetState = status,
                transitionSpec = {
                    slideInVertically { it } + fadeIn() togetherWith
                    slideOutVertically { -it } + fadeOut()
                },
                modifier = Modifier.testTag("status_text")
            ) { currentStatus ->
                Text(
                    text = when (currentStatus) {
                        ProcessingStatus.QUEUED -> "Queued for Processing"
                        ProcessingStatus.PROCESSING -> "Processing Video"
                        ProcessingStatus.COMPLETED -> "Transcription Complete"
                        ProcessingStatus.FAILED -> "Processing Failed"
                        ProcessingStatus.UNKNOWN -> "Unknown Status"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = when (currentStatus) {
                        ProcessingStatus.PROCESSING -> MaterialTheme.colorScheme.primary
                        ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                        ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Current step with animation
            if (status == ProcessingStatus.PROCESSING) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                    },
                    modifier = Modifier.testTag("current_step")
                ) { step ->
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Estimated time
                estimatedTime?.let { time ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "~${time}s remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("estimated_time")
                        )
                    }
                }
            }
            
            // Animated dots for processing
            if (status == ProcessingStatus.PROCESSING && isAnimating) {
                Spacer(modifier = Modifier.height(12.dp))
                PluctAnimatedDots(
                    modifier = Modifier.testTag("animated_dots")
                )
            }
        }
    }
}

@Composable
fun PluctAnimatedDots(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = dot1Alpha)
                )
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = dot2Alpha)
                )
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = dot3Alpha)
                )
        )
    }
}

@Composable
fun PluctLiveStatusBar(
    status: ProcessingStatus,
    progress: Int,
    currentStep: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                when (status) {
                    ProcessingStatus.PROCESSING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)
                    ProcessingStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
            .testTag("live_status_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status icon
        Icon(
            imageVector = when (status) {
                ProcessingStatus.PROCESSING -> Icons.Default.PlayArrow
                ProcessingStatus.COMPLETED -> Icons.Default.CheckCircle
                ProcessingStatus.FAILED -> Icons.Default.Error
                else -> Icons.Default.HourglassEmpty
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = when (status) {
                ProcessingStatus.PROCESSING -> MaterialTheme.colorScheme.primary
                ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        
        // Progress bar
        if (status == ProcessingStatus.PROCESSING) {
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .testTag("status_progress_bar"),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // Current step
        Text(
            text = currentStep,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("status_step")
        )
        
        // Progress percentage
        Text(
            text = "$progress%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = when (status) {
                ProcessingStatus.PROCESSING -> MaterialTheme.colorScheme.primary
                ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.testTag("status_percentage")
        )
    }
}
