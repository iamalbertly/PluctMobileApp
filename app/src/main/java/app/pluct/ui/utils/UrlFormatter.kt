package app.pluct.ui.utils

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles URL formatting and normalization for different video platforms
 */
object UrlFormatter {
    private const val TAG = "UrlFormatter"
    
    /**
     * Format and clean video URLs for processing with enhanced validation
     */
    suspend fun formatVideoUrl(videoUrl: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Formatting video URL: $videoUrl")
            
            // Clean the URL first
            val cleanUrl = cleanUrl(videoUrl)
            Log.d(TAG, "Cleaned URL: $cleanUrl")
            
            // Handle TikTok URLs specifically
            val formattedUrl = when {
                cleanUrl.contains("vm.tiktok.com") -> {
                    // Keep vm.tiktok.com URLs as-is (preferred format)
                    val vmUrl = cleanUrl.replace(Regex("https?://"), "https://")
                    val vmUrlWithSlash = if (!vmUrl.endsWith("/")) "$vmUrl/" else vmUrl
                    Log.d(TAG, "Formatted vm.tiktok.com URL: $vmUrlWithSlash")
                    vmUrlWithSlash
                }
                
                cleanUrl.contains("tiktok.com") && cleanUrl.contains("/video/") -> {
                    // Convert full tiktok.com URLs to vm.tiktok.com format
                    val videoId = cleanUrl.substringAfter("/video/").substringBefore("?")
                    val shortUrl = "https://vm.tiktok.com/$videoId/"
                    Log.d(TAG, "Converted full TikTok URL to short format: $shortUrl")
                    shortUrl
                }
                
                cleanUrl.contains("tiktok.com") -> {
                    // For any other TikTok URLs, try to expand and validate
                    val httpsUrl = cleanUrl.replace(Regex("https?://"), "https://")
                    Log.d(TAG, "Using standard TikTok URL format: $httpsUrl")
                    
                    // Try to expand short URLs to canonical format
                    try {
                        val expandedUrl = expandUrl(httpsUrl)
                        if (expandedUrl != httpsUrl) {
                            Log.d(TAG, "Expanded URL: $expandedUrl")
                            return@withContext expandedUrl
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to expand URL, using original: ${e.message}")
                    }
                    
                    httpsUrl
                }
                
                else -> {
                    Log.d(TAG, "Non-TikTok URL detected: $cleanUrl")
                    cleanUrl
                }
            }
            
            // Validate the final URL
            val validatedUrl = validateUrl(formattedUrl)
            Log.d(TAG, "Final validated URL: $validatedUrl")
            
            validatedUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting URL: ${e.message}", e)
            cleanUrl(videoUrl) // Return cleaned original URL as fallback
        }
    }
    
    /**
     * Clean URL by removing deep link prefixes and trimming whitespace
     */
    private fun cleanUrl(videoUrl: String): String {
        // Ensure the URL doesn't contain the deep link prefix
        val cleanUrl = if (videoUrl.contains("url=")) {
            try {
                // Try to extract the actual URL from the deep link format
                val decodedUrl = android.net.Uri.decode(videoUrl.substringAfter("url="))
                Log.d(TAG, "Extracted URL from deep link: $decodedUrl")
                decodedUrl
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract URL from deep link: ${e.message}", e)
                videoUrl
            }
        } else {
            videoUrl
        }
        
        // Trim whitespace and normalize
        return cleanUrl.trim()
    }
    
    /**
     * Expand short URLs using HTTP HEAD request to get canonical URL
     */
    private suspend fun expandUrl(shortUrl: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Expanding URL: $shortUrl")
            
            val url = URL(shortUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) Pluct/1.0")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.instanceFollowRedirects = true
            
            val responseCode = connection.responseCode
            Log.d(TAG, "HEAD request response code: $responseCode")
            
            if (responseCode in 200..399) {
                val location = connection.getHeaderField("Location")
                if (location != null) {
                    Log.d(TAG, "Found redirect location: $location")
                    return@withContext location
                }
                
                val canonical = connection.getHeaderField("Link")
                if (canonical != null && canonical.contains("rel=\"canonical\"")) {
                    val canonicalUrl = canonical.substringAfter("<").substringBefore(">")
                    Log.d(TAG, "Found canonical URL: $canonicalUrl")
                    return@withContext canonicalUrl
                }
            }
            
            // If no expansion found, return original URL
            shortUrl
        } catch (e: Exception) {
            Log.w(TAG, "Failed to expand URL: ${e.message}")
            shortUrl
        }
    }
    
    /**
     * Validate URL format and ensure it's properly formatted
     */
    private fun validateUrl(url: String): String {
        try {
            // Ensure proper protocol
            val validatedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            
            // Validate URL format
            val urlObj = URL(validatedUrl)
            Log.d(TAG, "URL validation successful: ${urlObj.toString()}")
            
            return urlObj.toString()
        } catch (e: Exception) {
            Log.e(TAG, "URL validation failed: ${e.message}")
            return url // Return original URL if validation fails
        }
    }
    
    /**
     * Check if URL is valid for processing
     */
    fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            true
        } catch (e: Exception) {
            false
        }
    }
}
