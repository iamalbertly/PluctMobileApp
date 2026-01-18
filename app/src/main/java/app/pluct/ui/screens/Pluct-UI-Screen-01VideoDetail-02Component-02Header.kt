package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.VideoItem
// UX FIX: Removed PluctOfflineBadge import - component deprecated

/**
 * Pluct-UI-Screen-01VideoDetail-02Component-02Header
 * Simplified header for Video Details screen showing only essential information
 * Follows naming convention: [Project]-[Module]-[Feature]-[SubFeature]-[Sequence][Responsibility]
 * Displays: Title, Author, Offline Badge (no technical details)
 */
@Composable
fun PluctVideoDetailHeader(
    video: VideoItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Title (large, bold, prominent)
        // UX FIX: Use improved title fallback logic (same as video list)
        Text(
            text = getVideoDisplayTitle(video),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Author (secondary, if available)
        if (video.author.isNotBlank()) {
            Text(
                text = "by ${video.author}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // UX FIX: Removed PluctOfflineBadge - all transcripts are saved offline by default
    }
}

/**
 * UX FIX: Improved video title fallback logic (shared with video list)
 * Provides meaningful titles even when metadata is unavailable
 * Priority: title > author > URL extraction > generic fallback
 */
@Composable
private fun getVideoDisplayTitle(video: VideoItem): String {
    return when {
        video.title.isNotBlank() -> video.title
        video.author.isNotBlank() -> "Video by @${video.author}"
        video.url.contains("@") -> {
            // Extract creator handle from TikTok URL using regex
            val handleMatch = Regex("@([^/?]+)").find(video.url)
            if (handleMatch != null) {
                val handle = handleMatch.groupValues[1]
                "Video by @$handle"
            } else {
                "TikTok Video"
            }
        }
        else -> "TikTok Video"
    }
}
