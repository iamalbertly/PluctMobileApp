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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
@Composable
fun PluctProcessingIndicator(
    currentJobId: String? = null,
    progress: Int = 0,
    currentOperation: String? = null,
    estimatedTimeRemaining: String? = null,
    showTimeoutWarning: Boolean = false,
    debugInfo: TranscriptionDebugInfo? = null,
    showDebugPanel: Boolean = false,
    embeddedFlat: Boolean = false
) {
    var isDebugExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val inferredProgress = debugInfo?.currentStep?.let { progressForStep(it, debugInfo.pollingAttempt, debugInfo.maxPollingAttempts) } ?: 0
    val displayProgress = maxOf(progress, inferredProgress).coerceIn(0, 99)
    val isRetryingPoll = debugInfo?.pollingAttempt != null &&
        (debugInfo.pollingAttempt ?: 0) > 1 &&
        debugInfo.currentStep == OperationStep.POLLING
    val barColor = if (isRetryingPoll) {
        Color(0xFFE46A1A)
    } else {
        MaterialTheme.colorScheme.primary
    }

    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (embeddedFlat) Modifier else Modifier.padding(16.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!embeddedFlat) {
                    if (displayProgress > 0) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            progress = displayProgress / 100f
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = humanPhaseText(debugInfo, currentOperation, displayProgress),
                        style = if (embeddedFlat) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isRetryingPoll) Color(0xFFE46A1A) else MaterialTheme.colorScheme.primary
                    )
                    if (estimatedTimeRemaining != null) {
                        Text(
                            text = estimatedTimeRemaining,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = (maxOf(displayProgress, 4) / 100f).coerceIn(0.04f, 0.99f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(top = if (embeddedFlat) 6.dp else 10.dp)
                    .semantics {
                        contentDescription = "Progress bar $displayProgress percent"
                        testTag = "progress_bar"
                    },
                color = barColor,
                trackColor = barColor.copy(alpha = 0.22f)
            )

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

            if (debugInfo != null && showDebugPanel) {
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

    if (embeddedFlat) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Processing indicator"
                    testTag = "processing_indicator"
                }
        ) {
            body()
        }
    } else {
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
            body()
        }
    }
}

@Composable
fun ProcessingIndicator(
    message: String,
    isVisible: Boolean
) {
    if (isVisible) {
        PluctProcessingIndicator(currentOperation = message, showDebugPanel = false, embeddedFlat = false)
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

private fun humanPhaseText(debugInfo: TranscriptionDebugInfo?, fallback: String?, progress: Int): String {
    val pollAttempt = debugInfo?.pollingAttempt
    val pollMax = debugInfo?.maxPollingAttempts
    val retrying = pollAttempt != null && pollMax != null && pollAttempt > 1 &&
        debugInfo.currentStep == OperationStep.POLLING
    if (retrying) {
        return "Retrying... ($pollAttempt/$pollMax)"
    }
    return when (debugInfo?.currentStep) {
        OperationStep.CANONICALIZE, OperationStep.METADATA -> "Getting video info…"
        OperationStep.CHECK_BALANCE, OperationStep.VEND_TOKEN -> "Preparing…"
        OperationStep.SUBMIT -> "Starting…"
        OperationStep.POLLING -> "Processing \u2022 ${progress.coerceIn(0, 99)}%"
        OperationStep.COMPLETED -> "Processing \u2022 100%"
        OperationStep.FAILED -> "! Try again"
        null -> fallback?.takeIf { it.isNotBlank() }?.take(40)
            ?: if (progress > 0) "Processing \u2022 ${progress.coerceIn(0, 99)}%" else "Waiting"
    }
}
