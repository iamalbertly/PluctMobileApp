package app.pluct.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingTier

/**
 * Tier selection component for Pluct capture sheet
 */
@Composable
fun PluctCaptureTierSelection(
    onTierSelected: (ProcessingTier) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Scan Option
        PluctTierSelectionCard(
            title = "Quick Scan",
            subtitle = "Free • Fast transcript",
            icon = "⚡️",
            description = "Get a quick transcript in seconds",
            onClick = { 
                android.util.Log.d("PluctCaptureTierSelection", "Quick Scan selected")
                onTierSelected(ProcessingTier.QUICK_SCAN) 
            },
            isRecommended = false,
            modifier = Modifier.fillMaxWidth()
        )
        
        // AI Analysis Option
        PluctTierSelectionCard(
            title = "AI Analysis",
            subtitle = "Premium • Deep insights",
            icon = "✨",
            description = "AI-powered summary, key takeaways & actionable steps",
            onClick = { 
                android.util.Log.d("PluctCaptureTierSelection", "AI Analysis selected")
                onTierSelected(ProcessingTier.AI_ANALYSIS) 
            },
            isRecommended = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PluctTierSelectionCard(
    title: String,
    subtitle: String,
    icon: String,
    description: String,
    onClick: () -> Unit,
    isRecommended: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .clickable { 
                isPressed = true
                onClick()
            }
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isRecommended) 6.dp else 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 16.dp)
            )
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Recommended",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3
                )
            }
        }
    }
}
