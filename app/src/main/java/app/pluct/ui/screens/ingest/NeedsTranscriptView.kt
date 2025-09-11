package app.pluct.ui.screens.ingest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.pluct.ui.screens.ingest.components.AutoTranscribeCard
import app.pluct.ui.screens.ingest.components.ManualTranscriptCard
import app.pluct.ui.screens.ingest.components.TranscriptErrorDisplay
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
        AutoTranscribeCard(
            hasLaunchedWebActivity = uiState.hasLaunchedWebActivity,
            onLaunchWebTranscript = onLaunchWebTranscript
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Show manual entry only after a failure
        if (uiState.error != null || uiState.webErrorCode != null) {
            ManualTranscriptCard(
                transcriptText = transcriptText,
                onTranscriptTextChange = { transcriptText = it },
                onSaveTranscript = onSaveTranscript,
                language = language,
                onLanguageChange = { language = it }
            )
        }
        
        // Show error messages if any
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            TranscriptErrorDisplay(
                error = error,
                onRetry = onLaunchWebTranscript
            )
        }
    }
}
