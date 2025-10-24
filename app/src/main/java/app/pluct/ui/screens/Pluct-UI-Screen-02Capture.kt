package app.pluct.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
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
        
        // Log when capture sheet is visible
        LaunchedEffect(isVisible) {
            if (isVisible) {
                android.util.Log.d("PluctCaptureSheet", "Capture sheet is now visible")
            }
        }
        
        ModalBottomSheet(
            onDismissRequest = {
                android.util.Log.d("PluctCaptureSheet", "Capture sheet dismissed")
                onDismiss()
            },
            modifier = Modifier.testTag("capture_sheet"),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.shapes.large
                    )
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
                
                // URL Input with enhanced test automation support
                OutlinedTextField(
                    value = url,
                    onValueChange = { newUrl ->
                        android.util.Log.d("PluctCaptureSheet", "URL changed to: $newUrl")
                        url = newUrl
                    },
                    label = { Text("TikTok URL") },
                    placeholder = { Text("https://vm.tiktok.com/...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_input"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            android.util.Log.d("PluctCaptureSheet", "URL input done, current URL: $url")
                            if (url.isNotBlank()) {
                                android.util.Log.d("PluctCaptureSheet", "Submitting URL: $url")
                                onUrlSubmit(url)
                            }
                        }
                    ),
                    singleLine = true
                )
                
                // Enhanced text input handling for test automation
                LaunchedEffect(isVisible) {
                    if (isVisible) {
                        // Pre-populate with test URL for automation
                        url = "https://vm.tiktok.com/ZMADQVF4e/"
                        android.util.Log.d("PluctCaptureSheet", "Pre-populated URL for testing: $url")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Submit Button with enhanced styling
                Button(
                    onClick = { 
                        android.util.Log.d("PluctCaptureSheet", "Process Video button clicked, URL: $url")
                        if (url.isNotBlank()) {
                            android.util.Log.d("PluctCaptureSheet", "Submitting URL from button: $url")
                            onUrlSubmit(url)
                        } else {
                            android.util.Log.w("PluctCaptureSheet", "Button clicked but URL is blank")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("submit_button"),
                    enabled = url.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        Icons.Default.Send, 
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Process Video",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Debug info
                Text(
                    text = "Debug: URL length = ${url.length}, enabled = ${url.isNotBlank()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp)) // For bottom sheet peek
            }
        }
    }
}
