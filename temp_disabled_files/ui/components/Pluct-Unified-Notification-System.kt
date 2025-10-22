package app.pluct.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Unified notification system for Pluct app
 * Handles all notifications, progress updates, and status messages
 */
@Composable
fun PluctUnifiedNotificationSystem(
    notifications: List<PluctNotification>,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (notifications.isNotEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            notifications.forEach { notification ->
                PluctNotificationItem(
                    notification = notification,
                    onDismiss = { onDismiss(notification.id) }
                )
            }
        }
    }
}

@Composable
fun PluctNotificationItem(
    notification: PluctNotification,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.8f)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.8f)
        ) + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (notification.type) {
                    NotificationType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                    NotificationType.ERROR -> MaterialTheme.colorScheme.errorContainer
                    NotificationType.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                    NotificationType.INFO -> MaterialTheme.colorScheme.secondaryContainer
                    NotificationType.PROGRESS -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Icon(
                    imageVector = notification.icon,
                    contentDescription = null,
                    tint = when (notification.type) {
                        NotificationType.SUCCESS -> MaterialTheme.colorScheme.primary
                        NotificationType.ERROR -> MaterialTheme.colorScheme.error
                        NotificationType.WARNING -> MaterialTheme.colorScheme.tertiary
                        NotificationType.INFO -> MaterialTheme.colorScheme.secondary
                        NotificationType.PROGRESS -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = when (notification.type) {
                            NotificationType.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
                            NotificationType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                            NotificationType.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                            NotificationType.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
                            NotificationType.PROGRESS -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    if (notification.message.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notification.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (notification.type) {
                                NotificationType.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                NotificationType.ERROR -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                NotificationType.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                NotificationType.INFO -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                NotificationType.PROGRESS -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            }
                        )
                    }
                    
                    // Progress bar for progress notifications
                    if (notification.type == NotificationType.PROGRESS && notification.progress != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = notification.progress / 100f,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    }
                }
                
                // Dismiss button
                IconButton(
                    onClick = {
                        isVisible = false
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = when (notification.type) {
                            NotificationType.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            NotificationType.ERROR -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
                            NotificationType.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                            NotificationType.INFO -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                            NotificationType.PROGRESS -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

data class PluctNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String = "",
    val progress: Int? = null,
    val icon: ImageVector = Icons.Default.Info,
    val autoDismiss: Boolean = true,
    val duration: Long = 5000L
)

enum class NotificationType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO,
    PROGRESS
}

/**
 * Notification manager for handling notifications state
 */
@Composable
fun rememberNotificationManager(): NotificationManager {
    return remember { NotificationManager() }
}

class NotificationManager {
    private val _notifications = mutableStateListOf<PluctNotification>()
    val notifications: List<PluctNotification> = _notifications
    
    fun showNotification(notification: PluctNotification) {
        _notifications.add(notification)
        
        if (notification.autoDismiss) {
            // Auto-dismiss after duration
            // This would need to be handled in a coroutine scope
        }
    }
    
    fun dismissNotification(id: String) {
        _notifications.removeAll { it.id == id }
    }
    
    fun clearAll() {
        _notifications.clear()
    }
    
    fun showSuccess(title: String, message: String = "") {
        showNotification(
            PluctNotification(
                id = generateId(),
                type = NotificationType.SUCCESS,
                title = title,
                message = message,
                icon = Icons.Default.CheckCircle
            )
        )
    }
    
    fun showError(title: String, message: String = "") {
        showNotification(
            PluctNotification(
                id = generateId(),
                type = NotificationType.ERROR,
                title = title,
                message = message,
                icon = Icons.Default.Error
            )
        )
    }
    
    fun showProgress(title: String, message: String = "", progress: Int = 0) {
        showNotification(
            PluctNotification(
                id = generateId(),
                type = NotificationType.PROGRESS,
                title = title,
                message = message,
                progress = progress,
                icon = Icons.Default.HourglassEmpty,
                autoDismiss = false
            )
        )
    }
    
    fun showInfo(title: String, message: String = "") {
        showNotification(
            PluctNotification(
                id = generateId(),
                type = NotificationType.INFO,
                title = title,
                message = message,
                icon = Icons.Default.Info
            )
        )
    }
    
    private fun generateId(): String = System.currentTimeMillis().toString()
}
