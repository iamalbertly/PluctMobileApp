package app.pluct.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem

/**
 * Pluct-UI-Screen-01HomeScreen-02TimeSavedCard - Time Saved metric card.
 */
@Composable
fun PluctTimeSavedCard(videos: List<VideoItem>) {
    val completedVideos = videos.filter { it.status == ProcessingStatus.COMPLETED }

    if (completedVideos.isEmpty()) return

    val totalDurationSeconds = completedVideos.sumOf { it.duration }
    val readingTimeSeconds = completedVideos.sumOf { (it.transcript?.length ?: 0) / 5 }
    val timeSavedMinutes = maxOf(0, (totalDurationSeconds - readingTimeSeconds) / 60)

    if (timeSavedMinutes <= 0) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Saved",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "$timeSavedMinutes min",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${completedVideos.size} transcript${if (completedVideos.size > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
