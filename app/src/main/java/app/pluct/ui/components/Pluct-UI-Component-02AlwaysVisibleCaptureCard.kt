package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pluct.data.entity.ProcessingTier

/**
 * Pluct-UI-Component-02AlwaysVisibleCaptureCard - Always visible capture card with two-button choice engine
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Provides the main input interface with free and premium tier options
 */
@Composable
fun PluctAlwaysVisibleCaptureCard(
    preFilledUrl: String? = null,
    onTierSubmit: (String, ProcessingTier) -> Unit = { _, _ -> },
    isProcessing: Boolean = false,
    currentJobId: String? = null,
    freeUsesRemaining: Int = 3,
    creditBalance: Int = 0
) {
    var urlText by remember { mutableStateOf(preFilledUrl ?: "") }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "TikTok URL input card" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Enter TikTok URL",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { contentDescription = "Enter TikTok URL label" }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                placeholder = { Text("https://vm.tiktok.com/...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "TikTok URL input field" },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                enabled = !isProcessing
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isProcessing) {
                PluctProcessingIndicator(currentJobId = currentJobId)
            } else {
                PluctTwoButtonChoiceEngine(
                    urlText = urlText,
                    freeUsesRemaining = freeUsesRemaining,
                    creditBalance = creditBalance,
                    onTierSubmit = onTierSubmit
                )
            }
        }
    }
}

@Composable
private fun PluctProcessingIndicator(currentJobId: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = "Processing...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (currentJobId != null) {
                    Text(
                        text = "Job ID: $currentJobId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PluctTwoButtonChoiceEngine(
    urlText: String,
    freeUsesRemaining: Int,
    creditBalance: Int,
    onTierSubmit: (String, ProcessingTier) -> Unit
) {
    val isButtonEnabled = urlText.isNotBlank()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left Card: Free Tier Button
        Box(modifier = Modifier.weight(1f)) {
            PluctFreeTierButton(
                isEnabled = isButtonEnabled && (freeUsesRemaining > 0 || creditBalance > 0),
                freeUsesRemaining = freeUsesRemaining,
                onTierSubmit = { onTierSubmit(urlText, ProcessingTier.FREE) }
            )
        }
        
        // Right Card: Premium Tier Button
        Box(modifier = Modifier.weight(1f)) {
            PluctPremiumTierButton(
                isEnabled = isButtonEnabled && creditBalance >= 2,
                onTierSubmit = { onTierSubmit(urlText, ProcessingTier.AI_ANALYSIS) }
            )
        }
    }
}

@Composable
private fun PluctFreeTierButton(
    isEnabled: Boolean,
    freeUsesRemaining: Int,
    onTierSubmit: () -> Unit
) {
    Card(
        modifier = Modifier
            .semantics { testTag = "extract_script_button" },
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onTierSubmit,
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (freeUsesRemaining > 0) {
                        "📄 Extract Script"
                    } else {
                        "📄 Extract Script (1 Coin)"
                    }
                )
            }
            
            if (freeUsesRemaining > 0) {
                Text(
                    text = "($freeUsesRemaining free extractions remaining)",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { 
                        contentDescription = "Free extractions remaining: $freeUsesRemaining" 
                    }
                )
            }
        }
    }
}

@Composable
private fun PluctPremiumTierButton(
    isEnabled: Boolean,
    onTierSubmit: () -> Unit
) {
    Card(
        modifier = Modifier
            .semantics { testTag = "generate_insights_button" },
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onTierSubmit,
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("✨ Generate Insights (2 Coins)")
            }
            
            Text(
                text = "AI summary, key points & full transcript",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { 
                    contentDescription = "AI summary, key points and full transcript" 
                }
            )
        }
    }
}
