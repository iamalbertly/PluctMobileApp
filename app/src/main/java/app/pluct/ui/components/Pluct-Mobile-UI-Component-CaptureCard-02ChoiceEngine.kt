package app.pluct.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    onInsightsClick: () -> Unit,
    submittingLabel: String = "Starting...",
    submittingHint: String = "Please wait"
) {
    val isExtractEnabled = urlText.isNotBlank() && (freeUsesRemaining > 0 || creditBalance > 0)
    val buttonText = if (isSubmitting) submittingLabel else "Extract Script"
    val subText = when {
        isSubmitting -> submittingHint
        freeUsesRemaining > 0 -> "Free (uses left: $freeUsesRemaining)"
        creditBalance > 0 -> "1 Credit (balance: $creditBalance)"
        else -> "Get Credits"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Processing options" },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Primary Unified Action Button
        Button(
            onClick = {
                if (freeUsesRemaining == 0 && creditBalance == 0) {
                    onGetCoins()
                } else {
                    onTierSubmit()
                }
            },
            enabled = urlText.isNotBlank() && !isSubmitting,
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Extract Script option"
                    testTag = "extract_script_button"
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (freeUsesRemaining == 0 && creditBalance == 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (isSubmitting) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier
                        .height(24.dp)
                        .width(24.dp)
                        .padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = subText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }
        }
    }
}
