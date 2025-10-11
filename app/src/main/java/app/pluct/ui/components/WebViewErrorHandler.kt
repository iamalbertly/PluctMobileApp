package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pluct.utils.Constants
import app.pluct.utils.NetworkUtils

/**
 * Intelligent WebView error handler with connectivity-aware recovery options
 */
@Composable
fun WebViewErrorHandler(
    errorCode: String?,
    errorMessage: String?,
    urlUsed: String?,
    onRetry: () -> Unit,
    onManualMode: () -> Unit,
    onReturnToMain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, title, message, color, showRetry, showManual, showReturn) = when (errorCode) {
        "timeout" -> {
            ErrorDisplayData(
                icon = Icons.Default.Schedule,
                title = "Processing Timeout",
                message = Constants.ErrorMessages.TIMEOUT_ERROR,
                color = MaterialTheme.colorScheme.tertiary,
                showRetry = true,
                showManual = true,
                showReturn = true
            )
        }
        "network_error", "webview_error" -> {
            ErrorDisplayData(
                icon = Icons.Default.WifiOff,
                title = "Network Error",
                message = Constants.ErrorMessages.NETWORK_ERROR,
                color = MaterialTheme.colorScheme.error,
                showRetry = true,
                showManual = true,
                showReturn = true
            )
        }
        "service_unavailable" -> {
            ErrorDisplayData(
                icon = Icons.Default.CloudOff,
                title = "Service Unavailable",
                message = Constants.ErrorMessages.SERVICE_UNAVAILABLE,
                color = MaterialTheme.colorScheme.error,
                showRetry = true,
                showManual = true,
                showReturn = true
            )
        }
        "invalid_url" -> {
            ErrorDisplayData(
                icon = Icons.Default.LinkOff,
                title = "Invalid URL",
                message = Constants.ErrorMessages.INVALID_URL,
                color = MaterialTheme.colorScheme.error,
                showRetry = false,
                showManual = true,
                showReturn = true
            )
        }
        "invalid_data" -> {
            ErrorDisplayData(
                icon = Icons.Default.VideoLibrary,
                title = "Video Not Found",
                message = Constants.ErrorMessages.INVALID_DATA,
                color = MaterialTheme.colorScheme.error,
                showRetry = false,
                showManual = true,
                showReturn = true
            )
        }
        "no_subtitles" -> {
            ErrorDisplayData(
                icon = Icons.Default.ClosedCaptionOff,
                title = "No Subtitles Available",
                message = Constants.ErrorMessages.NO_SUBTITLES,
                color = MaterialTheme.colorScheme.tertiary,
                showRetry = false,
                showManual = true,
                showReturn = true
            )
        }
        "processing_timeout" -> {
            ErrorDisplayData(
                icon = Icons.Default.Schedule,
                title = "Processing Timeout",
                message = Constants.ErrorMessages.PROCESSING_TIMEOUT,
                color = MaterialTheme.colorScheme.tertiary,
                showRetry = true,
                showManual = true,
                showReturn = true
            )
        }
        "generic_error" -> {
            ErrorDisplayData(
                icon = Icons.Default.Error,
                title = "Processing Error",
                message = Constants.ErrorMessages.GENERIC_ERROR,
                color = MaterialTheme.colorScheme.error,
                showRetry = true,
                showManual = true,
                showReturn = true
            )
        }
        "red_error_text" -> {
            ErrorDisplayData(
                icon = Icons.Default.Warning,
                title = "Service Error",
                message = Constants.ErrorMessages.RED_ERROR_TEXT,
                color = MaterialTheme.colorScheme.error,
                showRetry = true,
                showManual = true,
                showReturn = true
            )
        }
        "error_container" -> {
            ErrorDisplayData(
                icon = Icons.Default.Warning,
                title = "Service Error",
                message = Constants.ErrorMessages.ERROR_CONTAINER,
                color = MaterialTheme.colorScheme.error,
                showRetry = true,
                showManual = true,
                showReturn = true
            )
        }
        "webview_crash" -> {
            ErrorDisplayData(
                icon = Icons.Default.BugReport,
                title = "Browser Error",
                message = "A browser error occurred. This may be due to memory issues or network problems.",
                color = MaterialTheme.colorScheme.error,
                showRetry = true,
                showManual = true,
                showReturn = true
            )
        }
        else -> {
            ErrorDisplayData(
                icon = Icons.Default.Error,
                title = "Transcript Error",
                message = errorMessage ?: Constants.ErrorMessages.UNKNOWN_ERROR,
                color = MaterialTheme.colorScheme.error,
                showRetry = true,
                showManual = true,
                showReturn = true
            )
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = color
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Error message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            // URL used (if available)
            urlUsed?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "URL: $url",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
            // Action buttons
            if (showRetry || showManual || showReturn) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showRetry) {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = color
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Try Again")
                        }
                    }
                    
                    if (showManual) {
                        OutlinedButton(
                            onClick = onManualMode,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Try Manual Mode")
                        }
                    }
                    
                    if (showReturn) {
                        OutlinedButton(
                            onClick = onReturnToMain,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Return to Main Page")
                        }
                    }
                }
            }
            
            // Helpful tips
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "💡 Tips:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getHelpfulTips(errorCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Data class for error display information
 */
private data class ErrorDisplayData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val message: String,
    val color: androidx.compose.ui.graphics.Color,
    val showRetry: Boolean,
    val showManual: Boolean,
    val showReturn: Boolean
)

/**
 * Get helpful tips based on error type
 */
private fun getHelpfulTips(errorCode: String?): String {
    return when (errorCode) {
        "timeout" -> "• Check your internet connection\n• Try again during off-peak hours\n• Use manual mode for better control"
        "network_error", "webview_error" -> "• Check your internet connection\n• Try switching between WiFi and mobile data\n• Restart the app if the problem persists"
        "service_unavailable" -> "• The service may be temporarily down\n• Try again in a few minutes\n• Use manual mode as an alternative"
        "invalid_url" -> "• Make sure the URL is from a supported platform\n• Check that the video is public and accessible\n• Try copying the URL again"
        "invalid_data" -> "• The video may be private or deleted\n• Try a different video link\n• Make sure the video is public and accessible"
        "no_subtitles" -> "• This video may not have captions enabled\n• Try a different video with captions\n• Use manual mode to enter text directly"
        "processing_timeout" -> "• The video may be too long or complex\n• Try a shorter video\n• Check your internet connection"
        "generic_error", "red_error_text", "error_container" -> "• Try a different video link\n• Check your internet connection\n• Use manual mode as an alternative"
        "webview_crash" -> "• Close other apps to free up memory\n• Restart the app\n• Try manual mode which uses less resources"
        else -> "• Check your internet connection\n• Try again in a few moments\n• Use manual mode if automatic processing fails"
    }
}
