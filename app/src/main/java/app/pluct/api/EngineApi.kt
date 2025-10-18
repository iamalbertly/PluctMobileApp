package app.pluct.api

import retrofit2.Response
import retrofit2.http.*

interface EngineApi {
    @POST("v1/vend-token")
    suspend fun vendToken(
        @Header("Authorization") bearer: String,
        @Header("X-Client-Request-Id") reqId: String,
        @Body body: Map<String, String>
    ): Response<Map<String, String>>

    @POST("ttt/transcribe")
    suspend fun transcribe(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @GET("ttt/status/{id}")
    suspend fun status(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): Response<Map<String, Any>>

    @GET("meta")
    suspend fun meta(@Query("url") url: String): Response<Map<String, Any>>
}
