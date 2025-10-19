package app.pluct.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingTier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.pluct.R
import app.pluct.viewmodel.CaptureRequest
import app.pluct.viewmodel.HomeViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureInsightSheet(
    captureRequest: CaptureRequest,
    onDismiss: () -> Unit,
    onTierSelected: (ProcessingTier, String?) -> Unit,
    onUrlChange: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") viewModel: HomeViewModel
) {
    // Debug logging
    android.util.Log.i("CaptureInsightSheet", "🎯 RENDERING CAPTURE SHEET for URL: ${captureRequest.url}")
    android.util.Log.i("CaptureInsightSheet", "🎯 Capture sheet component is being composed")
    android.util.Log.i("CaptureInsightSheet", "🎯 Capture request details - URL: ${captureRequest.url}, Caption: ${captureRequest.caption}")
    android.util.Log.i("CaptureInsightSheet", "🎯 ModalBottomSheet is about to be rendered")
    
    // Use ModalBottomSheet for proper bottom sheet behavior with auto-expansion
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(Unit) { sheetState.expand() }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        // Precompute localized labels usable in non-composable scopes
        val captureCd = stringResource(R.string.cd_capture_sheet)

        // Main content with modern design
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .semantics { contentDescription = captureCd }
                .testTag("sheet_capture")
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Capture This Insight",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // URL input field (moved from Home screen)
            PluctCompactUrlBox(
                value = captureRequest.url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Modern tier selection with enhanced design
            PluctCaptureTierSelection(
                url = captureRequest.url,
                credits = 10, // TODO: Get actual credits from viewModel
                onTierSelected = onTierSelected
            )
        }
    }
}

private fun extractCreatorFromUrl(url: String): String {
    return try {
        when {
            url.contains("@") -> {
                val startIndex = url.indexOf("@") + 1
                val endIndex = url.indexOf("/", startIndex)
                if (endIndex == -1) url.substring(startIndex) else url.substring(startIndex, endIndex)
            }
            url.contains("vm.tiktok.com") -> "creator"
            else -> "creator"
        }
    } catch (e: Exception) {
        "creator"
    }
}