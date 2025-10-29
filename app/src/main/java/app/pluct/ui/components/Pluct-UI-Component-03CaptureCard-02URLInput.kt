package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Clear
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

/**
 * Pluct-UI-Component-03CaptureCard-02URLInput - URL input field component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single source of truth for URL input functionality
 */
@Composable
fun PluctURLInputField(
    urlText: String,
    onUrlTextChange: (String) -> Unit,
    validationError: String?,
    isProcessing: Boolean,
    onFocusChanged: (androidx.compose.ui.focus.FocusState) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    
    OutlinedTextField(
        value = urlText,
        onValueChange = onUrlTextChange,
        label = { Text("Paste Video Link") },
        placeholder = { Text("e.g., https://vm.tiktok.com/...") },
        trailingIcon = {
            Row {
                IconButton(
                    onClick = {
                        clipboardManager.getText()?.let { clipboardText ->
                            val pasted = clipboardText.text
                            onUrlTextChange(pasted)
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
                IconButton(
                    onClick = { onUrlTextChange("") },
                    modifier = Modifier.semantics {
                        contentDescription = "Clear URL"
                        testTag = "clear_button"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear"
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { 
                contentDescription = "TikTok URL input field"
                testTag = "video_url_input"
            }
            .onFocusChanged(onFocusChanged),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { keyboardController?.hide() }
        ),
        isError = validationError != null,
        supportingText = {
            validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        },
        enabled = !isProcessing
    )
}
