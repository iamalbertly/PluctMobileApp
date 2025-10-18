package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pluct-Header-Compact - Unified compact header component with integrated credit balance
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Replaces redundant PluctHomeTopBar and PluctHomeWelcomeSection
 */
@Composable
fun PluctHeaderCompact(
    creditBalance: Int = 0,
    isCreditBalanceLoading: Boolean = false,
    creditBalanceError: String? = null,
    onRefreshCreditBalance: () -> Unit = {}
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top row with title and credit balance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pluct",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Elegant credit balance display
            PluctCreditBalanceDisplay(
                creditBalance = creditBalance,
                isLoading = isCreditBalanceLoading,
                error = creditBalanceError,
                onRefresh = onRefreshCreditBalance
            )
        }
        
        Text(
            text = "Summarize any TikTok instantly — learn, store, revisit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
