package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingStatus
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Pluct-UI-Screen-01Home - Main home screen with full functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctHomeScreen(
    videos: List<VideoItem> = emptyList(),
    onCaptureClick: () -> Unit = {},
    onVideoClick: (VideoItem) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen")
    ) {
        // Top Bar
        PluctHomeTopBar()
        
        // Main Content
        if (videos.isEmpty()) {
            PluctHomeEmptyState(onCaptureClick = onCaptureClick)
        } else {
            PluctHomeContent(
                videos = videos,
                onVideoClick = onVideoClick,
                onCaptureClick = onCaptureClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctHomeTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Pluct",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        },
        modifier = Modifier.testTag("app_title")
    )
}

@Composable
fun PluctHomeEmptyState(onCaptureClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Pluct",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No transcripts yet",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("transcripts_section")
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Process your first TikTok video to see transcripts here",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("instructions")
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        FloatingActionButton(
            onClick = onCaptureClick,
            modifier = Modifier.testTag("capture_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Capture Insight")
        }
    }
}

@Composable
fun PluctHomeContent(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onCaptureClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Recent Transcripts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("transcripts_section")
            )
        }
        
        items(videos) { video ->
            PluctVideoCard(
                video = video,
                onClick = { onVideoClick(video) }
            )
        }
    }
    
    // FAB for new capture
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        FloatingActionButton(
            onClick = onCaptureClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("capture_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Capture Insight")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctVideoCard(
    video: VideoItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "by ${video.author}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            PluctStatusPill(status = video.status)
            
            if (video.transcript != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = video.transcript,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctStatusPill(status: ProcessingStatus) {
    val (text, color) = when (status) {
        ProcessingStatus.QUEUED -> "Queued" to MaterialTheme.colorScheme.primary
        ProcessingStatus.PROCESSING -> "Processing" to MaterialTheme.colorScheme.secondary
        ProcessingStatus.COMPLETED -> "Completed" to MaterialTheme.colorScheme.tertiary
        ProcessingStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
        ProcessingStatus.UNKNOWN -> "Unknown" to MaterialTheme.colorScheme.outline
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.testTag("status_pill")
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
