package app.pluct.data.manager

import android.content.Context
import android.util.Log
import app.pluct.data.service.HuggingFaceTranscriptionService
import app.pluct.ui.utils.TranscriptProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Transcription-Manager-Coordinator - Simplified coordinator for transcription management
 */
@Singleton
class PluctTranscriptionManagerCoordinator @Inject constructor(
    private val context: Context,
    private val huggingFaceService: HuggingFaceTranscriptionService
) {
    companion object {
        private const val TAG = "PluctTranscriptionManagerCoordinator"
    }
    
    private val providerSelector = PluctTranscriptionProviderSelector(context)
    private val executor = PluctTranscriptionExecutor(huggingFaceService)
    
    suspend fun transcribeVideo(
        videoUrl: String,
        onProgress: (String) -> Unit = {},
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            Log.d(TAG, "Starting transcription for: $videoUrl")
            
            val availableProviders = providerSelector.getAvailableProviders()
            Log.d(TAG, "Available providers: ${availableProviders.map { it.name }}")
            
            if (availableProviders.isEmpty()) {
                Log.e(TAG, "No transcription providers available")
                onError("no_providers_available")
                return
            }
            
            for (provider in availableProviders) {
                Log.d(TAG, "Trying provider: ${provider.name}")
                onProgress("Trying ${provider.name}...")
                
                val success = when (provider) {
                    TranscriptProvider.HUGGINGFACE -> executor.tryHuggingFaceTranscription(videoUrl, onProgress, onSuccess, onError)
                    TranscriptProvider.TOKAUDIT -> executor.tryTokAuditTranscription(videoUrl, onProgress, onSuccess, onError)
                    TranscriptProvider.GETTRANSCRIBE -> executor.tryGetTranscribeTranscription(videoUrl, onProgress, onSuccess, onError)
                    TranscriptProvider.OPENAI -> executor.tryOpenAITranscription(videoUrl, onProgress, onSuccess, onError)
                }
                
                if (success) {
                    return
                }
                
                Log.w(TAG, "Provider ${provider.name} failed, trying next...")
            }
            
            Log.e(TAG, "All transcription providers failed")
            onError("all_providers_failed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in transcription manager: ${e.message}", e)
            onError("manager_error: ${e.message}")
        }
    }
    
    fun getPrimaryProvider(): TranscriptProvider {
        return providerSelector.getPrimaryProvider()
    }
    
    fun hasAvailableProviders(): Boolean {
        return providerSelector.hasAvailableProviders()
    }
    
    suspend fun getProviderStatus(): Map<TranscriptProvider, Boolean> {
        return providerSelector.getProviderStatus()
    }
}
