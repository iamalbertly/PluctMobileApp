package app.pluct.utils

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

object UrlResolver {
    private const val TAG = "UrlResolver"

    fun resolveTikTokUrl(input: String, timeoutMs: Int = 6000): String {
        return try {
            if (!input.contains("vm.tiktok.com")) return input
            val url = URL(input)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) Pluct/1.0")
            }
            conn.connect()
            val finalUrl = conn.url.toString()
            conn.disconnect()
            Log.d(TAG, "Resolved TikTok URL: $input -> $finalUrl")
            finalUrl
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve TikTok URL, using original: ${e.message}")
            input
        }
    }
}
