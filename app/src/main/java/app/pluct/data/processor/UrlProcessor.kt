package app.pluct.data.processor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.HttpURLConnection

/**
 * Centralized URL processing logic for the application.
 * Consolidates functionality from UrlProcessingUtils and IngestViewModel.
 */
class UrlProcessor {
    companion object {
        private const val TAG = "UrlProcessor"
    }
    
    /**
     * Process and validate a URL for transcript extraction
     */
    suspend fun processAndValidateUrl(url: String): UrlProcessingResult {
        return try {
            // Extract URL from text if needed
            val extractedUrl = extractUrlFromText(url) ?: url
            
            // Normalize the URL
            val normalizedUrl = normalizeUrl(extractedUrl)
            
            // Validate TikTok URL
            if (!isValidTikTokUrl(normalizedUrl)) {
                return UrlProcessingResult.Invalid(
                    "Invalid TikTok URL. Please provide a valid TikTok video link.",
                    "invalid_tiktok_url"
                )
            }
            
            // Process URL for optimal format
            val processedUrl = processUrl(normalizedUrl)
            
            UrlProcessingResult.Success(processedUrl, normalizedUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing URL: ${e.message}", e)
            UrlProcessingResult.Error("Failed to process URL: ${e.message}")
        }
    }
    
    /**
     * Extract URL from text that might contain other content
     */
    private fun extractUrlFromText(text: String): String? {
        val urlRegex = Regex("(https?://[^\\s]+)")
        return urlRegex.find(text)?.value
    }
    
    /**
     * Normalize a URL by cleaning deep links and removing UTM parameters
     */
    private fun normalizeUrl(url: String): String {
        return try {
            // Check if URL contains a deep link format and extract the actual URL
            val cleanUrl = if (url.contains("url=")) {
                try {
                    android.net.Uri.decode(url.substringAfter("url="))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to extract URL from deep link: ${e.message}", e)
                    url
                }
            } else {
                url
            }
            
            Log.d(TAG, "Normalizing URL. Original: $url, Pre-cleaned: $cleanUrl")
            
            val uri = URL(cleanUrl)
            val host = uri.host.lowercase()
            val path = uri.path
            val query = uri.query?.let { query ->
                // Remove UTM parameters
                query.split("&")
                    .filterNot { it.startsWith("utm_") }
                    .joinToString("&")
                    .takeIf { it.isNotEmpty() }
                    ?.let { "?$it" } ?: ""
            } ?: ""
            
            val normalizedUrl = "${uri.protocol}://$host$path$query".removeSuffix("/")
            Log.d(TAG, "Normalized URL: $normalizedUrl")
            normalizedUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error normalizing URL: ${e.message}", e)
            url
        }
    }
    
    /**
     * Validate if a URL is a valid TikTok URL
     */
    private fun isValidTikTokUrl(url: String): Boolean {
        return try {
            val uri = URL(url)
            val host = uri.host.lowercase()
            val path = uri.path
            
            // Check for TikTok domains
            val isTikTokDomain = host.contains("tiktok.com") || 
                                host.contains("vm.tiktok.com") ||
                                host.contains("vt.tiktok.com")
            
            // Check for video path patterns
            val hasVideoPath = path.contains("/video/") || 
                              path.matches(Regex("/[A-Za-z0-9]{9,}/?")
                              )
            
            Log.d(TAG, "URL validation: host=$host, path=$path, isTikTokDomain=$isTikTokDomain, hasVideoPath=$hasVideoPath")
            
            isTikTokDomain && hasVideoPath
        } catch (e: Exception) {
            Log.e(TAG, "Error validating TikTok URL: ${e.message}")
            false
        }
    }
    
    /**
     * Process URL based on its type for optimal script.tokaudit.io compatibility
     */
    private suspend fun processUrl(url: String): String {
        return when {
            url.contains("vm.tiktok.com") -> {
                Log.d(TAG, "Keeping vm.tiktok.com URL as-is for script.tokaudit.io: $url")
                url
            }
            url.contains("tiktok.com/@") -> {
                // Try to convert to vm.tiktok.com format
                try {
                    val match = Regex("tiktok\\.com/@[^/]+/video/(\\d+)").find(url)
                    if (match != null) {
                        val videoId = match.groupValues[1]
                        val vmUrl = "https://vm.tiktok.com/ZMA${videoId.substring(0, 6)}/"
                        Log.d(TAG, "Converted to vm.tiktok.com format: $vmUrl")
                        vmUrl
                    } else {
                        // If conversion fails, use canonicalized URL
                        val canonicalUrl = withContext(Dispatchers.IO) {
                            canonicalizeUrl(url)
                        }
                        Log.d(TAG, "Could not convert to vm.tiktok.com, canonicalized: $canonicalUrl")
                        canonicalUrl
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting to vm.tiktok.com format", e)
                    // Fall back to canonicalization
                    val canonicalUrl = withContext(Dispatchers.IO) {
                        canonicalizeUrl(url)
                    }
                    Log.d(TAG, "Canonicalized URL after error: $canonicalUrl")
                    canonicalUrl
                }
            }
            else -> {
                val canonicalUrl = withContext(Dispatchers.IO) {
                    canonicalizeUrl(url)
                }
                Log.d(TAG, "Canonicalized non-TikTok URL: $canonicalUrl")
                canonicalUrl
            }
        }
    }
    
    /**
     * Canonicalize URL by following redirects
     */
    private suspend fun canonicalizeUrl(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.instanceFollowRedirects = true
                connection.connect()
                
                // Check if there's a redirect
                val finalUrl = connection.url.toString()
                if (finalUrl != url) {
                    Log.d(TAG, "URL redirected from $url to $finalUrl")
                }
                connection.disconnect()
                finalUrl
            } catch (e: Exception) {
                Log.e(TAG, "Error canonicalizing URL: ${e.message}", e)
                url // Return original URL if canonicalization fails
            }
        }
    }
    
    /**
     * Extract host from URL for display purposes
     */
    fun extractHostFromUrl(url: String): String {
        return try {
            val parsedUrl = URL(url)
            parsedUrl.host
        } catch (e: Exception) {
            url
        }
    }
}

/**
 * Result of URL processing operation
 */
sealed class UrlProcessingResult {
    data class Success(val processedUrl: String, val normalizedUrl: String) : UrlProcessingResult()
    data class Invalid(val message: String, val errorCode: String) : UrlProcessingResult()
    data class Error(val message: String) : UrlProcessingResult()
}