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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.services.TranscriptionDebugInfo

/**
 * Pluct-UI-Component-03CaptureCard-04ProcessingIndicator - Processing indicator component.
 */
/**
 * UX IMPROVEMENT #4: Ensure progress indicators persist correctly when app is backgrounded
 * Progress state is managed by LaunchedEffect and persists across configuration changes
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
    // Remember progress to persist across recompositions
    val rememberedProgress = remember(progress) { progress }
    val rememberedOperation = remember(currentOperation) { currentOperation }
    var isDebugExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Processing indicator"
                testTag = "processing_indicator"
            },
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
                val displayProgress = rememberedProgress.coerceAtLeast(progress)
                if (displayProgress > 0) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        progress = displayProgress / 100f
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
                        text = rememberedOperation ?: currentOperation ?: "Processing...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // UX IMPROVEMENT #2: Clearer progress information with phase context
                    // UX IMPROVEMENT #4: Use remembered progress to persist state
                    if (displayProgress > 0) {
                        Text(
                            text = "$displayProgress% complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.semantics {
                                contentDescription = "Progress: $displayProgress percent complete"
                                testTag = "progress_percentage"
                            }
                        )
                        if (estimatedTimeRemaining == null) {
                            // UX IMPROVEMENT: Duolingo-style friendly, encouraging progress messages
                            Text(
                                text = when {
                                    progress < 15 -> "Getting things ready..."
                                    progress < 30 -> "Listening to your video..."
                                    progress < 50 -> "You're making great progress!"
                                    progress < 70 -> "Almost halfway there..."
                                    progress < 85 -> "The AI is working its magic!"
                                    progress < 95 -> "Just a few more seconds..."
                                    else -> "Putting the finishing touches!"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // UX IMPROVEMENT: Friendly starting message
                        Text(
                            text = "Hang tight! We're getting your transcript ready...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.semantics {
                                contentDescription = "Starting transcription"
                                testTag = "progress_starting"
                            }
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

            // UX IMPROVEMENT #5: Helpful guidance when transcription takes longer than expected
            if (showTimeoutWarning) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Timeout warning"
                            testTag = "timeout_warning"
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Still working on it!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This one's taking a bit longer - don't worry, we've got this! Feel free to come back later, we'll keep working in the background.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
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
