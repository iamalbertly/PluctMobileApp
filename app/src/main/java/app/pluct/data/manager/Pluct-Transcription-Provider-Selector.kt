package app.pluct.data.manager

import android.content.Context
import app.pluct.ui.utils.ProviderSettings
import app.pluct.ui.utils.TranscriptProvider

/**
 * Pluct-Transcription-Provider-Selector - Selects and manages transcription providers
 */
class PluctTranscriptionProviderSelector(private val context: Context) {
    
    fun getAvailableProviders(): List<TranscriptProvider> {
        return ProviderSettings.getAvailableProviders(context)
    }
    
    fun getPrimaryProvider(): TranscriptProvider {
        val availableProviders = getAvailableProviders()
        return availableProviders.firstOrNull() ?: TranscriptProvider.HUGGINGFACE
    }
    
    fun hasAvailableProviders(): Boolean {
        return getAvailableProviders().isNotEmpty()
    }
    
    suspend fun getProviderStatus(): Map<TranscriptProvider, Boolean> {
        val status = mutableMapOf<TranscriptProvider, Boolean>()
        
        status[TranscriptProvider.HUGGINGFACE] = ProviderSettings.isProviderEnabled(context, TranscriptProvider.HUGGINGFACE)
        status[TranscriptProvider.TOKAUDIT] = ProviderSettings.isProviderEnabled(context, TranscriptProvider.TOKAUDIT)
        status[TranscriptProvider.GETTRANSCRIBE] = ProviderSettings.isProviderEnabled(context, TranscriptProvider.GETTRANSCRIBE)
        status[TranscriptProvider.OPENAI] = ProviderSettings.isProviderEnabled(context, TranscriptProvider.OPENAI) && 
                                          !ProviderSettings.getApiKey(context, TranscriptProvider.OPENAI).isNullOrBlank()
        
        return status
    }
}
