package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingTier
import android.content.Intent

/**
 * Pluct-UI-Screen-VideoDetail-03Content - Content component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single source of truth for content functionality
 */
@Composable
fun PluctVideoDetailContent(
    video: VideoItem,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Video Metadata Card
        PluctVideoMetadataCard(video = video)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Transcript Card
        PluctTranscriptCard(
            video = video,
            clipboardManager = clipboardManager
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action Buttons
        PluctVideoActionButtons(
            video = video,
            onUpgradeClick = onUpgradeClick,
            context = context
        )
    }
}

@Composable
private fun PluctVideoMetadataCard(video: VideoItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Video Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "URL: ${video.url}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Status: ${video.status}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Tier: ${video.tier}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (video.id.isNotEmpty()) {
                Text(
                    text = "ID: ${video.id}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PluctTranscriptCard(
    video: VideoItem,
    clipboardManager: ClipboardManager
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transcript",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (video.transcript != null) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(video.transcript))
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Copy transcript"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = video.transcript ?: "No transcript available",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PluctVideoActionButtons(
    video: VideoItem,
    onUpgradeClick: () -> Unit,
    context: android.content.Context
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (video.tier) {
            ProcessingTier.EXTRACT_SCRIPT -> {
                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Upgrade to AI Insights"
                        }
                ) {
                    Text("Upgrade to AI Insights")
                }
            }
            ProcessingTier.GENERATE_INSIGHTS -> {
                Button(
                    onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, video.transcript ?: "")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share transcript"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Share transcript"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Transcript")
                }
            }
            ProcessingTier.FREE,
            ProcessingTier.STANDARD,
            ProcessingTier.PREMIUM,
            ProcessingTier.AI_ANALYSIS,
            ProcessingTier.DEEP_ANALYSIS,
            ProcessingTier.PREMIUM_INSIGHTS -> {
                // Default action for other tiers
                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Upgrade"
                        }
                ) {
                    Text("Upgrade")
                }
            }
        }
    }
}
