package app.pluct.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import app.pluct.data.preferences.PluctUserPreferences
import app.pluct.services.PluctCoreValidationInputSanitizer

/**
 * Pluct-UI-Screen-01MainActivity-01IntentHandler - Intent handling logic
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Single source of truth for intent processing
 */
object PluctUIScreen01MainActivityIntentHandler {
    
    /**
     * Handle TikTok intent from share or deep link
     */
    fun handleTikTokIntent(
        intent: Intent,
        context: android.content.Context,
        validator: PluctCoreValidationInputSanitizer
    ) {
        Log.d("IntentHandler", "Handling intent: ${intent.action}")
        try {
            when (intent.action) {
                Intent.ACTION_SEND -> {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    Log.d("IntentHandler", "Received shared text: $sharedText")
                    processSharedTikTokText(sharedText, context, validator)
                }
                Intent.ACTION_VIEW -> {
                    val uri = intent.data
                    Log.d("IntentHandler", "Received URI: $uri")
                    
                    if (uri?.scheme == "pluct") {
                        when (uri.host) {
                            "ingest" -> {
                                val url = uri.getQueryParameter("url")
                                Log.d("IntentHandler", "Deep link URL: $url")
                                if (url != null && validator.validateUrl(url).isValid) {
                                    PluctUserPreferences.setPrefilledUrl(context, url)
                                    PluctUserPreferences.setIntentFeedback(
                                        context,
                                        "TikTok video link received!",
                                        false
                                    )
                                } else {
                                    PluctUserPreferences.setIntentFeedback(
                                        context,
                                        "Shared link is missing or not a TikTok URL.",
                                        true
                                    )
                                }
                            }
                            "debug" -> {
                                Log.d("IntentHandler", "Debug deep link received")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IntentHandler", "Error handling intent: ${e.message}", e)
            PluctUserPreferences.setIntentFeedback(
                context,
                "An error occurred while processing the shared content. Please try again.",
                true
            )
        }
    }
    
    /**
     * Process shared TikTok text and extract URL
     */
    private fun processSharedTikTokText(
        sharedText: String?,
        context: android.content.Context,
        validator: PluctCoreValidationInputSanitizer
    ) {
        if (sharedText.isNullOrBlank()) {
            Log.w("IntentHandler", "Shared text is null or blank")
            PluctUserPreferences.setIntentFeedback(
                context,
                "No content was shared. Please share a valid TikTok video link.",
                true
            )
            return
        }

        // Extract first URL from shared text
        val urlRegex = Regex("(https?://[^\\s]+)", RegexOption.IGNORE_CASE)
        val extractedUrl = urlRegex.find(sharedText)?.value?.trim()
        
        if (extractedUrl == null) {
            Log.w("IntentHandler", "Unable to extract URL from shared text.")
            PluctUserPreferences.setIntentFeedback(
                context,
                "Could not find a link in the shared content. Copy the TikTok link and try again.",
                true
            )
            return
        }

        // Use unified validator service
        val validationResult = validator.validateUrl(extractedUrl)
        if (!validationResult.isValid) {
            Log.w("IntentHandler", "Shared URL is not valid: $extractedUrl - ${validationResult.errorMessage}")
            PluctUserPreferences.setIntentFeedback(
                context,
                validationResult.errorMessage ?: "Only TikTok video links are supported. Please share a TikTok URL.",
                true
            )
            return
        }

        Log.d("IntentHandler", "TikTok URL detected: $extractedUrl")
        PluctUserPreferences.setPrefilledUrl(context, validationResult.sanitizedValue)
        PluctUserPreferences.setIntentFeedback(
            context,
            "TikTok video link received!",
            false
        )
    }
}






