package app.pluct.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pluct-Status-RealTime - Real-time status updates for transcription progress
 * Single source of truth for status updates
 * Adheres to 300-line limit with smart separation of concerns
 */

data class StatusUpdate(
    val id: String,
    val message: String,
    val progress: Int = 0,
    val stage: String = "IDLE",
    val timestamp: Long = System.currentTimeMillis()
)

class StatusUpdateManager {
    private val _currentStatus = MutableStateFlow<StatusUpdate?>(null)
    val currentStatus: StateFlow<StatusUpdate?> = _currentStatus.asStateFlow()
    
    private val _statusHistory = MutableStateFlow<List<StatusUpdate>>(emptyList())
    val statusHistory: StateFlow<List<StatusUpdate>> = _statusHistory.asStateFlow()

    fun updateStatus(message: String, progress: Int = 0, stage: String = "PROCESSING") {
        val status = StatusUpdate(
            id = System.currentTimeMillis().toString(),
            message = message,
            progress = progress,
            stage = stage
        )
        
        _currentStatus.value = status
        _statusHistory.value = _statusHistory.value + status
    }

    fun clearStatus() {
        _currentStatus.value = null
    }

    fun getStatusHistory(): List<StatusUpdate> = _statusHistory.value
}

@Composable
fun rememberStatusUpdateManager(): StatusUpdateManager {
    return remember { StatusUpdateManager() }
}

@Composable
fun PluctStatusUpdateCard(
    status: StatusUpdate?,
    onDismiss: () -> Unit
) {
    status?.let { currentStatus ->
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(durationMillis = 300)),
            exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(durationMillis = 300))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (currentStatus.stage) {
                        "PROCESSING" -> Color(0xFF2196F3)
                        "COMPLETED" -> Color(0xFF4CAF50)
                        "FAILED" -> Color(0xFFF44336)
                        else -> Color(0xFF9E9E9E)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentStatus.message,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Text(
                            text = "${currentStatus.progress}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (currentStatus.progress > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = currentStatus.progress / 100f,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PluctStatusHistory(
    statusHistory: List<StatusUpdate>,
    modifier: Modifier = Modifier
) {
    if (statusHistory.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Status History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                statusHistory.takeLast(5).forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = status.message,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${status.progress}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

