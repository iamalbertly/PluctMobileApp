package app.pluct.utils

import java.util.regex.Pattern

/**
 * TikTok URL validation and normalization utilities
 */
object TikTokUrlValidator {
    
    private val TIKTOK_REGEX = Regex(
        pattern = """^(https?:\/\/)?((www|m|vm|vt)\.)?tiktok\.com(\/|$)""",
        options = setOf(RegexOption.IGNORE_CASE)
    )
    
    /**
     * Check if URL is a valid TikTok URL
     */
    fun isAllowedTikTokUrl(url: String): Boolean {
        if (url.isBlank()) return false
        
        val trimmed = url.trim()
        return TIKTOK_REGEX.containsMatchIn(trimmed)
    }
    
    /**
     * Normalize TikTok URL for faster processing
     */
    fun normalizeTikTokUrl(url: String): String {
        if (!isAllowedTikTokUrl(url)) return url
        
        var normalized = url.trim()
        
        // Remove common query parameters that don't affect content
        normalized = normalized.replace(Regex("""[?&]utm_[^&]*"""), "")
        normalized = normalized.replace(Regex("""[?&]is_from_webapp[^&]*"""), "")
        normalized = normalized.replace(Regex("""[?&]sender_device[^&]*"""), "")
        normalized = normalized.replace(Regex("""[?&]sender_web_id[^&]*"""), "")
        
        // Clean up trailing ? or & if they're now at the end
        normalized = normalized.replace(Regex("""[?&]+$"""), "")
        
        return normalized
    }
    
    /**
     * Toggle trailing slash for retry scenarios
     */
    fun toggleTrailingSlash(url: String): String {
        return if (url.endsWith("/")) {
            url.dropLast(1)
        } else {
            "$url/"
        }
    }
}
