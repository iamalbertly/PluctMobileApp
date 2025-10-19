package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.VideoItem

/**
 * Compact recent transcripts with horizontal scrolling and count badge
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctCompactRecentTranscripts(
    sections: List<VideoItem>,
    onSeeAll: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recent_transcripts")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transcripts",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                AssistChip(
                    onClick = onSeeAll,
                    label = { Text("See all") },
                    modifier = Modifier.testTag("see_all_button")
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Badge(
                    modifier = Modifier.testTag("transcript_count")
                ) { 
                    Text(sections.size.toString()) 
                }
            }
            
            // Horizontally scrollable cards
            LazyRow(
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.testTag("transcripts_list")
            ) {
                items(sections) { video ->
                    PluctCompactTranscriptCard(
                        video = video,
                        onClick = { onVideoClick(video) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluctCompactTranscriptCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = {
            android.util.Log.i("PluctCompactTranscriptCard", "🎯 TRANSCRIPT CARD CLICKED: ${video.title}")
            onClick()
        },
        modifier = modifier
            .width(200.dp)
            .testTag("transcript_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = video.title ?: "TikTok Video",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "@${video.author ?: "user"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status pill
            PluctStatusPill(
                status = video.status,
                modifier = Modifier.testTag("status_pill")
            )
        }
    }
}
