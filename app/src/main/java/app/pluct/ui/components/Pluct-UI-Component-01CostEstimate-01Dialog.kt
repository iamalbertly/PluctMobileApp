package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pluct-UI-Component-01CostEstimate-01Dialog - Cost estimate confirmation dialog
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Shows estimated cost before transcription and requires user confirmation
 */
@Composable
fun PluctCostEstimateDialog(
    estimatedCost: Int,
    videoUrl: String,
    currentBalance: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Confirm Transcription",
                modifier = Modifier.semantics {
                    contentDescription = "Cost estimate dialog title"
                    testTag = "cost_estimate_dialog_title"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Cost estimate dialog content"
                        testTag = "cost_estimate_dialog_content"
                    }
            ) {
                Text(
                    text = "This video will cost approximately:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "$estimatedCost credit${if (estimatedCost != 1) "s" else ""}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Your current balance: $currentBalance credits",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (currentBalance < estimatedCost) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ You don't have enough credits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Do you want to continue?",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Note: Credits are held during processing and automatically refunded if transcription fails.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = currentBalance >= estimatedCost,
                modifier = Modifier.semantics {
                    contentDescription = "Confirm transcription button"
                    testTag = "cost_estimate_confirm_button"
                }
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.semantics {
                    contentDescription = "Cancel transcription button"
                    testTag = "cost_estimate_cancel_button"
                }
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.semantics {
            contentDescription = "Cost estimate confirmation dialog"
            testTag = "cost_estimate_dialog"
        }
    )
}
