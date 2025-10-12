package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingTier
import app.pluct.viewmodel.CaptureRequest
import app.pluct.viewmodel.HomeViewModel
import app.pluct.purchase.CoinManager
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureInsightSheet(
    captureRequest: CaptureRequest,
    onDismiss: () -> Unit,
    onTierSelected: (ProcessingTier) -> Unit,
    viewModel: HomeViewModel,
    coinManager: CoinManager = hiltViewModel()
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Text(
                text = "Capture This Insight",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Coin balance display
            CoinBalanceDisplay(coinManager = coinManager)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content Preview
            ContentPreviewCard(
                url = captureRequest.url,
                caption = captureRequest.caption
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tier Selection
            TierSelectionSection(
                onTierSelected = onTierSelected,
                coinManager = coinManager
            )
        }
    }
}