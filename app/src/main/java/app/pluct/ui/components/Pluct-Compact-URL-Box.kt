package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Slim URL input box with inline Process button
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Composable
fun PluctCompactUrlBox(
    value: String,
    onValueChange: (String) -> Unit,
    onProcess: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("url_box")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("url_input"),
                singleLine = true,
                minLines = 1,
                maxLines = 1,
                placeholder = { 
                    Text("https://www.tiktok.com/@…/video/…") 
                },
                label = { Text("TikTok URL") }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            FilledTonalButton(
                onClick = onProcess,
                enabled = enabled,
                modifier = Modifier.testTag("process_button")
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Process")
            }
        }
    }
}
