package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.pluct.data.repository.PluctVideoRepository

/**
 * Pluct-Mobile-UI-Component-CaptureCard-01URLInput
 * Modern, sleek URL input field with integrated paste and history actions.
 * Follows strict naming convention: [Scope]-[Responsibility]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctURLInputField(
    urlText: String,
    onUrlTextChange: (String) -> Unit,
    validationError: String?,
    isProcessing: Boolean,
    onFocusChanged: (androidx.compose.ui.focus.FocusState) -> Unit,
    videoRepository: PluctVideoRepository? = null
) {
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // Premium Input Field Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), // Subtle background
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (validationError != null) MaterialTheme.colorScheme.error 
                       else if (urlText.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                       else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                // Input Field
                androidx.compose.foundation.text.BasicTextField(
                    value = urlText,
                    onValueChange = onUrlTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged(onFocusChanged)
                        .semantics {
                            contentDescription = "Video URL input field"
                            testTag = "url_input_field"
                        },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = androidx.compose.ui.Alignment.CenterStart) {
                            if (urlText.isEmpty()) {
                                Text(
                                    "Paste TikTok Link",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                // Actions
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    if (urlText.isNotEmpty()) {
                        IconButton(
                            onClick = { onUrlTextChange("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear URL",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        // Paste Button
                        IconButton(
                            onClick = {
                                val clipData = clipboardManager.getText()
                                if (clipData != null) {
                                    onUrlTextChange(clipData.toString())
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .semantics { 
                                    contentDescription = "Paste from clipboard"
                                    testTag = "paste_button"
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (validationError != null) {
            Text(
                text = validationError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp)
                    .semantics { testTag = "url_validation_error" }
            )
        }
    }
}
