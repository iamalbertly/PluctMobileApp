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
    onTierSelected: (ProcessingTier) -> Unit,
    viewModel: HomeViewModel
) {
    // Debug logging
    android.util.Log.d("CaptureInsightSheet", "Rendering capture sheet for URL: ${captureRequest.url}")
    android.util.Log.d("CaptureInsightSheet", "Capture sheet component is being composed")
    
    // Use ModalBottomSheet for proper bottom sheet behavior
    ModalBottomSheet(
        onDismissRequest = onDismiss
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
            
            // Modern content preview with gradient
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Creator info with modern styling
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Video",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "@${extractCreatorFromUrl(captureRequest.url)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Caption with modern typography
                    if (!captureRequest.caption.isNullOrEmpty()) {
                        Text(
                            text = captureRequest.caption,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                        )
                    } else {
                        Text(
                            text = "No caption available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Modern tier selection with enhanced design
            PluctCaptureTierSelection(
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