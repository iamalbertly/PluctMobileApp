package app.pluct.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pluct-Mobile-UI-Component-CaptureCard-02ChoiceEngine
 * Compact, modern choice engine with horizontal layout.
 */
@Composable
fun PluctChoiceEngine(
    urlText: String,
    freeUsesRemaining: Int,
    creditBalance: Int,
    isSubmitting: Boolean,
    onTierSubmit: () -> Unit,
    onGetCoins: () -> Unit,
    onInsightsClick: () -> Unit
) {
    val isExtractEnabled = urlText.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Processing options"
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                Log.d("ChoiceEngine", "Extract Script clicked")
                onTierSubmit()
            },
            enabled = isExtractEnabled && !isSubmitting,
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Extract Script"
                    testTag = "extract_script_button"
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Extract Script",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = if (freeUsesRemaining > 0) "Free (uses left: $freeUsesRemaining)" else "1 Coin (balance: $creditBalance)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Free uses: $freeUsesRemaining  •  Credits: $creditBalance",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FilledTonalButton(
                onClick = {
                    Log.d("ChoiceEngine", "AI Insights clicked")
                    if (creditBalance < 2) {
                        onGetCoins()
                    } else {
                        onInsightsClick()
                    }
                },
                enabled = urlText.isNotBlank() && !isSubmitting,
                modifier = Modifier
                    .height(44.dp)
                    .semantics {
                        contentDescription = "AI Insights option"
                        testTag = "generate_insights_button"
                    },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Insights",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                    Text(
                        text = if (creditBalance >= 2) "2 Coins" else "Get Coins",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
