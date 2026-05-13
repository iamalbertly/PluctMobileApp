package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.VideoItem

/**
 * Pluct-UI-Screen-01VideoDetail-02Component-02Header
 * Simplified header for Video Details screen showing only essential information
 * Follows naming convention: [Project]-[Module]-[Feature]-[SubFeature]-[Sequence][Responsibility]
 * Displays: Title, Author (no technical details)
 */
@Composable
fun PluctVideoDetailHeader(
    video: VideoItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = getVideoDetailDisplayTitle(video),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        val displayAuthor = getVideoDetailDisplayAuthor(video)
        if (displayAuthor.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = displayAuthor,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/**
 * UX FIX: Improved video title fallback logic (shared with video list)
 * Provides meaningful titles even when metadata is unavailable
 * Priority: title > author > URL extraction > generic fallback
 */
internal fun getVideoDetailDisplayTitle(video: VideoItem): String {
    return when {
        video.title.isNotBlank() && !isGenericVideoTitle(video.title) -> video.title
        !video.transcript.isNullOrBlank() -> transcriptPreviewTitle(video.transcript)
        getVideoDetailDisplayAuthor(video).isNotBlank() -> getVideoDetailDisplayAuthor(video).removePrefix("by ")
        video.url.contains("vm.tiktok.com", ignoreCase = true) ||
            video.url.contains("vt.tiktok.com", ignoreCase = true) ||
            video.url.contains("tiktok.com/t/", ignoreCase = true) -> {
            val m = Regex("[/]([A-Za-z0-9]{7,})").find(video.url)
            if (m != null) {
                "Video · ${m.groupValues[1].take(12)}"
            } else {
                tikTokUrlTitleFallback(video.url)
            }
        }
        video.url.contains("@") -> {
            val handleMatch = Regex("@([^/?]+)").find(video.url)
            if (handleMatch != null) {
                val handle = handleMatch.groupValues[1]
                "Video by @$handle"
            } else {
                "TikTok transcript"
            }
        }
        else -> "TikTok transcript"
    }
}

private fun tikTokUrlTitleFallback(url: String): String {
    val handleMatch = Regex("@([^/?]+)").find(url)
    return if (handleMatch != null) {
        "Video by @${handleMatch.groupValues[1]}"
    } else {
        "TikTok transcript"
    }
}

internal fun getVideoDetailDisplayAuthor(video: VideoItem): String {
    val author = video.author.trim().trimStart('@')
    if (author.isBlank() || isGenericAuthor(author)) {
        return ""
    }
    return "by @$author"
}

private fun isGenericVideoTitle(title: String): Boolean {
    val normalized = title.trim().lowercase()
    return normalized == "tiktok video" ||
        normalized == "queued video" ||
        normalized == "tiktok transcript" ||
        normalized == "tiktok - make your day"
}

private fun isGenericAuthor(author: String): Boolean {
    val normalized = author.trim().lowercase()
    return normalized == "tiktok user" ||
        normalized == "tiktok" ||
        normalized == "creator"
}

private fun transcriptPreviewTitle(transcript: String?): String {
    val compact = transcript.orEmpty()
        .replace(Regex("\\s+"), " ")
        .trim()
    if (compact.isBlank()) {
        return "TikTok transcript"
    }

    val sentence = compact
        .split('.', '?', '!')
        .firstOrNull()
        ?.trim()
        .orEmpty()
        .ifBlank { compact }

    return if (sentence.length <= 72) {
        sentence
    } else {
        sentence.take(69).trimEnd() + "..."
    }
}
