package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pluct.R

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
    val permissionTitle = stringResource(R.string.contextual_permission_title)
    val cdPermissionTitle = stringResource(R.string.cd_contextual_permission_title)
    val cdPermissionContent = stringResource(R.string.cd_contextual_permission_content)
    val iconEmoji = stringResource(R.string.contextual_permission_icon)
    val bodyPrimary = stringResource(R.string.contextual_permission_body_primary)
    val bodySecondary = stringResource(R.string.contextual_permission_body_secondary)
    val enableLabel = stringResource(R.string.contextual_permission_enable)
    val notNowLabel = stringResource(R.string.contextual_permission_not_now)
    val cdEnable = stringResource(R.string.cd_contextual_permission_enable)
    val cdNotNow = stringResource(R.string.cd_contextual_permission_not_now)
    val cdDialog = stringResource(R.string.cd_contextual_permission_dialog)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = permissionTitle,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    contentDescription = cdPermissionTitle
                    testTag = "contextual_permission_title"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = cdPermissionContent
                        testTag = "contextual_permission_content"
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = iconEmoji,
                    fontSize = 48.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = bodyPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = bodySecondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onEnable,
                modifier = Modifier.semantics {
                    contentDescription = cdEnable
                    testTag = "contextual_permission_enable_button"
                }
            ) {
                Text(enableLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = cdNotNow
                    testTag = "contextual_permission_dismiss_button"
                }
            ) {
                Text(notNowLabel)
            }
        },
        modifier = Modifier.semantics {
            contentDescription = cdDialog
            testTag = "contextual_permission_dialog"
        }
    )
}
