package app.pluct.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URL
import java.net.HttpURLConnection
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Metadata-01Extractor - TikTok metadata extraction service
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@Singleton
class PluctMetadataExtractor @Inject constructor() {
    
    data class TikTokMetadata(
        val title: String,
        val description: String,
        val author: String,
        val authorHandle: String,
        val duration: Int,
        val thumbnailUrl: String,
        val videoUrl: String,
        val originalUrl: String,
        val extractedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Extract metadata from TikTok URL
     */
    suspend fun extractMetadata(url: String): Result<TikTokMetadata> = withContext(Dispatchers.IO) {
        try {
            Log.d("PluctMetadataExtractor", "Starting metadata extraction for: $url")
            
            // First, resolve the short URL to get the full URL
            val resolvedUrl = resolveShortUrl(url)
            Log.d("PluctMetadataExtractor", "Resolved URL: $resolvedUrl")
            
            // Fetch the HTML content
            val htmlContent = fetchHtmlContent(resolvedUrl)
            Log.d("PluctMetadataExtractor", "Fetched HTML content (${htmlContent.length} chars)")
            
            // Parse the HTML to extract metadata
            val metadata = parseHtmlContent(htmlContent, url)
            Log.d("PluctMetadataExtractor", "Extracted metadata: $metadata")
            
            Result.success(metadata)
        } catch (e: Exception) {
            Log.e("PluctMetadataExtractor", "Failed to extract metadata", e)
            Result.failure(e)
        }
    }
    
    /**
     * Resolve short URL to full URL
     */
    private fun resolveShortUrl(shortUrl: String): String {
        return try {
            val connection = URL(shortUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.requestMethod = "HEAD"
            connection.connect()
            
            val location = connection.getHeaderField("Location")
            connection.disconnect()
            
            location ?: shortUrl
        } catch (e: Exception) {
            Log.w("PluctMetadataExtractor", "Failed to resolve short URL, using original", e)
            shortUrl
        }
    }
    
    /**
     * Fetch HTML content from URL
     */
    private fun fetchHtmlContent(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate")
        connection.setRequestProperty("Connection", "keep-alive")
        connection.setRequestProperty("Upgrade-Insecure-Requests", "1")
        
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val content = reader.readText()
        reader.close()
        connection.disconnect()
        
        return content
    }
    
    /**
     * Parse HTML content to extract metadata
     */
    private fun parseHtmlContent(html: String, originalUrl: String): TikTokMetadata {
        val doc = Jsoup.parse(html)
        
        // Extract title from various possible sources
        val title = extractTitle(doc)
        
        // Extract description
        val description = extractDescription(doc)
        
        // Extract author information
        val (author, authorHandle) = extractAuthor(doc)
        
        // Extract duration
        val duration = extractDuration(doc)
        
        // Extract thumbnail URL
        val thumbnailUrl = extractThumbnail(doc)
        
        // Extract video URL
        val videoUrl = extractVideoUrl(doc)
        
        return TikTokMetadata(
            title = title,
            description = description,
            author = author,
            authorHandle = authorHandle,
            duration = duration,
            thumbnailUrl = thumbnailUrl,
            videoUrl = videoUrl,
            originalUrl = originalUrl
        )
    }
    
    private fun extractTitle(doc: org.jsoup.nodes.Document): String {
        // Try multiple sources for title
        val titleSelectors = listOf(
            "meta[property=\"og:title\"]",
            "meta[name=\"twitter:title\"]",
            "title",
            "h1",
            "[data-e2e=\"video-title\"]"
        )
        
        for (selector in titleSelectors) {
            val element = doc.selectFirst(selector)
            if (element != null) {
                val title = element.attr("content").ifEmpty { element.text() }
                if (title.isNotBlank() && title != "TikTok") {
                    return title.trim()
                }
            }
        }
        
        return "TikTok Video"
    }
    
    private fun extractDescription(doc: org.jsoup.nodes.Document): String {
        val descriptionSelectors = listOf(
            "meta[property=\"og:description\"]",
            "meta[name=\"twitter:description\"]",
            "meta[name=\"description\"]",
            "[data-e2e=\"video-desc\"]"
        )
        
        for (selector in descriptionSelectors) {
            val element = doc.selectFirst(selector)
            if (element != null) {
                val description = element.attr("content").ifEmpty { element.text() }
                if (description.isNotBlank()) {
                    return description.trim()
                }
            }
        }
        
        return ""
    }
    
    private fun extractAuthor(doc: org.jsoup.nodes.Document): Pair<String, String> {
        // Try to extract author from various sources
        val authorSelectors = listOf(
            "meta[property=\"og:site_name\"]",
            "meta[name=\"twitter:site\"]",
            "[data-e2e=\"video-author-uniqueid\"]",
            "[data-e2e=\"video-author-nickname\"]"
        )
        
        for (selector in authorSelectors) {
            val element = doc.selectFirst(selector)
            if (element != null) {
                val author = element.attr("content").ifEmpty { element.text() }
                if (author.isNotBlank()) {
                    val handle = if (author.startsWith("@")) author else "@$author"
                    return Pair(author.trim(), handle.trim())
                }
            }
        }
        
        return Pair("Unknown Author", "@unknown")
    }
    
    private fun extractDuration(doc: org.jsoup.nodes.Document): Int {
        // Try to extract duration from video metadata
        val durationSelectors = listOf(
            "meta[property=\"video:duration\"]",
            "meta[name=\"twitter:player:stream:content_type\"]",
            "[data-e2e=\"video-duration\"]"
        )
        
        for (selector in durationSelectors) {
            val element = doc.selectFirst(selector)
            if (element != null) {
                val duration = element.attr("content").ifEmpty { element.text() }
                if (duration.isNotBlank()) {
                    try {
                        return duration.toIntOrNull() ?: 0
                    } catch (e: Exception) {
                        // Continue to next selector
                    }
                }
            }
        }
        
        return 0
    }
    
    private fun extractThumbnail(doc: org.jsoup.nodes.Document): String {
        val thumbnailSelectors = listOf(
            "meta[property=\"og:image\"]",
            "meta[name=\"twitter:image\"]",
            "meta[name=\"twitter:image:src\"]"
        )
        
        for (selector in thumbnailSelectors) {
            val element = doc.selectFirst(selector)
            if (element != null) {
                val thumbnail = element.attr("content")
                if (thumbnail.isNotBlank()) {
                    return thumbnail.trim()
                }
            }
        }
        
        return ""
    }
    
    private fun extractVideoUrl(doc: org.jsoup.nodes.Document): String {
        val videoSelectors = listOf(
            "meta[property=\"og:video\"]",
            "meta[property=\"og:video:url\"]",
            "meta[name=\"twitter:player:stream\"]"
        )
        
        for (selector in videoSelectors) {
            val element = doc.selectFirst(selector)
            if (element != null) {
                val videoUrl = element.attr("content")
                if (videoUrl.isNotBlank()) {
                    return videoUrl.trim()
                }
            }
        }
        
        return ""
    }
}
