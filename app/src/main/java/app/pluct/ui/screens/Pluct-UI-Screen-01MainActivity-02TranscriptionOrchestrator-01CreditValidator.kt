package app.pluct.ui.screens

import android.util.Log
import app.pluct.core.error.PluctCoreError03UserMessageFormatter
import app.pluct.data.entity.ProcessingTier
import app.pluct.services.PluctCoreAPIDetailedError
import app.pluct.services.PluctCoreValidationInputSanitizer
import javax.inject.Inject

/**
 * Pluct-UI-Screen-01MainActivity-02TranscriptionOrchestrator-01CreditValidator
 * Follows naming convention: [Project]-[UI]-[Screen]-[MainActivity]-[TranscriptionOrchestrator]-[CreditValidator]
 * 6 scope layers: Project, UI, Screen, MainActivity, TranscriptionOrchestrator, CreditValidator
 * 
 * Wrapper around PluctCoreValidationInputSanitizer.validateCredits() for UI layer.
 * Delegates to single source of truth in InputSanitizer to eliminate duplication.
 */
class PluctUIScreen01MainActivityTranscriptionOrchestratorCreditValidator @Inject constructor(
    private val validator: PluctCoreValidationInputSanitizer
) {
    
    /**
     * Result of credit validation
     */
    data class CreditValidationResult(
        val hasEnoughCredits: Boolean,
        val userMessage: String?,
        val error: PluctCoreAPIDetailedError?
    )
    
    /**
     * Validate user has sufficient credits/free uses for requested tier
     * Delegates to InputSanitizer.validateCredits() as single source of truth
     * @param tier The processing tier requested
     * @param currentBalance Current credit balance
     * @param currentFreeUses Current free uses remaining
     * @return CreditValidationResult with validation outcome
     */
    fun validateCredits(
        tier: ProcessingTier,
        currentBalance: Int,
        currentFreeUses: Int
    ): CreditValidationResult {
        // Use unified validator as single source of truth
        val validationResult = validator.validateCredits(tier, currentBalance, currentFreeUses)
        
        if (validationResult.isValid) {
            return CreditValidationResult(
                hasEnoughCredits = true,
                userMessage = null,
                error = null
            )
        }
        
        // Insufficient credits - format user-friendly error message using unified formatter
        Log.w("CreditValidator", "Insufficient credits for tier: $tier")
        val userMessage = PluctCoreError03UserMessageFormatter.formatUserMessage(
            error = null,
            technicalMessage = validationResult.errorMessage ?: "Insufficient credits",
            errorCode = "INSUFFICIENT_CREDITS",
            httpStatus = 402,
            context = "video processing"
        )
        
        // Create a detailed error for the handler
        val error = PluctCoreAPIDetailedError(
            userMessage = userMessage.message,
            technicalDetails = app.pluct.services.TechnicalErrorDetails(
                serviceName = "TranscriptionOrchestrator",
                operation = "Check Credits",
                endpoint = "local",
                requestMethod = "CHECK",
                requestUrl = "local",
                responseStatusCode = 402,
                errorCode = "INSUFFICIENT_CREDITS"
            )
        )
        
        return CreditValidationResult(
            hasEnoughCredits = false,
            userMessage = userMessage.message,
            error = error
        )
    }
}
