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
import app.pluct.ui.components.PluctInsufficientCreditsDialog
import app.pluct.ui.components.PluctRateLimitDialog

/**
 * Pluct-UI-Error-01UnifiedHandler
 * Centralized error handling for Business Engine errors
 * Maps technical errors to user-friendly dialogs
 */
@Composable
fun PluctBusinessEngineErrorHandler(
    error: Throwable?,
    onDismiss: () -> Unit,
    onPurchaseCredits: () -> Unit,
    onRetry: () -> Unit
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
        
        when {
            // 402 Insufficient Credits
            details.errorCode == "INSUFFICIENT_CREDITS" || details.responseStatusCode == 402 -> {
                // Parse required credits from body or default to 1
                // Body might be "Balance: 0" or JSON
                val currentBalance = try {
                    details.responseBody.substringAfter("Balance: ").trim().toIntOrNull() ?: 0
                } catch (e: Exception) { 0 }
                
                PluctInsufficientCreditsDialog(
                    currentBalance = currentBalance,
                    requiredCredits = 1, // Default cost
                    onPurchase = onPurchaseCredits,
                    onDismiss = onDismiss
                )
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
            
            // 502/500 Upstream Error
            details.responseStatusCode in 500..599 -> {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text("Service Unavailable") },
                    text = { Text(error.userMessage) },
                    confirmButton = {
                        TextButton(onClick = onRetry) {
                            Text("Retry")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Default Detailed Error
            else -> {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text("Error") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(error.userMessage)
                            if (showDetails) {
                                ErrorDetails(detailsText)
                            } else {
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
                            TextButton(onClick = onDismiss) {
                                Text("OK")
                            }
                        }
                    }
                )
            }
        }
    } else {
        // Generic Throwable
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Error") },
            text = { Text(error.message ?: "An unexpected error occurred") },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK")
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
