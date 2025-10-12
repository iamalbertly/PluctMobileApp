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
 * Pluct WebView Error Handler - Core error handling component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Composable
fun PluctWebViewErrorHandler(
    errorCode: String?,
    errorMessage: String?,
    urlUsed: String?,
    onRetry: () -> Unit,
    onManualMode: () -> Unit,
    onReturnToMain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val errorData = PluctWebViewErrorDataProvider.getErrorData(errorCode)
    
    PluctWebViewErrorDisplay(
        errorData = errorData,
        errorMessage = errorMessage,
        urlUsed = urlUsed,
        onRetry = onRetry,
        onManualMode = onManualMode,
        onReturnToMain = onReturnToMain,
        modifier = modifier
    )
}

@Composable
private fun PluctWebViewErrorDisplay(
    errorData: PluctWebViewErrorData,
    errorMessage: String?,
    urlUsed: String?,
    onRetry: () -> Unit,
    onManualMode: () -> Unit,
    onReturnToMain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = errorData.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error icon
            Icon(
                imageVector = errorData.icon,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = errorData.color
            )
            
            // Error title
            Text(
                text = errorData.title,
                style = MaterialTheme.typography.headlineSmall,
                color = errorData.color,
                textAlign = TextAlign.Center
            )
            
            // Error message
            Text(
                text = errorData.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            // Additional error details
            if (!errorMessage.isNullOrBlank()) {
                PluctWebViewErrorDetails(
                    errorMessage = errorMessage,
                    urlUsed = urlUsed
                )
            }
            
            // Action buttons
            PluctWebViewErrorActions(
                errorData = errorData,
                onRetry = onRetry,
                onManualMode = onManualMode,
                onReturnToMain = onReturnToMain
            )
        }
    }
}

@Composable
private fun PluctWebViewErrorDetails(
    errorMessage: String,
    urlUsed: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Error Details:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (!urlUsed.isNullOrBlank()) {
                Text(
                    text = "URL: $urlUsed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PluctWebViewErrorActions(
    errorData: PluctWebViewErrorData,
    onRetry: () -> Unit,
    onManualMode: () -> Unit,
    onReturnToMain: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (errorData.showRetry) {
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
        
        if (errorData.showManual) {
            OutlinedButton(
                onClick = onManualMode,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Manual Mode",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manual Mode")
            }
        }
        
        if (errorData.showReturn) {
            TextButton(
                onClick = onReturnToMain,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Return to Main",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Return to Main")
            }
        }
    }
}

/**
 * Error data provider for WebView errors
 */
object PluctWebViewErrorDataProvider {
    fun getErrorData(errorCode: String?): PluctWebViewErrorData {
        return when (errorCode) {
            "timeout" -> PluctWebViewErrorData(
                icon = Icons.Default.Schedule,
                title = "Processing Timeout",
                message = Constants.ErrorMessages.TIMEOUT_ERROR,
                color = MaterialTheme.colorScheme.tertiary,
                showRetry = true,
                showManual = true,
                showReturn = true
            )
            "network_error", "webview_error" -> PluctWebViewErrorData(
                icon = Icons.Default.WifiOff,
                title = "Network Error",
                message = Constants.ErrorMessages.NETWORK_ERROR,
                color = MaterialTheme.colorScheme.error,
                showRetry = true,
                showManual = true,
                showReturn = true
            )
            "transcript_not_found" -> PluctWebViewErrorData(
                icon = Icons.Default.Description,
                title = "Transcript Not Found",
                message = Constants.ErrorMessages.TRANSCRIPT_NOT_FOUND,
                color = MaterialTheme.colorScheme.secondary,
                showRetry = false,
                showManual = true,
                showReturn = true
            )
            "provider_error" -> PluctWebViewErrorData(
                icon = Icons.Default.Error,
                title = "Provider Error",
                message = Constants.ErrorMessages.PROVIDER_ERROR,
                color = MaterialTheme.colorScheme.error,
                showRetry = true,
                showManual = true,
                showReturn = true
            )
            else -> PluctWebViewErrorData(
                icon = Icons.Default.Warning,
                title = "Unknown Error",
                message = Constants.ErrorMessages.UNKNOWN_ERROR,
                color = MaterialTheme.colorScheme.onSurface,
                showRetry = true,
                showManual = true,
                showReturn = true
            )
        }
    }
}

data class PluctWebViewErrorData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val message: String,
    val color: androidx.compose.ui.graphics.Color,
    val showRetry: Boolean,
    val showManual: Boolean,
    val showReturn: Boolean
)
