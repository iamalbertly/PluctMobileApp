package app.pluct.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.DebugLogEntry
import app.pluct.data.entity.LogLevel
import app.pluct.core.debug.PluctCoreDebug01LogManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pluct-UI-Component-DebugLog-Viewer - Debug log viewer component
 * Displays debug logs with expandable entries and copy-to-clipboard functionality
 */
@Composable
fun PluctDebugLogViewer(
    logs: List<DebugLogEntry>,
    debugLogManager: PluctCoreDebug01LogManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    
    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedLevel by remember { mutableStateOf<LogLevel?>(null) }
    
    // Get unique categories
    val categories = remember(logs) {
        logs.map { it.category }.distinct().sorted()
    }
    
    // Filter logs
    val filteredLogs = remember(logs, searchQuery, selectedCategory, selectedLevel) {
        logs.filter { log ->
            val matchesSearch = searchQuery.isBlank() || 
                log.message.contains(searchQuery, ignoreCase = true) ||
                log.operation.contains(searchQuery, ignoreCase = true) ||
                log.requestUrl.contains(searchQuery, ignoreCase = true) ||
                log.category.contains(searchQuery, ignoreCase = true)
            
            val matchesCategory = selectedCategory == null || log.category == selectedCategory
            val matchesLevel = selectedLevel == null || log.level == selectedLevel
            
            matchesSearch && matchesCategory && matchesLevel
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Debug Logs (${filteredLogs.size}/${logs.size})")
                    if (logs.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    // Clear all logs
                                    logs.forEach { debugLogManager.deleteLog(it) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear all logs")
                        }
                    }
                }
            }
        },
        text = {
            Column {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search logs...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All categories chip
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All") }
                    )
                    
                    // Category chips
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { 
                                selectedCategory = if (selectedCategory == category) null else category
                            },
                            label = { Text(category) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Level filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedLevel == null,
                        onClick = { selectedLevel = null },
                        label = { Text("All Levels") }
                    )
                    FilterChip(
                        selected = selectedLevel == LogLevel.ERROR,
                        onClick = { 
                            selectedLevel = if (selectedLevel == LogLevel.ERROR) null else LogLevel.ERROR
                        },
                        label = { Text("ERROR") }
                    )
                    FilterChip(
                        selected = selectedLevel == LogLevel.WARNING,
                        onClick = { 
                            selectedLevel = if (selectedLevel == LogLevel.WARNING) null else LogLevel.WARNING
                        },
                        label = { Text("WARNING") }
                    )
                    FilterChip(
                        selected = selectedLevel == LogLevel.INFO,
                        onClick = { 
                            selectedLevel = if (selectedLevel == LogLevel.INFO) null else LogLevel.INFO
                        },
                        label = { Text("INFO") }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Log list
                if (filteredLogs.isEmpty()) {
                    Text(
                        "No logs match your filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLogs) { log ->
                            DebugLogItem(
                                log = log,
                                debugLogManager = debugLogManager,
                                clipboardManager = clipboardManager,
                                onDelete = {
                                    scope.launch {
                                        debugLogManager.deleteLog(log)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DebugLogItem(
    log: DebugLogEntry,
    debugLogManager: PluctCoreDebug01LogManager,
    clipboardManager: ClipboardManager,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())
    val scope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (log.level) {
                LogLevel.ERROR -> MaterialTheme.colorScheme.errorContainer
                LogLevel.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                LogLevel.INFO -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.category,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (log.level) {
                                LogLevel.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                                LogLevel.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                                LogLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = log.operation,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val formattedLog = debugLogManager.formatLogForClipboard(log)
                                val clipData = ClipData.newPlainText("Debug Log", formattedLog)
                                clipboardManager.setPrimaryClip(clipData)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy to clipboard",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete this log",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Summary (always visible)
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2
            )
            
            // Expanded details
            if (isExpanded) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                if (log.requestUrl.isNotEmpty()) {
                    DetailSection("Request URL", log.requestUrl)
                }
                
                if (log.requestPayload.isNotEmpty()) {
                    DetailSection("Request Details", log.requestPayload)
                }
                
                if (log.requestMethod.isNotEmpty()) {
                    DetailSection("Method", log.requestMethod)
                }
                
                if (log.responseStatusCode > 0) {
                    DetailSection("Response Status", log.responseStatusCode.toString())
                }
                
                if (log.responseBody.isNotEmpty()) {
                    DetailSection("Response/Error Details", log.responseBody)
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
        )
    }
}
