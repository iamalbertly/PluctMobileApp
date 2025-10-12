package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pluct.data.entity.ProcessingTier
import app.pluct.purchase.CoinManager
import android.widget.Toast

@Composable
fun TierSelectionSection(
    onTierSelected: (ProcessingTier) -> Unit,
    coinManager: CoinManager
) {
    // Get current coin balance
    val coinBalance by coinManager.getCoinBalanceFlow().collectAsState(initial = 0)
    val canAffordAI = coinBalance >= 1
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Tier Selection
        Text(
            text = "Choose Your Analysis",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Free Tier Option
            TierOptionCard(
                modifier = Modifier.weight(1f),
                icon = "⚡️",
                title = "Quick Scan",
                description = "Free, automated transcript. Good for a quick look, may contain errors.",
                buttonText = "Scan for Free",
                isEnabled = true,
                onClick = { onTierSelected(ProcessingTier.QUICK_SCAN) }
            )
            
            // Premium Tier Option
            TierOptionCard(
                modifier = Modifier.weight(1f),
                icon = "✨",
                title = "Pluct AI Analysis",
                description = "High-accuracy transcript, AI summary, key takeaways, and actionable steps.",
                buttonText = if (canAffordAI) "Analyze (1 Pluct Coin)" else "Analyze (1 Pluct Coin) - Insufficient coins",
                isEnabled = canAffordAI,
                onClick = { 
                    if (canAffordAI) {
                        onTierSelected(ProcessingTier.AI_ANALYSIS)
                    } else {
                        Toast.makeText(LocalContext.current, "Insufficient coins. Get more to use AI Analysis.", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
        
        // Get more coins link (only show if no coins)
        if (!canAffordAI) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = { /* TODO: Open purchase screen */ },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "You have 0 coins. Get more.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TierOptionCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    description: String,
    buttonText: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Button
            Button(
                onClick = onClick,
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnabled) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(buttonText)
            }
        }
    }
}
