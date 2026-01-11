package app.pluct.ui.components.error.log

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import app.pluct.data.entity.DebugLogEntry
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pluct-UI-Component-Error-Log-08Section
 * Collapsible error log section showing recent errors
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[SubScope]-[Responsibility]
 */
@Composable
fun PluctErrorLogSection(
    errorLogs: List<DebugLogEntry>,
    onCopyError: (DebugLogEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    
    if (errorLogs.isEmpty()) {
        return // Don't show section if no errors
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Error log section"
                testTag = "error_log_section"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Errors (${errorLogs.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (isExpanded && errorLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                errorLogs.take(3).forEach { error ->
                    ErrorLogItem(
                        error = error,
                        onCopy = {
                            val errorText = buildString {
                                appendLine("Error: ${error.message}")
                                appendLine("Category: ${error.category}")
                                appendLine("Operation: ${error.operation}")
                                appendLine("Time: ${formatTimestamp(error.timestamp)}")
                                if (error.errorCode.isNotBlank()) {
                                    appendLine("Error Code: ${error.errorCode}")
                                }
                                if (error.responseStatusCode > 0) {
                                    appendLine("Status: ${error.responseStatusCode}")
                                }
                                if (error.stackTrace.isNotBlank()) {
                                    appendLine("\nStackTrace:\n${error.stackTrace}")
                                }
                            }
                            clipboardManager.setText(AnnotatedString(errorText))
                            onCopyError(error)
                        }
                    )
                    if (error != errorLogs.take(3).last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorLogItem(
    error: DebugLogEntry,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = error.message.take(100) + if (error.message.length > 100) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${error.category} • ${formatTimestamp(error.timestamp)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy error",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}






