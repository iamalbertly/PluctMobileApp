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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem
import app.pluct.services.TranscriptionDebugInfo
import app.pluct.ui.components.PluctProcessingIndicator
import kotlinx.coroutines.launch

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
    debugInfo: TranscriptionDebugInfo? = null,
    snackbarHostState: SnackbarHostState? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
                        text = getVideoDisplayTitle(video),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.semantics {
                        contentDescription = "View Details"
                        testTag = "view_details_button"
                    }
                ) {
                    Text("View Details")
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
                // Add success indicator
                Row(
                    modifier = Modifier.padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Transcript ready",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.transcript.take(150) + if (video.transcript.length > 150) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f
                )
                
                // One-tap copy button
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            video.transcript?.let { transcript ->
                                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clipData = ClipData.newPlainText("Pluct Transcript", transcript)
                                clipboardManager.setPrimaryClip(clipData)
                                // Show confirmation via snackbar
                                snackbarHostState?.let { snackbar ->
                                    scope.launch {
                                        snackbar.showSnackbar(
                                            message = "Transcript copied to clipboard",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Copy transcript to clipboard"
                            testTag = "copy_transcript_button"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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

/**
 * UX FIX: Improved video title fallback logic
 * Provides meaningful titles even when metadata is unavailable
 * Priority: title > author > URL extraction > generic fallback
 */
@Composable
private fun getVideoDisplayTitle(video: VideoItem): String {
    return when {
        video.title.isNotBlank() -> video.title
        video.author.isNotBlank() -> "Video by @${video.author}"
        video.url.contains("@") -> {
            // Extract creator handle from TikTok URL using regex
            val handleMatch = Regex("@([^/?]+)").find(video.url)
            if (handleMatch != null) {
                val handle = handleMatch.groupValues[1]
                "Video by @$handle"
            } else {
                "TikTok Video"
            }
        }
        else -> "TikTok Video"
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
                    // UX FIX: Use improved title fallback instead of "Untitled Video"
                    val displayTitle = when {
                        video.title.isNotBlank() -> video.title
                        video.author.isNotBlank() -> "Video by @${video.author}"
                        video.url.contains("@") -> {
                            val handleMatch = Regex("@([^/?]+)").find(video.url)
                            if (handleMatch != null) "Video by @${handleMatch.groupValues[1]}" else "TikTok Video"
                        }
                        else -> "TikTok Video"
                    }
                    appendLine("Transcript for $displayTitle")
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

/**
 * UX IMPROVEMENT: Minimal empty state - avoids duplicate text with capture card
 * Shows only what capture card doesn't: demo button + visual hint
 */
@Composable
fun PluctEmptyStateMessage(onDemoLinkClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Empty state - no transcripts yet"
                testTag = "empty_state_message"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Simple visual indicator
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Single line - no duplicate with capture card
            Text(
                text = "Your transcripts appear here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDemoLinkClick,
                modifier = Modifier.semantics {
                    contentDescription = "Try demo"
                    testTag = "empty_state_demo_button"
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Try Demo")
            }
        }
    }
}
