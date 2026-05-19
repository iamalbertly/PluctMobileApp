package app.pluct.shared

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class PluctApiClient(
    baseUrl: String,
    private val httpClient: HttpClient = defaultHttpClient()
) {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    suspend fun checkUserBalance(userJwt: String): CreditBalanceResponse {
        return httpClient.get("$normalizedBaseUrl/v1/credits/balance") {
            bearerAuth(userJwt)
        }.body()
    }

    suspend fun getEstimate(userJwt: String, url: String): EstimateResponse {
        return httpClient.get("$normalizedBaseUrl/estimate") {
            bearerAuth(userJwt)
            url {
                parameters.append("url", url)
            }
        }.body()
    }

    suspend fun vendToken(userJwt: String, request: VendTokenRequest): VendTokenResponse {
        return httpClient.post("$normalizedBaseUrl/v1/vend-token") {
            bearerAuth(userJwt)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun quote(userJwt: String, request: QuoteRequest): QuoteResponse {
        return httpClient.post("$normalizedBaseUrl/v1/quote") {
            bearerAuth(userJwt)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun fulfill(userJwt: String, request: FulfillRequest): FulfillResponse {
        return httpClient.post("$normalizedBaseUrl/v1/fulfill") {
            bearerAuth(userJwt)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun pollJob(jobId: String, userJwt: String): TranscriptionStatusResponse {
        return httpClient.get("$normalizedBaseUrl/v1/jobs/$jobId") {
            bearerAuth(userJwt)
        }.body()
    }

    suspend fun submitTranscription(serviceToken: String, request: TranscriptionRequest): TranscriptionResponse {
        return httpClient.post("$normalizedBaseUrl/ttt/transcribe") {
            bearerAuth(serviceToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun pollTranscription(jobId: String, userJwt: String): TranscriptionStatusResponse {
        return httpClient.get("$normalizedBaseUrl/ttt/poll/$jobId") {
            bearerAuth(userJwt)
        }.body()
    }

    companion object {
        fun defaultHttpClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
    }
}
