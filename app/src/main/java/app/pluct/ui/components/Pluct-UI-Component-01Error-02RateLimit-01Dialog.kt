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
                Text("You've reached the rate limit for token requests.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Limit: 10 requests per hour")
                if (resetDate != null) {
                    Text("Try again after: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(resetDate)}")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This helps prevent abuse and ensures fair usage for all users.",
                    style = MaterialTheme.typography.bodySmall
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
