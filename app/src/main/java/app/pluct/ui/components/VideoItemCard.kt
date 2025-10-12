package app.pluct.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem
import app.pluct.ui.navigation.Screen
import app.pluct.ui.utils.DateFormatter
import app.pluct.ui.utils.UrlUtils
import app.pluct.viewmodel.HomeViewModel

@Composable
fun VideoItemCard(video: VideoItem, navController: NavController, viewModel: HomeViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Only allow navigation if processing is completed
                if (video.status == ProcessingStatus.COMPLETED) {
                    navController.navigate("${Screen.Ingest.route}?url=${video.sourceUrl}")
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (video.status) {
                ProcessingStatus.PENDING, ProcessingStatus.TRANSCRIBING, ProcessingStatus.ANALYZING -> 
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ProcessingStatus.FAILED -> 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ProcessingStatus.COMPLETED -> 
                    MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Title
                Text(
                    text = video.title ?: UrlUtils.extractHostFromUrl(video.sourceUrl),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                // Status indicator
                ProcessingStatusIndicator(video.status)
                
                // Menu button
                IconButton(
                    onClick = { showMenu = true }
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                
                // Dropdown menu
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Author (if available)
            if (!video.author.isNullOrEmpty()) {
                Text(
                    text = "by ${video.author}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            // Description (if available)
            if (!video.description.isNullOrEmpty()) {
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            // URL and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = UrlUtils.extractHostFromUrl(video.sourceUrl),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = DateFormatter.formatRelativeDate(video.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Video") },
            text = { Text("Are you sure you want to delete this video? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVideo(video.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProcessingStatusIndicator(status: ProcessingStatus) {
    val (icon, text, color) = when (status) {
        ProcessingStatus.PENDING -> Triple(Icons.Default.Schedule, "Pending", MaterialTheme.colorScheme.onSurfaceVariant)
        ProcessingStatus.TRANSCRIBING -> Triple(Icons.Default.AutoFixHigh, "Transcribing", MaterialTheme.colorScheme.primary)
        ProcessingStatus.ANALYZING -> Triple(Icons.Default.Psychology, "Analyzing", MaterialTheme.colorScheme.primary)
        ProcessingStatus.COMPLETED -> Triple(Icons.Default.CheckCircle, "Completed", MaterialTheme.colorScheme.primary)
        ProcessingStatus.FAILED -> Triple(Icons.Default.Error, "Failed", MaterialTheme.colorScheme.error)
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}
