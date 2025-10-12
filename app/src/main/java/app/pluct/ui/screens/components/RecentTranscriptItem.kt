package app.pluct.ui.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.pluct.data.entity.ProcessingStatus
import app.pluct.viewmodel.HomeViewModel

/**
 * Individual recent transcript item
 */
@Composable
fun RecentTranscriptItem(
    video: app.pluct.data.entity.VideoItem,
    navController: NavController,
    viewModel: HomeViewModel
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            Icon(
                imageVector = when (video.status) {
                    ProcessingStatus.PENDING -> Icons.Default.Schedule
                    ProcessingStatus.TRANSCRIBING -> Icons.Default.AutoFixHigh
                    ProcessingStatus.ANALYZING -> Icons.Default.Psychology
                    ProcessingStatus.COMPLETED -> Icons.Default.CheckCircle
                    ProcessingStatus.FAILED -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when (video.status) {
                    ProcessingStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ProcessingStatus.TRANSCRIBING, ProcessingStatus.ANALYZING -> MaterialTheme.colorScheme.primary
                    ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    ProcessingStatus.FAILED -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.sourceUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = when (video.status) {
                        ProcessingStatus.PENDING -> "Pending processing..."
                        ProcessingStatus.TRANSCRIBING -> "Transcribing audio..."
                        ProcessingStatus.ANALYZING -> "Analyzing content..."
                        ProcessingStatus.COMPLETED -> video.title ?: "Ready to view"
                        ProcessingStatus.FAILED -> video.failureReason ?: "Processing failed"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            // Action Buttons
            Row {
                IconButton(
                    onClick = {
                        if (video.status == ProcessingStatus.COMPLETED) {
                            navController.navigate("ingest?url=${video.sourceUrl}")
                        }
                    },
                    enabled = video.status == ProcessingStatus.COMPLETED
                ) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = "Open",
                        modifier = Modifier.size(20.dp),
                        tint = if (video.status == ProcessingStatus.COMPLETED) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                
                IconButton(
                    onClick = { showDeleteDialog = true }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("Delete Transcript")
            },
            text = {
                Text(
                    "Are you sure you want to delete this transcript?\n\n" +
                    "This will permanently remove:\n" +
                    "• The video entry\n" +
                    "• All associated transcripts\n" +
                    "• Any generated summaries\n" +
                    "• All related artifacts\n\n" +
                    "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVideo(video.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
