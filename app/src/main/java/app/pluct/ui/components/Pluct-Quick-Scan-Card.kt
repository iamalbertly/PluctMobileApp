package app.pluct.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import android.util.Log

/**
 * Quick Scan Card - fully clickable with stable test tags
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Composable
fun QuickScanCard(
    url: String?,
    credits: Int,
    onStart: (reqId: String) -> Unit
) {
    val enabled = !url.isNullOrBlank() && credits > 0
    val reqId = remember { UUID.randomUUID().toString() }
    var firing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { 
                contentDescription = "quick_scan"
                role = Role.Button
            }
            .testTag("quick_scan")
            .clickable(enabled = enabled && !firing, role = Role.Button) {
                if (!firing) {
                    firing = true
                    android.util.Log.i("TTT", "QUICK_SCAN_UI_CLICK reqId=$reqId")
                    android.util.Log.i("pluct-http", """PLUCT_HTTP>OUT {"event":"ui_click","target":"quick_scan","reqId":"$reqId","timestamp":${System.currentTimeMillis()}}""")
                    onStart(reqId)
                    // Reset firing state after a delay
                    coroutineScope.launch {
                        delay(1000)
                        firing = false
                    }
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (enabled) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Text(
                text = "⚡️",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 16.dp)
            )
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Quick Scan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Free • Fast transcript",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Get a quick transcript in seconds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3
                )
            }
            
            // Action icon
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Start Quick Scan",
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
