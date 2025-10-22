package app.pluct.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * Error notification manager for handling error notifications
 * Simple implementation for testing purposes
 */
class ErrorNotificationManager {
    private val _notifications = MutableStateFlow<List<PluctNotification>>(emptyList())
    val notifications: StateFlow<List<PluctNotification>> = _notifications.asStateFlow()
    
    fun showNetworkError() {
        val notification = PluctNotification(
            id = UUID.randomUUID().toString(),
            type = NotificationType.ERROR,
            title = "Network Error",
            message = "Unable to connect to the server. Please check your internet connection.",
            icon = androidx.compose.material.icons.Icons.Default.WifiOff
        )
        addNotification(notification)
    }
    
    fun showValidationError(message: String) {
        val notification = PluctNotification(
            id = UUID.randomUUID().toString(),
            type = NotificationType.ERROR,
            title = "Validation Error",
            message = message,
            icon = androidx.compose.material.icons.Icons.Default.Warning
        )
        addNotification(notification)
    }
    
    fun showApiError(message: String) {
        val notification = PluctNotification(
            id = UUID.randomUUID().toString(),
            type = NotificationType.ERROR,
            title = "API Error",
            message = message,
            icon = androidx.compose.material.icons.Icons.Default.Error
        )
        addNotification(notification)
    }
    
    fun showTimeoutError() {
        val notification = PluctNotification(
            id = UUID.randomUUID().toString(),
            type = NotificationType.ERROR,
            title = "Timeout Error",
            message = "The request timed out. Please try again.",
            icon = androidx.compose.material.icons.Icons.Default.Schedule
        )
        addNotification(notification)
    }
    
    private fun addNotification(notification: PluctNotification) {
        val current = _notifications.value.toMutableList()
        current.add(notification)
        _notifications.value = current
    }
    
    fun dismissNotification(id: String) {
        val current = _notifications.value.toMutableList()
        current.removeAll { it.id == id }
        _notifications.value = current
    }
    
    fun clearAll() {
        _notifications.value = emptyList()
    }
}
