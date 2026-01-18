package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
 * Pluct-UI-Component-08InlineHint-01Card
 * Non-blocking inline hint card for users who skip onboarding tutorial
 * Follows naming convention: [Project]-[UI]-[Component]-[InlineHint]-[Sequence][Card]
 */
@Composable
fun PluctUIComponent08InlineHint01Card(
    onShowTutorial: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                testTag = "inline_hint_card"
                contentDescription = "Inline tutorial hint"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Hint icon and text
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "💡",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Share any TikTok video with Pluct to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    TextButton(
                        onClick = onShowTutorial,
                        modifier = Modifier.semantics {
                            testTag = "inline_hint_show_tutorial_button"
                            contentDescription = "Show me how"
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "Show me how →",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Dismiss button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    testTag = "inline_hint_dismiss_button"
                    contentDescription = "Dismiss hint"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
