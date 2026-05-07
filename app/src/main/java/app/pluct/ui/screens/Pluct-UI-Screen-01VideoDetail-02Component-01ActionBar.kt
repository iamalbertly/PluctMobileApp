package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Pluct-UI-Screen-01VideoDetail-02Component-01ActionBar
 * Sticky action bar for Video Details screen with Copy, Share, Export actions
 * Follows naming convention: [Project]-[Module]-[Feature]-[SubFeature]-[Sequence][Responsibility]
 * Always visible at top of screen for immediate access to primary actions
 */
@Composable
fun PluctVideoDetailActionBar(
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FilledTonalIconButton(
                onClick = onCopy,
                modifier = Modifier
                    .semantics {
                        contentDescription = "Copy transcript to clipboard"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(Modifier.width(8.dp))
            
            OutlinedIconButton(
                onClick = onShare,
                modifier = Modifier
                    .semantics {
                        contentDescription = "Share transcript"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(Modifier.width(8.dp))
            
            OutlinedIconButton(
                onClick = onExport,
                modifier = Modifier
                    .semantics {
                        contentDescription = "Export to TXT file"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Export",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
