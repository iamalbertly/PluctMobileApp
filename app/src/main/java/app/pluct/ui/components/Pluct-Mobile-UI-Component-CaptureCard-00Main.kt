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
import app.pluct.core.network.PluctNetworkConnectivityChecker
import app.pluct.ui.components.PluctUIComponent03CaptureCard05State01Manager
import app.pluct.ui.components.PluctUIComponent03CaptureCard06Queue01Prompt
import kotlinx.coroutines.delay
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.QueueReason
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.PluctCoreValidationInputSanitizer
import app.pluct.services.OperationStep
import app.pluct.ui.models.data.PersistentError
import app.pluct.core.credit.PluctCoreCredit01AtomicReservation01Service
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import app.pluct.notification.PluctNotificationHelper
import app.pluct.core.permission.PluctCorePermission01Manager

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
    onViewInLogs: (() -> Unit)? = null,
    onQueueForLater: ((String, QueueReason) -> Unit)? = null,
    isLoadingCreditBalance: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var urlText by remember { mutableStateOf(preFilledUrl ?: "") }
    var hasFocus by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showGetCoinsDialog by remember { mutableStateOf(false) }
    var showInsightsDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf<String?>(null) }
    var persistentError by remember { mutableStateOf<PersistentError?>(null) }
    var isPrefilledUrl by remember { mutableStateOf(preFilledUrl != null && preFilledUrl.isNotBlank()) }
    var timedOutOnce by remember { mutableStateOf(false) }
    var showQueuePrompt by remember { mutableStateOf(false) }
    var queuePromptReason by remember { mutableStateOf<String?>(null) }
    var isAutoSubmitting by remember { mutableStateOf(false) }
    var lastStepChangeTime by remember { mutableStateOf<Long?>(null) }
    var lastObservedStep by remember { mutableStateOf<OperationStep?>(null) }
    val debugInfo by (apiService?.transcriptionDebugFlow ?: MutableStateFlow(null)).collectAsState()
    val sanitizer = remember { PluctCoreValidationInputSanitizer() }
    val currentValidation = remember(urlText) { sanitizer.validateUrl(urlText) }
    val isUrlValid = urlText.isNotBlank() && currentValidation.isValid
    val atomicCreditService = remember { PluctCoreCredit01AtomicReservation01Service() }
    var progressNotificationsReady by remember { mutableStateOf(PluctCorePermission01Manager.hasNotificationPermission(context)) }
    var backgroundProcessingReady by remember { mutableStateOf(PluctCorePermission01Manager.isBatteryOptimizationExempt(context)) }

    fun refreshProgressReliabilityState() {
        PluctCorePermission01Manager.invalidateCache()
        progressNotificationsReady = PluctCorePermission01Manager.hasNotificationPermission(context)
        backgroundProcessingReady = PluctCorePermission01Manager.isBatteryOptimizationExempt(context)
    }

    LaunchedEffect(isSubmitting, preFilledUrl) {
        refreshProgressReliabilityState()
    }

    // Define submitExtract before LaunchedEffects that use it
    val submitExtract: () -> Unit = submit@{
        Log.d("CaptureCard", "=== submitExtract CALLED ===")
        Log.d("CaptureCard", "isSubmitting=$isSubmitting, urlText='$urlText', apiService=${if (apiService != null) "present" else "NULL"}")
        
        if (isSubmitting) {
            Log.d("CaptureCard", "Already submitting, returning early")
            return@submit
        }
        isSubmitting = true
        validationError = null
        persistentError = null
        processingMessage = null
        timedOutOnce = false
        showQueuePrompt = false
        queuePromptReason = null
        Log.d("CaptureCard", "Set isSubmitting=true")

        val validationResult = sanitizer.validateUrl(urlText)
        val normalizedUrl = if (validationResult.isValid) validationResult.sanitizedValue else urlText
        Log.d("CaptureCard", "URL validation: isValid=${validationResult.isValid}, normalizedUrl='$normalizedUrl', error=${validationResult.errorMessage}")
        
        if (!validationResult.isValid) {
            Log.w("CaptureCard", "URL validation failed: ${validationResult.errorMessage}")
            validationError = validationResult.errorMessage
            urlText = ""
            isSubmitting = false
            isAutoSubmitting = false
            return@submit
        }

        if (creditBalance < 1 && freeUsesRemaining <= 0) {
            Log.d("CaptureCard", "No credits available; queueing instead of submitting")
            onQueueForLater?.invoke(normalizedUrl, QueueReason.INSUFFICIENT_CREDITS)
            showQueuePrompt = true
            queuePromptReason = "Insufficient credits (need 1 credit or free use)"
            isSubmitting = false
            isAutoSubmitting = false
            return@submit
        }

        // Atomically reserve credit before submission
        var reservationId: String? = null
        scope.launch {
            val reservationResult = atomicCreditService.reserveCredit(
                amount = 1,
                currentBalance = creditBalance,
                currentFreeUses = freeUsesRemaining
            )
            
            if (!reservationResult.success) {
                // Insufficient credits - auto-queue
                if (onQueueForLater != null) {
                    onQueueForLater(normalizedUrl, QueueReason.INSUFFICIENT_CREDITS)
                }
                isSubmitting = false
                isAutoSubmitting = false
                return@launch
            }
            
            reservationId = reservationResult.reservationId
            Log.d("CaptureCard", "Credit reserved: $reservationId")
            
            // UX FIX: Removed duplicate notification - Worker handles initial notification
            // The background worker (Pluct-Core-Background-01TranscriptionWorker) will show
            // the progress notification when it starts processing, preventing duplicate notifications
        }

        val onComplete: () -> Unit = {
            Log.d("CaptureCard", "onComplete called - resetting state")
            processingMessage = null
            urlText = ""
            validationError = null
            persistentError = null
            isSubmitting = false
            timedOutOnce = false
            // Commit reservation on success
            reservationId?.let { id ->
                scope.launch {
                    atomicCreditService.commitReservation(id)
                }
            }
        }

        val costLabel = if (freeUsesRemaining > 0) "Free (free uses left: $freeUsesRemaining)" else "Costs 1 credit (balance: $creditBalance)"
        processingMessage = "Starting transcription... $costLabel"
        Log.d("CaptureCard", "Set processingMessage: $processingMessage")

        if (apiService != null) {
            Log.d("CaptureCard", "API service available, calling handleCompleteAPIFlow")
            PluctUIComponent03CaptureCardAPIFlow.handleCompleteAPIFlow(
                normalizedUrl = normalizedUrl,
                apiService = apiService,
                debugLogManager = debugLogManager,
                onSuccess = {
                    Log.d("CaptureCard", "API flow completed successfully")
                    persistentError = null // Clear on success
                    onTierSubmit(normalizedUrl, ProcessingTier.EXTRACT_SCRIPT)
                    onComplete()
                },
                onError = { error ->
                    Log.e("CaptureCard", "API flow failed: $error")
                    processingMessage = null
                    reservationId?.let { id ->
                        scope.launch {
                            atomicCreditService.releaseReservation(id)
                        }
                    }
                    // Create persistent error that won't auto-dismiss
                    persistentError = PersistentError(
                        message = error,
                        url = normalizedUrl,
                        timestamp = System.currentTimeMillis(),
                        category = "API_ERROR"
                    )
                    isSubmitting = false
                    isAutoSubmitting = false
                },
                context = context,
                shouldMinimize = isAutoSubmitting // Minimize when auto-submitting
            )
        } else {
            Log.w("CaptureCard", "⚠️ API service is NULL, using fallback submit")
            onTierSubmit(normalizedUrl, ProcessingTier.EXTRACT_SCRIPT)
            onComplete()
        }
    }

    // Clear error only on successful completion (not auto-dismiss)
    LaunchedEffect(debugInfo?.currentStep) {
        if (debugInfo?.currentStep == OperationStep.COMPLETED) {
            persistentError = null
            processingMessage = null
            isSubmitting = false
            isAutoSubmitting = false
            lastStepChangeTime = null
            lastObservedStep = null
        } else if (debugInfo?.currentStep == OperationStep.FAILED) {
            processingMessage = null
            isSubmitting = false
            isAutoSubmitting = false
            lastStepChangeTime = null
            lastObservedStep = null
        }
    }

    LaunchedEffect(preFilledUrl) {
        if (!preFilledUrl.isNullOrBlank()) {
            val validationResult = sanitizer.validateUrl(preFilledUrl)
            if (validationResult.isValid) {
                val sanitizedUrl = validationResult.sanitizedValue
                urlText = sanitizedUrl
                isPrefilledUrl = true
                validationError = null
                persistentError = null
                if (sanitizer.isTikTokUrl(sanitizedUrl)) {
                    // TECHNICAL DEBT CLEANUP #1: Removed pre-warming call (function deprecated)
                }
            } else {
                validationError = validationResult.errorMessage
            }
        }
    }

    // Auto-submit on intent receive when credits available
    // Wait for credit balance to finish loading before deciding to submit or queue
    LaunchedEffect(preFilledUrl, creditBalance, freeUsesRemaining, isSubmitting, isLoadingCreditBalance) {
        Log.d("CaptureCard", "Auto-submit LaunchedEffect triggered: preFilledUrl=$preFilledUrl, creditBalance=$creditBalance, freeUsesRemaining=$freeUsesRemaining, isSubmitting=$isSubmitting, isLoadingCreditBalance=$isLoadingCreditBalance")
        
        if (!preFilledUrl.isNullOrBlank() && 
            !isSubmitting && 
            !isAutoSubmitting &&
            !isLoadingCreditBalance && // Wait for balance to finish loading
            (creditBalance >= 1 || freeUsesRemaining > 0)) {
            
            Log.d("CaptureCard", "Auto-submit conditions met, validating URL: $preFilledUrl")
            val validationResult = sanitizer.validateUrl(preFilledUrl)
            if (validationResult.isValid) {
                isAutoSubmitting = true
                Log.d("CaptureCard", "URL validated, waiting 800ms before auto-submit...")
                // Technical Debt #1: Use timing constants instead of hardcoded delays
                delay(app.pluct.core.timing.PluctCoreTiming01Constants.UI_STATE_PROPAGATION_DELAY_MS)
                
                // Check credits again (may have changed)
                val currentBalance = creditBalance
                val currentFreeUses = freeUsesRemaining
                
                if (currentBalance >= 1 || currentFreeUses > 0) {
                    Log.d("CaptureCard", "Auto-submitting URL: $preFilledUrl (balance=$currentBalance, freeUses=$currentFreeUses)")
                    submitExtract()
                } else {
                    Log.d("CaptureCard", "Credits depleted during delay, queueing instead")
                    // Credits depleted, queue instead
                    if (onQueueForLater != null) {
                        onQueueForLater(validationResult.sanitizedValue, QueueReason.INSUFFICIENT_CREDITS)
                    }
                    isAutoSubmitting = false
                }
            } else {
                Log.w("CaptureCard", "URL validation failed: ${validationResult.errorMessage}")
                isAutoSubmitting = false
            }
        } else {
            if (preFilledUrl.isNullOrBlank()) {
                Log.d("CaptureCard", "Auto-submit skipped: preFilledUrl is blank")
            } else if (isSubmitting) {
                Log.d("CaptureCard", "Auto-submit skipped: already submitting")
            } else if (isAutoSubmitting) {
                Log.d("CaptureCard", "Auto-submit skipped: already auto-submitting")
            } else if (isLoadingCreditBalance) {
                Log.d("CaptureCard", "Auto-submit skipped: credit balance still loading")
            } else if (creditBalance < 1 && freeUsesRemaining <= 0) {
                Log.d("CaptureCard", "Auto-submit skipped: insufficient credits (balance=$creditBalance, freeUses=$freeUsesRemaining)")
                val validationResult = sanitizer.validateUrl(preFilledUrl)
                if (validationResult.isValid) {
                    onQueueForLater?.invoke(validationResult.sanitizedValue, QueueReason.INSUFFICIENT_CREDITS)
                    showQueuePrompt = true
                    queuePromptReason = "Insufficient credits (need 1 credit or free use)"
                }
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
                    // TECHNICAL DEBT CLEANUP #1: Removed pre-warming call (function deprecated)
                }
            }
        }
    }
    
    // Pre-validation: Check network/credits before submission
    LaunchedEffect(urlText, creditBalance, freeUsesRemaining, isSubmitting) {
        val (shouldShow, reason) = PluctUIComponent03CaptureCard05State01Manager.shouldShowQueuePrompt(
            urlText = urlText,
            creditBalance = creditBalance,
            freeUsesRemaining = freeUsesRemaining,
            isSubmitting = isSubmitting,
            context = context,
            isUrlValid = isUrlValid
        )
        showQueuePrompt = shouldShow
        queuePromptReason = reason
    }

    // Intelligent timeout: track API progress via step changes
    LaunchedEffect(isSubmitting, debugInfo?.currentStep) {
        PluctUIComponent03CaptureCard05State01Manager.monitorTimeout(
            isSubmitting = isSubmitting,
            debugInfo = debugInfo,
            context = context,
            onTimeout = { error ->
                persistentError = error
                isSubmitting = false
                processingMessage = null
                timedOutOnce = true
            },
            lastStepChangeTime = lastStepChangeTime,
            lastObservedStep = lastObservedStep,
            updateLastStepChangeTime = { lastStepChangeTime = it },
            updateLastObservedStep = { lastObservedStep = it }
        )
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Capture Video" }
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Helper message as collapsible info icon (progressive disclosure)
            ctaHelperMessage?.let { helper ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    var showDialog by remember { mutableStateOf(false) }
                    
                    IconButton(
                        onClick = { showDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .semantics {
                                contentDescription = "Show help information"
                                testTag = "helper_info_button"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    if (showDialog) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { Text("Information") },
                            text = {
                                Column {
                                    Text(
                                        text = helper,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (helper.contains("Add credits", ignoreCase = true) && onRequestCredits != null) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        TextButton(onClick = { 
                                            showDialog = false
                                            onRequestCredits()
                                        }) {
                                            Text("Request Credits")
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showDialog = false }) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                }
            }
            
            PluctURLInputField(
                urlText = urlText,
                onUrlTextChange = { newValue ->
                    if (persistentError != null) {
                        persistentError = null
                    }
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
                videoRepository = videoRepository,
                onSubmit = if (isUrlValid && !isSubmitting) { { submitExtract() } } else null,
                freeUsesRemaining = freeUsesRemaining,
                creditBalance = creditBalance,
                isSubmitting = isSubmitting
            )

            // Queue prompt for offline/no-credit scenarios
            PluctUIComponent03CaptureCard06Queue01Prompt(
                showQueuePrompt = showQueuePrompt,
                queuePromptReason = queuePromptReason,
                urlText = urlText,
                isSubmitting = isSubmitting,
                onQueueForLater = { url, reason ->
                    onQueueForLater?.invoke(url, reason)
                    showQueuePrompt = false
                },
                onRequestCredits = onRequestCredits,
                onShowGetCoinsDialog = { showGetCoinsDialog = true },
                sanitizer = sanitizer
            )

            val shouldShowProgressFix = (isUrlValid || isSubmitting || isAutoSubmitting || !preFilledUrl.isNullOrBlank()) &&
                (!progressNotificationsReady || (isSubmitting && !backgroundProcessingReady))
            if (shouldShowProgressFix) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Progress permission fix"
                            testTag = "progress_permission_fix"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (!progressNotificationsReady) Icons.Default.Notifications else Icons.Default.BatterySaver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!progressNotificationsReady) "Bell -> Progress" else "Battery -> Keep",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(
                        onClick = {
                            if (!progressNotificationsReady) {
                                PluctCorePermission01Manager.openNotificationSettings(context)
                            } else {
                                PluctCorePermission01Manager.openBatteryOptimizationSettings(context)
                            }
                            scope.launch {
                                repeat(4) {
                                    delay(1500)
                                    refreshProgressReliabilityState()
                                }
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Fix progress permission"
                            testTag = "progress_permission_fix_button"
                        }
                    ) {
                        Text("Fix")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Processing state is now handled by inline submit button with CircularProgressIndicator
            if (persistentError == null && isSubmitting) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Unified error state: only show API errors in banner, validation errors show inline in input field
            val unifiedErrorState = remember(persistentError, showQueuePrompt) {
                when {
                    showQueuePrompt -> null // Queue prompt handles error display
                    persistentError != null -> persistentError
                    else -> null
                }
            }

            // Show persistent error banner (doesn't auto-dismiss) - only API errors, not validation errors
            // Validation errors are shown inline below the input field
            unifiedErrorState?.let { error ->
                PluctUIComponent03CaptureCardErrorDisplay(
                    message = error.message,
                    onDismiss = { persistentError = null }, // Manual dismiss only
                    error = error.error,
                    debugInfo = debugInfo,
                    debugLogManager = debugLogManager,
                    onViewInLogs = onViewInLogs,
                    onAddCredits = { showGetCoinsDialog = true },
                    onCheckConnection = {
                        // Could open network settings or just retry
                        persistentError = null // Clear on retry attempt
                        isSubmitting = true
                        processingMessage = "Retrying..."
                        submitExtract()
                    },
                    onRetry = {
                        persistentError = null // Clear on retry attempt
                        isSubmitting = true
                        processingMessage = "Retrying..."
                        submitExtract()
                    }
                )
            }

            val activeStep = debugInfo?.currentStep

            // Keep the in-flight message aligned with the latest backend step so users see movement.
            // Remove duplicate processingMessage when button shows status
            LaunchedEffect(activeStep, isSubmitting) {
                if (isSubmitting && activeStep != null) {
                    // Button shows status, don't duplicate with processingMessage
                    processingMessage = null
                }
            }

            // Processing indicator only shown when actively processing
            // Inline submit button in input field handles submission, no separate button needed
            if (isProcessing) {
                PluctProcessingIndicator(
                    currentJobId = currentJobId,
                    debugInfo = debugInfo
                )
            }
            
            // Low credits warning (only when URL is valid and credits are low)
            if (isUrlValid && freeUsesRemaining <= 0 && creditBalance in 1..1 && !isProcessing) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { showGetCoinsDialog = true },
                    modifier = Modifier.semantics {
                        contentDescription = "Get coins button"
                        testTag = "get_coins_button"
                    }
                ) {
                    Text("Low credits - add more")
                }
            }

            // Removed duplicate status text - button already shows status via submittingLabel
            if (timedOutOnce && !isSubmitting && persistentError == null) {
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
