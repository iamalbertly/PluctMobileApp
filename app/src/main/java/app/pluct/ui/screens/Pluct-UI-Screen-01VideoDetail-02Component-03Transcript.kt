package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Transcript",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = video.transcript ?: "No transcript available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5f
            )
        }
    }
}
