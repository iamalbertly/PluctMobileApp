package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
 * Pluct-UI-Screen-VideoDetail - Video detail screen with tier-specific actions
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Displays video metadata and provides tier-specific action buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctVideoDetailScreen(
    video: VideoItem,
    onBackClick: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video Details") },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.semantics {
                            contentDescription = "Back button"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Information Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Video Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics {
                            contentDescription = "Video information section"
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "URL: ${video.url}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "Video URL: ${video.url}"
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Status: ${video.status}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "Video status: ${video.status}"
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Tier: ${video.tier.name}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "Processing tier: ${video.tier.name}"
                        }
                    )
                }
            }
            
            // Transcript/Summary Card
            if (video.transcript != null && video.transcript.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (video.tier == ProcessingTier.AI_ANALYSIS) {
                                "AI Summary & Transcript"
                            } else {
                                "Transcript"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics {
                                contentDescription = if (video.tier == ProcessingTier.AI_ANALYSIS) {
                                    "AI summary and transcript section"
                                } else {
                                    "Transcript section"
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = video.transcript,
                            fontSize = 14.sp,
                            modifier = Modifier.semantics {
                                contentDescription = "Transcript content"
                            }
                        )
                    }
                }
            }
            
            // Tier-Specific Actions
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Actions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics {
                            contentDescription = "Actions section"
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    when (video.tier) {
                        ProcessingTier.FREE -> {
                            // Free tier actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(video.transcript ?: ""))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics {
                                            contentDescription = "Copy script button"
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Script")
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        val shareIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, video.transcript ?: "")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Script"))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics {
                                            contentDescription = "Share script button"
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share Script")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Upgrade button
                            Button(
                                onClick = onUpgradeClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription = "Upgrade to full insights button"
                                    },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("✨ Upgrade to Full Insights (2 Coins)")
                            }
                        }
                        
                        ProcessingTier.AI_ANALYSIS -> {
                            // Premium tier actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        // Copy AI summary (first part of transcript)
                                        val summary = video.transcript?.take(200) ?: ""
                                        clipboardManager.setText(AnnotatedString(summary))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics {
                                            contentDescription = "Copy summary button"
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Summary")
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(video.transcript ?: ""))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics {
                                            contentDescription = "Copy transcript button"
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Transcript")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedButton(
                                onClick = {
                                    val shareText = "AI Summary:\n${video.transcript?.take(200)}\n\nFull Transcript:\n${video.transcript}"
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Insight"))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription = "Share insight button"
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Insight")
                            }
                        }
                        
                        else -> {
                            // Default actions for other tiers
                            Text(
                                text = "No specific actions available for this tier",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
