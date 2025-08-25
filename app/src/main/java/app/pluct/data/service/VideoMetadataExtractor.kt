package app.pluct.data.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service for extracting metadata from video URLs
 */
class VideoMetadataExtractor {
    companion object {
        private const val TAG = "VideoMetadataExtractor"
    }

    /**
     * Extract metadata from a video URL
     * @param videoUrl The URL of the video
     * @return A JSON string containing the extracted metadata
     */
    suspend fun extractVideoMetadata(videoUrl: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Extracting metadata for URL: $videoUrl")
            
            // Simple metadata extraction from URL
            val uri = URL(videoUrl)
            val host = uri.host ?: ""
            val path = uri.path ?: ""
            
            var title = "TikTok Video"
            var author = ""
            
            // Try to extract more metadata from TikTok
            if (host.contains("tiktok")) {
                try {
                    val connection = URL(videoUrl).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    
                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = StringBuilder()
                        var line: String?
                        
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        
                        reader.close()
                        
                        // Extract metadata from HTML
                        val html = response.toString()
                        
                        // Try to find JSON-LD metadata
                        val jsonLdRegex = "<script type=\"application/ld\\+json\">(.*?)</script>".toRegex(RegexOption.DOT_MATCHES_ALL)
                        val jsonLdMatch = jsonLdRegex.find(html)
                        
                        if (jsonLdMatch != null) {
                            val jsonLd = jsonLdMatch.groupValues[1]
                            try {
                                val json = JSONObject(jsonLd)
                                title = json.optString("name", title)
                                
                                val authorObj = json.optJSONObject("author")
                                if (authorObj != null) {
                                    author = authorObj.optString("name", "")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing JSON-LD: ${e.message}")
                            }
                        }
                        
                        // If JSON-LD failed, try meta tags
                        if (title == "TikTok Video") {
                            val titleRegex = "<meta property=\"og:title\" content=\"(.*?)\"".toRegex()
                            val titleMatch = titleRegex.find(html)
                            if (titleMatch != null) {
                                title = titleMatch.groupValues[1]
                            }
                            
                            val authorRegex = "<meta property=\"og:creator\" content=\"(.*?)\"".toRegex()
                            val authorMatch = authorRegex.find(html)
                            if (authorMatch != null) {
                                author = authorMatch.groupValues[1]
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting detailed metadata: ${e.message}")
                }
            }
            
            // Create metadata JSON
            val metadataJson = JSONObject().apply {
                put("title", title)
                put("author", author)
                put("host", host)
                put("path", path)
                put("url", videoUrl)
                put("timestamp", System.currentTimeMillis())
            }
            
            Log.d(TAG, "Extracted video metadata: $metadataJson")
            return@withContext metadataJson.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting metadata: ${e.message}")
            return@withContext "{}"
        }
    }
}
