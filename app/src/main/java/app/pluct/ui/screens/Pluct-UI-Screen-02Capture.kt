package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Pluct-UI-Screen-02Capture - Capture sheet for URL input
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctCaptureSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onUrlSubmit: (String) -> Unit
) {
    if (isVisible) {
        var url by remember { mutableStateOf("") }
        
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = Modifier.testTag("capture_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Capture This Insight",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("capture_sheet_title")
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // URL Input
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("TikTok URL") },
                    placeholder = { Text("https://vm.tiktok.com/...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_input"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            if (url.isNotBlank()) {
                                onUrlSubmit(url)
                            }
                        }
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Submit Button
                Button(
                    onClick = { 
                        if (url.isNotBlank()) {
                            onUrlSubmit(url)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_button"),
                    enabled = url.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Process Video")
                }
                
                Spacer(modifier = Modifier.height(32.dp)) // For bottom sheet peek
            }
        }
    }
}
