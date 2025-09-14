package app.pluct.ui.utils

import android.content.Context
import android.preference.PreferenceManager

enum class TranscriptProvider { TOKAUDIT, GETTRANSCRIBE, OPENAI }

object ProviderSettings {
    private const val KEY_PROVIDER = "transcript_provider"
    private const val KEY_TOKAUDIT_API_KEY = "tokaudit_api_key"
    private const val KEY_GETTRANSCRIBE_API_KEY = "gettranscribe_api_key"
    private const val KEY_OPENAI_API_KEY = "openai_api_key"
    private const val KEY_TOKAUDIT_ENABLED = "tokaudit_enabled"
    private const val KEY_GETTRANSCRIBE_ENABLED = "gettranscribe_enabled"
    private const val KEY_OPENAI_ENABLED = "openai_enabled"

    fun getSelectedProvider(context: Context): TranscriptProvider {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val raw = prefs.getString(KEY_PROVIDER, TranscriptProvider.GETTRANSCRIBE.name) ?: TranscriptProvider.GETTRANSCRIBE.name
        return runCatching { TranscriptProvider.valueOf(raw) }.getOrDefault(TranscriptProvider.GETTRANSCRIBE)
    }

    fun setSelectedProvider(context: Context, provider: TranscriptProvider) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(KEY_PROVIDER, provider.name).apply()
    }

    fun getApiKey(context: Context, provider: TranscriptProvider): String? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return when (provider) {
            TranscriptProvider.TOKAUDIT -> prefs.getString(KEY_TOKAUDIT_API_KEY, null)
            TranscriptProvider.GETTRANSCRIBE -> prefs.getString(KEY_GETTRANSCRIBE_API_KEY, null)
            TranscriptProvider.OPENAI -> prefs.getString(KEY_OPENAI_API_KEY, null)
        }
    }

    fun setApiKey(context: Context, provider: TranscriptProvider, apiKey: String?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        when (provider) {
            TranscriptProvider.TOKAUDIT -> editor.putString(KEY_TOKAUDIT_API_KEY, apiKey)
            TranscriptProvider.GETTRANSCRIBE -> editor.putString(KEY_GETTRANSCRIBE_API_KEY, apiKey)
            TranscriptProvider.OPENAI -> editor.putString(KEY_OPENAI_API_KEY, apiKey)
        }
        editor.apply()
    }
    
    fun isProviderEnabled(context: Context, provider: TranscriptProvider): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return when (provider) {
            TranscriptProvider.TOKAUDIT -> prefs.getBoolean(KEY_TOKAUDIT_ENABLED, true)
            TranscriptProvider.GETTRANSCRIBE -> prefs.getBoolean(KEY_GETTRANSCRIBE_ENABLED, true)
            TranscriptProvider.OPENAI -> prefs.getBoolean(KEY_OPENAI_ENABLED, false) // Default off
        }
    }
    
    fun setProviderEnabled(context: Context, provider: TranscriptProvider, enabled: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        when (provider) {
            TranscriptProvider.TOKAUDIT -> editor.putBoolean(KEY_TOKAUDIT_ENABLED, enabled)
            TranscriptProvider.GETTRANSCRIBE -> editor.putBoolean(KEY_GETTRANSCRIBE_ENABLED, enabled)
            TranscriptProvider.OPENAI -> editor.putBoolean(KEY_OPENAI_ENABLED, enabled)
        }
        editor.apply()
    }
    
    fun getAvailableProviders(context: Context): List<TranscriptProvider> {
        return TranscriptProvider.values().filter { isProviderEnabled(context, it) }
    }
}


