package app.pluct.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ContentPreviewCard(
    url: String,
    caption: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Thumbnail with Coil
            val thumbnailUrl = extractThumbnailUrl(url)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Video thumbnail",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // From field
            Text(
                text = "From: @${extractCreatorFromUrl(url)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Caption
            if (!caption.isNullOrEmpty()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = "No caption available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // URL
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Extract thumbnail URL from TikTok video URL
 */
private fun extractThumbnailUrl(videoUrl: String): String? {
    return try {
        when {
            videoUrl.contains("tiktok.com") -> {
                val videoId = videoUrl.substringAfterLast("/")
                "https://p16-sign-va.tiktokcdn-us.com/obj/tos-useast2a-p-0068-tx/placeholder_thumbnail.jpg"
            }
            videoUrl.contains("vm.tiktok.com") -> {
                "https://p16-sign-va.tiktokcdn-us.com/obj/tos-useast2a-p-0068-tx/placeholder_thumbnail.jpg"
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Extract creator name from TikTok URL
 */
private fun extractCreatorFromUrl(videoUrl: String): String {
    return try {
        when {
            videoUrl.contains("@") -> {
                val startIndex = videoUrl.indexOf("@") + 1
                val endIndex = videoUrl.indexOf("/", startIndex)
                if (endIndex == -1) videoUrl.substring(startIndex) else videoUrl.substring(startIndex, endIndex)
            }
            videoUrl.contains("vm.tiktok.com") -> {
                // For short URLs, we can't extract creator easily, so use a fallback
                "creator"
            }
            else -> "creator"
        }
    } catch (e: Exception) {
        "creator"
    }
}
