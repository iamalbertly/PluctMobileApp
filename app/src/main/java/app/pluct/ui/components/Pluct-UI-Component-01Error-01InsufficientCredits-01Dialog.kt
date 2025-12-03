package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pluct-UI-Component-01Error-01InsufficientCredits-01Dialog
 * Dialog shown when user has insufficient credits (402 Payment Required)
 * Follows naming convention: [Project]-[Module]-[Component]-[Feature]-[Sequence][Responsibility]
 */
@Composable
fun PluctInsufficientCreditsDialog(
    currentBalance: Int,
    requiredCredits: Int,
    onPurchase: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AccountBalanceWallet, "Credits") },
        title = { Text("Insufficient Credits") },
        text = {
            Column {
                Text("You need $requiredCredits credits to continue.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Current balance: $currentBalance credits")
                Text("Required: $requiredCredits credits")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Purchase ${requiredCredits - currentBalance} more credits to transcribe this video.",
                    style = MaterialTheme.typography.bodySmall
                )
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
                Text("Purchase Credits")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
