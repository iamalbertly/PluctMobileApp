package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.pluct.data.entity.VideoItem

/**
 * Pluct-UI-Screen-VideoDetail-01Main - Main video detail screen orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single source of truth for video detail screen functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctVideoDetailScreen(
    video: VideoItem,
    onBackClick: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    Scaffold(
        topBar = {
            PluctVideoDetailTopBar(
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        PluctVideoDetailContent(
            video = video,
            onUpgradeClick = onUpgradeClick,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
