package app.pluct.data

import android.util.Log
import java.util.UUID

/**
 * Pluct-Business-Engine-Core - Unified Business Engine client
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Coordinates all Business Engine operations using focused components
 */
class PluctBusinessEngineCore(
    private val baseUrl: String
) {
    private val healthService = PluctBusinessEngineHealth(baseUrl)
    private val balanceService = PluctBusinessEngineBalance(baseUrl)
    private val tokenService = PluctBusinessEngineToken(baseUrl)
    private val transcriptionService = PluctBusinessEngineTranscription(baseUrl)
    private val metadataService = PluctBusinessEngineMetadata(baseUrl)
    
    companion object {
        private const val TAG = "PluctBusinessEngineCore"
    }

    /**
     * Check Business Engine health
     */
    suspend fun health(): Health = healthService.health()

    /**
     * Get user credit balance
     */
    suspend fun balance(userJwt: String): Balance = balanceService.balance(userJwt)

    /**
     * Vend a short-lived token for transcription
     */
    suspend fun vendShortToken(userJwt: String, clientRequestId: String = UUID.randomUUID().toString()): VendResult = 
        tokenService.vendShortToken(userJwt, clientRequestId)

    /**
     * Start transcription process
     */
    suspend fun transcribe(url: String, token: String): String = 
        transcriptionService.transcribe(url, token)

    /**
     * Fetch enhanced metadata for a video URL
     */
    suspend fun fetchMetadata(url: String): Metadata = 
        metadataService.fetchMetadata(url)

    /**
     * Legacy method for backward compatibility
     */
    suspend fun vendToken(): VendResult {
        throw EngineError.Upstream(401, "vendToken() deprecated - supply user JWT via vendShortToken(userJwt)")
    }
}