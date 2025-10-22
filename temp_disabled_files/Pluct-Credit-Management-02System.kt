package app.pluct.ui.components

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingTier
import kotlinx.coroutines.delay

/**
 * Pluct-Credit-Management-02System - Comprehensive credit management system
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Implements credit balance display, purchase options, and usage tracking
 */
@Composable
fun PluctCreditManagementSystem(
    currentBalance: Int,
    isLoading: Boolean,
    onPurchaseCredits: () -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("credit_management_card"),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    text = "Credits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    PluctCreditBalanceDisplay(
                        balance = currentBalance,
                        modifier = Modifier.testTag("credit_balance_display")
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Credit usage breakdown
            PluctCreditUsageBreakdown(
                balance = currentBalance,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewHistory,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("view_history_button")
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("History")
                }
                
                Button(
                    onClick = onPurchaseCredits,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("purchase_credits_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buy Credits")
                }
            }
        }
    }
}

@Composable
fun PluctCreditBalanceDisplay(
    balance: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = when {
            balance > 10 -> MaterialTheme.colorScheme.primary
            balance > 5 -> MaterialTheme.colorScheme.secondary
            balance > 0 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
        },
        modifier = modifier
    ) {
        Text(
            text = balance.toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun PluctCreditUsageBreakdown(
    balance: Int,
    modifier: Modifier = Modifier
) {
    val tiers = listOf(
        ProcessingTier.QUICK_SCAN to 1,
        ProcessingTier.DEEP_ANALYSIS to 3,
        ProcessingTier.PREMIUM_INSIGHTS to 5
    )
    
    Column(
        modifier = modifier
    ) {
        Text(
            text = "Credit Usage:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        tiers.forEach { (tier, cost) ->
            PluctCreditUsageItem(
                tier = tier.name,
                cost = cost,
                canAfford = balance >= cost,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (tier != tiers.last().first) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun PluctCreditUsageItem(
    tier: String,
    cost: Int,
    canAfford: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tier.replace("_", " "),
            style = MaterialTheme.typography.bodySmall,
            color = if (canAfford) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (canAfford) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }
        ) {
            Text(
                text = "$cost credits",
                style = MaterialTheme.typography.labelSmall,
                color = if (canAfford) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun PluctCreditPurchaseDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onPurchase: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Purchase Credits") },
            text = {
                Column {
                    Text("Choose a credit package:")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val packages = listOf(
                        10 to 5.99,
                        25 to 12.99,
                        50 to 19.99,
                        100 to 34.99
                    )
                    
                    packages.forEach { (credits, price) ->
                        PluctCreditPackageItem(
                            credits = credits,
                            price = price,
                            onSelect = { onPurchase(credits) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (credits != packages.last().first) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PluctCreditPackageItem(
    credits: Int,
    price: Double,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onSelect,
        modifier = modifier.testTag("credit_package_$credits"),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$credits Credits",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$${String.format("%.2f", price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
