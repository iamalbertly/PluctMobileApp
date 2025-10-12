package app.pluct.utils

import android.util.Log
import java.net.URL
import java.util.regex.Pattern

/**
 * Consolidated URL utility functions for Pluct
 * Single source of truth for all URL operations
 */
object PluctUrlUtils {
    private const val TAG = "PluctUrlUtils"
    
    /**
     * Extract URL from text that may contain other content
     */
    fun extractUrlFromText(text: String): String? {
        try {
            val urlPattern = Pattern.compile(
                "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = urlPattern.matcher(text)
            
            if (matcher.find()) {
                val url = matcher.group(1)
                Log.d(TAG, "Extracted URL: $url")
                return url
            }
            
            Log.w(TAG, "No URL found in text: $text")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting URL from text: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Extract host from URL
     */
    fun extractHostFromUrl(url: String): String {
        try {
            val urlObj = URL(url)
            return urlObj.host
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting host from URL: ${e.message}", e)
            return "unknown"
        }
    }
    
    /**
     * Extract host and path from URL (for display purposes)
     */
    fun extractHostAndPathFromUrl(url: String): String {
        try {
            val uri = URL(url)
            return "${uri.host}${uri.path}"
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting host and path from URL: ${e.message}", e)
            return url
        }
    }
    
    /**
     * Validate if URL is a valid TikTok URL
     */
    fun isValidTikTokUrl(url: String): Boolean {
        try {
            val urlObj = URL(url)
            val host = urlObj.host.lowercase()
            return host.contains("tiktok.com") || host.contains("vm.tiktok.com")
        } catch (e: Exception) {
            Log.e(TAG, "Error validating TikTok URL: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Encode URL for deep linking
     */
    fun encodeUrlForDeepLink(url: String): String {
        return try {
            java.net.URLEncoder.encode(url, "UTF-8")
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding URL: ${e.message}", e)
            url
        }
    }
}
