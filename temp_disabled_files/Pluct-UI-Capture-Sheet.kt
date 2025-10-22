package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingTier

/**
 * Pluct-UI-Capture-Sheet - Capture sheet component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Implements modern capture sheet with URL input and tier selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctCaptureSheet(
    onDismiss: () -> Unit,
    onCapture: (String, ProcessingTier) -> Unit,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf("") }
    var selectedTier by remember { mutableStateOf(ProcessingTier.QUICK_SCAN) }
    var showTierSelection by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("capture_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Capture TikTok Video",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // URL input
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("TikTok URL") },
                placeholder = { Text("https://vm.tiktok.com/...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("url_input"),
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null)
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tier selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Processing Tier:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                TextButton(
                    onClick = { showTierSelection = true },
                    modifier = Modifier.testTag("tier_selection_button")
                ) {
                    Text(selectedTier.name.replace("_", " "))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = { 
                        if (url.isNotBlank()) {
                            onCapture(url, selectedTier)
                        }
                    },
                    enabled = url.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("capture_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Capture")
                }
            }
        }
    }
    
    // Tier selection dialog
    if (showTierSelection) {
        PluctTierSelectionDialog(
            selectedTier = selectedTier,
            onTierSelected = { 
                selectedTier = it
                showTierSelection = false
            },
            onDismiss = { showTierSelection = false }
        )
    }
}

@Composable
fun PluctTierSelectionDialog(
    selectedTier: ProcessingTier,
    onTierSelected: (ProcessingTier) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Processing Tier") },
        text = {
            Column {
                ProcessingTier.values().forEach { tier ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTier == tier,
                            onClick = { onTierSelected(tier) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = tier.name.replace("_", " "),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = getTierDescription(tier),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

private fun getTierDescription(tier: ProcessingTier): String {
    return when (tier) {
        ProcessingTier.QUICK_SCAN -> "Fast processing, basic insights"
        ProcessingTier.DEEP_ANALYSIS -> "Detailed analysis, comprehensive insights"
        ProcessingTier.PREMIUM_INSIGHTS -> "Advanced AI analysis, premium features"
    }
}
