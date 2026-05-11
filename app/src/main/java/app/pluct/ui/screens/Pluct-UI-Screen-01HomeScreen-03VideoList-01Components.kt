package app.pluct.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pluct.core.error.PluctCoreError08OutcomeFamily
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem
import app.pluct.services.TranscriptionDebugInfo
import app.pluct.ui.components.PluctProcessingIndicator
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

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
    val title = getVideoDisplayTitle(video)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { if (video.status == ProcessingStatus.COMPLETED) showQuickActions = true }
            )
            .semantics { contentDescription = "Video item: $title" },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoThumb(video)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    StatusBadge(video)
                }
                CompactVideoMeta(video)
                if (video.status == ProcessingStatus.PROCESSING) {
                    PluctProcessingIndicator(
                        currentOperation = debugInfo?.getCurrentOperationDescription() ?: "${video.progress}% working",
                        debugInfo = debugInfo,
                        currentJobId = debugInfo?.jobId
                    )
                }
                if (!video.transcript.isNullOrBlank()) {
                    Text(
                        text = video.transcript.take(76) + if (video.transcript.length > 76) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            CardActions(video, onRetry, onDelete, snackbarHostState, context, scope)
        }
        QuickActionsMenu(showQuickActions, video, { showQuickActions = false }, onDelete, context)
    }
}

@Composable
private fun VideoThumb(video: VideoItem) {
    val color = statusColor(video.status)
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (video.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        }
        Icon(statusIcon(video.status), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun CompactVideoMeta(video: VideoItem) {
    val author = getVideoDetailDisplayAuthor(video)
    val meta = buildList {
        if (author.isNotBlank()) add(author)
        if (video.duration > 0) add("${video.duration}s")
        video.confidence?.let { add("${(it * 100).toInt()}%") }
    }.joinToString("  ")
    if (meta.isNotBlank()) {
        Text(
            text = meta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CardActions(
    video: VideoItem,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    snackbarHostState: SnackbarHostState?,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (video.status == ProcessingStatus.FAILED) {
            IconButton(onClick = onRetry, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = MaterialTheme.colorScheme.error)
            }
        } else if (!video.transcript.isNullOrBlank()) {
            IconButton(
                onClick = { copyTranscript(video, context, snackbarHostState, scope) },
                modifier = Modifier
                    .size(38.dp)
                    .semantics {
                        contentDescription = "Copy transcript"
                        testTag = "copy_transcript_button"
                    }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusBadge(video: VideoItem) {
    Surface(color = statusColor(video.status).copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(statusIcon(video.status), contentDescription = null, tint = statusColor(video.status), modifier = Modifier.size(13.dp))
            Text(statusLabel(video), style = MaterialTheme.typography.labelSmall, color = statusColor(video.status), fontWeight = FontWeight.Bold)
        }
    }
}

private fun copyTranscript(video: VideoItem, context: Context, snackbarHostState: SnackbarHostState?, scope: kotlinx.coroutines.CoroutineScope) {
    val transcript = video.transcript ?: return
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText("Pluct Transcript", transcript))
    snackbarHostState?.let { scope.launch { it.showSnackbar("Copied", duration = SnackbarDuration.Short) } }
}

private fun statusLabel(video: VideoItem): String = when (video.status) {
    ProcessingStatus.COMPLETED -> "OK"
    ProcessingStatus.PROCESSING -> "${video.progress.coerceIn(0, 99)}%"
    ProcessingStatus.FAILED -> PluctCoreError08OutcomeFamily.shortLabel(
        PluctCoreError08OutcomeFamily.fromVideoItem(video)
    )
    ProcessingStatus.QUEUED -> "..."
    ProcessingStatus.UNKNOWN -> "?"
}

private fun statusIcon(status: ProcessingStatus): ImageVector = when (status) {
    ProcessingStatus.COMPLETED -> Icons.Default.CheckCircle
    ProcessingStatus.PROCESSING -> Icons.Default.Refresh
    ProcessingStatus.FAILED -> Icons.Default.ErrorOutline
    ProcessingStatus.QUEUED -> Icons.Default.Schedule
    ProcessingStatus.UNKNOWN -> Icons.Default.Help
}

private fun statusColor(status: ProcessingStatus): Color = when (status) {
    ProcessingStatus.COMPLETED -> Color(0xFF0B8F6A)
    ProcessingStatus.PROCESSING -> Color(0xFF5E35D6)
    ProcessingStatus.FAILED -> Color(0xFFC62828)
    ProcessingStatus.QUEUED -> Color(0xFFE46A1A)
    ProcessingStatus.UNKNOWN -> Color(0xFF68707A)
}

private fun getVideoDisplayTitle(video: VideoItem): String =
    getVideoDetailDisplayTitle(video).ifBlank { "TikTok" }

@Composable
private fun QuickActionsMenu(visible: Boolean, video: VideoItem, onDismiss: () -> Unit, onDelete: () -> Unit, context: Context) {
    DropdownMenu(expanded = visible, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Copy") },
            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
            onClick = {
                video.transcript?.let {
                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("Pluct Transcript", it))
                }
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Share") },
            leadingIcon = { Icon(Icons.Default.Share, null) },
            onClick = { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, shareText(video)); type = "text/plain"
            }, "Share")); onDismiss() }
        )
        DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { onDelete(); onDismiss() })
    }
}

private fun shareText(video: VideoItem): String = buildString {
    appendLine("Transcript for ${getVideoDisplayTitle(video)}")
    if (video.author.isNotBlank()) appendLine("By ${video.author}")
    appendLine("URL: ${video.url}")
    appendLine()
    appendLine(video.transcript ?: "No transcript available")
}

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Paste -> Text", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(onClick = onDemoLinkClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Demo")
            }
        }
    }
}
