package app.pluct.ui.screens

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
import androidx.compose.ui.unit.sp
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingTier
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.ui.components.PluctHeaderWithRefreshableBalance
import app.pluct.ui.components.PluctUIComponent03CaptureCard

/**
 * Pluct-UI-Screen-01HomeScreen - Main home screen with video list and capture interface
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Provides the main interface for video transcription and management
 */
@Composable
fun PluctHomeScreen(
    creditBalance: Int,
    freeUsesRemaining: Int,
    videos: List<VideoItem>,
    isLoading: Boolean,
    errorMessage: String?,
    onTierSubmit: (String, ProcessingTier) -> Unit,
    onRetryVideo: (VideoItem) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    prefilledUrl: String? = null,
    apiService: PluctCoreAPIUnifiedService? = null,
    onRefreshCreditBalance: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            PluctHeaderWithRefreshableBalance(
                creditBalance = creditBalance,
                isCreditBalanceLoading = isLoading,
                creditBalanceError = errorMessage,
                onRefreshCreditBalance = onRefreshCreditBalance,
                onSettingsClick = { /* TODO: Implement settings */ }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Capture card
            item {
                PluctUIComponent03CaptureCard(
                    freeUsesRemaining = freeUsesRemaining,
                    creditBalance = creditBalance,
                    onTierSubmit = onTierSubmit,
                    preFilledUrl = prefilledUrl,
                    apiService = apiService
                )
            }
            
            // Video list
            if (videos.isNotEmpty()) {
                items(videos) { video ->
                    PluctVideoItemCard(
                        video = video,
                        onRetry = { onRetryVideo(video) },
                        onDelete = { onDeleteVideo(video) }
                    )
                }
            } else {
                item {
                    PluctEmptyStateMessage()
                }
            }
        }
    }
}

/**
 * Video item card component
 */
@Composable
private fun PluctVideoItemCard(
    video: VideoItem,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Video item: ${video.title}" },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Video title and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = video.status.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (video.status) {
                        app.pluct.data.entity.ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        app.pluct.data.entity.ProcessingStatus.PROCESSING -> MaterialTheme.colorScheme.secondary
                        app.pluct.data.entity.ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Video URL
            Text(
                text = video.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Transcript preview
            if (!video.transcript.isNullOrEmpty()) {
                Text(
                    text = video.transcript.take(100) + if (video.transcript.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (video.status == app.pluct.data.entity.ProcessingStatus.FAILED) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Retry")
                    }
                }
                
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

/**
 * Empty state message
 */
@Composable
private fun PluctEmptyStateMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📱",
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Your captured insights will appear here",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Paste a video URL above to get started with transcription",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}