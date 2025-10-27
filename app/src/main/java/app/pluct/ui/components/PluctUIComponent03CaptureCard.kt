package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pluct.data.entity.ProcessingTier

/**
 * Pluct-UI-Component-03CaptureCard - Unified, professional capture interface
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single, cohesive component combining URL input and choice engine buttons
 */
@Composable
fun PluctUIComponent03CaptureCard(
    freeUsesRemaining: Int = 3,
    creditBalance: Int = 0,
    onTierSubmit: (String, ProcessingTier) -> Unit = { _, _ -> },
    isProcessing: Boolean = false,
    currentJobId: String? = null
) {
    var urlText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    var showGetCoinsDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Video capture card" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // URL Input Field
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("Paste Video Link") },
                placeholder = { Text("e.g., https://vm.tiktok.com/...") },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            clipboardManager.getText()?.let { clipboardText ->
                                urlText = clipboardText.text
                            }
                        },
                        modifier = Modifier.semantics { 
                            contentDescription = "Paste from clipboard"
                            testTag = "paste_button"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { 
                        contentDescription = "Video URL input field"
                        testTag = "video_url_input"
                    },
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
                // Processing State
                PluctProcessingIndicator(currentJobId = currentJobId)
            } else {
                // Choice Engine Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Choice Card: Extract Script
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .semantics { 
                                contentDescription = "Extract Script option"
                                testTag = "extract_script_button"
                            }
                    ) {
                        PluctExtractScriptCard(
                            urlText = urlText,
                            freeUsesRemaining = freeUsesRemaining,
                            creditBalance = creditBalance,
                            onTierSubmit = { onTierSubmit(urlText, ProcessingTier.FREE) }
                        )
                    }
                    
                    // Right Choice Card: Generate Insights
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
                            onTierSubmit = { onTierSubmit(urlText, ProcessingTier.AI_ANALYSIS) },
                            onGetCoins = { showGetCoinsDialog = true }
                        )
                    }
                }
            }
        }
    }
    
    // Get Coins Dialog
    if (showGetCoinsDialog) {
        AlertDialog(
            onDismissRequest = { showGetCoinsDialog = false },
            title = { Text("Coming Soon") },
            text = { 
                Text("Premium AI analysis will be available for purchase shortly. We're working hard to bring this feature to you!") 
            },
            confirmButton = {
                TextButton(onClick = { showGetCoinsDialog = false }) {
                    Text("Got it")
                }
            }
        )
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
                        text = "Job ID: ${currentJobId.take(8)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PluctExtractScriptCard(
    urlText: String,
    freeUsesRemaining: Int,
    creditBalance: Int,
    onTierSubmit: () -> Unit
) {
    val isEnabled = urlText.isNotBlank() && (freeUsesRemaining > 0 || creditBalance >= 1)
    
    Column(
        modifier = Modifier
            .padding(16.dp)
            .semantics { 
                contentDescription = "Extract Script option"
                testTag = "extract_script_button"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon
        Text(
            text = "📄",
            fontSize = 32.sp,
            modifier = Modifier.semantics { 
                contentDescription = "Document icon for script extraction" 
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title
        Text(
            text = "Extract Script",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { 
                contentDescription = "Extract Script title" 
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Description
        Text(
            text = "Get the text transcript. Fast and free for your first 3 uses.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .height(64.dp)
                .semantics { 
                    contentDescription = "Extract Script description" 
                }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Button
        Button(
            onClick = onTierSubmit,
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
        // Icon
        Text(
            text = "✨",
            fontSize = 32.sp,
            modifier = Modifier.semantics { 
                contentDescription = "Sparkles icon for AI insights" 
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title
        Text(
            text = "AI Insights",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { 
                contentDescription = "Generate Insights title" 
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Description
        Text(
            text = "AI summary, key points & high-accuracy transcript.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .height(64.dp)
                .semantics { 
                    contentDescription = "Generate Insights description" 
                }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Button
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
            Text("Use\n(2 Coins)") //How do i make this taxt not word-wrap?
            
        }
        
        // Get Coins Button (if needed)
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
