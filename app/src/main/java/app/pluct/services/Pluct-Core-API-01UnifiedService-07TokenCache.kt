package app.pluct.services

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Core-API-01UnifiedService-07TokenCache - Single source of truth for service token caching.
 * Handles token persistence and validation without business logic.
 */
@Singleton
class PluctCoreAPIUnifiedServiceTokenCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TOKEN_LEEWAY_MS = 60_000L // Refresh if less than 1 minute left (token lasts 15min)
        private const val PREFS_NAME = "pluct_api_cache"
        private const val KEY_TOKEN = "service_token"
        private const val KEY_EXPIRES_AT = "service_token_expires_at"
    }

    private data class CachedServiceToken(val token: String, val expiresAt: Long)
    private val tokenPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var cachedServiceToken: CachedServiceToken? = null

    init {
        cachedServiceToken = loadPersistedServiceToken()
    }

    fun getValidToken(): String? {
        if (cachedServiceToken == null) {
            cachedServiceToken = loadPersistedServiceToken()
        }
        val now = System.currentTimeMillis()
        return cachedServiceToken
            ?.takeIf { now + TOKEN_LEEWAY_MS < it.expiresAt }
            ?.token
    }

    fun cacheToken(token: String, expiresIn: Int?) {
        val now = System.currentTimeMillis()
        val ttlMs = ((expiresIn ?: 900).coerceAtLeast(60)) * 1000L
        cachedServiceToken = CachedServiceToken(token, now + ttlMs)
        persistServiceToken(token, now + ttlMs)
    }

    fun clearToken() {
        cachedServiceToken = null
        clearPersistedServiceToken()
    }

    private fun loadPersistedServiceToken(): CachedServiceToken? {
        val token = tokenPrefs.getString(KEY_TOKEN, null) ?: return null
        val expiresAt = tokenPrefs.getLong(KEY_EXPIRES_AT, 0L)
        if (expiresAt <= System.currentTimeMillis()) {
            clearPersistedServiceToken()
            return null
        }
        return CachedServiceToken(token, expiresAt)
    }

    private fun persistServiceToken(token: String, expiresAt: Long) {
        tokenPrefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    private fun clearPersistedServiceToken() {
        tokenPrefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }
}

