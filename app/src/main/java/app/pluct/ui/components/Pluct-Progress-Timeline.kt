package app.pluct.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pluct-Progress-Timeline - Live progress timeline component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Shows real-time progress through Business Engine stages
 */
@Composable
fun PluctProgressTimeline(
    currentStage: String,
    progress: Float = 0f,
    modifier: Modifier = Modifier
) {
    val steps = listOf("Token", "Transcribe", "Summarize", "Done")
    val currentStageIndex = when (currentStage) {
        "TOKEN" -> 0
        "TRANSCRIBE" -> 1
        "SUMMARIZE" -> 2
        "COMPLETE" -> 3
        else -> -1
    }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val isActive = index <= currentStageIndex
            val isCurrent = index == currentStageIndex
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stage indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            color = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Stage label
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                )
            }
            
            // Connector line (except for last item)
            if (index < steps.size - 1) {
                Spacer(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .background(
                            color = if (index < currentStageIndex) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}
