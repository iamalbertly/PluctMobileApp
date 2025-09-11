package app.pluct.utils

import android.util.Log
import java.net.URL
import java.util.regex.Pattern

/**
 * URL utility functions
 */
object UrlUtils {
    private const val TAG = "UrlUtils"
    
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

