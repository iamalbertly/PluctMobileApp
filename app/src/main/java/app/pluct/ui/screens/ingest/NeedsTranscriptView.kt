package app.pluct.ui.screens.ingest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.pluct.viewmodel.IngestUiState

/**
 * View component for the transcript entry stage
 */
@Composable
fun NeedsTranscriptView(
    url: String,
    uiState: IngestUiState,
    onSaveTranscript: (String, String?) -> Unit,
    onLaunchWebTranscript: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var transcriptText by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("en") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Get Transcript",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Video URL: $url",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Option 1: Auto transcribe via Web
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Auto transcribe via Web",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Automatically extract transcript from TikTok, YouTube, or Instagram videos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Show status based on whether automatic process has started
                if (uiState.hasLaunchedWebActivity) {
                    // Show loading state
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Processing transcript automatically...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    // Show button to launch manually
                    Button(
                        onClick = onLaunchWebTranscript,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Get Transcript Automatically")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "This will automatically:\n• Open script.tokaudit.io\n• Fill in your video URL\n• Process the transcript\n• Return the result to you",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Option 2: Manual paste
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Manual transcript entry",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Or paste your transcript manually:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                BasicTextField(
                    value = transcriptText,
                    onValueChange = { transcriptText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .verticalScroll(rememberScrollState()),
                    decorationBox = { innerTextField ->
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Box(modifier = Modifier.padding(12.dp)) {
                                if (transcriptText.isEmpty()) {
                                    Text(
                                        text = "Paste your transcript here...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        onClick = {
                            if (transcriptText.isNotBlank()) {
                                onSaveTranscript(transcriptText, language)
                            }
                        },
                        enabled = transcriptText.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Transcript")
                    }
                }
            }
        }
    }
}
