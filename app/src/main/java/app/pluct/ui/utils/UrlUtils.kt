package app.pluct.ui.utils

import java.net.URL

object UrlUtils {
    /**
     * Extract host and path from a URL
     */
    fun extractHostFromUrl(url: String): String {
        return try {
            val uri = URL(url)
            "${uri.host}${uri.path}"
        } catch (e: Exception) {
            url
        }
    }
}
