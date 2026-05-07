package app.pluct.ui.screens

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.PluctCoreAPIDetailedError
import app.pluct.services.PluctCoreUserIdentification
import app.pluct.ui.components.PluctUIComponent05Notification01SnackbarManager
import app.pluct.core.debug.PluctCoreDebug01LogManager
import androidx.compose.material3.SnackbarHostState

/**
 * Pluct-UI-Screen-01MainActivity-05CreditManager
 * Follows naming convention: [Project]-[UI]-[Screen]-[MainActivity]-[CreditManager]
 * 5 scope layers: Project, UI, Screen, MainActivity, CreditManager
 * Handles credit balance fetching and token vending
 */
class PluctUIScreen01MainActivity05CreditManager(
    private val apiService: PluctCoreAPIUnifiedService,
    private val userIdentification: PluctCoreUserIdentification,
    private val debugLogManager: PluctCoreDebug01LogManager
) {
    suspend fun fetchCreditBalance(
        onBalanceLoaded: (Int, Int) -> Unit
    ) {
        PluctUIScreen01MainActivityTranscriptionOrchestrator.loadInitialData(
            apiService, userIdentification, debugLogManager
        ) { balance, freeUses ->
            onBalanceLoaded(balance, freeUses)
        }
    }

    suspend fun vendTokenWithBalanceUpdate(
        reason: String,
        creditBalance: Int,
        freeUsesRemaining: Int,
        hasVendTokenAttempted: Boolean,
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        onBalanceUpdate: (Int, Int) -> Unit,
        onCtaMessageUpdate: (String?) -> Unit,
        onVendTokenAttempted: () -> Unit
    ) {
        // Skip vending when free uses are available or balance is zero
        if (freeUsesRemaining > 0) {
            onVendTokenAttempted()
            debugLogManager.logInfo(
                category = "CREDIT_CHECK",
                operation = "vendToken",
                message = "Skipping vendToken; free uses available",
                details = "Balance=$creditBalance FreeUses=$freeUsesRemaining Reason=$reason"
            )
            return
        }
        if (creditBalance <= 0) {
            onVendTokenAttempted()
            debugLogManager.logWarning(
                category = "CREDIT_CHECK",
                operation = "vendToken",
                message = "Skipping vendToken; no credits available (would trigger 402)",
                details = "Balance=$creditBalance FreeUses=$freeUsesRemaining Reason=$reason"
            )
            onCtaMessageUpdate("Add credits to unlock transcription")
            return
        }

        // Avoid spamming vend-token when we already have credits
        if (creditBalance > 0 && freeUsesRemaining > 0 && reason == "app_launch") {
            onVendTokenAttempted()
            debugLogManager.logInfo(
                category = "CREDIT_CHECK",
                operation = "vendToken",
                message = "Skipping vendToken; credits already available",
                details = "Balance=$creditBalance FreeUses=$freeUsesRemaining"
            )
            return
        }

        onVendTokenAttempted()
        debugLogManager.logInfo(
            category = "CREDIT_CHECK",
            operation = "vendToken",
            message = "Vend token requested",
            details = "Reason: $reason",
            requestUrl = "${app.pluct.core.api.PluctCoreAPI00Constants.BASE_URL}/v1/vend-token",
            requestMethod = "POST",
            requestPayload = """{"userId":"${userIdentification.userId}"}"""
        )

        val vendResult = apiService.vendToken()
        vendResult.fold(
            onSuccess = { vend ->
                val newBalance = maxOf(creditBalance, vend.balanceAfter)
                val newFreeUses = maxOf(freeUsesRemaining, vend.balanceAfter)
                onBalanceUpdate(newBalance, newFreeUses)
                onCtaMessageUpdate(
                    if (vend.balanceAfter >= 1) {
                        "You have ${vend.balanceAfter} free credits"
                    } else {
                        "Add credits to unlock transcription"
                    }
                )
                debugLogManager.logInfo(
                    category = "CREDIT_CHECK",
                    operation = "vendToken",
                    message = "Vend token succeeded",
                    details = "Balance after vend: ${vend.balanceAfter}; Reason: $reason",
                    requestUrl = "${app.pluct.core.api.PluctCoreAPI00Constants.BASE_URL}/v1/vend-token",
                    requestMethod = "POST"
                )

                // UX IMPROVEMENT #5: Ensure balance updates are immediately visible
                // Refresh balance to show updated gem counter immediately
                val balanceResult = apiService.checkUserBalance()
                balanceResult.onSuccess { balance ->
                    // Force immediate UI update
                    onBalanceUpdate(balance.balance, freeUsesRemaining)
                    val totalCredits = maxOf(0, balance.main + balance.bonus)
                    onBalanceUpdate(totalCredits, maxOf(newFreeUses, totalCredits))
                }
            },
            onFailure = { error ->
                val detailed = error as? PluctCoreAPIDetailedError
                val statusCode = detailed?.technicalDetails?.responseStatusCode

                if (detailed != null) {
                    debugLogManager.logAPIError(detailed, "CREDIT_CHECK")
                } else {
                    debugLogManager.logError(
                        category = "CREDIT_CHECK",
                        operation = "vendToken",
                        message = error.message ?: "Vend token failed",
                        exception = error,
                        requestUrl = "${app.pluct.core.api.PluctCoreAPI00Constants.BASE_URL}/v1/vend-token"
                    )
                }

                when (statusCode) {
                    402 -> {
                        onCtaMessageUpdate("Add credits to unlock transcription")
                        PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                            scope,
                            snackbarHostState,
                            "No credits available. Contact support@pluct.app to add credits.",
                            actionLabel = "Request credits"
                        )
                    }
                    401 -> {
                        onCtaMessageUpdate("Authentication required. Please restart the app.")
                        PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                            scope,
                            snackbarHostState,
                            "Session expired. Restart the app to continue.",
                            actionLabel = null
                        )
                    }
                    429 -> {
                        onCtaMessageUpdate("Rate limit exceeded. Please wait a moment.")
                        PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                            scope,
                            snackbarHostState,
                            "Too many requests. Please wait a moment and try again.",
                            actionLabel = null
                        )
                    }
                    else -> {
                        // Network or server error - show user-friendly message
                        val errorMessage = when {
                            error.message?.contains("network", ignoreCase = true) == true -> 
                                "Network error. Check your connection and try again."
                            error.message?.contains("timeout", ignoreCase = true) == true -> 
                                "Request timed out. Please try again."
                            else -> 
                                "Unable to refresh credits. Please try again later."
                        }
                        onCtaMessageUpdate(errorMessage)
                        Log.e("CreditManager", "Balance refresh failed: ${error.message}", error)
                    }
                }
            }
        )
    }
}
