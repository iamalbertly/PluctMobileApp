package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.QueueReason
import app.pluct.data.entity.VideoItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pluct-UI-Component-04Queue-01ItemCard
 * Card component for displaying a queued video item
 * Follows naming convention: [Project]-[Module]-[Component]-[Feature]-[Sequence][Responsibility]
 */
@Composable
fun PluctQueueItemCard(
    video: VideoItem,
    position: Int,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with position and reason
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#$position",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    QueueReasonBadge(reason = video.queueReason)
                }
                
                if (video.queuedAt != null) {
                    Text(
                        text = formatQueuedTime(video.queuedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            // URL (truncated)
            Text(
                text = video.url,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry Now")
                }
                
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
private fun QueueReasonBadge(reason: QueueReason?) {
    val (text, icon) = when (reason) {
        QueueReason.NO_INTERNET -> "No Internet" to Icons.Default.WifiOff
        QueueReason.INSUFFICIENT_CREDITS -> "Waiting for Credits" to Icons.Default.AccountBalanceWallet
        QueueReason.RATE_LIMITED -> "Rate Limited" to Icons.Default.Schedule
        QueueReason.SERVICE_UNAVAILABLE -> "Service Unavailable" to Icons.Default.Error
        null -> "Queued" to Icons.Default.Schedule
    }
    
    AssistChip(
        onClick = {},
        label = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(text, style = MaterialTheme.typography.labelSmall)
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

private fun formatQueuedTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60000
    val hours = minutes / 60
    
    return when {
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hour(s) ago"
        else -> {
            val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}



