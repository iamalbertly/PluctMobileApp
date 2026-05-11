package app.pluct.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem
import app.pluct.services.TranscriptionDebugInfo

/**
 * Pluct-UI-Screen-02LibraryTab-01Screen — full list of saved jobs (Realism & Simplicity: one library).
 */
@Composable
fun PluctUIScreen02LibraryTab01Screen(
    paddingValues: PaddingValues,
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onRetryVideo: (VideoItem) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    snackbarHostState: SnackbarHostState,
    debugInfo: TranscriptionDebugInfo?
) {
    val unique = remember(videos) {
        videos
            .groupBy { it.url.trim().lowercase() }
            .mapNotNull { (_, group) -> group.maxByOrNull { it.timestamp } }
            .sortedByDescending { it.timestamp }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 10.dp)
            .semantics {
                testTag = "library_screen_list"
                contentDescription = "Library"
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "All saved videos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { testTag = "library_screen_subtitle" }
            )
        }
        if (unique.isEmpty()) {
            item {
                Text(
                    text = "Nothing here yet. Transcribe from Home.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { testTag = "library_empty_state" }
                )
            }
        } else {
            items(unique, key = { it.id }) { video ->
                PluctVideoItemCard(
                    video = video,
                    onClick = { onVideoClick(video) },
                    onRetry = { onRetryVideo(video) },
                    onDelete = { onDeleteVideo(video) },
                    debugInfo = if (video.status == ProcessingStatus.PROCESSING &&
                        debugInfo?.url == video.url
                    ) {
                        debugInfo
                    } else {
                        null
                    },
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}
