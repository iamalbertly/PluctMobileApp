package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.VideoItem

/**
 * Pluct-UI-Screen-01VideoDetail-02Component-03Transcript
 * Transcript display component for Video Details screen
 * Follows naming convention: [Project]-[Module]-[Feature]-[SubFeature]-[Sequence][Responsibility]
 * Shows full transcript text in readable format
 */
@Composable
fun PluctVideoDetailTranscript(
    video: VideoItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transcript",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // UX FIX #3: Display confidence score if available
                video.confidence?.let { confidence ->
                    Surface(
                        color = when {
                            confidence >= 0.9 -> MaterialTheme.colorScheme.primaryContainer
                            confidence >= 0.7 -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "${(confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            when {
                video.transcript.isNullOrBlank() -> {
                    Text(
                        text = "No transcript yet. Try again in a moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f
                    )
                }
                else -> {
                    Text(
                        text = video.transcript,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f
                    )
                }
            }
        }
    }
}
