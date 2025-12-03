package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pluct-UI-Component-01Welcome-01Dialog-01Main
 * Welcome dialog for first-time users
 * Follows naming convention: [Project]-[Module]-[Component]-[Feature]-[Sequence][Responsibility]
 */
@Composable
fun PluctWelcomeDialog(
    onDismiss: () -> Unit,
    onGetStarted: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Welcome to Pluct!") },
        text = {
            Column(
                modifier = Modifier.semantics {
                    contentDescription = "Welcome highlights"
                    testTag = "welcome_dialog_body"
                }
            ) {
                Text(
                    "Get started with 3 FREE transcriptions!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("No credit card required")
                Text("Instant transcripts")
                Text("Auto-copy to clipboard")
            }
        },
        confirmButton = {
            Button(
                onClick = onGetStarted,
                modifier = Modifier.semantics {
                    contentDescription = "Get Started"
                    testTag = "welcome_get_started_button"
                }
            ) {
                Text("Get Started")
            }
        },
        modifier = Modifier.semantics { testTag = "welcome_dialog" }
    )
}
