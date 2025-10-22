package app.pluct.api

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct TTTranscribe Authenticator (neutralized) - mobile no longer signs requests.
 * Retained only for timestamp utility to avoid broad refactors.
 */
@Singleton
class PluctTTTranscribeAuthenticator @Inject constructor() {
    /**
     * Generate current timestamp in milliseconds
     */
    fun generateTimestamp(): String {
        return System.currentTimeMillis().toString()
    }
}
