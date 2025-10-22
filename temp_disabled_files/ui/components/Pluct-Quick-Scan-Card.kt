package app.pluct.ui.components

import android.util.Log
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
import androidx.compose.runtime.rememberCoroutineScope
import java.util.UUID
import androidx.compose.ui.platform.LocalContext
import android.view.View
import android.widget.Button
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Quick Scan Card - fully clickable with stable test tags
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Composable
fun QuickScanCard(
    url: String?,
    credits: Int,
    onStart: (clientRequestId: String) -> Unit
) {
    val enabled = !url.isNullOrBlank() && credits > 0
    val clientRequestId = remember { UUID.randomUUID().toString() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "quick_scan"     // <- test anchor (content-desc)
                role = Role.Button
            }
            .clickable(enabled = enabled, role = Role.Button) {
                Log.i("TTT", "QUICK_SCAN_UI_CLICK id=$clientRequestId")
                onStart(clientRequestId)                  // enqueue work (below)
            }
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (enabled) 2.dp else 0.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚡️"); Spacer(Modifier.width(12.dp))
            Column { 
                Text("Quick Scan"); 
                Text("Free • Fast transcript") 
            }
        }
    }
}