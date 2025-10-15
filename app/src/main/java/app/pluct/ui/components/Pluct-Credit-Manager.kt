package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.utils.BusinessEngineCreditManager
import kotlinx.coroutines.launch

/**
 * Credit Management UI Component
 * Allows users to view and purchase credits
 */
@Composable
fun PluctCreditManager(
    userId: String = "mobile",
    onCreditsUpdated: (Int) -> Unit = {}
) {
    var credits by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var purchaseAmount by remember { mutableStateOf(10) }
    val scope = rememberCoroutineScope()

    // Load initial credits
    LaunchedEffect(userId) {
        scope.launch {
            isLoading = true
            val userCredits = BusinessEngineCreditManager.getUserCredits(userId)
            credits = userCredits ?: 0
            isLoading = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = "Credits",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Credits",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Credits Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "$credits",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "credits available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Purchase Button
            Button(
                onClick = { showPurchaseDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Credits",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Purchase Credits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // Info Text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Info",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Credits are used for AI-powered video analysis",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Purchase Dialog
    if (showPurchaseDialog) {
        PluctCreditPurchaseDialog(
            onDismiss = { showPurchaseDialog = false },
            onPurchase = { amount ->
                scope.launch {
                    isLoading = true
                    val success = BusinessEngineCreditManager.purchaseCredits(userId, amount)
                    if (success) {
                        credits += amount
                        onCreditsUpdated(credits)
                    }
                    isLoading = false
                    showPurchaseDialog = false
                }
            },
            initialAmount = purchaseAmount
        )
    }
}

@Composable
private fun PluctCreditPurchaseDialog(
    onDismiss: () -> Unit,
    onPurchase: (Int) -> Unit,
    initialAmount: Int = 10
) {
    var selectedAmount by remember { mutableStateOf(initialAmount) }
    val creditOptions = listOf(5, 10, 25, 50, 100)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Purchase Credits",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select the number of credits to purchase:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Credit amount options
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(creditOptions.size) { index ->
                        val amount = creditOptions[index]
                        Button(
                            onClick = { selectedAmount = amount },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAmount == amount) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text("$amount")
                        }
                    }
                }
                
                Text(
                    text = "Selected: $selectedAmount credits",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPurchase(selectedAmount) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Purchase")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
