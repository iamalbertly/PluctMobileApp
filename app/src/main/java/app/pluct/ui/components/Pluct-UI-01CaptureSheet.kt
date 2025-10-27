package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pluct.data.entity.ProcessingTier

/**
 * Pluct-UI-01CaptureSheet - Capture sheet UI component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Extracted from MainActivity to reduce file size
 */
@Composable
fun PluctCaptureSheet(
    onDismiss: () -> Unit,
    onUrlSubmit: (String, ProcessingTier) -> Unit,
    selectedTier: ProcessingTier,
    onTierChange: (ProcessingTier) -> Unit
) {
    var url by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Capture Video",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("TikTok URL") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "TikTok URL input field"
                }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { onUrlSubmit(url, selectedTier) },
                enabled = url.isNotEmpty(),
                modifier = Modifier.semantics {
                    contentDescription = "Submit button"
                }
            ) {
                Text("Submit")
            }
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Cancel button"
                }
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun PluctQuickScanSheet(
    onDismiss: () -> Unit,
    onUrlSubmit: (String, ProcessingTier) -> Unit,
    selectedTier: ProcessingTier,
    onTierChange: (ProcessingTier) -> Unit
) {
    var url by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Quick Scan",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("TikTok URL") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "TikTok URL input field for quick scan"
                }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { onUrlSubmit(url, selectedTier) },
                enabled = url.isNotEmpty(),
                modifier = Modifier.semantics {
                    contentDescription = "Quick scan button"
                }
            ) {
                Text("Quick Scan")
            }
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Cancel button"
                }
            ) {
                Text("Cancel")
            }
        }
    }
}
