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
@Suppress("UNUSED_PARAMETER") // onInsightsClick reserved for future use
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

    // UX IMPROVEMENT: Clearer button text based on state
    val buttonText = when {
        isSubmitting -> submittingLabel
        urlText.isBlank() -> "Paste a TikTok link to start"
        else -> "Extract Script"
    }

    // UX IMPROVEMENT: Friendly, informative subtext
    val subText = when {
        isSubmitting -> submittingHint
        urlText.isBlank() -> "Share from TikTok or paste a link above"
        freeUsesRemaining > 0 -> "Free - $freeUsesRemaining uses left"
        creditBalance > 0 -> "Uses 1 credit ($creditBalance available)"
        else -> "Tap to get more credits"
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
                android.util.Log.d("ChoiceEngine", "Extract Script button clicked: freeUses=$freeUsesRemaining, credits=$creditBalance, urlText='$urlText'")
                if (freeUsesRemaining == 0 && creditBalance == 0) {
                    android.util.Log.d("ChoiceEngine", "No credits, calling onGetCoins")
                    onGetCoins()
                } else {
                    android.util.Log.d("ChoiceEngine", "Has credits, calling onTierSubmit")
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
                // UX FIX: Better contrast for disabled state - higher alpha for readability
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                // UX FIX: Subtext color adapts to button state for proper contrast
                val isEnabled = urlText.isNotBlank() && !isSubmitting
                Text(
                    text = subText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    maxLines = 1
                )
            }
        }
    }
}
