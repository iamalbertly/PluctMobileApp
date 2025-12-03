package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem

/**
 * Pluct-UI-Component-04OfflineIndicator-01Badge - Offline availability badge
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Sequence][Responsibility]
 * Shows when transcripts are cached locally and available offline
 */
@Composable
fun PluctOfflineBadge(video: VideoItem) {
    if (video.transcript != null && video.status == ProcessingStatus.COMPLETED) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Cached offline",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Saved Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
