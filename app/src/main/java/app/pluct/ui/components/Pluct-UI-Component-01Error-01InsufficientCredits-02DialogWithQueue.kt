package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import app.pluct.R

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
    val title = stringResource(R.string.insufficient_credits_title)
    val cdWallet = stringResource(R.string.cd_insufficient_credits_wallet_icon)
    val cdSchedule = stringResource(R.string.cd_insufficient_credits_schedule_icon)
    val cdDialog = stringResource(R.string.cd_insufficient_credits_dialog_queue)
    val cdAddCredits = stringResource(R.string.cd_insufficient_credits_add_credits)
    val cdSaveLater = stringResource(R.string.cd_insufficient_credits_save_for_later)
    val cdCancel = stringResource(R.string.cd_insufficient_credits_cancel)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.semantics {
            contentDescription = cdDialog
            testTag = "insufficient_credits_dialog_queue"
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.insufficient_credits_need, requiredCredits))
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.insufficient_credits_current_balance, currentBalance))
                Text(stringResource(R.string.insufficient_credits_required, requiredCredits))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = cdSchedule,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        stringResource(R.string.insufficient_credits_queue_suggestion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (queuedCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        pluralStringResource(
                            R.plurals.insufficient_credits_queued_videos,
                            queuedCount,
                            queuedCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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
                        contentDescription = cdAddCredits
                        testTag = "insufficient_credits_add_credits_button"
                    }
            ) {
                Text(stringResource(R.string.insufficient_credits_add_credits_now))
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onQueueForLater,
                    modifier = Modifier.semantics {
                        contentDescription = cdSaveLater
                        testTag = "insufficient_credits_save_later_button"
                    }
                ) {
                    Text(stringResource(R.string.insufficient_credits_save_for_later))
                }
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
        }
    )
}
