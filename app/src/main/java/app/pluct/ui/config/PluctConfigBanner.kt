package app.pluct.ui.config

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun PluctConfigBanner(
    isDegraded: Boolean,
    message: String,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = isDegraded) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("config_banner") // REQUIRED for UI assertion
                    .semantics { contentDescription = "config_status:degraded" }, // REQUIRED for status assertion
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(Modifier.padding(12.dp)) {
                    Text(message)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Dismiss") }
                }
            }
        }
    }
}

@Composable
fun ConfigBannerHost(
    isDegraded: Boolean,
    message: String,
    onDismiss: () -> Unit
) {
    PluctConfigBanner(
        isDegraded = isDegraded,
        message = message,
        onDismiss = onDismiss
    )
}
