package app.pluct.ui.error

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pluct.core.error.ErrorEnvelope
import app.pluct.core.log.PluctLogger
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pluct-UI-Error-01BannerSystem - Modern error notification system
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Features: Stacking banners, detailed modals, modern design
 */
@Composable
fun PluctErrorBannerSystem(
    errors: List<ErrorEnvelope>,
    onDismiss: (Int) -> Unit,
    onShowDetails: (ErrorEnvelope) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        errors.forEachIndexed { index, error ->
            PluctErrorBannerItem(
                error = error,
                index = index,
                onDismiss = { onDismiss(index) },
                onShowDetails = { onShowDetails(error) }
            )
        }
    }
}

@Composable
private fun PluctErrorBannerItem(
    error: ErrorEnvelope,
    index: Int,
    onDismiss: () -> Unit,
    onShowDetails: () -> Unit
) {
    val backgroundColor = when (error.code) {
        "AUTH_401", "SCOPE_403" -> MaterialTheme.colorScheme.errorContainer
        "CREDITS_402" -> Color(0xFFFF9800).copy(alpha = 0.1f)
        "RATE_429" -> Color(0xFF9C27B0).copy(alpha = 0.1f)
        "SERVER_500" -> MaterialTheme.colorScheme.errorContainer
        "NET_TIMEOUT", "NET_IO" -> Color(0xFF2196F3).copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.errorContainer
    }

    val iconColor = when (error.code) {
        "AUTH_401", "SCOPE_403" -> MaterialTheme.colorScheme.error
        "CREDITS_402" -> Color(0xFFFF9800)
        "RATE_429" -> Color(0xFF9C27B0)
        "SERVER_500" -> MaterialTheme.colorScheme.error
        "NET_TIMEOUT", "NET_IO" -> Color(0xFF2196F3)
        else -> MaterialTheme.colorScheme.error
    }

    val icon = when (error.code) {
        "AUTH_401", "SCOPE_403" -> Icons.Default.Lock
        "CREDITS_402" -> Icons.Default.AttachMoney
        "RATE_429" -> Icons.Default.Speed
        "SERVER_500" -> Icons.Default.Error
        "NET_TIMEOUT", "NET_IO" -> Icons.Default.WifiOff
        else -> Icons.Default.Warning
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("error_banner")
                .semantics { contentDescription = "error_code:${error.code}" }
                .clickable { onShowDetails() },
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = getErrorTitle(error.code),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    Text(
                        text = error.message,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = getErrorTimestamp(),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
                    )
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss error",
                        tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PluctErrorDetailsModal(
    error: ErrorEnvelope,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (error.code) {
                            "AUTH_401", "SCOPE_403" -> Icons.Default.Lock
                            "CREDITS_402" -> Icons.Default.AttachMoney
                            "RATE_429" -> Icons.Default.Speed
                            "SERVER_500" -> Icons.Default.Error
                            "NET_TIMEOUT", "NET_IO" -> Icons.Default.WifiOff
                            else -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getErrorTitle(error.code),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Error Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = error.message,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Error Code: ${error.code}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Text(
                        text = "Source: ${error.source}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    if (error.requestId != null) {
                        Text(
                            text = "Request ID: ${error.requestId}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    if (error.details.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Additional Details:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        error.details.forEach { (key, value) ->
                            Text(
                                text = "$key: $value",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Time: ${getErrorTimestamp()}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

private fun getErrorTitle(code: String): String {
    return when (code) {
        "AUTH_401" -> "Authentication Failed"
        "CREDITS_402" -> "Insufficient Credits"
        "SCOPE_403" -> "Access Denied"
        "RATE_429" -> "Rate Limit Exceeded"
        "SERVER_500" -> "Server Error"
        "NET_TIMEOUT" -> "Connection Timeout"
        "NET_IO" -> "Network Error"
        "VALIDATION_ERROR" -> "Validation Error"
        "API_ERROR" -> "API Error"
        else -> "Error"
    }
}

private fun getErrorTimestamp(): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date())
}
