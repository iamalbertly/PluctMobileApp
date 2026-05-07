package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.R

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
    val cdTitle = stringResource(R.string.cd_cost_estimate_dialog_title)
    val cdContent = stringResource(R.string.cd_cost_estimate_dialog_content)
    val cdConfirm = stringResource(R.string.cd_cost_estimate_confirm_button)
    val cdCancelBtn = stringResource(R.string.cd_cost_estimate_cancel_button)
    val cdDialog = stringResource(R.string.cd_cost_estimate_dialog)
    val creditLine = pluralStringResource(
        R.plurals.cost_estimate_credit_line,
        estimatedCost,
        estimatedCost
    )

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = stringResource(R.string.cost_estimate_confirm_title),
                modifier = Modifier.semantics {
                    contentDescription = cdTitle
                    testTag = "cost_estimate_dialog_title"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = cdContent
                        testTag = "cost_estimate_dialog_content"
                    }
            ) {
                Text(
                    text = stringResource(R.string.cost_estimate_approx),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = creditLine,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.cost_estimate_balance, currentBalance),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (currentBalance < estimatedCost) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.cost_estimate_not_enough),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.cost_estimate_continue_question),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.cost_estimate_note_refund),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = currentBalance >= estimatedCost,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = cdConfirm
                        testTag = "cost_estimate_confirm_button"
                    }
            ) {
                Text(stringResource(R.string.cost_estimate_continue))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.semantics {
                    contentDescription = cdCancelBtn
                    testTag = "cost_estimate_cancel_button"
                }
            ) {
                Text(stringResource(R.string.cost_estimate_cancel))
            }
        },
        modifier = Modifier.semantics {
            contentDescription = cdDialog
            testTag = "cost_estimate_dialog"
        }
    )
}
