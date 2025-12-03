package app.pluct.services

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import app.pluct.architecture.PluctComponent
import java.net.URL
import java.util.regex.Pattern

/**
 * Pluct-Core-Validation-01InputSanitizer - Input validation and sanitization service
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Provides comprehensive input validation for URLs, user inputs, and API responses
 */
@Singleton
class PluctCoreValidationInputSanitizer @Inject constructor() : PluctComponent {
    
    companion object {
        private const val TAG = "PluctInputSanitizer"
        
        // URL patterns for supported platforms
        // Updated to handle both short and long TikTok URLs with query parameters
        private val TIKTOK_URL_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?(?:vm\\.)?tiktok\\.com/(?:@[\\w.-]+/video/)?[A-Za-z0-9]+(?:/|\\?)?",
            Pattern.CASE_INSENSITIVE
        )
        
        // Pattern to extract clean TikTok URL from long share URLs
        private val TIKTOK_VIDEO_ID_PATTERN = Pattern.compile(
            "tiktok\\.com/(?:@([\\w.-]+)/video/)?([0-9]+)",
            Pattern.CASE_INSENSITIVE
        )
        
        private val YOUTUBE_URL_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?(?:m\\.)?youtube\\.com/watch\\?v=[A-Za-z0-9_-]+",
            Pattern.CASE_INSENSITIVE
        )
        
        private val YOUTUBE_SHORT_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?youtu\\.be/[A-Za-z0-9_-]+",
            Pattern.CASE_INSENSITIVE
        )
        
        // General URL pattern
        private val URL_PATTERN = Pattern.compile(
            "https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?",
            Pattern.CASE_INSENSITIVE
        )
        
        // Maximum lengths
        private const val MAX_URL_LENGTH = 2048
        private const val MAX_TITLE_LENGTH = 500
        private const val MAX_DESCRIPTION_LENGTH = 2000
        private const val MAX_AUTHOR_LENGTH = 100
        
        /**
         * Sanitize TikTok URL by extracting video ID and creating clean URL
         */
        private fun sanitizeTikTokUrl(url: String): String {
            val matcher = TIKTOK_VIDEO_ID_PATTERN.matcher(url)
            if (matcher.find()) {
                val username = matcher.group(1)
                val videoId = matcher.group(2)
                
                return if (username != null && videoId != null) {
                    "https://www.tiktok.com/@$username/video/$videoId"
                } else if (videoId != null) {
                    // If no username, just use video ID (will work with API)
                    "https://www.tiktok.com/video/$videoId"
                } else {
                    url // Return original if can't extract
                }
            }
            return url
        }
    }
    
    override val componentId: String = "pluct-core-validation-input-sanitizer"
    override val dependencies: List<String> = emptyList()
    
    override fun initialize() {
        Log.d(TAG, "Initializing PluctCoreValidationInputSanitizer")
    }
    
    override fun cleanup() {
        Log.d(TAG, "Cleaning up PluctCoreValidationInputSanitizer")
    }
    
    /**
     * Validate and sanitize URL
     */
    fun validateUrl(url: String): ValidationResult {
        if (url.isBlank()) {
            return ValidationResult(false, errorMessage = "URL cannot be empty")
        }
        
        val trimmedUrl = url.trim()
        
        if (trimmedUrl.length > MAX_URL_LENGTH) {
            return ValidationResult(
                isValid = false,
                errorMessage = "URL too long (max $MAX_URL_LENGTH characters)"
            )
        }
        
        // Check if URL is properly formatted
        try {
            URL(trimmedUrl)
        } catch (e: Exception) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Invalid URL format: ${e.message}"
            )
        }
        
        // Check for supported platforms and sanitize
        val warnings = mutableListOf<String>()
        var sanitizedUrl = trimmedUrl
        val isSupported = when {
            trimmedUrl.contains("tiktok.com", ignoreCase = true) -> {
                // Sanitize TikTok URL to remove query parameters
                sanitizedUrl = sanitizeTikTokUrl(trimmedUrl)
                Log.d(TAG, "TikTok URL sanitized: $trimmedUrl -> $sanitizedUrl")
                warnings.add("TikTok URL detected")
                true
            }
            YOUTUBE_URL_PATTERN.matcher(trimmedUrl).matches() || 
            YOUTUBE_SHORT_PATTERN.matcher(trimmedUrl).matches() -> {
                warnings.add("YouTube URL detected")
                true
            }
            URL_PATTERN.matcher(trimmedUrl).matches() -> {
                warnings.add("Generic URL detected - may not be supported")
                true
            }
            else -> false
        }
        
        if (!isSupported) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Unsupported URL format"
            )
        }
        
        return ValidationResult(
            isValid = true,
            sanitizedValue = sanitizedUrl,
            warnings = warnings
        )
    }
    
    /**
     * Validate and sanitize title
     */
    fun validateTitle(title: String): ValidationResult {
        if (title.isBlank()) {
            return ValidationResult(false, errorMessage = "Title cannot be empty")
        }
        
        val sanitized = title.trim()
        
        if (sanitized.length > MAX_TITLE_LENGTH) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Title too long (max $MAX_TITLE_LENGTH characters)"
            )
        }
        
        // Remove potentially dangerous characters
        val cleaned = sanitized.replace(Regex("[<>\"'&]"), "")
        
        return ValidationResult(
            isValid = true,
            sanitizedValue = cleaned
        )
    }
    
    /**
     * Validate and sanitize description
     */
    fun validateDescription(description: String): ValidationResult {
        if (description.isBlank()) {
            return ValidationResult(true, sanitizedValue = "") // Empty description is OK
        }
        
        val sanitized = description.trim()
        
        if (sanitized.length > MAX_DESCRIPTION_LENGTH) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Description too long (max $MAX_DESCRIPTION_LENGTH characters)"
            )
        }
        
        // Remove potentially dangerous characters
        val cleaned = sanitized.replace(Regex("[<>\"'&]"), "")
        
        return ValidationResult(
            isValid = true,
            sanitizedValue = cleaned
        )
    }
    
    /**
     * Validate and sanitize author name
     */
    fun validateAuthor(author: String): ValidationResult {
        if (author.isBlank()) {
            return ValidationResult(false, errorMessage = "Author cannot be empty")
        }
        
        val sanitized = author.trim()
        
        if (sanitized.length > MAX_AUTHOR_LENGTH) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Author name too long (max $MAX_AUTHOR_LENGTH characters)"
            )
        }
        
        // Remove potentially dangerous characters
        val cleaned = sanitized.replace(Regex("[<>\"'&]"), "")
        
        return ValidationResult(
            isValid = true,
            sanitizedValue = cleaned
        )
    }
    
    /**
     * Validate API response data
     */
    fun validateApiResponse(response: String): ValidationResult {
        if (response.isBlank()) {
            return ValidationResult(false, errorMessage = "API response cannot be empty")
        }
        
        // Check for potential security issues
        val dangerousPatterns = listOf(
            "<script",
            "javascript:",
            "vbscript:",
            "onload=",
            "onerror=",
            "onclick="
        )
        
        val hasDangerousContent = dangerousPatterns.any { pattern ->
            response.contains(pattern, ignoreCase = true)
        }
        
        if (hasDangerousContent) {
            return ValidationResult(
                isValid = false,
                errorMessage = "API response contains potentially dangerous content"
            )
        }
        
        return ValidationResult(
            isValid = true,
            sanitizedValue = response.trim()
        )
    }
    
    /**
     * Sanitize HTML content
     */
    fun sanitizeHtml(html: String): String {
        return html
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("&[^;]+;"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

/**
 * Validation result data class
 */
data class ValidationResult(
    val isValid: Boolean,
    val sanitizedValue: String = "",
    val errorMessage: String? = null,
    val warnings: List<String> = emptyList()
)