package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import app.pluct.R

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
    val title = stringResource(R.string.insufficient_credits_title)
    val cdWallet = stringResource(R.string.cd_insufficient_credits_wallet_icon)
    val cdDialog = stringResource(R.string.cd_insufficient_credits_dialog)
    val cdPurchase = stringResource(R.string.cd_insufficient_credits_purchase)
    val cdCancel = stringResource(R.string.cd_insufficient_credits_cancel)
    val needMore = (requiredCredits - currentBalance).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.semantics {
            contentDescription = cdDialog
            testTag = "insufficient_credits_dialog"
        },
        icon = {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = cdWallet
            )
        },
        title = {
            Text(text = title)
        },
        text = {
            Column {
                Text(stringResource(R.string.insufficient_credits_need, requiredCredits))
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.insufficient_credits_current_balance, currentBalance))
                Text(stringResource(R.string.insufficient_credits_required, requiredCredits))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.insufficient_credits_purchase_more, needMore),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.insufficient_credits_note_refund),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onPurchase,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = cdPurchase
                        testTag = "insufficient_credits_purchase_button"
                    }
            ) {
                Text(stringResource(R.string.insufficient_credits_purchase))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = cdCancel
                    testTag = "insufficient_credits_cancel_button"
                }
            ) {
                Text(stringResource(R.string.insufficient_credits_cancel))
            }
        }
    )
}
