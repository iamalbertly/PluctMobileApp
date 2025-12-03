package app.pluct.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp

/**
 * Dialogs used by the capture card.
 */
@Composable
fun PluctCaptureCardGetCoinsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Coming Soon") },
        text = {
            Text("Premium AI analysis will be available for purchase shortly. We're working hard to bring this feature to you!")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

@Composable
fun PluctCaptureCardInsightsDialog(
    urlText: String,
    onDismiss: () -> Unit
) {
    var queryText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ask Insights") },
        text = {
            Column {
                Text("Ask anything about this video:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Is this factual?") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val jsonPrompt = """
                        {
                            "video_url": "$urlText",
                            "user_query": "$queryText",
                            "context": "video_transcription"
                        }
                    """.trimIndent()
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(jsonPrompt))
                    Log.d("CaptureCard", "Insights prompt copied to clipboard: $jsonPrompt")
                    onDismiss()
                }
            ) {
                Text("Ask")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
