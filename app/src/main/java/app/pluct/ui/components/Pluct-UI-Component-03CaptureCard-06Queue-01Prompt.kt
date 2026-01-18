package app.pluct.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.QueueReason
import app.pluct.services.PluctCoreValidationInputSanitizer

/**
 * Pluct-UI-Component-03CaptureCard-06Queue-01Prompt
 * Follows naming convention: [Project]-[UI]-[Component]-[CaptureCard]-[Queue]-[Sequence][Prompt]
 * 6 scope layers: Project, UI, Component, CaptureCard, Queue, Sequence, Prompt
 * Single source of truth for queue prompt UI
 */
@Composable
fun PluctUIComponent03CaptureCard06Queue01Prompt(
    showQueuePrompt: Boolean,
    queuePromptReason: String?,
    urlText: String,
    isSubmitting: Boolean,
    onQueueForLater: ((String, QueueReason) -> Unit)?,
    onRequestCredits: (() -> Unit)?,
    onShowGetCoinsDialog: () -> Unit,
    sanitizer: PluctCoreValidationInputSanitizer
) {
    if (!showQueuePrompt || isSubmitting || urlText.isBlank()) {
        return
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        queuePromptReason?.contains("internet", ignoreCase = true) == true -> Icons.Default.WifiOff
                        queuePromptReason?.contains("credits", ignoreCase = true) == true -> Icons.Default.AccountBalanceWallet
                        else -> Icons.Filled.Info
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = queuePromptReason ?: "Cannot process now",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val normalizedUrl = if (urlText.isNotBlank()) {
                            val validationResult = sanitizer.validateUrl(urlText)
                            if (validationResult.isValid) validationResult.sanitizedValue else urlText
                        } else ""
                        
                        if (normalizedUrl.isNotBlank() && onQueueForLater != null) {
                            val reason = when {
                                queuePromptReason?.contains("internet", ignoreCase = true) == true -> 
                                    QueueReason.NO_INTERNET
                                queuePromptReason?.contains("credits", ignoreCase = true) == true -> 
                                    QueueReason.INSUFFICIENT_CREDITS
                                else -> QueueReason.SERVICE_UNAVAILABLE
                            }
                            onQueueForLater(normalizedUrl, reason)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save for Later")
                }
                if (queuePromptReason?.contains("credits", ignoreCase = true) == true && onRequestCredits != null) {
                    OutlinedButton(
                        onClick = onShowGetCoinsDialog,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Credits")
                    }
                }
            }
        }
    }
}
