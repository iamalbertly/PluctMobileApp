package app.pluct.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pluct-UI-Header-03Comprehensive - Comprehensive header with credit balance and settings
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctComprehensiveHeader(
    creditBalance: Int,
    isCreditBalanceLoading: Boolean,
    creditBalanceError: String?,
    onRefreshCreditBalance: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = "Pluct",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("app_title")
            )
        },
        actions = {
            // Credit balance display
            PluctCreditBalanceButton(
                creditBalance = creditBalance,
                isLoading = isCreditBalanceLoading,
                error = creditBalanceError,
                onRefresh = onRefreshCreditBalance,
                modifier = Modifier.testTag("credit_balance_section")
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Settings button
            PluctSettingsIconButton(
                onSettingsClick = onSettingsClick,
                modifier = Modifier.testTag("settings_section")
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        modifier = modifier.testTag("comprehensive_header")
    )
}

@Composable
fun PluctModernHeader(
    creditBalance: Int,
    isCreditBalanceLoading: Boolean,
    creditBalanceError: String?,
    onRefreshCreditBalance: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("modern_header"),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // App title and actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App title
                Text(
                    text = "Pluct",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("app_title")
                )
                
                // Actions row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Credit balance
                    PluctCreditBalanceDisplay(
                        creditBalance = creditBalance,
                        isLoading = isCreditBalanceLoading,
                        error = creditBalanceError,
                        onRefresh = onRefreshCreditBalance,
                        modifier = Modifier.testTag("credit_balance_section")
                    )
                    
                    // Settings
                    PluctSettingsButton(
                        onSettingsClick = onSettingsClick,
                        modifier = Modifier.testTag("settings_section")
                    )
                }
            }
        }
    }
}
