package app.pluct.api

import retrofit2.Response
import retrofit2.http.*

interface EngineApi {
    @POST("vend-token")
    suspend fun vendToken(@Body body: Map<String, String>): Response<Map<String, String>>

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

    @POST("meta/resolve")
    suspend fun resolveMeta(@Body body: Map<String, String>): Response<Map<String, Any>>
}
