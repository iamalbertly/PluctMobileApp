package app.pluct.ui.components

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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.services.TranscriptionDebugInfo

/**
 * Pluct-UI-Component-03CaptureCard-04ProcessingIndicator - Processing indicator component.
 */
@Composable
fun PluctProcessingIndicator(
    currentJobId: String? = null,
    progress: Int = 0,
    currentOperation: String? = null,
    estimatedTimeRemaining: String? = null,
    showTimeoutWarning: Boolean = false,
    debugInfo: TranscriptionDebugInfo? = null
) {
    var isDebugExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Processing indicator" },
        colors = CardDefaults.cardColors(
            containerColor = if (showTimeoutWarning) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (progress > 0) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        progress = progress / 100f
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentOperation ?: "Processing...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (progress > 0) {
                        Text(
                            text = "$progress% complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (currentJobId != null) {
                        Text(
                            text = "Job ID: ${currentJobId.take(8)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (estimatedTimeRemaining != null) {
                        Text(
                            text = "Estimated time: $estimatedTimeRemaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showTimeoutWarning) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This is taking longer than expected. The operation may timeout soon.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (debugInfo != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Debug Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(debugInfo.getFormattedDebugText()))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy debug info",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { isDebugExpanded = !isDebugExpanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isDebugExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isDebugExpanded) "Collapse" else "Expand"
                            )
                        }
                    }
                }

                if (isDebugExpanded) {
                    PluctProcessingDebugDetails(debugInfo)
                }
            }
        }
    }
}

@Composable
fun ProcessingIndicator(
    message: String,
    isVisible: Boolean
) {
    if (isVisible) {
        PluctProcessingIndicator(currentOperation = message)
    }
}
