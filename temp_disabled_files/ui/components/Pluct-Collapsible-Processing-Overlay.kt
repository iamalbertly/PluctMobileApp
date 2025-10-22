package app.pluct.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Pluct-Collapsible-Processing-Overlay - Collapsible background processing overlay
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Replaces blocking modal with collapsible bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctCollapsibleProcessingOverlay(
    currentStage: String,
    progress: Float,
    onMinimize: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    // Auto-collapse after 3 seconds if not actively processing
    LaunchedEffect(currentStage) {
        if (currentStage == "IDLE" || currentStage == "COMPLETE") {
            delay(3000)
            isExpanded = false
        }
    }
    
    // Mini progress card when collapsed
    if (!isExpanded) {
        MiniProgressCard(
            currentStage = currentStage,
            progress = progress,
            onExpand = { isExpanded = true },
            modifier = modifier
        )
    } else {
        // Full bottom sheet when expanded
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header with minimize button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Processing Video",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row {
                        IconButton(onClick = onMinimize) {
                            Icon(
                                Icons.Default.Minimize,
                                contentDescription = "Minimize",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onCancel) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress timeline
                PluctProgressTimeline(
                    currentStage = currentStage,
                    progress = progress
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Status text
                Text(
                    text = when (currentStage) {
                        "TOKEN" -> "Getting processing token..."
                        "TRANSCRIBE" -> "Transcribing video audio..."
                        "SUMMARIZE" -> "Generating AI summary..."
                        "COMPLETE" -> "Processing complete!"
                        else -> "Preparing..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onMinimize,
                        colors = ButtonDefaults.textButtonColors()
                    ) {
                        Text("Hide to background")
                    }
                    
                    if (currentStage != "COMPLETE") {
                        TextButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniProgressCard(
    currentStage: String,
    progress: Float,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onExpand() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
                
                Text(
                    text = when (currentStage) {
                        "TOKEN" -> "Getting token..."
                        "TRANSCRIBE" -> "Transcribing..."
                        "SUMMARIZE" -> "Summarizing..."
                        "COMPLETE" -> "Complete!"
                        else -> "Processing..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Icon(
                Icons.Default.ExpandLess,
                contentDescription = "Expand",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
