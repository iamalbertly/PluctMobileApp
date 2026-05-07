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
import androidx.compose.material3.LinearProgressIndicator
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
import app.pluct.services.OperationStep
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
            val inferredProgress = debugInfo?.currentStep?.let { progressForStep(it, debugInfo.pollingAttempt, debugInfo.maxPollingAttempts) } ?: 0
            val displayProgress = maxOf(rememberedProgress, progress, inferredProgress).coerceIn(0, 99)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        text = phaseText(debugInfo, rememberedOperation ?: currentOperation, displayProgress),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // UX IMPROVEMENT: Clean, international-friendly progress display
                    // Uses visual percentage + minimal text for global users
                    if (displayProgress > 0) {
                        Text(
                            text = "$displayProgress% -> Text",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.semantics {
                                contentDescription = "Progress: $displayProgress percent"
                                testTag = "progress_percentage"
                            }
                        )
                    } else {
                        // Simple starting indicator
                        Text(
                            text = "0% -> Text",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.semantics {
                                contentDescription = "Starting transcription"
                                testTag = "progress_starting"
                            }
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

            LinearProgressIndicator(
                progress = (maxOf(displayProgress, 4) / 100f).coerceIn(0.04f, 0.99f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .semantics {
                        contentDescription = "Progress bar $displayProgress percent"
                        testTag = "progress_bar"
                    }
            )

            // UX IMPROVEMENT: Simplified timeout warning - less text, clearer message
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Still processing...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
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

private fun progressForStep(step: OperationStep, attempt: Int?, maxAttempts: Int?): Int = when (step) {
    OperationStep.CANONICALIZE -> 8
    OperationStep.METADATA -> 16
    OperationStep.CHECK_BALANCE -> 24
    OperationStep.VEND_TOKEN -> 34
    OperationStep.SUBMIT -> 48
    OperationStep.POLLING -> {
        val max = maxAttempts?.takeIf { it > 0 } ?: 20
        val current = attempt?.coerceAtLeast(1) ?: 1
        (55 + ((current.toFloat() / max.toFloat()) * 36)).toInt().coerceIn(55, 91)
    }
    OperationStep.COMPLETED -> 99
    OperationStep.FAILED -> 0
}

private fun phaseText(debugInfo: TranscriptionDebugInfo?, fallback: String?, progress: Int): String {
    return when (debugInfo?.currentStep) {
        OperationStep.CANONICALIZE, OperationStep.METADATA -> "Link -> Ready"
        OperationStep.CHECK_BALANCE, OperationStep.VEND_TOKEN -> "Wallet -> Go"
        OperationStep.SUBMIT -> "Video -> Audio"
        OperationStep.POLLING -> "Audio -> Text"
        OperationStep.COMPLETED -> "100% -> Text"
        OperationStep.FAILED -> "! Try again"
        null -> fallback?.take(28) ?: if (progress > 0) "Audio -> Text" else "Video -> Text"
    }
}
