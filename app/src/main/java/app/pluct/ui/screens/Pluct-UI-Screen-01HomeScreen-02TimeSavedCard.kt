package app.pluct.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Time Saved",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "$timeSavedMinutes minutes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "by reading ${completedVideos.size} transcript${if (completedVideos.size > 1) "s" else ""} instead of watching",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
