package app.pluct.ui.error

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.pluct.core.error.ErrorEnvelope
import app.pluct.core.log.PluctLogger
import kotlinx.coroutines.launch

@Composable
fun PluctErrorBanner(
    errors: List<ErrorEnvelope>,
    onDismiss: (Int) -> Unit
) {
    AnimatedVisibility(visible = errors.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            val e = errors.first()
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("error_banner") // REQUIRED for UI assertion
                    .semantics { contentDescription = "error_code:${e.code}" }, // REQUIRED for code assertion
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(Modifier.padding(12.dp)) {
                    Text(e.message)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { onDismiss(0) }) { Text("Dismiss") }
                }
            }
        }
    }
}

@Composable
fun ErrorBannerHost(center: ErrorCenter) {
    val scope = rememberCoroutineScope()
    var queue by remember { mutableStateOf(listOf<ErrorEnvelope>()) }
    var selectedError by remember { mutableStateOf<ErrorEnvelope?>(null) }
    var showDetailsModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        center.events.collect { e ->
            android.util.Log.d("ErrorBannerHost", "Received error: ${e.code} - ${e.message}")
            PluctLogger.logError(e)
            queue = queue + e
        }
    }

    android.util.Log.d("ErrorBannerHost", "Rendering with ${queue.size} errors")
    
    // Show modern error banner system
    PluctErrorBannerSystem(
        errors = queue,
        onDismiss = { index -> 
            queue = queue.toMutableList().also { it.removeAt(index) }
        },
        onShowDetails = { error ->
            selectedError = error
            showDetailsModal = true
        }
    )
    
    // Show details modal
    selectedError?.let { error ->
        PluctErrorDetailsModal(
            error = error,
            isVisible = showDetailsModal,
            onDismiss = { 
                showDetailsModal = false
                selectedError = null
            }
        )
    }
}
