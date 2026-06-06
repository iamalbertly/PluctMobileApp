package app.pluct.ui.screens

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import app.pluct.shared.PluctRequestIds
import app.pluct.services.PluctCoreUserIdentification
import app.pluct.core.debug.PluctCoreDebug01LogManager
import app.pluct.ui.components.PluctUIComponent05Notification01SnackbarManager
import androidx.compose.material3.SnackbarHostState
import app.pluct.core.api.PluctCoreAPI00Constants
import app.pluct.services.PluctCoreAPIUnifiedService

/**
 * Pluct-UI-Screen-01MainActivity-07CreditRequestHandler
 * Follows naming convention: [Project]-[UI]-[Screen]-[MainActivity]-[CreditRequestHandler]
 * 5 scope layers: Project, UI, Screen, MainActivity, CreditRequestHandler
 * Handles credit request logic extracted from MainActivity
 */
object PluctUIScreen01MainActivity07CreditRequestHandler {
    
    @Composable
    fun createOnRequestCredits(
        apiService: PluctCoreAPIUnifiedService,
        userIdentification: PluctCoreUserIdentification,
        debugLogManager: PluctCoreDebug01LogManager,
        snackbarHostState: SnackbarHostState
    ): (String) -> Unit {
        val scope = rememberCoroutineScope()
        
        return { confirmation ->
            val requestId = PluctRequestIds.generateCreditRequestId()
            val userId = userIdentification.userId
            val timestamp = System.currentTimeMillis()

            Log.d("PluctAPI", "CREDIT_REQUEST requestCredits id=$requestId userId=$userId confirmation=$confirmation timestamp=$timestamp")
            
            // Log BEFORE request
            debugLogManager.logInfo(
                category = "CREDIT_REQUEST",
                operation = "requestCredits",
                message = "Credit request initiated",
                details = buildString {
                    appendLine("Request ID: $requestId")
                    appendLine("User ID: $userId")
                    appendLine("Confirmation Text: $confirmation")
                    appendLine("Timestamp: $timestamp")
                },
                requestUrl = "${PluctCoreAPI00Constants.BASE_URL}/v1/credits/request",
                requestMethod = "POST",
                requestPayload = buildString {
                    appendLine("{")
                    appendLine("  \"userId\": \"$userId\",")
                    appendLine("  \"confirmation\": \"$confirmation\",")
                    appendLine("  \"clientRequestId\": \"$requestId\",")
                    appendLine("  \"timestamp\": $timestamp")
                    appendLine("}")
                }
            )
            
            // Log success (since this is a manual confirmation flow)
            scope.launch {
                apiService.requestCreditTopUp(confirmation, requestId).fold(
                    onSuccess = {
                        debugLogManager.logInfo(
                            category = "CREDIT_REQUEST",
                            operation = "requestCredits",
                            message = "Credit request acknowledged",
                            details = buildString {
                                appendLine("Request ID: $requestId")
                                appendLine("Status: Pending admin review")
                                appendLine("User will be notified when credits are applied")
                            },
                            requestUrl = "${PluctCoreAPI00Constants.BASE_URL}/v1/credits/request",
                            requestMethod = "POST"
                        )
                        PluctUIComponent05Notification01SnackbarManager.showSuccessAsync(
                            scope,
                            snackbarHostState,
                            "Request sent (ID: ${requestId.take(8)}). We'll verify your payment and apply credits."
                        )
                    },
                    onFailure = { error ->
                        debugLogManager.logError(
                            category = "CREDIT_REQUEST",
                            operation = "requestCredits",
                            message = error.message ?: "Credit request failed",
                            exception = error,
                            requestUrl = "${PluctCoreAPI00Constants.BASE_URL}/v1/credits/request"
                        )
                        PluctUIComponent05Notification01SnackbarManager.showErrorAsync(
                            scope,
                            snackbarHostState,
                            "Could not send request. Check your connection and try again.",
                            actionLabel = null
                        )
                    }
                )
            }
        }
    }
}
