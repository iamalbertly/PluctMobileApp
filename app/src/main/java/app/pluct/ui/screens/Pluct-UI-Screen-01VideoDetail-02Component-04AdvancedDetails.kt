package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.VideoItem

/**
 * Pluct-UI-Screen-01VideoDetail-02Component-04AdvancedDetails
 * Collapsible advanced details section for Video Details screen
 * Follows naming convention: [Project]-[Module]-[Feature]-[SubFeature]-[Sequence][Responsibility]
 * Hides technical information (URL, Status, Tier, ID) by default
 */
@Composable
fun PluctVideoDetailAdvancedSection(
    video: VideoItem,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Toggle button
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advanced Details",
                    style = MaterialTheme.typography.titleSmall
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
        }
        
        // Collapsible content
        if (expanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow(label = "URL", value = video.url)
                    DetailRow(label = "Status", value = video.status.name)
                    DetailRow(label = "Tier", value = video.tier.name)
                    DetailRow(label = "ID", value = video.id)
                    DetailRow(label = "Duration", value = "${video.duration}s")
                    if (video.timestamp > 0) {
                        DetailRow(
                            label = "Created",
                            value = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(video.timestamp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.7f)
        )
    }
}
