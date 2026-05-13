package app.pluct.ui.components

// SIZE-EXEMPT: cohesive capture URL row, wallet tile, paste/extract, and example hint share one layout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import android.view.HapticFeedbackConstants
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreValidationInputSanitizer
import coil.compose.AsyncImage

/**
 * Pluct-Mobile-UI-Component-CaptureCard-01URLInput
 * Smart input: wallet column, field, circular submit; example or metadata line below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun PluctURLInputField(
    urlText: String,
    onUrlTextChange: (String) -> Unit,
    validationError: String?,
    isProcessing: Boolean,
    onFocusChanged: (androidx.compose.ui.focus.FocusState) -> Unit,
    videoRepository: PluctVideoRepository? = null,
    onSubmit: (() -> Unit)? = null,
    freeUsesRemaining: Int = 0,
    creditBalance: Int = 0,
    isSubmitting: Boolean = false,
    onWalletClick: (() -> Unit)? = null,
    isLoadingCreditBalance: Boolean = false,
    metadataPreview: VideoItem? = null,
    showClipboardTikTokPasteChip: Boolean = false,
    fieldRowMinHeight: Dp = 52.dp
) {
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val sanitizer = remember { PluctCoreValidationInputSanitizer() }
    val isUrlValid = remember(urlText, validationError) {
        urlText.isNotBlank() && (validationError == null || sanitizer.validateUrl(urlText).isValid)
    }
    val urlNorm = remember(urlText, validationError) {
        val r = sanitizer.validateUrl(urlText)
        if (r.isValid) normalizeUrl(r.sanitizedValue) else ""
    }
    val showMetaRow = metadataPreview != null && urlNorm.isNotEmpty() &&
        normalizeUrl(metadataPreview.url) == urlNorm
    val showFab = urlText.isNotEmpty() && isUrlValid && onSubmit != null
    val walletLabel = when {
        isLoadingCreditBalance -> "…"
        freeUsesRemaining > 0 -> "$freeUsesRemaining"
        else -> "$creditBalance"
    }
    val walletDescription = when {
        isLoadingCreditBalance -> "Loading wallet amount"
        freeUsesRemaining > 0 -> "Free uses: $freeUsesRemaining. Tap to refresh."
        else -> "Wallet amount: $creditBalance. Tap to refresh."
    }

    fun pasteFromClipboard() {
        val clipData = clipboardManager.getText() ?: return
        val pasted = clipData.toString()
        val hasRepeatedUrl = pasted.indexOf("http", startIndex = 1, ignoreCase = true) > 0
        val normalized = if (pasted.any { it.isWhitespace() } && !hasRepeatedUrl) {
            sanitizer.extractUrlFromText(pasted)
        } else {
            pasted
        }
        val candidate = normalized.trim()
        if (candidate.isNotBlank() && sanitizer.isTikTokUrl(candidate)) {
            onUrlTextChange(candidate)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                testTag = "capture_component_label"
                contentDescription = "Capture link"
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 56.dp, max = 76.dp)
                    .defaultMinSize(minHeight = 72.dp)
                    .clickable(enabled = onWalletClick != null) { onWalletClick?.invoke() }
                    .semantics {
                        testTag = "capture_wallet_chip"
                        contentDescription = walletDescription
                    },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isLoadingCreditBalance) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = walletLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = if (freeUsesRemaining > 0) "free" else "credits",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(fieldRowMinHeight - 8.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            )

            Column(modifier = Modifier.weight(1f)) {
                if (showMetaRow) {
                    val meta = metadataPreview!!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (meta.thumbnailUrl.isNotBlank()) {
                            AsyncImage(
                                model = meta.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            val handle = meta.author.trim().let { a ->
                                when {
                                    a.isBlank() -> ""
                                    a.startsWith("@") -> a
                                    else -> "@$a"
                                }
                            }
                            if (handle.isNotBlank()) {
                                Text(
                                    text = handle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val dur = formatDurationForPreview(meta.duration)
                            Text(
                                text = listOfNotNull(dur.takeIf { it.isNotBlank() }, "Ready").joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(fieldRowMinHeight),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = when {
                            validationError != null -> MaterialTheme.colorScheme.error
                            urlText.isNotEmpty() && isUrlValid -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BasicTextField(
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
                                .onFocusChanged { state ->
                                    if (state.isFocused && urlText.isBlank()) {
                                        val clip = clipboardManager.getText()?.toString().orEmpty()
                                        val extracted = sanitizer.extractUrlFromText(clip).trim()
                                        if (extracted.isNotBlank() && sanitizer.isTikTokUrl(extracted)) {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            onUrlTextChange(extracted)
                                        }
                                    }
                                    onFocusChanged(state)
                                }
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
                                    if (showFab && !isSubmitting && onSubmit != null) {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        onSubmit.invoke()
                                    }
                                }
                            ),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (urlText.isEmpty()) {
                                        Text(
                                            "Paste TikTok link here",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

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
                        } else if (showClipboardTikTokPasteChip) {
                            AssistChip(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    pasteFromClipboard()
                                },
                                label = { Text("Paste") },
                                modifier = Modifier
                                    .semantics {
                                        testTag = "paste_button"
                                        contentDescription = "Paste TikTok link"
                                    },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    pasteFromClipboard()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .semantics {
                                        contentDescription = "Paste TikTok link"
                                        testTag = "paste_button"
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (showFab && !isSubmitting && onSubmit != null) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onSubmit.invoke()
                                }
                            },
                            enabled = showFab && !isSubmitting,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (showFab) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                )
                                .alpha(if (showFab) 1f else 0.45f)
                                .semantics {
                                    contentDescription = "Start"
                                    testTag = "extract_script_button"
                                }
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = if (showFab) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!showMetaRow) {
                Text(
                    text = "Example: https://vt.tiktok.com/...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
                modifier = Modifier
                    .padding(start = 4.dp, top = 8.dp)
                    .fillMaxWidth()
                    .semantics {
                        testTag = "capture_url_example_hint"
                        contentDescription = "Example TikTok link"
                    }
            )
        }

        if (validationError != null) {
            Text(
                text = shortValidationMessage(validationError),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(start = 4.dp, top = 6.dp)
                    .semantics {
                        contentDescription = validationError
                        testTag = "url_validation_error"
                    }
            )
        }
    }
}

private fun normalizeUrl(url: String): String =
    url.trim().lowercase().removeSuffix("/")

private fun formatDurationForPreview(seconds: Long): String {
    if (seconds <= 0L) return ""
    val m = seconds / 60
    val s = seconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", m, s)
}

private fun shortValidationMessage(error: String): String {
    val lower = error.lowercase()
    return when {
        "one" in lower -> "This does not look like a TikTok link"
        "incomplete" in lower || "full" in lower -> "This does not look like a TikTok link"
        "profile" in lower || "search" in lower || "video" in lower -> "This does not look like a TikTok link"
        "long" in lower -> "This does not look like a TikTok link"
        else -> "This does not look like a TikTok link"
    }
}
