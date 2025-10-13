package app.pluct.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pluct-Status-Tracking-Component - Comprehensive status tracking for pending work
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Shows real-time status of TTTranscribe integration and background processing
 */
@Composable
fun PluctStatusTrackingComponent(
    statusItems: List<StatusItem>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Processing Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                TextButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (statusItems.isEmpty()) {
                Text(
                    text = "No active processing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(statusItems) { item ->
                        StatusItemCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusItemCard(item: StatusItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (item.status) {
                ProcessingStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                ProcessingStatus.TRANSCRIBING -> MaterialTheme.colorScheme.primaryContainer
                ProcessingStatus.ANALYZING -> MaterialTheme.colorScheme.secondaryContainer
                ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                ProcessingStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                StatusIndicator(status = item.status)
            }
            
            if (item.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (item.progress > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = item.progress / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = when (item.status) {
                        ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    }
                )
            }
            
            if (item.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (item.timestamp != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Updated: ${formatTimestamp(item.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusIndicator(status: ProcessingStatus) {
    val (color, text) = when (status) {
        ProcessingStatus.PENDING -> MaterialTheme.colorScheme.outline to "Pending"
        ProcessingStatus.TRANSCRIBING -> MaterialTheme.colorScheme.primary to "Transcribing"
        ProcessingStatus.ANALYZING -> MaterialTheme.colorScheme.secondary to "Analyzing"
        ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary to "Completed"
        ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error to "Failed"
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

data class StatusItem(
    val id: String,
    val title: String,
    val description: String = "",
    val details: String = "",
    val status: ProcessingStatus,
    val progress: Int = 0,
    val timestamp: Date? = null,
    val tier: ProcessingTier? = null
)

private fun formatTimestamp(timestamp: Date): String {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(timestamp)
}
