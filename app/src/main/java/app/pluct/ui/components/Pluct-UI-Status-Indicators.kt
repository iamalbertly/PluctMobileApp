package app.pluct.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier

/**
 * Pluct-UI-Status-Indicators - Status and tier indicator components
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */

@Composable
fun PluctAnimatedStatusIndicator(
    status: ProcessingStatus,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (status) {
        ProcessingStatus.COMPLETED -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        ProcessingStatus.TRANSCRIBING -> Icons.Default.Refresh to MaterialTheme.colorScheme.secondary
        ProcessingStatus.ANALYZING -> Icons.Default.Refresh to MaterialTheme.colorScheme.secondary
        ProcessingStatus.PENDING -> Icons.Default.Schedule to MaterialTheme.colorScheme.outline
        ProcessingStatus.FAILED -> Icons.Default.Error to MaterialTheme.colorScheme.error
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "status_animation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = status.name,
            tint = color,
            modifier = Modifier
                .size(16.dp)
                .let { if (status == ProcessingStatus.TRANSCRIBING || status == ProcessingStatus.ANALYZING) it.rotate(rotation) else it }
        )
    }
}

@Composable
fun PluctStatusChip(status: ProcessingStatus) {
    val (text, color) = when (status) {
        ProcessingStatus.COMPLETED -> "Completed" to MaterialTheme.colorScheme.primary
        ProcessingStatus.TRANSCRIBING -> "Transcribing" to MaterialTheme.colorScheme.secondary
        ProcessingStatus.ANALYZING -> "Analyzing" to MaterialTheme.colorScheme.secondary
        ProcessingStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.outline
        ProcessingStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.height(24.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PluctTierChip(tier: ProcessingTier) {
    val (text, color) = when (tier) {
        ProcessingTier.QUICK_SCAN -> "Quick Scan" to MaterialTheme.colorScheme.secondary
        ProcessingTier.AI_ANALYSIS -> "AI Analysis" to MaterialTheme.colorScheme.primary
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.height(20.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
