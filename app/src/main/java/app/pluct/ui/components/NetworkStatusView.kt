package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pluct.utils.NetworkUtils
import app.pluct.utils.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Network status display component with intelligent connectivity detection
 */
@Composable
fun NetworkStatusView(
    context: android.content.Context,
    onRetry: () -> Unit,
    onManualMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var connectivityState by remember { mutableStateOf(NetworkUtils.ConnectivityState.UNKNOWN) }
    var networkQuality by remember { mutableStateOf(NetworkUtils.NetworkQuality.UNRELIABLE) }
    var isTesting by remember { mutableStateOf(false) }
    var lastTestTime by remember { mutableStateOf(0L) }
    
    val scope = rememberCoroutineScope()
    
    // Test network connectivity
    fun testConnectivity() {
        if (isTesting) return
        
        scope.launch {
            isTesting = true
            connectivityState = NetworkUtils.getConnectivityState(context)
            
            if (connectivityState != NetworkUtils.ConnectivityState.DISCONNECTED) {
                networkQuality = NetworkUtils.testServiceConnectivity()
            }
            
            lastTestTime = System.currentTimeMillis()
            isTesting = false
        }
    }
    
    // Auto-test on first load
    LaunchedEffect(Unit) {
        testConnectivity()
    }
    
    // Auto-retest every 30 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000)
            testConnectivity()
        }
    }
    
    val (icon, title, message, color, showRetry, showManual) = when {
        isTesting -> {
            NetworkStatusData(
                icon = Icons.Default.Refresh,
                title = "Testing Connection...",
                message = "Checking network and service availability",
                color = MaterialTheme.colorScheme.primary,
                showRetry = false,
                showManual = false
            )
        }
        connectivityState == NetworkUtils.ConnectivityState.DISCONNECTED -> {
            NetworkStatusData(
                icon = Icons.Default.WifiOff,
                title = "No Internet Connection",
                message = Constants.ErrorMessages.NO_INTERNET,
                color = MaterialTheme.colorScheme.error,
                showRetry = true,
                showManual = false
            )
        }
        connectivityState == NetworkUtils.ConnectivityState.CONNECTED_UNRELIABLE -> {
            NetworkStatusData(
                icon = Icons.Default.WifiOff,
                title = "Unstable Connection",
                message = "Your internet connection appears to be unstable",
                color = MaterialTheme.colorScheme.error,
                showRetry = true,
                showManual = true
            )
        }
        connectivityState == NetworkUtils.ConnectivityState.CONNECTED_SLOW -> {
            NetworkStatusData(
                icon = Icons.Default.Speed,
                title = "Slow Connection",
                message = Constants.ErrorMessages.SLOW_CONNECTION,
                color = MaterialTheme.colorScheme.tertiary,
                showRetry = true,
                showManual = true
            )
        }
        networkQuality == NetworkUtils.NetworkQuality.NONE -> {
            NetworkStatusData(
                icon = Icons.Default.CloudOff,
                title = "Service Unavailable",
                message = Constants.ErrorMessages.SERVICE_UNAVAILABLE,
                color = MaterialTheme.colorScheme.error,
                showRetry = true,
                showManual = true
            )
        }
        networkQuality == NetworkUtils.NetworkQuality.UNRELIABLE -> {
            NetworkStatusData(
                icon = Icons.Default.Warning,
                title = "Poor Connection",
                message = "Connection to transcript service is unreliable",
                color = MaterialTheme.colorScheme.tertiary,
                showRetry = true,
                showManual = true
            )
        }
        networkQuality == NetworkUtils.NetworkQuality.POOR -> {
            NetworkStatusData(
                icon = Icons.Default.Speed,
                title = "Slow Service",
                message = "Transcript service is responding slowly",
                color = MaterialTheme.colorScheme.tertiary,
                showRetry = true,
                showManual = true
            )
        }
        else -> {
            NetworkStatusData(
                icon = Icons.Default.Wifi,
                title = "Connection Good",
                message = "Network connection is available",
                color = MaterialTheme.colorScheme.primary,
                showRetry = false,
                showManual = false
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
            
            // Message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            // Action buttons
            if (showRetry || showManual) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showRetry) {
                        Button(
                            onClick = {
                                testConnectivity()
                                onRetry()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = color
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry")
                        }
                    }
                    
                    if (showManual) {
                        OutlinedButton(
                            onClick = onManualMode,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manual Mode")
                        }
                    }
                }
            }
            
            // Last test time
            if (lastTestTime > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last checked: ${formatTimeAgo(lastTestTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Data class for network status information
 */
private data class NetworkStatusData(
    val icon: ImageVector,
    val title: String,
    val message: String,
    val color: androidx.compose.ui.graphics.Color,
    val showRetry: Boolean,
    val showManual: Boolean
)

/**
 * Format time ago for display
 */
private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}
