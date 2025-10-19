package app.pluct.ui.components

import java.text.SimpleDateFormat
import java.util.*

/**
 * Pluct-UI-Utils - Utility functions for UI components
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */

/**
 * Format timestamp to human-readable time ago string
 */
fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val date = Date(timestamp)
            val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
            formatter.format(date)
        }
    }
}
