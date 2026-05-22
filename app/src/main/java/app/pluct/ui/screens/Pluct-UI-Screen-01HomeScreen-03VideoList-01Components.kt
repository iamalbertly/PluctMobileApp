package app.pluct.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pluct.notification.PluctNotificationCancelReceiver
import app.pluct.core.error.PluctCoreError08OutcomeFamily
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem
import app.pluct.services.TranscriptionDebugInfo
import app.pluct.ui.components.PluctProcessingIndicator
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PluctVideoItemCard(
    video: VideoItem,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    debugInfo: TranscriptionDebugInfo? = null,
    snackbarHostState: SnackbarHostState? = null,
    onRequestCredits: (() -> Unit)? = null,
    showProcessingDebugPanel: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var overflowOpen by remember { mutableStateOf(false) }
    val title = getVideoDetailDisplayTitle(video)
    val family = remember(video.id, video.status, video.failureReason) {
        PluctCoreError08OutcomeFamily.fromVideoItem(video)
    }
    val handle = remember(video.author, video.url) {
        displayTikTokHandle(video)
    }
    val timeLine = remember(video.timestamp, video.queuedAt, video.status, handle) {
        "${handle.ifBlank { "TikTok" }} · ${compactEventTime(video)}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { if (video.status == ProcessingStatus.COMPLETED) overflowOpen = true }
            )
            .semantics { contentDescription = "Video item: $title $timeLine" },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoThumbWithDuration(video)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (timeLine.isNotBlank()) {
                    Text(
                        text = timeLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                VideoStatusBlock(
                    video = video,
                    debugInfo = debugInfo,
                    showProcessingDebugPanel = showProcessingDebugPanel
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimaryVideoAction(
                    video = video,
                    family = family,
                    onRetry = onRetry,
                    onRequestCredits = onRequestCredits,
                    snackbarHostState = snackbarHostState,
                    context = context,
                    scope = scope
                )
                Box {
                    IconButton(
                        onClick = { overflowOpen = true },
                        modifier = Modifier
                            .size(44.dp)
                            .semantics {
                                contentDescription = "More actions"
                                testTag = "video_item_overflow"
                            }
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    VideoOverflowMenu(
                        expanded = overflowOpen,
                        onDismiss = { overflowOpen = false },
                        video = video,
                        onRetry = onRetry,
                        onDelete = onDelete,
                        context = context,
                        snackbarHostState = snackbarHostState,
                        scope = scope
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoStatusBlock(
    video: VideoItem,
    debugInfo: TranscriptionDebugInfo?,
    showProcessingDebugPanel: Boolean
) {
    when (video.status) {
        ProcessingStatus.QUEUED -> {
            Text(
                text = compactHistorySecondaryLine(video),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        ProcessingStatus.PROCESSING -> {
            PluctProcessingIndicator(
                progress = video.progress,
                debugInfo = debugInfo,
                embeddedFlat = true,
                showDebugPanel = showProcessingDebugPanel
            )
        }
        ProcessingStatus.FAILED -> {
            val family = PluctCoreError08OutcomeFamily.fromVideoItem(video)
            val headline = when (family) {
                PluctCoreError08OutcomeFamily.CREDITS -> "Needs credits"
                PluctCoreError08OutcomeFamily.SERVER_NET, PluctCoreError08OutcomeFamily.WAIT ->
                    "Temporary issue"
                else -> "Could not finish"
            }
            Text(
                text = compactHistorySecondaryLine(video).ifBlank { headline },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = when (family) {
                    PluctCoreError08OutcomeFamily.CREDITS -> Color(0xFFE46A1A)
                    else -> MaterialTheme.colorScheme.error
                }
            )
            if (family == PluctCoreError08OutcomeFamily.SERVER_NET || family == PluctCoreError08OutcomeFamily.WAIT) {
                Text(
                    text = "Saved for retry",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ProcessingStatus.COMPLETED -> {
            Text(
                text = compactHistorySecondaryLine(video),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0B8F6A)
            )
        }
        ProcessingStatus.UNKNOWN -> {
            Text(
                text = "Unknown",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrimaryVideoAction(
    video: VideoItem,
    family: PluctCoreError08OutcomeFamily,
    onRetry: () -> Unit,
    onRequestCredits: (() -> Unit)?,
    snackbarHostState: SnackbarHostState?,
    context: Context,
    scope: CoroutineScope
) {
    when (video.status) {
        ProcessingStatus.COMPLETED -> {
            IconButton(
                onClick = { copyTranscript(video, context, snackbarHostState, scope) },
                modifier = Modifier
                    .size(44.dp)
                    .semantics {
                        contentDescription = "Copy transcript"
                        testTag = "copy_transcript_button"
                    }
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        ProcessingStatus.FAILED -> {
            when (family) {
                PluctCoreError08OutcomeFamily.CREDITS -> {
                    if (onRequestCredits != null) {
                        IconButton(
                            onClick = onRequestCredits,
                            modifier = Modifier
                                .size(44.dp)
                                .semantics {
                                    contentDescription = "Add credits"
                                    testTag = "video_add_credits_button"
                                }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFE46A1A))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(44.dp))
                    }
                }
                else -> {
                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier
                            .size(44.dp)
                            .semantics {
                                contentDescription = "Retry"
                                testTag = "video_retry_button"
                            }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        ProcessingStatus.PROCESSING, ProcessingStatus.QUEUED -> {
            IconButton(
                onClick = { requestCancelTranscription(context, video.url) },
                modifier = Modifier
                    .size(44.dp)
                    .semantics {
                        contentDescription = "Stop"
                        testTag = "video_cancel_button"
                    }
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> Spacer(modifier = Modifier.width(44.dp))
    }
}

@Composable
private fun VideoOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    video: VideoItem,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    context: Context,
    snackbarHostState: SnackbarHostState?,
    scope: CoroutineScope
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (video.status == ProcessingStatus.COMPLETED && !video.transcript.isNullOrBlank()) {
            DropdownMenuItem(
                text = { Text("Copy") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                onClick = {
                    copyTranscript(video, context, snackbarHostState, scope)
                    onDismiss()
                }
            )
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { Icon(Icons.Default.Share, null) },
                onClick = {
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, shareText(video))
                                type = "text/plain"
                            },
                            "Share"
                        )
                    )
                    onDismiss()
                }
            )
        }
        if (video.status == ProcessingStatus.FAILED &&
            PluctCoreError08OutcomeFamily.fromVideoItem(video) == PluctCoreError08OutcomeFamily.CREDITS
        ) {
            DropdownMenuItem(
                text = { Text("Retry") },
                leadingIcon = { Icon(Icons.Default.Refresh, null) },
                onClick = {
                    onRetry()
                    onDismiss()
                }
            )
        }
        DropdownMenuItem(
            text = { Text("Delete") },
            leadingIcon = { Icon(Icons.Default.Delete, null) },
            onClick = {
                onDelete()
                onDismiss()
            }
        )
    }
}

@Composable
private fun VideoThumbWithDuration(video: VideoItem) {
    val scheme = MaterialTheme.colorScheme
    val placeholderBg = scheme.surfaceVariant.copy(alpha = 0.55f)
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (video.thumbnailUrl.isNotBlank()) Color(0xFF22222A) else placeholderBg),
        contentAlignment = Alignment.Center
    ) {
        if (video.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = "Video thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))
        } else {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = scheme.onSurfaceVariant.copy(alpha = 0.9f),
                modifier = Modifier.size(28.dp)
            )
        }
        val dur = formatDurationClock(video.duration)
        if (dur.isNotBlank()) {
            Text(
                text = dur,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

private fun formatDurationClock(seconds: Long): String {
    if (seconds <= 0L) return ""
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", m, s)
}

private fun displayTikTokHandle(video: VideoItem): String {
    val author = video.author.trim().removePrefix("@")
    val fromUrl = Regex("""/(@[A-Za-z0-9_.-]+)/""").find(video.url)?.groupValues?.getOrNull(1)?.removePrefix("@").orEmpty()
    val value = when {
        author.isNotBlank() && !author.equals("creator", ignoreCase = true) -> author
        fromUrl.isNotBlank() -> fromUrl
        else -> "TikTok"
    }
    return if (value.equals("TikTok", ignoreCase = true)) "TikTok" else "@$value"
}

private fun compactEventTime(video: VideoItem): String {
    val ts = video.queuedAt ?: video.timestamp
    if (ts <= 0L) return "saved time"
    val dayKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val now = System.currentTimeMillis()
    val today = dayKey.format(Date(ts)) == dayKey.format(Date(now))
    val yesterday = dayKey.format(Date(ts)) == dayKey.format(Date(now - 24L * 60L * 60L * 1000L))
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
    return when {
        today -> "Today $time"
        yesterday -> "Yesterday"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ts))
    }
}

private fun compactHistorySecondaryLine(video: VideoItem): String {
    val duration = when {
        video.duration <= 0L -> ""
        video.duration < 60L -> "${video.duration}s"
        else -> "${video.duration / 60L}m ${video.duration % 60L}s"
    }
    val status = when (video.status) {
        ProcessingStatus.COMPLETED -> "Done"
        ProcessingStatus.PROCESSING -> "Working"
        ProcessingStatus.QUEUED -> "Waiting"
        ProcessingStatus.FAILED -> "Saved"
        else -> "Saved"
    }
    return listOf(duration, "Text", status).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun requestCancelTranscription(context: Context, rawUrl: String) {
    val url = rawUrl.trim()
    if (url.isBlank()) return
    val notificationId = url.hashCode() and 0x7FFFFFFF
    val intent = Intent(context, PluctNotificationCancelReceiver::class.java).apply {
        action = PluctNotificationCancelReceiver.ACTION_CANCEL_TRANSCRIPTION
        putExtra(PluctNotificationCancelReceiver.EXTRA_URL, url)
        putExtra(PluctNotificationCancelReceiver.EXTRA_NOTIFICATION_ID, notificationId)
    }
    context.sendBroadcast(intent)
}

private fun copyTranscript(video: VideoItem, context: Context, snackbarHostState: SnackbarHostState?, scope: CoroutineScope) {
    val transcript = video.transcript ?: return
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText("Pluct Transcript", transcript))
    snackbarHostState?.let { scope.launch { it.showSnackbar("Copied", duration = SnackbarDuration.Short) } }
}

private fun shareText(video: VideoItem): String = buildString {
    appendLine("Transcript for ${getVideoDetailDisplayTitle(video)}")
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
