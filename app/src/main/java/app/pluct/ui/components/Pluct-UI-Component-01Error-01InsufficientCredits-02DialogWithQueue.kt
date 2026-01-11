package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pluct-UI-Component-01Error-01InsufficientCredits-02DialogWithQueue
 * Enhanced dialog shown when user has insufficient credits with "Save for Later" option
 * Follows naming convention: [Project]-[Module]-[Component]-[Feature]-[Sequence][Responsibility]
 */
@Composable
fun PluctInsufficientCreditsDialogWithQueue(
    currentBalance: Int,
    requiredCredits: Int,
    url: String,
    queuedCount: Int = 0,
    onPurchase: () -> Unit,
    onQueueForLater: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AccountBalanceWallet, "Credits") },
        title = { Text("Insufficient Credits") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("You need $requiredCredits credits to continue.")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Current balance: $currentBalance credits")
                Text("Required: $requiredCredits credits")
                Spacer(modifier = Modifier.height(12.dp))
                
                // Queue suggestion
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Save this video for later? We'll process it automatically once you add credits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (queuedCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "You have $queuedCount video(s) already in queue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Note: Credits are held during processing and automatically refunded if transcription fails.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            Button(onClick = onPurchase) {
                Text("Add Credits Now")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onQueueForLater) {
                    Text("Save for Later")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}






