package app.pluct.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Metadata-Service - TikTok metadata fetching service
 * Single source of truth for metadata operations
 * Adheres to 300-line limit with smart separation of concerns
 */

data class TikTokMetadata(
    val url: String,
    val title: String,
    val description: String,
    val author: String,
    val thumbnail: String,
    val duration: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class PluctMetadataService @Inject constructor() {
    
    suspend fun fetchMetadata(url: String): TikTokMetadata? = withContext(Dispatchers.IO) {
        try {
            Log.d("PluctMetadataService", "🔍 Fetching metadata for: $url")
            
            val doc: Document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get()
            
            val title = extractTitle(doc)
            val description = extractDescription(doc)
            val author = extractAuthor(doc)
            val thumbnail = extractThumbnail(doc)
            val duration = extractDuration(doc)
            
            val metadata = TikTokMetadata(
                url = url,
                title = title,
                description = description,
                author = author,
                thumbnail = thumbnail,
                duration = duration
            )
            
            Log.d("PluctMetadataService", "✅ Metadata fetched: $title by $author")
            metadata
            
        } catch (e: Exception) {
            Log.e("PluctMetadataService", "❌ Failed to fetch metadata", e)
            null
        }
    }
    
    private fun extractTitle(doc: Document): String {
        return doc.select("meta[property=og:title]").attr("content")
            .ifEmpty { doc.select("title").text() }
            .ifEmpty { "TikTok Video" }
    }
    
    private fun extractDescription(doc: Document): String {
        return doc.select("meta[property=og:description]").attr("content")
            .ifEmpty { doc.select("meta[name=description]").attr("content") }
            .ifEmpty { "TikTok video content" }
    }
    
    private fun extractAuthor(doc: Document): String {
        return doc.select("meta[property=og:site_name]").attr("content")
            .ifEmpty { doc.select("meta[name=author]").attr("content") }
            .ifEmpty { "TikTok Creator" }
    }
    
    private fun extractThumbnail(doc: Document): String {
        return doc.select("meta[property=og:image]").attr("content")
            .ifEmpty { doc.select("meta[property=twitter:image]").attr("content") }
            .ifEmpty { "" }
    }
    
    private fun extractDuration(doc: Document): Int {
        val durationText = doc.select("meta[property=video:duration]").attr("content")
        return try {
            durationText.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    fun normalizeTikTokUrl(url: String): String {
        return when {
            url.contains("vm.tiktok.com") -> url
            url.contains("tiktok.com") -> {
                // Extract video ID from full TikTok URL
                val videoId = url.substringAfterLast("/")
                "https://vm.tiktok.com/$videoId"
            }
            else -> url
        }
    }
    
    fun isValidTikTokUrl(url: String): Boolean {
        return url.contains("tiktok.com") || url.contains("vm.tiktok.com")
    }
}

