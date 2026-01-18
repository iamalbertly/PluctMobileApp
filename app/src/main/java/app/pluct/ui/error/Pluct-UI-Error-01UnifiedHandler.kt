package app.pluct.ui.error

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.pluct.services.PluctCoreAPIDetailedError
import app.pluct.core.error.PluctCoreError03UserMessageFormatter
import app.pluct.ui.components.PluctInsufficientCreditsDialog
import app.pluct.ui.components.PluctInsufficientCreditsDialogWithQueue
import app.pluct.ui.components.PluctRateLimitDialog

/**
 * Pluct-UI-Error-01UnifiedHandler
 * Centralized error handling for Business Engine errors
 * Uses PluctCoreError03UserMessageFormatter as single source of truth for error messages
 * Maps technical errors to user-friendly dialogs
 */
@Composable
fun PluctBusinessEngineErrorHandler(
    error: Throwable?,
    onDismiss: () -> Unit,
    onPurchaseCredits: () -> Unit,
    onRetry: () -> Unit,
    url: String? = null,
    queuedCount: Int = 0,
    onQueueForLater: (() -> Unit)? = null
) {
    if (error == null) return
    val clipboard = LocalClipboardManager.current
    var showDetails by remember { mutableStateOf(false) }
    var detailsText by remember { mutableStateOf("") }

    if (error is PluctCoreAPIDetailedError) {
        val details = error.technicalDetails
        detailsText = buildString {
            appendLine("Service: ${details.serviceName}")
            appendLine("Operation: ${details.operation}")
            appendLine("Endpoint: ${details.endpoint}")
            appendLine("Method: ${details.requestMethod}")
            appendLine("Request URL: ${details.requestUrl}")
            if (details.requestPayload.isNotBlank()) appendLine("Request Payload: ${details.requestPayload}")
            if (details.requestHeaders.isNotBlank()) appendLine("Request Headers: ${details.requestHeaders}")
            appendLine("Status: ${details.responseStatusCode} ${details.responseStatusMessage}")
            if (details.responseBody.isNotBlank()) appendLine("Response Body: ${details.responseBody}")
            if (details.responseHeaders.isNotBlank()) appendLine("Response Headers: ${details.responseHeaders}")
            appendLine("Error Type: ${details.errorType}")
            if (details.errorCode.isNotBlank()) appendLine("Error Code: ${details.errorCode}")
            appendLine("Timestamp: ${details.timestamp}")
        }
        
        // Use unified formatter as single source of truth for error messages
        val userMessage = PluctCoreError03UserMessageFormatter.formatUserMessage(
            error = error,
            technicalMessage = error.userMessage,
            errorCode = details.errorCode,
            httpStatus = details.responseStatusCode
        )
        
        when {
            // 402 Insufficient Credits
            details.errorCode == "INSUFFICIENT_CREDITS" || details.responseStatusCode == 402 -> {
                // Parse required credits from body or default to 1
                // Body might be "Balance: 0" or JSON
                val currentBalance = try {
                    details.responseBody.substringAfter("Balance: ").trim().toIntOrNull() ?: 0
                } catch (e: Exception) { 0 }
                
                // Use enhanced dialog with queue option if URL is available
                if (url != null && onQueueForLater != null) {
                    PluctInsufficientCreditsDialogWithQueue(
                        currentBalance = currentBalance,
                        requiredCredits = 1, // Default cost
                        url = url,
                        queuedCount = queuedCount,
                        onPurchase = onPurchaseCredits,
                        onQueueForLater = onQueueForLater,
                        onDismiss = onDismiss
                    )
                } else {
                    // Fallback to original dialog
                    PluctInsufficientCreditsDialog(
                        currentBalance = currentBalance,
                        requiredCredits = 1, // Default cost
                        onPurchase = onPurchaseCredits,
                        onDismiss = onDismiss
                    )
                }
            }
            
            // 429 Rate Limit
            details.errorCode == "RATE_LIMIT_EXCEEDED" || details.responseStatusCode == 429 -> {
                // Try to parse reset time from details if available
                // For now, just show generic message or parse from body if structured
                PluctRateLimitDialog(
                    resetTime = "", // TODO: Parse from response headers/body
                    onDismiss = onDismiss
                )
            }
            
            // 502/500 Upstream Error with retry guidance - use unified formatter
            details.responseStatusCode in 500..599 -> {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(userMessage.title) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(userMessage.message)
                            if (userMessage.technicalDetails != null) {
                                Text(
                                    text = "Technical details available. Tap 'Show Details' for more information.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        if (userMessage.retryable) {
                            TextButton(onClick = onRetry) {
                                Text(userMessage.action)
                            }
                        } else {
                            TextButton(onClick = onDismiss) {
                                Text("OK")
                            }
                        }
                    },
                    dismissButton = {
                        if (userMessage.retryable) {
                            TextButton(onClick = onDismiss) {
                                Text("Cancel")
                            }
                        }
                    }
                )
            }
            
            // Default Detailed Error - use unified formatter
            else -> {
                val hasUpstreamDetails = error.technicalDetails.responseBody.contains("upstream", ignoreCase = true) ||
                                        error.technicalDetails.responseBody.contains("\"error\"", ignoreCase = true)
                
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(userMessage.title) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(userMessage.message)
                            if (hasUpstreamDetails && !showDetails) {
                                Text(
                                    text = "Technical details available. Tap 'Show Details' for more information.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (showDetails) {
                                ErrorDetails(detailsText)
                            } else if (!hasUpstreamDetails) {
                                Text("Tap \"Show Details\" to view what was sent/received for debugging.")
                            }
                        }
                    },
                    confirmButton = {
                        Column {
                            TextButton(onClick = { showDetails = !showDetails }) {
                                Text(if (showDetails) "Hide Details" else "Show Details")
                            }
                            TextButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(detailsText))
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                Spacer(modifier = Modifier.height(0.dp))
                                Text("Copy Details")
                            }
                            if (userMessage.retryable) {
                                TextButton(onClick = onRetry) {
                                    Text(userMessage.action)
                                }
                            } else {
                                TextButton(onClick = onDismiss) {
                                    Text("OK")
                                }
                            }
                        }
                    }
                )
            }
        }
    } else {
        // Generic Throwable - use unified formatter
        val userMessage = PluctCoreError03UserMessageFormatter.formatUserMessage(
            error = error,
            technicalMessage = error.message
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(userMessage.title) },
            text = { Text(userMessage.message) },
            confirmButton = {
                if (userMessage.retryable) {
                    TextButton(onClick = onRetry) {
                        Text(userMessage.action)
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            }
        )
    }
}

@Composable
private fun ErrorDetails(details: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Error Details:")
        Text(details)
    }
}

// Removed determineRetryability - now using unified formatter's retryable flag
// Single source of truth: PluctCoreError03UserMessageFormatter determines retryability
