package app.pluct.shared

data class PluctUrlValidationResult(
    val isValid: Boolean,
    val sanitizedValue: String = "",
    val errorMessage: String? = null,
    val warnings: List<String> = emptyList()
)

private data class TikTokUrlParseResult(
    val isValid: Boolean,
    val sanitizedUrl: String? = null,
    val warning: String? = null,
    val error: String? = null
)

object PluctTikTokUrlValidator {
    private val urlInTextRegex = Regex("https?://\\S+", RegexOption.IGNORE_CASE)
    private val httpSignalRegex = Regex("https?://", RegexOption.IGNORE_CASE)
    private val shortIdRegex = Regex("^[A-Za-z0-9]{8,32}$")
    private val longVideoRegex = Regex("^/@([A-Za-z0-9._-]{2,64})/video/([0-9]{10,25})/?$")
    private val urlPartsRegex = Regex("^https?://([^/?#]+)([^?#]*)", RegexOption.IGNORE_CASE)
    private const val maxUrlLength = 2048

    fun extractUrlFromText(input: String): String {
        val cleaned = cleanInput(input)
        val candidate = urlInTextRegex.find(cleaned)?.value ?: cleaned
        return candidate.trim().trimEnd('.', ',', ';', ')', ']', '}', '"', '\'')
    }

    fun isTikTokUrl(url: String): Boolean = validateUrl(url).isValid

    fun isTikTokShortLink(url: String): Boolean {
        val parts = parseUrlParts(extractUrlFromText(url)) ?: return false
        return parts.first == "vm.tiktok.com" || parts.first == "vt.tiktok.com"
    }

    fun validateUrl(url: String): PluctUrlValidationResult {
        if (url.isBlank()) {
            return PluctUrlValidationResult(false, errorMessage = "Paste a TikTok link first.")
        }

        val cleanedInput = cleanInput(url)
        val urlMatches = urlInTextRegex.findAll(cleanedInput).map { it.value }.toList()
        if (httpSignalRegex.findAll(cleanedInput).count() > 1 || urlMatches.size > 1) {
            return PluctUrlValidationResult(false, errorMessage = "Paste one TikTok link only.")
        }

        val trimmedUrl = extractUrlFromText(url)
        if (trimmedUrl.length > maxUrlLength) {
            return PluctUrlValidationResult(false, errorMessage = "URL too long (max $maxUrlLength characters)")
        }

        val tiktok = parseTikTokUrl(trimmedUrl)
        if (!tiktok.isValid) {
            return PluctUrlValidationResult(false, errorMessage = tiktok.error ?: "Only TikTok video links work here.")
        }

        return PluctUrlValidationResult(
            isValid = true,
            sanitizedValue = tiktok.sanitizedUrl.orEmpty(),
            warnings = listOfNotNull(tiktok.warning)
        )
    }

    private fun cleanInput(input: String): String = input
        .replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")
        .trim()

    private fun parseTikTokUrl(candidate: String): TikTokUrlParseResult {
        val parsed = parseUrlParts(candidate)
            ?: return TikTokUrlParseResult(false, error = "Paste one full TikTok link.")
        val host = parsed.first
        val path = parsed.second

        return when (host) {
            "vt.tiktok.com", "vm.tiktok.com" -> {
                val id = path.trim('/').takeIf { shortIdRegex.matches(it) }
                    ?: return TikTokUrlParseResult(false, error = "TikTok link looks incomplete. Paste the full link again.")
                TikTokUrlParseResult(true, "https://$host/$id/", "TikTok short link ready")
            }
            "www.tiktok.com", "m.tiktok.com", "tiktok.com" -> {
                val match = longVideoRegex.matchEntire(path)
                    ?: return TikTokUrlParseResult(false, error = "Use a real TikTok video link, not a profile or search page.")
                val username = match.groupValues[1]
                val videoId = match.groupValues[2]
                TikTokUrlParseResult(true, "https://www.tiktok.com/@$username/video/$videoId", "TikTok video link ready")
            }
            else -> TikTokUrlParseResult(false, error = "Only TikTok video links work here.")
        }
    }

    private fun parseUrlParts(candidate: String): Pair<String, String>? {
        val match = urlPartsRegex.find(candidate) ?: return null
        val host = match.groupValues[1].lowercase().trimEnd('.')
        val path = match.groupValues[2].ifBlank { "/" }
        return host to path
    }
}
