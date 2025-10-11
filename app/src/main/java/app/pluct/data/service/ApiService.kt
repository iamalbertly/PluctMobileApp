package app.pluct.data.service

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// --- Data Transfer Objects ---
data class CreateUserRequest(val userId: String, val initialCredits: Int)
data class CreateUserResponse(val success: Boolean, val userId: String, val initialBalance: Int)
data class VendTokenRequest(val userId: String)
data class VendTokenResponse(val token: String)
data class TranscribeRequest(val url: String)
data class TranscribeResponse(val status: String, val transcript: String)

interface ApiService {
    // --- Pluct Business Engine Endpoints ---
    @POST("https://pluct-business-engine.romeo-lya2.workers.dev/user/create")
    suspend fun createUser(@Body request: CreateUserRequest): Response<CreateUserResponse>

    @POST("https://pluct-business-engine.romeo-lya2.workers.dev/vend-token")
    suspend fun vendToken(@Body request: VendTokenRequest): Response<VendTokenResponse>

    // --- TTTranscribe Service Endpoint ---
    @POST("https://iamromeoly-tttranscibe.hf.space/transcribe") // YOUR HUGGING FACE URL
    suspend fun transcribe(
        @Header("Authorization") token: String,
        @Body request: TranscribeRequest
    ): Response<TranscribeResponse>
}
