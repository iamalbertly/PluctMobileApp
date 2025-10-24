package app.pluct.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingTier

/**
 * Pluct-UI-Screen-03QuickScan - QuickScan tier selection interface
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctQuickScanSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onTierSelected: (ProcessingTier) -> Unit,
    selectedTier: ProcessingTier = ProcessingTier.STANDARD
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                android.util.Log.d("PluctQuickScanSheet", "QuickScan sheet dismissed")
                onDismiss()
            },
            modifier = Modifier.testTag("quickscan_sheet"),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.shapes.large
                    )
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose Processing Tier",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("quickscan_title")
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Select how you'd like to process this video",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("quickscan_description")
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Tier Selection Cards
                PluctTierSelectionCard(
                    tier = ProcessingTier.STANDARD,
                    title = "Quick Scan",
                    description = "Fast transcript generation",
                    icon = Icons.Default.FlashOn,
                    isSelected = selectedTier == ProcessingTier.STANDARD,
                    onClick = { onTierSelected(ProcessingTier.STANDARD) },
                    modifier = Modifier.testTag("quickscan_tier_card")
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                PluctTierSelectionCard(
                    tier = ProcessingTier.PREMIUM,
                    title = "Standard",
                    description = "Balanced processing with analysis",
                    icon = Icons.Default.Speed,
                    isSelected = selectedTier == ProcessingTier.PREMIUM,
                    onClick = { onTierSelected(ProcessingTier.PREMIUM) },
                    modifier = Modifier.testTag("standard_tier_card")
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                PluctTierSelectionCard(
                    tier = ProcessingTier.AI_ANALYSIS,
                    title = "Premium",
                    description = "Full AI analysis and insights",
                    icon = Icons.Default.Star,
                    isSelected = selectedTier == ProcessingTier.AI_ANALYSIS,
                    onClick = { onTierSelected(ProcessingTier.AI_ANALYSIS) },
                    modifier = Modifier.testTag("premium_tier_card")
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Process Button
                Button(
                    onClick = {
                        android.util.Log.d("PluctQuickScanSheet", "Process button clicked with tier: $selectedTier")
                        onTierSelected(selectedTier)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("process_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        "Start Processing",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp)) // For bottom sheet peek
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctTierSelectionCard(
    tier: ProcessingTier,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Card(
        onClick = {
            android.util.Log.d("PluctTierSelectionCard", "Tier card clicked: $tier")
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag("tier_card_${tier.name.lowercase()}"),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Selection indicator
            if (isSelected) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
