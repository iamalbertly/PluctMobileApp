package app.pluct.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp

/**
 * Pluct-UI-Screen-01HomeScreen-04Settings-04Credits - Credits request section in settings
 * Follows naming convention: [Project]-[UI]-[Screen]-[HomeScreen]-[Settings]-[Sequence][Credits]
 * 7 scope layers: Project, UI, Screen, HomeScreen, Settings, Sequence, Credits
 */
@Composable
fun PluctUIScreen01HomeScreen04Settings04CreditsSection(
    isRequesting: Boolean,
    referenceText: String,
    onReferenceTextChange: (String) -> Unit,
    isCreditRequestInFlight: Boolean,
    creditRequestStatus: String?,
    onRequestCredits: () -> Unit,
    onToggleRequesting: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isRequesting) {
            if (creditRequestStatus != null) {
                Text(
                    text = creditRequestStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (!isCreditRequestInFlight) {
                // Request form
                Text(
                    text = "Send payment, then paste the SMS here.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = referenceText,
                    onValueChange = onReferenceTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Paste payment confirmation message"
                            testTag = "settings_reference_input"
                        },
                    placeholder = { Text("Paste payment SMS") },
                    maxLines = 4,
                    minLines = 2,
                    enabled = !isCreditRequestInFlight
                )
            }
        } else {
            Button(
                onClick = onToggleRequesting,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Add wallet balance"
                        testTag = "settings_request_credits_button"
                    }
            ) {
                Text("Add Balance")
            }
        }
    }
}
