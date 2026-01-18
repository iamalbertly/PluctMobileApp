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
 * 
 * @deprecated All transcripts are saved offline by default (stored in local Room database).
 * This badge was redundant information that appeared on every completed video, creating
 * visual clutter without adding value. Can be re-implemented if cloud sync feature is added.
 */
@Deprecated(
    message = "All transcripts are saved offline by default. Badge removed to reduce UI clutter.",
    level = DeprecationLevel.HIDDEN
)
@Composable
fun PluctOfflineBadge(video: VideoItem) {
    // UX FIX: Removed - all videos are saved offline by default
    // No need to show redundant information
    // Component kept for API compatibility but renders nothing
}
