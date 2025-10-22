package app.pluct.data.provider

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Pluct-HuggingFace-API-Client - Handles API communication with Hugging Face
 */
class PluctHuggingFaceApiClient {
    companion object {
        private const val TAG = "PluctHuggingFaceApiClient"
        private const val BASE_URL = "https://iamromeoly-tttranscibe.hf.space"
        private const val HEALTH_ENDPOINT = "/health"
        private const val TRANSCRIBE_ENDPOINT = "/transcribe"
        private const val TRANSCRIBE_FORM_ENDPOINT = "/transcribe/form"
        private const val STATUS_ENDPOINT = "/transcribe"
    }
    
    @Serializable
    data class TranscriptionRequest(val url: String)
    
    @Serializable
    data class TranscriptionResponse(
        val id: String? = null,
        val job_id: String? = null,
        val status: String? = null,
        val message: String? = null,
        val queue_position: Int? = null,
        val estimated_wait_seconds: Int? = null,
        val audio_url: String? = null,
        val transcript_url: String? = null
    )
    
    @Serializable
    data class HealthResponse(
        val status: String,
        val queue: QueueInfo? = null
    )
    
    @Serializable
    data class QueueInfo(val estimated_wait_seconds: Int? = null)
    
    suspend fun checkHealth(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL$HEALTH_ENDPOINT")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val healthResponse = Json.decodeFromString<HealthResponse>(response)
                    Log.d(TAG, "Health check successful: ${healthResponse.status}")
                    true
                } else {
                    Log.e(TAG, "Health check failed with code: $responseCode")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Health check error: ${e.message}", e)
                false
            }
        }
    }
    
    suspend fun startTranscription(videoUrl: String): TranscriptionResponse? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting transcription for URL: $videoUrl")
                
                val jsonResponse = tryJsonPost(videoUrl)
                if (jsonResponse != null) return@withContext jsonResponse
                
                val getResponse = tryGetRequest(videoUrl)
                if (getResponse != null) return@withContext getResponse
                
                val formResponse = tryFormPost(videoUrl)
                return@withContext formResponse
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting transcription: ${e.message}", e)
                null
            }
        }
    }
    
    private suspend fun tryJsonPost(videoUrl: String): TranscriptionResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL$TRANSCRIBE_ENDPOINT")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                
                val request = TranscriptionRequest(url = videoUrl)
                val json = Json.encodeToString(TranscriptionRequest.serializer(), request)
                
                connection.outputStream.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                
                val responseCode = connection.responseCode
                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText() ?: ""
                }
                
                Log.d(TAG, "JSON POST response code: $responseCode, body: $response")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Json.decodeFromString<TranscriptionResponse>(response)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "JSON POST error: ${e.message}", e)
                null
            }
        }
    }
    
    private suspend fun tryGetRequest(videoUrl: String): TranscriptionResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val encodedUrl = URLEncoder.encode(videoUrl, "UTF-8")
                val url = URL("$BASE_URL$TRANSCRIBE_ENDPOINT?url=$encodedUrl")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                
                val responseCode = connection.responseCode
                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText() ?: ""
                }
                
                Log.d(TAG, "GET response code: $responseCode, body: $response")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Json.decodeFromString<TranscriptionResponse>(response)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "GET request error: ${e.message}", e)
                null
            }
        }
    }
    
    private suspend fun tryFormPost(videoUrl: String): TranscriptionResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL$TRANSCRIBE_FORM_ENDPOINT")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                
                val formData = "url=${URLEncoder.encode(videoUrl, "UTF-8")}"
                
                connection.outputStream.use { outputStream ->
                    outputStream.write(formData.toByteArray())
                }
                
                val responseCode = connection.responseCode
                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText() ?: ""
                }
                
                Log.d(TAG, "Form POST response code: $responseCode, body: $response")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Json.decodeFromString<TranscriptionResponse>(response)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Form POST error: ${e.message}", e)
                null
            }
        }
    }
    
    suspend fun pollTranscriptionStatus(jobId: String): TranscriptionResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL$STATUS_ENDPOINT/$jobId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseCode = connection.responseCode
                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText() ?: ""
                }
                
                Log.d(TAG, "Status poll response code: $responseCode, body: $response")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Json.decodeFromString<TranscriptionResponse>(response)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Status poll error: ${e.message}", e)
                null
            }
        }
    }
    
    suspend fun getTranscriptContent(transcriptUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val fullUrl = if (transcriptUrl.startsWith("http")) {
                    transcriptUrl
                } else {
                    "$BASE_URL$transcriptUrl"
                }
                
                val url = URL(fullUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    Log.d(TAG, "Transcript content retrieved, length: ${response.length}")
                    response
                } else {
                    Log.e(TAG, "Failed to get transcript content, code: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting transcript content: ${e.message}", e)
                null
            }
        }
    }
}
