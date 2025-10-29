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
 */
@Composable
private fun EmptyVideoList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No videos yet",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { 
                contentDescription = "No videos available"
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Process your first TikTok video to see it here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { 
                contentDescription = "Instructions to add videos"
            }
        )
    }
}
