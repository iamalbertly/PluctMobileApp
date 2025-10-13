package app.pluct.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem

/**
 * Pluct-Video-Card-Status - Video card status component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Composable
fun PluctVideoCardStatus(video: VideoItem) {
    val status = video.status ?: ProcessingStatus.PENDING
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = when (status) {
                        ProcessingStatus.COMPLETED -> Color.Green
                        ProcessingStatus.FAILED -> Color.Red
                        ProcessingStatus.TRANSCRIBING -> Color.Blue
                        ProcessingStatus.ANALYZING -> Color(0xFFFF9800)
                        else -> Color.Gray
                    },
                    shape = RoundedCornerShape(4.dp)
                )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Status text
        Text(
            text = when (status) {
                ProcessingStatus.COMPLETED -> "Completed"
                ProcessingStatus.FAILED -> "Failed"
                ProcessingStatus.TRANSCRIBING -> "Transcribing"
                ProcessingStatus.ANALYZING -> "Analyzing"
                else -> "Pending"
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = when (status) {
                ProcessingStatus.COMPLETED -> Color.Green
                ProcessingStatus.FAILED -> Color.Red
                ProcessingStatus.TRANSCRIBING -> Color.Blue
                ProcessingStatus.ANALYZING -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Progress indicator for active statuses
        if (status == ProcessingStatus.TRANSCRIBING || status == ProcessingStatus.ANALYZING) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
