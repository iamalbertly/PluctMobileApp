package app.pluct.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import app.pluct.ui.models.TranscriptionPhase

/**
 * Pluct-UI-Component-01TranscriptionStatus - Real-time transcription status display
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Provides real-time updates for transcription progress and results
 */
@Composable
fun PluctTranscriptionStatusCard(
    status: TranscriptionStatus,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Transcription status card showing ${status.currentStatus}"
            },
        colors = CardDefaults.cardColors(
            containerColor = when (status.currentStatus) {
                TranscriptionStatusType.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                TranscriptionStatusType.FAILED -> MaterialTheme.colorScheme.errorContainer
                TranscriptionStatusType.PROCESSING -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Status Header
            PluctTranscriptionStatusHeader(
                status = status,
                onRetry = onRetry,
                onDismiss = onDismiss
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status Content
            PluctTranscriptionStatusContent(status = status)
            
            // Multi-Phase Progress Indicator (if processing)
            if (status.currentStatus == TranscriptionStatusType.PROCESSING) {
                Spacer(modifier = Modifier.height(12.dp))
                PluctTranscriptionMultiPhaseIndicator(
                    phase = status.phase,
                    progress = status.progress
                )
                Spacer(modifier = Modifier.height(8.dp))
                PluctTranscriptionProgressBar(
                    progress = status.progress,
                    estimatedTime = status.estimatedTime,
                    currentOperation = status.currentOperation
                )
            }
            
            // Error Details (if failed)
            if (status.currentStatus == TranscriptionStatusType.FAILED && status.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                PluctTranscriptionErrorDetails(errorMessage = status.errorMessage)
            }
        }
    }
}

@Composable
private fun PluctTranscriptionStatusHeader(
    status: TranscriptionStatus,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated icon for processing state
            if (status.currentStatus == TranscriptionStatusType.PROCESSING) {
                PluctAnimatedProcessingIcon()
            } else {
                Icon(
                    imageVector = when (status.currentStatus) {
                        TranscriptionStatusType.COMPLETED -> Icons.Default.CheckCircle
                        TranscriptionStatusType.FAILED -> Icons.Default.Error
                        else -> Icons.Default.Refresh
                    },
                    contentDescription = "Status icon",
                    tint = when (status.currentStatus) {
                        TranscriptionStatusType.COMPLETED -> MaterialTheme.colorScheme.primary
                        TranscriptionStatusType.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = when (status.currentStatus) {
                    TranscriptionStatusType.COMPLETED -> "Transcription Complete"
                    TranscriptionStatusType.FAILED -> "Transcription Failed"
                    TranscriptionStatusType.PROCESSING -> status.currentOperation.takeIf { it.isNotEmpty() } ?: "Processing..."
                    else -> "Unknown Status"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    contentDescription = "Transcription status: ${status.currentStatus}"
                }
            )
        }
        
        Row {
            if (status.currentStatus == TranscriptionStatusType.FAILED) {
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier.semantics {
                        contentDescription = "Retry transcription"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry"
                    )
                }
            }
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Dismiss status card"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Dismiss"
                )
            }
        }
    }
}

@Composable
private fun PluctTranscriptionStatusContent(status: TranscriptionStatus) {
    Column {
        Text(
            text = "Job ID: ${status.jobId}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics {
                contentDescription = "Job ID: ${status.jobId}"
            }
        )
        
        if (status.url.isNotEmpty()) {
            Text(
                text = "URL: ${status.url}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics {
                    contentDescription = "Video URL: ${status.url}"
                }
            )
        }
        
        if (status.transcript.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Transcript Preview:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = status.transcript.take(200) + if (status.transcript.length > 200) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Animated processing icon with rotation
 */
@Composable
private fun PluctAnimatedProcessingIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "processing_icon")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = "Processing",
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.rotate(rotation)
    )
}

/**
 * Multi-phase progress indicator showing current phase
 */
@Composable
private fun PluctTranscriptionMultiPhaseIndicator(
    phase: TranscriptionPhase,
    progress: Int
) {
    val phases = listOf(
        TranscriptionPhase.PREPARING,
        TranscriptionPhase.DOWNLOADING,
        TranscriptionPhase.EXTRACTING,
        TranscriptionPhase.TRANSCRIBING,
        TranscriptionPhase.FINALIZING
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        phases.forEachIndexed { index, p ->
            val isActive = phases.indexOf(phase) >= index
            val isCurrent = phase == p
            
            // Phase indicator dot
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 12.dp else 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) {
                    // Pulsing animation for current phase
                    val infiniteTransition = rememberInfiniteTransition(label = "phase_pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(scale)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (isActive) 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PluctTranscriptionProgressBar(
    progress: Int,
    estimatedTime: Int?,
    currentOperation: String = ""
) {
    // Animated progress value for smooth transitions
    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "progress_animation"
    )
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (currentOperation.isNotEmpty()) {
                    Text(
                        text = currentOperation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Progress: $progress%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (estimatedTime != null) {
                Text(
                    text = "Est. ${estimatedTime}s remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .semantics {
                    contentDescription = "Transcription progress: $progress percent"
                },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PluctTranscriptionErrorDetails(errorMessage: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Error Details:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * Transcription status data class with enhanced fields
 */
data class TranscriptionStatus(
    val jobId: String,
    val currentStatus: TranscriptionStatusType,
    val progress: Int = 0,
    val transcript: String = "",
    val url: String = "",
    val errorMessage: String? = null,
    val estimatedTime: Int? = null,
    val currentOperation: String = "",
    val phase: TranscriptionPhase = TranscriptionPhase.PREPARING
)

/**
 * Transcription status types
 */
enum class TranscriptionStatusType {
    PROCESSING,
    COMPLETED,
    FAILED,
    UNKNOWN
}