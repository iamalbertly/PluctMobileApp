package app.pluct.utils

object PluctUrlUtils {
    fun isValidTikTokUrl(url: String): Boolean {
        return url.contains("tiktok.com") || url.contains("vm.tiktok.com")
    }
    
    fun normalizeUrl(url: String): String {
        return url.trim()
    }
}
