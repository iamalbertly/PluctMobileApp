package app.pluct.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreValidationInputSanitizer

/**
 * Pluct-Mobile-UI-Component-CaptureCard-01URLInput
 * Modern, sleek URL input field with integrated submit button (Twitter-style) and credit badge.
 * Follows strict naming convention: [Scope]-[Responsibility]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER") // isProcessing, videoRepository reserved for future use
fun PluctURLInputField(
    urlText: String,
    onUrlTextChange: (String) -> Unit,
    validationError: String?,
    isProcessing: Boolean, // Reserved for future processing state UI
    onFocusChanged: (androidx.compose.ui.focus.FocusState) -> Unit,
    videoRepository: PluctVideoRepository? = null, // Reserved for future history feature
    onSubmit: (() -> Unit)? = null,
    freeUsesRemaining: Int = 0,
    creditBalance: Int = 0, // Used in credit badge display
    isSubmitting: Boolean = false
) {
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    // Tech Debt 2: Validation needed for real-time UI feedback (button visibility)
    // Parent validates for submission, child validates for UI state - both needed, no duplication
    // Use validationError from parent as single source of truth when available
    val sanitizer = remember { PluctCoreValidationInputSanitizer() }
    val isUrlValid = remember(urlText, validationError) { 
        urlText.isNotBlank() && (validationError == null || sanitizer.validateUrl(urlText).isValid)
    }
    val showSubmitButton = urlText.isNotEmpty() && !isSubmitting && isUrlValid && onSubmit != null
    val showCreditBadge = urlText.isNotEmpty() && isUrlValid
    
    // UX FIX 1: Prevent credit badge from blocking input - use max width constraint
    val creditBadgeMaxWidth = 80.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        // Premium Input Field Container with inline submit button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), // Subtle background
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (validationError != null) MaterialTheme.colorScheme.error 
                       else if (urlText.isNotEmpty() && isUrlValid) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                       else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                // Credit Badge (left side, only when URL is valid)
                // UX FIX 1: Constrain badge width to prevent blocking long URLs
                AnimatedVisibility(
                    visible = showCreditBadge,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = if (freeUsesRemaining > 0) "Free ($freeUsesRemaining)" else "1 credit",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .widthIn(max = creditBadgeMaxWidth),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // Input Field
                androidx.compose.foundation.text.BasicTextField(
                    value = urlText,
                    onValueChange = { newValue ->
                        val hasRepeatedUrl = newValue.indexOf("http", startIndex = 1, ignoreCase = true) > 0
                        val normalized = if (newValue.any { it.isWhitespace() } && !hasRepeatedUrl) {
                            sanitizer.extractUrlFromText(newValue)
                        } else {
                            newValue
                        }
                        onUrlTextChange(normalized)
                    },
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
                        onDone = { 
                            keyboardController?.hide()
                            // UX FIX 3: Keyboard Done action submits when URL is valid
                            if (showSubmitButton && onSubmit != null) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onSubmit.invoke()
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = androidx.compose.ui.Alignment.CenterStart) {
                            if (urlText.isEmpty()) {
                                Text(
                                    "TikTok link",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                // Actions Row (Clear/Paste + Submit)
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Clear or Paste Button
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
                        // UX FIX 4: Visual feedback when paste button is clicked
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                val clipData = clipboardManager.getText()
                                if (clipData != null) {
                                    val pasted = clipData.toString()
                                    val hasRepeatedUrl = pasted.indexOf("http", startIndex = 1, ignoreCase = true) > 0
                                    val normalized = if (pasted.any { it.isWhitespace() } && !hasRepeatedUrl) {
                                        sanitizer.extractUrlFromText(pasted)
                                    } else {
                                        pasted
                                    }
                                    onUrlTextChange(normalized)
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

                    // Submit Button (Twitter-style, appears when URL is valid)
                    // UX FIX 2: Add haptic feedback on click
                    // UX FIX 5: Clear disabled state visual feedback
                    AnimatedVisibility(
                        visible = showSubmitButton,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        IconButton(
                            onClick = { 
                                // UX FIX 2: Haptic feedback on submit button click
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onSubmit?.invoke() 
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .semantics {
                                    contentDescription = "Extract Script"
                                    testTag = "extract_script_button"
                                },
                            enabled = !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Submit",
                                    // UX FIX 5: Clear disabled state - button only shows when enabled, so always use primary color
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
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
