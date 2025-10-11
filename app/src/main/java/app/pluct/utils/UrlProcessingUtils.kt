package app.pluct.utils

import android.util.Log
import java.net.URL

/**
 * URL processing utilities for TikTok URL normalization and validation
 */
object UrlProcessingUtils {
    private const val TAG = "UrlProcessingUtils"
    
    /**
     * Normalize a TikTok URL to vm.tiktok.com format
     */
    fun normalizeUrl(url: String): String {
        return try {
            when {
                url.contains("vm.tiktok.com") -> url
                url.contains("tiktok.com") -> {
                    // Convert tiktok.com URLs to vm.tiktok.com format
                    val urlObj = URL(url)
                    val path = urlObj.path
                    if (path.startsWith("/@")) {
                        // Handle @username format
                        val username = path.substring(2)
                        "https://vm.tiktok.com/$username"
                    } else if (path.startsWith("/")) {
                        // Handle /video/ format
                        val videoId = path.substringAfter("/video/").substringBefore("/")
                        "https://vm.tiktok.com/$videoId"
                    } else {
                        url
                    }
                }
                else -> url
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error normalizing URL: ${e.message}", e)
            url
        }
    }
    
    /**
     * Check if a URL is a valid TikTok URL
     */
    fun isValidTikTokUrl(url: String): Boolean {
        return try {
            when {
                url.contains("vm.tiktok.com") -> true
                url.contains("tiktok.com") -> true
                url.contains("tiktok.com/@") -> true
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating TikTok URL: ${e.message}", e)
            false
        }
    }
    
    /**
     * Process URL for optimal transcript extraction
     */
    fun processUrl(url: String): String {
        val normalized = normalizeUrl(url)
        Log.d(TAG, "Processed URL: $normalized")
        return normalized
    }
}
