package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

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
            
            // Progress Bar (if processing)
            if (status.currentStatus == TranscriptionStatusType.PROCESSING) {
                Spacer(modifier = Modifier.height(12.dp))
                PluctTranscriptionProgressBar(
                    progress = status.progress,
                    estimatedTime = status.estimatedTime
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
            Icon(
                imageVector = when (status.currentStatus) {
                    TranscriptionStatusType.COMPLETED -> Icons.Default.CheckCircle
                    TranscriptionStatusType.FAILED -> Icons.Default.Error
                    TranscriptionStatusType.PROCESSING -> Icons.Default.Refresh
                    else -> Icons.Default.Refresh
                },
                contentDescription = "Status icon",
                tint = when (status.currentStatus) {
                    TranscriptionStatusType.COMPLETED -> MaterialTheme.colorScheme.primary
                    TranscriptionStatusType.FAILED -> MaterialTheme.colorScheme.error
                    TranscriptionStatusType.PROCESSING -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = when (status.currentStatus) {
                    TranscriptionStatusType.COMPLETED -> "Transcription Complete"
                    TranscriptionStatusType.FAILED -> "Transcription Failed"
                    TranscriptionStatusType.PROCESSING -> "Processing..."
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

@Composable
private fun PluctTranscriptionProgressBar(
    progress: Int,
    estimatedTime: Int?
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Progress: $progress%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (estimatedTime != null) {
                Text(
                    text = "Est. ${estimatedTime}s remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = progress / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .semantics {
                    contentDescription = "Transcription progress: $progress percent"
                }
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
 * Transcription status data class
 */
data class TranscriptionStatus(
    val jobId: String,
    val currentStatus: TranscriptionStatusType,
    val progress: Int = 0,
    val transcript: String = "",
    val url: String = "",
    val errorMessage: String? = null,
    val estimatedTime: Int? = null
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