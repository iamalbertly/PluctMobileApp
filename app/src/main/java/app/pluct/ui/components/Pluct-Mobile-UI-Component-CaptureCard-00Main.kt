package app.pluct.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.PluctCoreValidationInputSanitizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Pluct-Mobile-UI-Component-CaptureCard-00Main
 * Main capture card orchestrator (single source of truth).
 */
@Composable
fun PluctUIComponent03CaptureCard(
    freeUsesRemaining: Int = 3,
    creditBalance: Int = 0,
    onTierSubmit: (String, ProcessingTier) -> Unit = { _, _ -> },
    isProcessing: Boolean = false,
    currentJobId: String? = null,
    preFilledUrl: String? = null,
    apiService: PluctCoreAPIUnifiedService? = null,
    videoRepository: PluctVideoRepository? = null,
    ctaHelperMessage: String? = null,
    onRequestCredits: (() -> Unit)? = null,
    debugLogManager: app.pluct.core.debug.PluctCoreDebug01LogManager? = null,
    onViewInLogs: (() -> Unit)? = null
) {
    var urlText by remember { mutableStateOf(preFilledUrl ?: "") }
    var hasFocus by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showGetCoinsDialog by remember { mutableStateOf(false) }
    var showInsightsDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }
    var isPrefilledUrl by remember { mutableStateOf(preFilledUrl != null && preFilledUrl.isNotBlank()) }
    var timedOutOnce by remember { mutableStateOf(false) }
    
    // Auto-show low credit modal when credits are 0 or negative
    LaunchedEffect(creditBalance, freeUsesRemaining) {
        if (creditBalance <= 0 && freeUsesRemaining <= 0 && !showGetCoinsDialog) {
            showGetCoinsDialog = true
        }
    }

    val debugInfo by (apiService?.transcriptionDebugFlow ?: MutableStateFlow(null)).collectAsState()
    val sanitizer = remember { PluctCoreValidationInputSanitizer() }
    val currentValidation = remember(urlText) { sanitizer.validateUrl(urlText) }
    val isUrlValid = urlText.isNotBlank() && currentValidation.isValid

    LaunchedEffect(preFilledUrl) {
        if (!preFilledUrl.isNullOrBlank()) {
            val validationResult = sanitizer.validateUrl(preFilledUrl)
            if (validationResult.isValid) {
                val sanitizedUrl = validationResult.sanitizedValue
                urlText = sanitizedUrl
                isPrefilledUrl = true
                validationError = null
                if (sanitizer.isTikTokUrl(sanitizedUrl)) {
                    apiService?.preWarmVideoProcessing(sanitizedUrl)
                }
            } else {
                validationError = validationResult.errorMessage
            }
        }
    }

    LaunchedEffect(urlText) {
        if (urlText.isNotBlank()) {
            delay(500)
            val validationResult = sanitizer.validateUrl(urlText)
            if (validationResult.isValid) {
                val sanitizedUrl = validationResult.sanitizedValue
                if (sanitizer.isTikTokUrl(sanitizedUrl)) {
                    apiService?.preWarmVideoProcessing(sanitizedUrl)
                }
            }
        }
    }

    val submitExtract: () -> Unit = submit@{
        if (isSubmitting) return@submit
        isSubmitting = true

        val validationResult = sanitizer.validateUrl(urlText)
        val normalizedUrl = if (validationResult.isValid) validationResult.sanitizedValue else urlText
        if (!validationResult.isValid) {
            validationError = validationResult.errorMessage
            urlText = ""
            isSubmitting = false
            return@submit
        }

        val onComplete: () -> Unit = {
            processingMessage = null
            urlText = ""
            validationError = null
            isSubmitting = false
            timedOutOnce = false
        }

        val costLabel = if (freeUsesRemaining > 0) "Free (free uses left: $freeUsesRemaining)" else "Costs 1 credit (balance: $creditBalance)"
        processingMessage = "Starting transcription... $costLabel"

        if (apiService != null) {
            apiError = null
            PluctUIComponent03CaptureCardAPIFlow.handleCompleteAPIFlow(
                normalizedUrl = normalizedUrl,
                apiService = apiService,
                debugLogManager = debugLogManager,
                onSuccess = {
                    Log.d("CaptureCard", "API flow completed successfully")
                    onTierSubmit(normalizedUrl, ProcessingTier.EXTRACT_SCRIPT)
                    onComplete()
                },
                onError = { error ->
                    Log.e("CaptureCard", "API flow failed: $error")
                    processingMessage = null
                    apiError = error
                    isSubmitting = false
                }
            )
        } else {
            Log.d("CaptureCard", "No API service available, using fallback submit")
            onTierSubmit(normalizedUrl, ProcessingTier.EXTRACT_SCRIPT)
            onComplete()
        }
    }

    // Safety net: if a start request stalls, release the button and surface a retry message.
    LaunchedEffect(isSubmitting) {
        if (isSubmitting) {
            kotlinx.coroutines.delay(20000)
            if (isSubmitting) {
                apiError = "Still starting. Please check your connection and retry."
                isSubmitting = false
                processingMessage = null
                timedOutOnce = true
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Always visible capture card"
                testTag = "capture_card_root"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Capture Video" }
        )
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = "Paste a TikTok link",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .semantics { testTag = "capture_component_label" }
            )
            Text(
                text = "We'll transcribe and auto-copy your results.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            ctaHelperMessage?.let { helper ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = helper,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    if (helper.contains("Add credits", ignoreCase = true) && onRequestCredits != null) {
                        TextButton(onClick = onRequestCredits) {
                            Text("Request Credits")
                        }
                    }
                }
            }
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
                        if (!isPrefilledUrl) {
                            urlText = ""
                            validationError = null
                        }
                        hasFocus = true
                    }
                    if (!state.isFocused) {
                        hasFocus = false
                        if (isPrefilledUrl && urlText.isBlank()) {
                            isPrefilledUrl = false
                        }
                    }
                },
                videoRepository = videoRepository
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Processing state is now handled within PluctChoiceEngine button
            if (apiError == null && isSubmitting) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            val currentApiError = apiError
            if (currentApiError != null) {
                PluctUIComponent03CaptureCardErrorDisplay(
                    message = currentApiError,
                    onDismiss = { apiError = null },
                    debugInfo = debugInfo,
                    debugLogManager = debugLogManager,
                    onViewInLogs = onViewInLogs,
                    creditBalance = creditBalance,
                    onAddCredits = { showGetCoinsDialog = true },
                    onCheckConnection = {
                        // Could open network settings or just retry
                        apiError = null
                        isSubmitting = true
                        processingMessage = "Retrying..."
                        submitExtract()
                    },
                    onRetry = {
                        apiError = null
                        isSubmitting = true
                        processingMessage = "Retrying..."
                        submitExtract()
                    }
                )
            }

            val activeStep = debugInfo?.currentStep

            // Keep the in-flight message aligned with the latest backend step so users see movement.
            LaunchedEffect(activeStep, isSubmitting) {
                if (isSubmitting && activeStep != null) {
                    processingMessage = when (activeStep) {
                        app.pluct.services.OperationStep.METADATA -> "Getting video details..."
                        app.pluct.services.OperationStep.VEND_TOKEN -> "Confirming credits and access..."
                        app.pluct.services.OperationStep.SUBMIT -> "Sending to Business Engine..."
                        app.pluct.services.OperationStep.POLLING -> "Waiting for transcript..."
                        else -> processingMessage
                    }
                }
            }

            if (!isProcessing) {
                PluctChoiceEngine(
                    urlText = urlText,
                    freeUsesRemaining = freeUsesRemaining,
                    creditBalance = creditBalance,
                    isSubmitting = isSubmitting,
                    onTierSubmit = submitExtract,
                    onGetCoins = { showGetCoinsDialog = true },
                    onInsightsClick = { showInsightsDialog = true },
                    submittingLabel = when (activeStep) {
                        app.pluct.services.OperationStep.METADATA -> "Getting video details..."
                        app.pluct.services.OperationStep.VEND_TOKEN -> "Claiming credits..."
                        app.pluct.services.OperationStep.SUBMIT -> "Submitting job..."
                        app.pluct.services.OperationStep.POLLING -> "Waiting for transcript..."
                        else -> "Starting..."
                    },
                    submittingHint = "Please wait"
                )

                if (isUrlValid && creditBalance < 2) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showGetCoinsDialog = true },
                        modifier = Modifier.semantics {
                            contentDescription = "Get coins button"
                            testTag = "get_coins_button"
                        }
                    ) {
                        Text("Low credits -- add more")
                    }
                }
            } else {
                PluctProcessingIndicator(
                    currentJobId = currentJobId,
                    debugInfo = debugInfo
                )
            }

            // Removed duplicate status text - button already shows status via submittingLabel
            if (timedOutOnce && !isSubmitting && apiError == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "If it stalls again, tap Request Credits to refresh access.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showGetCoinsDialog) {
        PluctCaptureCardGetCoinsDialog(onDismiss = { showGetCoinsDialog = false })
    }

    if (showInsightsDialog) {
        PluctCaptureCardInsightsDialog(
            urlText = urlText,
            onDismiss = { showInsightsDialog = false }
        )
    }
}
