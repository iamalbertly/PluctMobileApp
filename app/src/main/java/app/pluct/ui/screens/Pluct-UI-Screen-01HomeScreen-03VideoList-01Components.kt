package app.pluct.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem
import app.pluct.services.TranscriptionDebugInfo
import app.pluct.ui.components.PluctOfflineBadge
import app.pluct.ui.components.PluctProcessingIndicator

/**
 * Pluct-UI-Screen-01HomeScreen-03VideoList-01Components
 * Video list building blocks for Home screen.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PluctVideoItemCard(
    video: VideoItem,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    debugInfo: TranscriptionDebugInfo? = null
) {
    val context = LocalContext.current
    var showQuickActions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (video.status == ProcessingStatus.COMPLETED) {
                        showQuickActions = true
                    }
                }
            )
            .semantics { contentDescription = "Video item: ${video.title}" },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    StatusBadge(video)
                    Text(
                        text = if (video.title.isNotBlank()) video.title else "Untitled Video",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    PluctOfflineBadge(video = video)
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (video.status == ProcessingStatus.PROCESSING) {
                Spacer(modifier = Modifier.height(12.dp))
                PluctProcessingIndicator(
                    currentOperation = debugInfo?.getCurrentOperationDescription() ?: "Processing...",
                    debugInfo = debugInfo,
                    currentJobId = debugInfo?.jobId
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = video.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!video.transcript.isNullOrEmpty()) {
                Text(
                    text = video.transcript.take(100) + if (video.transcript.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            if (video.status == ProcessingStatus.FAILED) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Retry")
                }
            }
        }

        QuickActionsMenu(
            visible = showQuickActions,
            video = video,
            onDismiss = { showQuickActions = false },
            onDelete = {
                onDelete()
                showQuickActions = false
            },
            context = context
        )
    }
}

@Composable
private fun StatusBadge(video: VideoItem) {
    Surface(
        color = when (video.status) {
            ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
            ProcessingStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Text(
            text = video.status.name,
            style = MaterialTheme.typography.labelSmall,
            color = when (video.status) {
                ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.onPrimaryContainer
                ProcessingStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun QuickActionsMenu(
    visible: Boolean,
    video: VideoItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    context: Context
) {
    DropdownMenu(
        expanded = visible,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Copy Transcript") },
            leadingIcon = { Icon(Icons.Default.ContentCopy, "Copy") },
            onClick = {
                video.transcript?.let { transcript ->
                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText("Pluct Transcript", transcript)
                    clipboardManager.setPrimaryClip(clipData)
                }
                onDismiss()
            }
        )

        DropdownMenuItem(
            text = { Text("Share") },
            leadingIcon = { Icon(Icons.Default.Share, "Share") },
            onClick = {
                val shareText = buildString {
                    appendLine("Transcript for ${video.title.ifBlank { "Untitled Video" }}")
                    if (video.author.isNotBlank()) appendLine("By ${video.author}")
                    appendLine("URL: ${video.url}")
                    appendLine()
                    appendLine("Transcript:")
                    appendLine(video.transcript ?: "No transcript available")
                    appendLine()
                    appendLine("Generated by Pluct")
                }
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share transcript"))
                onDismiss()
            }
        )

        DropdownMenuItem(
            text = { Text("Delete") },
            leadingIcon = { Icon(Icons.Default.Delete, "Delete") },
            onClick = {
                onDelete()
                onDismiss()
            }
        )
    }
}

@Composable
fun PluctEmptyStateMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No transcripts yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = "Paste a TikTok link to get your first transcript.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
