package app.pluct.core.categorization

import android.util.Log
import app.pluct.services.PluctCoreAPIDetailedError

/**
 * Pluct-Core-Categorization-01ErrorClassifier - Centralized error categorization logic
 * Follows naming convention: [Project]-[Scope]-[Module]-[CoreResponsibility]
 * 
 * TECH DEBT FIX #2: Single source of truth for error categorization
 * Consolidates error categorization that was duplicated across:
 * - PluctUIError01UnifiedHandler
 * - PluctUIComponent03CaptureCardErrorDisplay
 * - PluctCoreErrorUserMessageFormatter03
 */
object PluctCoreCategorization01ErrorClassifier {
    private const val TAG = "ErrorClassifier"

    enum class ErrorCategory {
        NETWORK,           // Connection, timeout, DNS, SSL
        AUTHENTICATION,    // Auth token invalid/expired
        VALIDATION,        // Input validation, malformed request
        INSUFFICIENT_CREDITS, // Not enough credits
        RATE_LIMIT,        // Too many requests
        SERVER_ERROR,      // 5xx errors
        UNKNOWN            // Unclassified
    }

    data class CategorizedError(
        val category: ErrorCategory,
        val userFriendlyMessage: String,
        val isRetryable: Boolean,
        val suggestedActions: List<String>
    )

    /**
     * Categorize an error and provide user-friendly context
     */
    fun categorizeError(error: Throwable?, message: String? = null): CategorizedError {
        val finalMessage = message ?: error?.message ?: "Unknown error"
        val lowerMessage = finalMessage.lowercase()

        // Categorize based on error details
        val category = when {
            error is PluctCoreAPIDetailedError -> categorizDetailedError(error, lowerMessage)
            else -> categorizeByMessage(lowerMessage)
        }

        // Provide user-friendly message and actions based on category
        return when (category) {
            ErrorCategory.NETWORK -> CategorizedError(
                category = ErrorCategory.NETWORK,
                userFriendlyMessage = "Connection issue. Please check your internet and try again.",
                isRetryable = true,
                suggestedActions = listOf("Retry", "Check Connection")
            )
            ErrorCategory.AUTHENTICATION -> CategorizedError(
                category = ErrorCategory.AUTHENTICATION,
                userFriendlyMessage = "Authentication failed. Please sign in again.",
                isRetryable = false,
                suggestedActions = listOf("Sign In Again")
            )
            ErrorCategory.VALIDATION -> CategorizedError(
                category = ErrorCategory.VALIDATION,
                userFriendlyMessage = "Invalid input. Please check your entry and try again.",
                isRetryable = false,
                suggestedActions = listOf("Edit Input")
            )
            ErrorCategory.INSUFFICIENT_CREDITS -> CategorizedError(
                category = ErrorCategory.INSUFFICIENT_CREDITS,
                userFriendlyMessage = "Not enough credits. Add credits to continue.",
                isRetryable = false,
                suggestedActions = listOf("Add Credits")
            )
            ErrorCategory.RATE_LIMIT -> CategorizedError(
                category = ErrorCategory.RATE_LIMIT,
                userFriendlyMessage = "Too many requests. Wait a moment and try again.",
                isRetryable = true,
                suggestedActions = listOf("Wait and Retry")
            )
            ErrorCategory.SERVER_ERROR -> CategorizedError(
                category = ErrorCategory.SERVER_ERROR,
                userFriendlyMessage = "Server is temporarily unavailable. Please try again soon.",
                isRetryable = true,
                suggestedActions = listOf("Retry", "Try Later")
            )
            ErrorCategory.UNKNOWN -> CategorizedError(
                category = ErrorCategory.UNKNOWN,
                userFriendlyMessage = "An error occurred. Please try again.",
                isRetryable = true,
                suggestedActions = listOf("Retry")
            )
        }
    }

    /**
     * Categorize a detailed API error with HTTP status code
     */
    private fun categorizDetailedError(error: PluctCoreAPIDetailedError, lowerMessage: String): ErrorCategory {
        val statusCode = error.technicalDetails.responseStatusCode

        return when {
            // Network-level errors
            lowerMessage.contains("timeout") || lowerMessage.contains("connection") ||
            lowerMessage.contains("network") || lowerMessage.contains("unavailable") ||
            statusCode == 408 -> ErrorCategory.NETWORK

            // Authentication errors
            statusCode == 401 || statusCode == 403 ||
            lowerMessage.contains("authentication") || lowerMessage.contains("unauthorized") ->
                ErrorCategory.AUTHENTICATION

            // Validation errors
            statusCode == 400 ||
            lowerMessage.contains("invalid") || lowerMessage.contains("validation") ->
                ErrorCategory.VALIDATION

            // Credit/payment errors
            statusCode == 402 ||
            lowerMessage.contains("insufficient") || lowerMessage.contains("credits") ->
                ErrorCategory.INSUFFICIENT_CREDITS

            // Rate limiting
            statusCode == 429 -> ErrorCategory.RATE_LIMIT

            // Server errors
            statusCode >= 500 -> ErrorCategory.SERVER_ERROR

            else -> ErrorCategory.UNKNOWN
        }
    }

    /**
     * Categorize error based on message pattern alone
     */
    private fun categorizeByMessage(lowerMessage: String): ErrorCategory {
        return when {
            lowerMessage.contains("timeout") || lowerMessage.contains("connection") ||
            lowerMessage.contains("network") || lowerMessage.contains("unavailable") ||
            lowerMessage.contains("reset by peer") -> ErrorCategory.NETWORK

            lowerMessage.contains("authentication") || lowerMessage.contains("unauthorized") ||
            lowerMessage.contains("invalid token") || lowerMessage.contains("expired") ->
                ErrorCategory.AUTHENTICATION

            lowerMessage.contains("invalid") || lowerMessage.contains("validation") ->
                ErrorCategory.VALIDATION

            lowerMessage.contains("insufficient") || lowerMessage.contains("credits") ||
            lowerMessage.contains("payment") -> ErrorCategory.INSUFFICIENT_CREDITS

            lowerMessage.contains("rate limit") || lowerMessage.contains("too many") ->
                ErrorCategory.RATE_LIMIT

            lowerMessage.contains("server") || lowerMessage.contains("service unavailable") ->
                ErrorCategory.SERVER_ERROR

            else -> ErrorCategory.UNKNOWN
        }
    }
}
