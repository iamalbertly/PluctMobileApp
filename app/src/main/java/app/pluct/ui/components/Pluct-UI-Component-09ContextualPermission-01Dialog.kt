package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pluct-UI-Component-09ContextualPermission-01Dialog
 * Contextual permission request shown after first transcript completion
 * Follows naming convention: [Project]-[UI]-[Component]-[ContextualPermission]-[Sequence][Dialog]
 */
@Composable
fun PluctUIComponent09ContextualPermission01Dialog(
    onDismiss: () -> Unit,
    onEnable: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Get Notified",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    contentDescription = "Contextual permission title"
                    testTag = "contextual_permission_title"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Contextual permission content"
                        testTag = "contextual_permission_content"
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon
                Text(
                    text = "🔔",
                    fontSize = 48.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                // Value proposition
                Text(
                    text = "Get notified when transcripts finish, even when the app is closed.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "You can turn this off anytime in settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onEnable,
                modifier = Modifier.semantics {
                    contentDescription = "Enable notifications"
                    testTag = "contextual_permission_enable_button"
                }
            ) {
                Text("Enable")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Not now"
                    testTag = "contextual_permission_dismiss_button"
                }
            ) {
                Text("Not Now")
            }
        },
        modifier = Modifier.semantics {
            contentDescription = "Contextual permission dialog"
            testTag = "contextual_permission_dialog"
        }
    )
}
