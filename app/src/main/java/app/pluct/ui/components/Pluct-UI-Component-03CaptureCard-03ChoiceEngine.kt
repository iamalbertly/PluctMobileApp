package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingTier
import app.pluct.services.PluctCoreValidationInputSanitizer
import android.util.Log

/**
 * Pluct-UI-Component-03CaptureCard-03ChoiceEngine - Choice engine component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single source of truth for choice engine functionality
 */
@Composable
fun PluctChoiceEngine(
    urlText: String,
    freeUsesRemaining: Int,
    creditBalance: Int,
    sanitizer: PluctCoreValidationInputSanitizer,
    isSubmitting: Boolean,
    onTierSubmit: () -> Unit,
    onGetCoins: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Extract Script Card
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    enabled = urlText.isNotBlank() && (freeUsesRemaining > 0 || creditBalance >= 1)
                ) {
                    Log.d("ExtractScriptCard", "🚀 Card clicked! Calling onTierSubmit...")
                    onTierSubmit()
                }
                .semantics { 
                    contentDescription = "Extract Script option"
                    testTag = "extract_script_button"
                }
        ) {
            PluctExtractScriptCard(
                urlText = urlText,
                freeUsesRemaining = freeUsesRemaining,
                creditBalance = creditBalance,
                sanitizer = sanitizer,
                onTierSubmit = onTierSubmit
            )
        }
        
        // Generate Insights Card
        Card(
            modifier = Modifier
                .weight(1f)
                .semantics { 
                    contentDescription = "Generate Insights option"
                    testTag = "generate_insights_button"
                }
        ) {
            PluctGenerateInsightsCard(
                urlText = urlText,
                creditBalance = creditBalance,
                onTierSubmit = onTierSubmit,
                onGetCoins = onGetCoins
            )
        }
    }
}

@Composable
private fun PluctExtractScriptCard(
    urlText: String,
    freeUsesRemaining: Int,
    creditBalance: Int,
    sanitizer: PluctCoreValidationInputSanitizer,
    onTierSubmit: () -> Unit
) {
    val isEnabled = urlText.isNotBlank() && (freeUsesRemaining > 0 || creditBalance >= 1)
    
    // Debug logging for button state
    Log.d("ExtractScriptCard", "🔍 Button state check:")
    Log.d("ExtractScriptCard", "  - urlText: '$urlText' (blank: ${urlText.isBlank()})")
    Log.d("ExtractScriptCard", "  - freeUsesRemaining: $freeUsesRemaining")
    Log.d("ExtractScriptCard", "  - creditBalance: $creditBalance")
    Log.d("ExtractScriptCard", "  - isEnabled: $isEnabled")
    
    Column(
        modifier = Modifier
            .padding(16.dp)
            .semantics { 
                contentDescription = "Extract Script option"
                testTag = "extract_script_button"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📄",
            fontSize = 32.sp,
            modifier = Modifier.semantics { 
                contentDescription = "Document icon for script extraction" 
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Extract Script",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { 
                contentDescription = "Extract Script title" 
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Get the text transcript. Fast and free for your first 3 uses.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .height(64.dp)
                .semantics { 
                    contentDescription = "Extract Script description" 
                }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                Log.d("ExtractScriptCard", "🚀 Button onClick triggered! Calling onTierSubmit...")
                onTierSubmit()
            },
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { 
                    contentDescription = "Use free extraction button"
                    testTag = "extract_script_action_button"
                }
        ) {
            Text(
                text = if (freeUsesRemaining > 0) {
                    "FREE"
                } else {
                    "Use (1 Coin)"
                }
            )
        }
    }
}

@Composable
private fun PluctGenerateInsightsCard(
    urlText: String,
    creditBalance: Int,
    onTierSubmit: () -> Unit,
    onGetCoins: () -> Unit
) {
    val isEnabled = urlText.isNotBlank() && creditBalance >= 2
    val showGetCoinsButton = urlText.isNotBlank() && creditBalance < 2
    
    Column(
        modifier = Modifier
            .padding(16.dp)
            .semantics { 
                contentDescription = "Generate Insights option"
                testTag = "generate_insights_button"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "✨",
            fontSize = 32.sp,
            modifier = Modifier.semantics { 
                contentDescription = "Sparkles icon for AI insights" 
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "AI Insights",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { 
                contentDescription = "Generate Insights title" 
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "AI summary, key points & high-accuracy transcript.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .height(64.dp)
                .semantics { 
                    contentDescription = "Generate Insights description" 
                }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FilledTonalButton(
            onClick = onTierSubmit,
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { 
                    contentDescription = "Use AI insights button"
                    testTag = "generate_insights_action_button"
                }
        ) {
            Text("Use\n(2 Coins)")
        }
        
        if (showGetCoinsButton) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onGetCoins,
                modifier = Modifier.semantics { 
                    contentDescription = "Get coins button"
                    testTag = "get_coins_button"
                }
            ) {
                Text("Get Coins")
            }
        }
    }
}
