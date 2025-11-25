package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pluct.data.entity.ProcessingTier
import android.util.Log
import app.pluct.services.PluctCoreValidationInputSanitizer
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.core.error.PluctErrorUnifiedHandler
import app.pluct.core.error.PluctErrorUnifiedHandler.ErrorSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pluct-UI-Component-03CaptureCard - Main capture card orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single source of truth for capture card functionality
 */
@Composable
fun PluctUIComponent03CaptureCard(
    freeUsesRemaining: Int = 3,
    creditBalance: Int = 0,
    onTierSubmit: (String, ProcessingTier) -> Unit = { _, _ -> },
    isProcessing: Boolean = false,
    currentJobId: String? = null,
    preFilledUrl: String? = null,
    apiService: PluctCoreAPIUnifiedService? = null
) {
    var urlText by remember { mutableStateOf(preFilledUrl ?: "") }
    var hasFocus by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showGetCoinsDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val sanitizer = remember { PluctCoreValidationInputSanitizer() }
    val validation = remember(urlText) { sanitizer.validateUrl(urlText) }
    val isUrlValid = urlText.isNotBlank() && validation.isValid
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Always visible capture card" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        // Add Capture Video content description for test compatibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Capture Video" }
        )
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // URL Input Field
            PluctURLInputField(
                urlText = urlText,
                onUrlTextChange = { newValue ->
                    urlText = newValue
                    val result = sanitizer.validateUrl(newValue)
                    validationError = if (newValue.isBlank()) null else if (!result.isValid) result.errorMessage else null
                },
                validationError = validationError,
                isProcessing = isProcessing,
                onFocusChanged = { state ->
                    if (state.isFocused && !hasFocus) {
                        urlText = ""
                        validationError = null
                        hasFocus = true
                    }
                    if (!state.isFocused) {
                        hasFocus = false
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Processing Indicators and Error Display
            if (isSubmitting || processingMessage != null) {
                ProcessingIndicator(
                    message = processingMessage ?: "Processing...",
                    isVisible = isSubmitting || processingMessage != null
                )
            }
            
            // API Error Display
            val currentApiError = apiError
            if (currentApiError != null) {
                ErrorDisplay(
                    message = currentApiError,
                    onDismiss = { apiError = null }
                )
            }
            
            if (isProcessing) {
                // Processing State
                PluctProcessingIndicator(currentJobId = currentJobId)
            } else {
                // Choice Engine Row
                PluctChoiceEngine(
                    urlText = urlText,
                    freeUsesRemaining = freeUsesRemaining,
                    creditBalance = creditBalance,
                    sanitizer = sanitizer,
                    isSubmitting = isSubmitting,
                    onTierSubmit = { 
                        Log.d("CaptureCard", "🎯 onTierSubmit function called!")
                        if (isSubmitting) {
                            Log.d("CaptureCard", "⚠️ Already submitting, ignoring click")
                            return@PluctChoiceEngine
                        }
                        isSubmitting = true
                        val validationResult = sanitizer.validateUrl(urlText)
                        val normalizedUrl = if (validationResult.isValid) validationResult.sanitizedValue ?: urlText else urlText
                        Log.d("CaptureCard", "🚀 Extract Script button clicked with normalized URL: '$normalizedUrl'")
                        
                        val localValidation = sanitizer.validateUrl(normalizedUrl)
                        if (!localValidation.isValid) {
                            Log.e("CaptureCard", "❌ VALIDATION_ERROR: ${localValidation.errorMessage}")
                            PluctErrorUnifiedHandler().handleError(
                                IllegalArgumentException(localValidation.errorMessage ?: "Invalid URL"),
                                context = "URL input",
                                severity = ErrorSeverity.LOW,
                                retryable = false
                            )
                            urlText = ""
                            validationError = localValidation.errorMessage
                            isSubmitting = false
                            return@PluctChoiceEngine
                        }
                        
                        // Use complete Business Engine API flow if available
                        if (apiService != null) {
                            Log.d("CaptureCard", "🔗 Using complete Business Engine API flow")
                            processingMessage = "Starting transcription..."
                            apiError = null
                            handleCompleteAPIFlow(
                                normalizedUrl = localValidation.sanitizedValue ?: normalizedUrl,
                                apiService = apiService,
                                onSuccess = { result ->
                                    Log.d("CaptureCard", "✅ API flow completed successfully")
                                    processingMessage = null
                                    onTierSubmit(localValidation.sanitizedValue ?: normalizedUrl, ProcessingTier.EXTRACT_SCRIPT)
                                    urlText = ""
                                    validationError = null
                                    isSubmitting = false
                                },
                                onError = { error ->
                                    Log.e("CaptureCard", "❌ API flow failed: $error")
                                    processingMessage = null
                                    apiError = error
                                    validationError = error
                                    isSubmitting = false
                                }
                            )
                        } else {
                            Log.d("CaptureCard", "⚠️ No API service available, using fallback")
                            onTierSubmit(localValidation.sanitizedValue ?: normalizedUrl, ProcessingTier.EXTRACT_SCRIPT)
                            urlText = ""
                            validationError = null
                            isSubmitting = false
                        }
                    },
                    onGetCoins = { showGetCoinsDialog = true }
                )
                
                // Get Coins Button (if needed)
                if (isUrlValid && creditBalance < 2) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showGetCoinsDialog = true },
                        modifier = Modifier.semantics { 
                            contentDescription = "Get coins button"
                            testTag = "get_coins_button"
                        }
                    ) {
                        Text("Get Coins")
                    }
                }
            }
        }
    }
    
    // Get Coins Dialog
    if (showGetCoinsDialog) {
        AlertDialog(
            onDismissRequest = { showGetCoinsDialog = false },
            title = { Text("Coming Soon") },
            text = { 
                Text("Premium AI analysis will be available for purchase shortly. We're working hard to bring this feature to you!") 
            },
            confirmButton = {
                TextButton(onClick = { showGetCoinsDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

/**
 * Handle complete Business Engine API flow with comprehensive logging
 */
private fun handleCompleteAPIFlow(
    normalizedUrl: String,
    apiService: PluctCoreAPIUnifiedService,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    Log.d("CaptureCard", "🚀 Starting complete Business Engine API flow for URL: $normalizedUrl")
    
    CoroutineScope(Dispatchers.IO).launch {
        try {
            Log.d("CaptureCard", "📡 Calling processTikTokVideo API...")
            val result = apiService.processTikTokVideo(normalizedUrl)
            
            if (result.isSuccess) {
                val status = result.getOrNull()!!
                Log.d("CaptureCard", "✅ Complete API flow successful!")
                Log.d("CaptureCard", "📊 Final status: ${status.status}")
                Log.d("CaptureCard", "📝 Transcript: ${status.transcript}")
                Log.d("CaptureCard", "🎯 Confidence: ${status.confidence}")
                Log.d("CaptureCard", "🌍 Language: ${status.language}")
                Log.d("CaptureCard", "⏱️ Duration: ${status.duration}s")
                
                withContext(Dispatchers.Main) {
                    onSuccess("Transcription completed successfully!")
                }
            } else {
                val error = result.exceptionOrNull()
                val errorMessage = error?.message ?: "Unknown error"
                Log.e("CaptureCard", "❌ Complete API flow failed: $errorMessage", error)
                withContext(Dispatchers.Main) {
                    // Pass full error message which includes service details from parseResponse
                    onError(errorMessage)
                }
            }
        } catch (e: Exception) {
            Log.e("CaptureCard", "❌ Exception during complete API flow: ${e.message}", e)
            val errorMessage = e.message ?: "Unknown error occurred"
            withContext(Dispatchers.Main) {
                // Pass full error message which includes service details
                onError(errorMessage)
            }
        }
    }
}

/**
 * Processing indicator component
 */
@Composable
private fun ProcessingIndicator(
    message: String,
    isVisible: Boolean
) {
    if (isVisible) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Processing indicator" },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Error display component with detailed service information
 */
@Composable
private fun ErrorDisplay(
    message: String,
    onDismiss: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Error message" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Extract service name from error message if present
                val serviceName = extractServiceName(message)
                val shortMessage = if (serviceName != null) {
                    "Error from $serviceName: ${extractShortError(message)}"
                } else {
                    message.take(100) + if (message.length > 100) "..." else ""
                }
                
                Text(
                    text = shortMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                // Expand/collapse button for detailed error
                if (message.length > 100 || message.contains("Service:") || message.contains("Expected:")) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.semantics { contentDescription = if (isExpanded) "Hide details" else "Show details" }
                    ) {
                        Text(
                            text = if (isExpanded) "Hide" else "Details",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics { contentDescription = "Dismiss error" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Expanded detailed error information
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Extract service name from error message
 */
private fun extractServiceName(message: String): String? {
    return when {
        message.contains("Business Engine") -> "Business Engine (Cloudflare Workers)"
        message.contains("TTTranscribe") -> "TTTranscribe Service (Hugging Face)"
        message.contains("/ttt/") -> "TTTranscribe Service"
        message.contains("/v1/") -> "Business Engine"
        else -> null
    }
}

/**
 * Extract short error message
 */
private fun extractShortError(message: String): String {
    // Try to extract the main error message
    val lines = message.lines()
    for (line in lines) {
        if (line.contains("Parse Error:") || line.contains("Error:") || line.contains("failed")) {
            return line.replace("Parse Error:", "").replace("Error:", "").trim()
        }
    }
    return message.take(80) + if (message.length > 80) "..." else ""
}
