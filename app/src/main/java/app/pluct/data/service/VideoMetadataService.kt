package app.pluct.data.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class VideoMetadata(
    val title: String?,
    val description: String?,
    val thumbnailUrl: String?,
    val author: String?,
    val duration: String?,
    val viewCount: String?
)

class VideoMetadataService {
    
    companion object {
        private const val TAG = "VideoMetadataService"
    }
    
    suspend fun fetchVideoMetadata(videoUrl: String): VideoMetadata? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching metadata for URL: $videoUrl")
            
            // Canonicalize the URL first
            val canonicalUrl = canonicalizeUrl(videoUrl)
            Log.d(TAG, "Canonicalized URL: $canonicalUrl")
            
            // For TikTok URLs, we can try to extract basic info
            // Note: TikTok doesn't provide public APIs, so we'll extract what we can from the URL
            val metadata = extractBasicMetadata(canonicalUrl)
            
            Log.d(TAG, "Extracted metadata: $metadata")
            return@withContext metadata
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching video metadata", e)
            return@withContext null
        }
    }
    
    private fun canonicalizeUrl(url: String): String {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.instanceFollowRedirects = true
            
            val finalUrl = connection.url.toString()
            connection.disconnect()
            
            finalUrl
        } catch (e: Exception) {
            Log.w(TAG, "Failed to canonicalize URL, using original", e)
            url
        }
    }
    
    private fun extractBasicMetadata(url: String): VideoMetadata {
        return try {
            // Extract basic info from TikTok URL
            val title = extractTitleFromUrl(url)
            val author = extractAuthorFromUrl(url)
            val description = "Video from $author"
            
            VideoMetadata(
                title = title,
                description = description,
                thumbnailUrl = null, // TikTok doesn't provide public thumbnail URLs
                author = author,
                duration = null,
                viewCount = null
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract basic metadata", e)
            VideoMetadata(
                title = "TikTok Video",
                description = "Video content",
                thumbnailUrl = null,
                author = null,
                duration = null,
                viewCount = null
            )
        }
    }
    
    private fun extractTitleFromUrl(url: String): String? {
        return try {
            // For TikTok URLs, we can't get the actual title without scraping
            // So we'll create a generic title based on the URL
            when {
                url.contains("tiktok.com") -> "TikTok Video"
                url.contains("youtube.com") -> "YouTube Video"
                url.contains("instagram.com") -> "Instagram Reel"
                else -> "Social Media Video"
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractAuthorFromUrl(url: String): String? {
        return try {
            // Extract username from TikTok URL
            when {
                url.contains("@") -> {
                    val startIndex = url.indexOf("@") + 1
                    val endIndex = url.indexOf("/", startIndex)
                    if (endIndex == -1) url.substring(startIndex) else url.substring(startIndex, endIndex)
                }
                url.contains("vm.tiktok.com") -> {
                    // For short URLs, we can't extract creator easily
                    "creator"
                }
                else -> "creator"
            }
        } catch (e: Exception) {
            "creator"
        }
    }
}
