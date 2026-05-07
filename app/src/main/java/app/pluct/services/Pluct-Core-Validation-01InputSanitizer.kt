package app.pluct.services

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import java.net.URL
import java.net.HttpURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class PluctCoreValidationInputSanitizer @Inject constructor() {

    companion object {
        private const val TAG = "PluctInputSanitizer"

        private val URL_IN_TEXT_REGEX = Regex("https?://\\S+", RegexOption.IGNORE_CASE)
        private val HTTP_SIGNAL_REGEX = Regex("https?://", RegexOption.IGNORE_CASE)
        private val SHORT_ID_REGEX = Regex("^[A-Za-z0-9]{8,32}$")
        private val LONG_VIDEO_REGEX = Regex("^/@([A-Za-z0-9._-]{2,64})/video/([0-9]{10,25})/?$")
        private const val MAX_URL_LENGTH = 2048
        private const val MAX_TITLE_LENGTH = 500
        private const val MAX_DESCRIPTION_LENGTH = 2000
        private const val MAX_AUTHOR_LENGTH = 100
        private const val REDIRECT_HOPS = 3

        private fun cleanInput(input: String): String = input
            .replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")
            .trim()

        private fun parseTikTokUrl(candidate: String): TikTokUrlParseResult {
            val parsed = runCatching { URL(candidate) }.getOrNull()
                ?: return TikTokUrlParseResult(false, error = "Paste one full TikTok link.")
            val host = parsed.host.lowercase()
            val path = parsed.path.orEmpty()

            return when (host) {
                "vt.tiktok.com", "vm.tiktok.com" -> {
                    val id = path.trim('/').takeIf { SHORT_ID_REGEX.matches(it) }
                        ?: return TikTokUrlParseResult(false, error = "TikTok link looks incomplete. Paste the full link again.")
                    TikTokUrlParseResult(true, "https://$host/$id/", "TikTok short link ready")
                }
                "www.tiktok.com", "m.tiktok.com", "tiktok.com" -> {
                    val match = LONG_VIDEO_REGEX.matchEntire(path)
                        ?: return TikTokUrlParseResult(false, error = "Use a real TikTok video link, not a profile or search page.")
                    val username = match.groupValues[1]
                    val videoId = match.groupValues[2]
                    TikTokUrlParseResult(true, "https://www.tiktok.com/@$username/video/$videoId", "TikTok video link ready")
                }
                else -> TikTokUrlParseResult(false, error = "Only TikTok video links work here.")
            }
        }
    }

    fun isTikTokUrl(url: String): Boolean {
        val trimmedUrl = extractUrlFromText(url)
        return parseTikTokUrl(trimmedUrl).isValid
    }

    fun isTikTokShortLink(url: String): Boolean {
        val parsed = runCatching { URL(extractUrlFromText(url)) }.getOrNull() ?: return false
        return parsed.host.equals("vm.tiktok.com", true) || parsed.host.equals("vt.tiktok.com", true)
    }

    fun extractUrlFromText(input: String): String {
        val cleaned = cleanInput(input)
        val candidate = URL_IN_TEXT_REGEX.find(cleaned)?.value ?: cleaned
        return candidate
            .trim()
            .trimEnd('.', ',', ';', ')', ']', '}', '"', '\'')
    }

    fun validateUrl(url: String): ValidationResult {
        if (url.isBlank()) {
            return ValidationResult(false, errorMessage = "Paste a TikTok link first.")
        }

        val cleanedInput = cleanInput(url)
        val urlMatches = URL_IN_TEXT_REGEX.findAll(cleanedInput).map { it.value }.toList()
        if (HTTP_SIGNAL_REGEX.findAll(cleanedInput).count() > 1 || urlMatches.size > 1) {
            return ValidationResult(false, errorMessage = "Paste one TikTok link only.")
        }

        val trimmedUrl = extractUrlFromText(url)
        if (trimmedUrl.length > MAX_URL_LENGTH) {
            return ValidationResult(
                isValid = false,
                errorMessage = "URL too long (max $MAX_URL_LENGTH characters)"
            )
        }

        try {
            URL(trimmedUrl)
        } catch (e: Exception) {
            return ValidationResult(false, errorMessage = "Paste one full TikTok link.")
        }

        val tiktok = parseTikTokUrl(trimmedUrl)
        if (!tiktok.isValid) {
            return ValidationResult(false, errorMessage = tiktok.error ?: "Only TikTok video links work here.")
        }

        Log.d(TAG, "TikTok URL sanitized: $trimmedUrl -> ${tiktok.sanitizedUrl}")
        return ValidationResult(
            isValid = true,
            sanitizedValue = tiktok.sanitizedUrl.orEmpty(),
            warnings = listOfNotNull(tiktok.warning)
        )
    }

    suspend fun resolveTikTokRedirect(url: String, maxHops: Int = REDIRECT_HOPS): UrlResolutionResult {
        if (!isTikTokShortLink(url)) return UrlResolutionResult(originalUrl = url)

        return withContext(Dispatchers.IO) {
            var current = url
            val chain = mutableListOf(current)
            repeat(maxHops) {
                try {
                    val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = false
                        requestMethod = "GET"
                        connectTimeout = 5000
                        readTimeout = 5000
                        setRequestProperty("User-Agent", "PluctMobile/${app.pluct.BuildConfig.VERSION_NAME} (${app.pluct.BuildConfig.PLATFORM})")
                    }
                    connection.connect()
                    val location = connection.getHeaderField("Location")
                    if (location.isNullOrBlank()) {
                        return@withContext UrlResolutionResult(
                            originalUrl = url,
                            resolvedUrl = current,
                            redirectChain = chain.toList(),
                            error = null
                        )
                    }
                    val next = if (location.startsWith("http")) location else "https://www.tiktok.com$location"
                    chain.add(next)
                    if (!isTikTokShortLink(next)) {
                        val validation = validateUrl(next)
                        return@withContext UrlResolutionResult(
                            originalUrl = url,
                            resolvedUrl = if (validation.isValid) validation.sanitizedValue else next,
                            redirectChain = chain.toList(),
                            error = null
                        )
                    }
                    current = next
                } catch (e: Exception) {
                    return@withContext UrlResolutionResult(
                        originalUrl = url,
                        resolvedUrl = null,
                        redirectChain = chain.toList(),
                        error = e.message
                    )
                }
            }
            UrlResolutionResult(
                originalUrl = url,
                resolvedUrl = validateUrl(current).sanitizedValue.ifBlank { current },
                redirectChain = chain.toList(),
                error = "Max redirect depth reached"
            )
        }
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
    
    /**
     * Validate user has sufficient credits/free uses for requested processing tier
     * Single source of truth for credit validation logic
     * 
     * @param tier The processing tier requested
     * @param currentBalance Current credit balance
     * @param currentFreeUses Current free uses remaining
     * @return ValidationResult with validation outcome
     */
    fun validateCredits(
        tier: app.pluct.data.entity.ProcessingTier,
        currentBalance: Int,
        currentFreeUses: Int
    ): ValidationResult {
        val hasEnoughCredits = when (tier) {
            app.pluct.data.entity.ProcessingTier.EXTRACT_SCRIPT -> currentFreeUses > 0 || currentBalance >= 1
            app.pluct.data.entity.ProcessingTier.GENERATE_INSIGHTS -> currentBalance >= 2
            else -> false // Other tiers not supported yet
        }
        
        if (hasEnoughCredits) {
            return ValidationResult(
                isValid = true,
                sanitizedValue = ""
            )
        }
        
        // Insufficient credits - return user-friendly error message
        val errorMessage = when (tier) {
            app.pluct.data.entity.ProcessingTier.EXTRACT_SCRIPT -> 
                "Insufficient credits. You need 1 credit or a free use remaining. Current: $currentBalance credits, $currentFreeUses free uses."
            app.pluct.data.entity.ProcessingTier.GENERATE_INSIGHTS -> 
                "Insufficient credits. You need 2 credits for AI Insights. Current: $currentBalance credits."
            else -> 
                "Insufficient credits for this tier. Current: $currentBalance credits, $currentFreeUses free uses."
        }
        
        return ValidationResult(
            isValid = false,
            errorMessage = errorMessage
        )
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

data class UrlResolutionResult(
    val originalUrl: String,
    val resolvedUrl: String? = null,
    val redirectChain: List<String> = emptyList(),
    val error: String? = null
)

private data class TikTokUrlParseResult(
    val isValid: Boolean,
    val sanitizedUrl: String? = null,
    val warning: String? = null,
    val error: String? = null
)
