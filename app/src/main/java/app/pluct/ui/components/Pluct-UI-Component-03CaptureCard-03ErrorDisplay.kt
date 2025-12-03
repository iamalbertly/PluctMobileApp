package app.pluct.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.core.error.PluctCoreError05RetryManager
import app.pluct.services.TranscriptionDebugInfo

/**
 * Pluct-UI-Component-03CaptureCard-03ErrorDisplay - Error display component.
 */
@Composable
fun PluctUIComponent03CaptureCardErrorDisplay(
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    error: Throwable? = null,
    debugInfo: TranscriptionDebugInfo? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val isRetryable = remember(error, message) {
        if (error != null) {
            PluctCoreError05RetryManager.isRetryable(error)
        } else {
            val msg = message.lowercase()
            msg.contains("timeout") ||
                msg.contains("network") ||
                msg.contains("connection") ||
                msg.contains("server") ||
                msg.contains("service unavailable")
        }
    }

    val errorCategory = remember(message) {
        when {
            message.contains("network", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true) -> "Network"
            message.contains("timeout", ignoreCase = true) -> "Timeout"
            message.contains("invalid", ignoreCase = true) ||
                message.contains("validation", ignoreCase = true) -> "Validation"
            message.contains("insufficient", ignoreCase = true) ||
                message.contains("credits", ignoreCase = true) -> "Payment"
            message.contains("authentication", ignoreCase = true) ||
                message.contains("unauthorized", ignoreCase = true) -> "Authentication"
            else -> "API"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Error message" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (errorCategory) {
                            "Authentication" -> "Authentication failed"
                            "Network" -> "Network issue"
                            else -> "Something went wrong"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    if (debugInfo != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Flow ${debugInfo.flowRequestId.takeLast(6)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                fontFamily = FontFamily.Monospace
                            )
                            debugInfo.jobId?.let {
                                Text(
                                    text = "Job ${it.takeLast(6)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics { contentDescription = "Dismiss error" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val serviceName = extractServiceName(message)
                val shortMessage = when {
                    serviceName != null -> "Error from $serviceName: ${extractShortError(message)}"
                    message.contains("authentication", true) || message.contains("401") ->
                        "We couldn’t authenticate while checking status. We’ll retry once — or tap Retry."
                    else -> message.take(140) + if (message.length > 140) "..." else ""
                }

                Text(
                    text = shortMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = "Error message: $shortMessage"
                            testTag = "error_message_text"
                        }
                        .heightIn(max = 72.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                if (onRetry != null && isRetryable) {
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.semantics {
                            contentDescription = "Retry operation"
                            testTag = "error_retry_button"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Retry",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(
                    onClick = {
                        val textToCopy = debugInfo?.getFormattedDebugText() ?: message
                        clipboardManager.setText(AnnotatedString(textToCopy))
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Copy error details"
                        testTag = "error_copy_button"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                val hasDetailedInfo = message.length > 200 || debugInfo != null ||
                    message.contains("WHAT WAS SENT:") || message.contains("WHAT WAS RECEIVED:")
                if (hasDetailedInfo) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.semantics {
                            contentDescription = if (isExpanded) "Hide details" else "Show details"
                            testTag = "error_details_button"
                        }
                    ) {
                        Text(
                            text = if (isExpanded) "Hide" else "Details",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    message.lines().forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (debugInfo != null && !message.contains("WHAT WAS SENT:")) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Debug Details:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = debugInfo.getFormattedDebugText(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

private fun extractServiceName(message: String): String? {
    return when {
        message.contains("Business Engine") -> "Business Engine (Cloudflare Workers)"
        message.contains("TTTranscribe") -> "TTTranscribe Service (Hugging Face)"
        message.contains("/ttt/") -> "TTTranscribe Service"
        message.contains("/v1/") -> "Business Engine"
        else -> null
    }
}

private fun extractShortError(message: String): String {
    val lines = message.lines()
    for (line in lines) {
        if (line.contains("Parse Error:") || line.contains("Error:") || line.contains("failed", ignoreCase = true)) {
            return line.replace("Parse Error:", "").replace("Error:", "").trim()
        }
    }
    return message.take(80) + if (message.length > 80) "..." else ""
}
