package app.pluct.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.pluct.data.entity.VideoItem

/**
 * Pluct-UI-Component-04VideoList - Main video list orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single source of truth for video list functionality
 */
@Composable
fun PluctVideoListComponent(
    videos: List<VideoItem>,
    onVideoItemClick: (VideoItem) -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PluctVideoListContainer(
        videos = videos,
        onVideoItemClick = onVideoItemClick,
        onRefreshClick = onRefreshClick,
        modifier = modifier
    )
}