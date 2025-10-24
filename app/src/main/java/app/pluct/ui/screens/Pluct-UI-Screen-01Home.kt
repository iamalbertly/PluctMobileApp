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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingStatus
import app.pluct.ui.components.PluctComprehensiveHeader
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
    onVideoClick: (VideoItem) -> Unit = {},
    onRetryVideo: (VideoItem) -> Unit = {},
    onDeleteVideo: (VideoItem) -> Unit = {},
    creditBalance: Int = 10,
    isCreditBalanceLoading: Boolean = false,
    creditBalanceError: String? = null,
    onRefreshCreditBalance: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen")
    ) {
        // Enhanced Header with Credit Balance and Settings
        PluctComprehensiveHeader(
            creditBalance = creditBalance,
            isCreditBalanceLoading = isCreditBalanceLoading,
            creditBalanceError = creditBalanceError,
            onRefreshCreditBalance = onRefreshCreditBalance,
            onSettingsClick = onSettingsClick
        )
        
        // Main Content
        if (videos.isEmpty()) {
            PluctHomeEmptyState(onCaptureClick = onCaptureClick)
        } else {
            PluctHomeContentWithTranscripts(
                videos = videos,
                onVideoClick = onVideoClick,
                onRetryVideo = onRetryVideo,
                onDeleteVideo = onDeleteVideo,
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Enhanced welcome section with gradient background
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("welcome_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🚀 Welcome to Pluct",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Transform TikTok videos into insights",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("transcripts_section")
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Get instant transcripts and summaries from any TikTok video",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.testTag("instructions")
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Enhanced FAB with better styling
        FloatingActionButton(
            onClick = onCaptureClick,
            modifier = Modifier
                .testTag("capture_fab")
                .size(72.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                Icons.Default.Add, 
                contentDescription = "Capture Insight",
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Tap to start",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PluctHomeContentWithTranscripts(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onRetryVideo: (VideoItem) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    onCaptureClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf<VideoItem?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Text(
                text = "Recent Transcripts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("transcripts_section")
            )
            
            // Transcript list with swipe controls
            PluctTranscriptList(
                videos = videos,
                onVideoClick = onVideoClick,
                onRetryVideo = onRetryVideo,
                onDeleteVideo = { video ->
                    showDeleteDialog = video
                }
            )
        }
        
        // FAB for new capture
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
    
    // Delete confirmation dialog
    PluctDeleteConfirmationDialog(
        isVisible = showDeleteDialog != null,
        videoTitle = showDeleteDialog?.title ?: "",
        onConfirm = {
            showDeleteDialog?.let { video ->
                onDeleteVideo(video)
                showDeleteDialog = null
            }
        },
        onDismiss = { showDeleteDialog = null }
    )
}

@Composable
fun PluctHomeContent(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onCaptureClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
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
            .testTag("video_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with title and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "by ${video.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                PluctStatusPill(status = video.status)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress indicator for processing videos
            if (video.status == ProcessingStatus.PROCESSING) {
                LinearProgressIndicator(
                    progress = video.progress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Processing... ${video.progress}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Transcript preview
            if (video.transcript != null) {
                Text(
                    text = video.transcript,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else if (video.status == ProcessingStatus.QUEUED) {
                Text(
                    text = "Queued for processing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
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
