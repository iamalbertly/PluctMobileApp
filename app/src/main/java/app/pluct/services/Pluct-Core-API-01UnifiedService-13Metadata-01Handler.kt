package app.pluct.services

import app.pluct.core.api.PluctCoreAPI00Constants
import java.net.URLEncoder

/**
 * Pluct-Core-API-01UnifiedService-13Metadata-01Handler
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[Metadata]-[Handler]
 * 6 scope layers: Project, Core, API, UnifiedService, Metadata, Handler
 * Handles metadata retrieval operations
 * Extracted from UnifiedService-01Main to reduce file size and improve separation of concerns
 */
class PluctCoreAPI01UnifiedService13Metadata01Handler(
    private val jwtGenerator: PluctCoreAPIJWTGenerator,
    private val userIdentification: PluctCoreUserIdentification,
    private val authRetryHandler: PluctCoreAPI01UnifiedService15AuthRetry01Handler,
    private val execute: suspend (String, String, Map<String, Any>?, String?, Long?) -> Result<*>
) {
    /**
     * Get video metadata
     */
    suspend fun getMetadata(url: String, timeoutMs: Long? = null): Result<MetadataResponse> {
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        val userToken = jwtGenerator.generateUserJWT(userIdentification.userId)
        return authRetryHandler.executeWithProactiveRefresh(
            currentToken = userToken,
            apiCall = { token ->
                @Suppress("UNCHECKED_CAST")
                execute("GET", "/meta?url=$encodedUrl", null, token, timeoutMs) as Result<MetadataResponse>
            },
            retryBlock = { newToken ->
                @Suppress("UNCHECKED_CAST")
                execute("GET", "/meta?url=$encodedUrl", null, newToken, timeoutMs) as Result<MetadataResponse>
            }
        )
    }
}
