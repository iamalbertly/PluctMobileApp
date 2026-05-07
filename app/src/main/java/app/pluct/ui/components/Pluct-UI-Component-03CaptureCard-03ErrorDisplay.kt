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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import app.pluct.services.PluctCoreAPIDetailedError
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import app.pluct.core.debug.PluctCoreDebug01LogManager
import app.pluct.services.TranscriptionDebugInfo
import app.pluct.core.categorization.PluctCoreCategorization01ErrorClassifier

/**
 * Error recovery action data class
 */
data class ErrorRecoveryAction(
    val label: String,
    val action: () -> Unit,
    val priority: Int // 1 = primary, 2 = secondary
)

/**
 * Get recovery actions based on error type
 */
fun getRecoveryActions(
    error: Throwable?,
    message: String,
    onAddCredits: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onCheckConnection: (() -> Unit)? = null
): List<ErrorRecoveryAction> {
    val actions = mutableListOf<ErrorRecoveryAction>()

    if (isRemoteServiceIssue(message)) {
        onRetry?.let {
            actions.add(ErrorRecoveryAction(
                label = "Retry",
                action = it,
                priority = 1
            ))
        }
        return actions
    }
    
    // Use centralized error categorization
    val categorized = PluctCoreCategorization01ErrorClassifier.categorizeError(error, message)
    
    when (categorized.category) {
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.INSUFFICIENT_CREDITS -> {
            onAddCredits?.let {
                actions.add(ErrorRecoveryAction(
                    label = "Add Credits",
                    action = it,
                    priority = 1
                ))
            }
        }
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.NETWORK -> {
            onCheckConnection?.let {
                actions.add(ErrorRecoveryAction(
                    label = "Check Connection",
                    action = it,
                    priority = 1
                ))
            }
            onRetry?.let {
                actions.add(ErrorRecoveryAction(
                    label = "Retry",
                    action = it,
                    priority = 2
                ))
            }
        }
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.RATE_LIMIT -> {
            // Rate limit errors suggest queueing (handled by parent)
            onRetry?.let {
                actions.add(ErrorRecoveryAction(
                    label = "Retry Later",
                    action = it,
                    priority = 1
                ))
            }
        }
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.AUTHENTICATION -> {
            // Auth errors need re-login (handled by parent with token refresh)
            onRetry?.let {
                actions.add(ErrorRecoveryAction(
                    label = "Retry",
                    action = it,
                    priority = 1
                ))
            }
        }
        else -> {
            // Default: offer retry
            onRetry?.let {
                actions.add(ErrorRecoveryAction(
                    label = "Retry",
                    action = it,
                    priority = 1
                ))
            }
        }
    }
    
    return actions.sortedBy { it.priority }
}

/**
 * Pluct-UI-Component-03CaptureCard-03ErrorDisplay - Error display component.
 */
@Composable
fun PluctUIComponent03CaptureCardErrorDisplay(
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    error: Throwable? = null,
    debugInfo: TranscriptionDebugInfo? = null,
    debugLogManager: PluctCoreDebug01LogManager? = null,
    onViewInLogs: (() -> Unit)? = null,
    onAddCredits: (() -> Unit)? = null,
    onCheckConnection: (() -> Unit)? = null
) {
    // Auto-persist error on first render
    LaunchedEffect(message, error) {
        debugLogManager?.logError(
            category = "USER_FACING_ERROR",
            operation = "captureCardError",
            message = message,
            exception = error,
            requestUrl = debugInfo?.url ?: "",
            requestPayload = debugInfo?.getFormattedDebugText() ?: ""
        )
    }
    var isExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val categorizedError = remember(error, message) {
        PluctCoreCategorization01ErrorClassifier.categorizeError(error, message)
    }
    val errorTitle = remember(message, categorizedError) {
        friendlyErrorTitle(message, categorizedError.category)
    }
    val shortMessage = remember(message, categorizedError) {
        friendlyErrorMessage(message, categorizedError)
    }
    val hasDetailedInfo = message.length > 200 || debugInfo != null ||
        message.contains("WHAT WAS SENT:") || message.contains("WHAT WAS RECEIVED:")
    val recoveryActions = remember(error, message, onAddCredits, onRetry, onCheckConnection) {
        getRecoveryActions(
            error = error,
            message = message,
            onAddCredits = onAddCredits,
            onRetry = onRetry,
            onCheckConnection = onCheckConnection
        )
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
                        text = errorTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
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

            Text(
                text = shortMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Error: $shortMessage"
                        testTag = "error_message_text"
                    }
            )

            if (recoveryActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    recoveryActions.take(2).forEach { action ->
                    TextButton(
                        onClick = action.action,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .semantics {
                                contentDescription = "Recovery action: ${action.label}"
                                testTag = "error_recovery_${action.label.lowercase().replace(" ", "_")}"
                            }
                    ) {
                        if (action.label == "Retry") {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (action.priority == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                }
            }

            if (onViewInLogs != null || hasDetailedInfo) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                if (onViewInLogs != null && debugLogManager != null) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onViewInLogs()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "View error in debug logs"
                            testTag = "error_view_logs_button"
                        }
                    ) {
                        Text(
                            text = "Logs",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
                }
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
                            text = "Support details:",
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

private fun isRemoteServiceIssue(message: String): Boolean {
    return message.contains("TTTranscribe", ignoreCase = true) ||
        message.contains("transcription service", ignoreCase = true) ||
        message.contains("service is waking", ignoreCase = true) ||
        message.contains("upstream_error", ignoreCase = true) ||
        message.contains("503")
}

private fun friendlyErrorTitle(
    message: String,
    category: PluctCoreCategorization01ErrorClassifier.ErrorCategory
): String {
    if (isRemoteServiceIssue(message)) return "Service"
    return when (category) {
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.AUTHENTICATION -> "Session"
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.NETWORK -> "Network"
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.VALIDATION -> "Invalid"
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.INSUFFICIENT_CREDITS -> "Credits"
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.RATE_LIMIT -> "Busy"
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.SERVER_ERROR -> "Service"
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.UNKNOWN -> "Error"
    }
}

private fun friendlyErrorMessage(
    message: String,
    categorizedError: PluctCoreCategorization01ErrorClassifier.CategorizedError
): String {
    if (isRemoteServiceIssue(message)) return "Service is waking up. Tap Retry soon."
    return when (categorizedError.category) {
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.AUTHENTICATION ->
            "Session expired. Tap Retry."
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.NETWORK ->
            "No connection. Check network."
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.VALIDATION ->
            "Check the TikTok link."
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.INSUFFICIENT_CREDITS ->
            "Out of credits."
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.RATE_LIMIT ->
            "Too many tries. Wait a moment."
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.SERVER_ERROR ->
            "Service is busy. Tap Retry soon."
        PluctCoreCategorization01ErrorClassifier.ErrorCategory.UNKNOWN ->
            message.take(60) + if (message.length > 60) "..." else ""
    }
}
