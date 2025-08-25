package app.pluct.ui.screens.ingest

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.pluct.viewmodel.IngestUiState

/**
 * Error handling component for the Ingest screen
 */
@Composable
fun IngestErrorView(
    uiState: IngestUiState,
    onClearError: () -> Unit,
    onRetryWebTranscript: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // General error display
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onClearError,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
        
        // Web transcript error display
        uiState.webErrorCode?.let { errorCode ->
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Auto-transcription Failed",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = getErrorMessage(errorCode, uiState.webErrorMessage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClearError,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Dismiss")
                        }
                        Button(
                            onClick = onRetryWebTranscript,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper function to get user-friendly error messages
 */
fun getErrorMessage(errorCode: String, customMessage: String?): String {
    return when (errorCode) {
        "timeout" -> "The transcript extraction timed out. Please try again or use manual entry."
        "invalid_url" -> "The provided URL is not valid or not supported. Please check the URL and try again."
        "blocked" -> "Access to the video was blocked. The video may be private or restricted."
        "ratelimited" -> "Too many requests. Please wait a moment and try again."
        "captcha" -> "A CAPTCHA was encountered. Please try again later."
        "webview_error" -> "A browser error occurred. Please try again or use manual entry."
        "automation_error" -> "The automation failed. Please try again or use manual entry."
        "save_error" -> "Failed to save the transcript. Please try again."
        "user_cancelled" -> "Transcript extraction was cancelled."
        else -> customMessage ?: "An unexpected error occurred. Please try again."
    }
}
