package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pluct-UI-Component-01Error-02RateLimit-01Dialog
 * Dialog shown when user hits rate limit (429 Too Many Requests)
 * Follows naming convention: [Project]-[Module]-[Component]-[Feature]-[Sequence][Responsibility]
 */
@Composable
fun PluctRateLimitDialog(
    resetTime: String,
    onDismiss: () -> Unit
) {
    val resetDate = remember(resetTime) {
        try {
            // Parse ISO 8601 format
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                .parse(resetTime)
        } catch (e: Exception) {
            null
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Timer, "Rate Limit") },
        title = { Text("Too Many Requests") },
        text = {
            Column {
                // UX IMPROVEMENT #3: Better rate limit messaging with specific guidance
                Text("You've reached the rate limit for token requests.")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Limit: 10 requests per hour",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                if (resetDate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val now = System.currentTimeMillis()
                    val resetTime = resetDate.time
                    val minutesUntilReset = ((resetTime - now) / 60000).toInt().coerceAtLeast(0)
                    Text(
                        "⏰ Try again in ${if (minutesUntilReset > 0) "$minutesUntilReset minute${if (minutesUntilReset != 1) "s" else ""}" else "less than a minute"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Or after: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(resetDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "💡 Tip: You can queue videos for later processing. They'll automatically process when the rate limit resets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
