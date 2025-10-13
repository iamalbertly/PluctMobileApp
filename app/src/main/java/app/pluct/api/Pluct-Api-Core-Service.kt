package app.pluct.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Pluct Core API Service - Single source of truth for all API communications
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Consolidated from ApiService.kt and TTTranscribeApiService.kt
 */

// --- Business Engine DTOs ---
data class CreateUserRequest(val userId: String, val initialCredits: Int)
data class CreateUserResponse(val success: Boolean, val userId: String, val initialBalance: Int)
data class VendTokenRequest(val userId: String)
data class VendTokenResponse(val token: String)

// --- TTTranscribe DTOs ---
data class TTTranscribeRequest(val url: String)
data class TTTranscribeResponse(
    val request_id: String,
    val status: String,
    val lang: String,
    val duration_sec: Double,
    val transcript: String,
    val transcript_sha256: String,
    val source: SourceInfo,
    val billed_tokens: Int,
    val elapsed_ms: Long,
    val ts: String
)

data class SourceInfo(
    val canonical_url: String,
    val video_id: String
)

// --- AI Analysis DTOs ---
data class AIAnalysisRequest(val transcript: String, val analysisType: String = "comprehensive")
data class AIAnalysisResponse(
    val summary: String,
    val keyTakeaways: List<String>,
    val actionableSteps: List<String>
)

/**
 * Core API Service Interface
 * Handles all external API communications for Pluct
 */
interface PluctCoreApiService {
    // --- Business Engine Endpoints ---
    @POST("https://pluct-business-engine.romeo-lya2.workers.dev/user/create")
    suspend fun createUser(@Body request: CreateUserRequest): Response<CreateUserResponse>

    @POST("https://pluct-business-engine.romeo-lya2.workers.dev/vend-token")
    suspend fun vendToken(@Body request: VendTokenRequest): Response<VendTokenResponse>

    // --- TTTranscribe via Pluct Proxy (single mobile-safe endpoint) ---
    @POST("https://pluct-business-engine.romeo-lya2.workers.dev/ttt/transcribe")
    suspend fun transcribeViaPluctProxy(
        @Header("Authorization") authorization: String,
        @Body request: TTTranscribeRequest
    ): Response<TTTranscribeResponse>

    // --- AI Analysis Endpoints ---
    @POST("https://api.tttranscribe.com/v1/analyze")
    suspend fun analyzeTranscript(
        @Header("Authorization") token: String,
        @Body request: AIAnalysisRequest
    ): Response<AIAnalysisResponse>
}
