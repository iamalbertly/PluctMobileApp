package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.VideoItem

/**
 * Pluct-UI-Component-04VideoList-02Container - Video list container
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single source of truth for video list container
 */
@Composable
fun PluctVideoListContainer(
    videos: List<VideoItem>,
    onVideoItemClick: (VideoItem) -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { 
                contentDescription = "Video list with ${videos.size} items"
            }
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Videos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { 
                    contentDescription = "Your Videos section"
                }
            )
            
            TextButton(
                onClick = onRefreshClick,
                modifier = Modifier.semantics { 
                    contentDescription = "Refresh video list"
                }
            ) {
                Text("Refresh")
            }
        }
        
        // Video List
        if (videos.isEmpty()) {
            EmptyVideoList()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(videos) { video ->
                    PluctVideoListItem(
                        video = video,
                        onClick = { onVideoItemClick(video) }
                    )
                }
            }
        }
    }
}

/**
 * Empty state when no videos are available
 * UX IMPROVEMENT: Duolingo-style encouraging empty state with friendly tone
 */
@Composable
private fun EmptyVideoList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .semantics {
                contentDescription = "Empty video list - ready to start"
                testTag = "empty_video_list"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ready to get started!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics {
                contentDescription = "Ready to get started"
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your first transcript awaits...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Share a TikTok video or paste a link above to see the magic happen!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics {
                contentDescription = "Share a TikTok video to get started"
            }
        )
    }
}
