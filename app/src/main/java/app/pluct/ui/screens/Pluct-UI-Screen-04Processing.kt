package app.pluct.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem

/**
 * Pluct-UI-Screen-04Processing - Processing status and progress display
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@Composable
fun PluctProcessingStatusCard(
    video: VideoItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("processing_status_card"),
        colors = CardDefaults.cardColors(
            containerColor = when (video.status) {
                ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                ProcessingStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Status header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (video.status) {
                            ProcessingStatus.COMPLETED -> Icons.Default.CheckCircle
                            ProcessingStatus.FAILED -> Icons.Default.Error
                            ProcessingStatus.PROCESSING -> Icons.Default.PlayArrow
                            else -> Icons.Default.HourglassEmpty
                        },
                        contentDescription = null,
                        tint = when (video.status) {
                            ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error
                            ProcessingStatus.PROCESSING -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = when (video.status) {
                            ProcessingStatus.QUEUED -> "Queued"
                            ProcessingStatus.PROCESSING -> "Processing"
                            ProcessingStatus.COMPLETED -> "Completed"
                            ProcessingStatus.FAILED -> "Failed"
                            ProcessingStatus.UNKNOWN -> "Unknown"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when (video.status) {
                            ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error
                            ProcessingStatus.PROCESSING -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.testTag("processing_status_text")
                    )
                }
                
                // Progress percentage
                if (video.status == ProcessingStatus.PROCESSING) {
                    Text(
                        text = "${video.progress}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.testTag("progress_percentage")
                    )
                }
            }
            
            // Progress bar
            if (video.status == ProcessingStatus.PROCESSING || video.status == ProcessingStatus.QUEUED) {
                Spacer(modifier = Modifier.height(12.dp))
                
                LinearProgressIndicator(
                    progress = video.progress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .testTag("progress_bar"),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                        Text(
                            text = when (video.status) {
                                ProcessingStatus.QUEUED -> "Waiting in queue... This may take a few moments"
                                ProcessingStatus.PROCESSING -> when {
                                    video.progress < 25 -> "Connecting to transcription service..."
                                    video.progress < 50 -> "Downloading video content..."
                                    video.progress < 75 -> "Processing audio..."
                                    video.progress < 90 -> "Generating transcript..."
                                    else -> "Finalizing transcript..."
                                }
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("progress_description")
                        )
            }
            
            // Transcript preview
            if (video.transcript != null && video.transcript.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Transcript Preview:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("transcript_label")
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = video.transcript,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transcript_preview")
                )
            }
            
            // Error message
            if (video.status == ProcessingStatus.FAILED && video.failureReason != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("error_message"),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = video.failureReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PluctProcessingOverlay(
    isVisible: Boolean,
    video: VideoItem?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible && video != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .testTag("processing_overlay")
        ) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .testTag("processing_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Processing icon
                    Icon(
                        imageVector = when (video.status) {
                            ProcessingStatus.COMPLETED -> Icons.Default.CheckCircle
                            ProcessingStatus.FAILED -> Icons.Default.Error
                            ProcessingStatus.PROCESSING -> Icons.Default.PlayArrow
                            else -> Icons.Default.HourglassEmpty
                        },
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("processing_icon"),
                        tint = when (video.status) {
                            ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error
                            ProcessingStatus.PROCESSING -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Video title and creator
                    if (video.title.isNotBlank() && video.title != "TikTok Video") {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("video_title")
                        )
                        
                        if (video.author.isNotBlank() && video.author != "Unknown Author") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "by $video.author",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("video_author")
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Status text
                    Text(
                        text = when (video.status) {
                            ProcessingStatus.QUEUED -> "Queued for Processing"
                            ProcessingStatus.PROCESSING -> "Processing Video"
                            ProcessingStatus.COMPLETED -> "Processing Complete"
                            ProcessingStatus.FAILED -> "Processing Failed"
                            ProcessingStatus.UNKNOWN -> "Unknown Status"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("processing_title")
                    )
                    
                    // Progress bar
                    if (video.status == ProcessingStatus.PROCESSING || video.status == ProcessingStatus.QUEUED) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LinearProgressIndicator(
                            progress = video.progress / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .testTag("overlay_progress_bar"),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "${video.progress}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.testTag("overlay_progress_text")
                        )
                    }
                    
                    // Transcript preview
                    if (video.transcript != null && video.transcript.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Transcript Generated:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("transcript_generated_label")
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = video.transcript,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("overlay_transcript")
                        )
                    }
                    
                    // Dismiss button
                    if (video.status == ProcessingStatus.COMPLETED || video.status == ProcessingStatus.FAILED) {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dismiss_button")
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}
