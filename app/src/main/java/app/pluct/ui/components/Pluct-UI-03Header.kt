package app.pluct.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctHomeShellTopBar(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PluctUIComponent02Branding01LogoMark(size = 30.dp)
                Text(
                    text = "Pluct",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        actions = {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(44.dp)
                    .semantics {
                        contentDescription = "Settings button"
                        testTag = "settings_button"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = modifier
            .padding(horizontal = 24.dp)
            .semantics {
                contentDescription = "App header"
                testTag = "home_shell_top_bar"
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctHeader(
    creditBalance: Int,
    isCreditBalanceLoading: Boolean,
    creditBalanceError: String?,
    onRefreshCreditBalance: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PluctHeaderBar(
        creditBalance = creditBalance,
        isCreditBalanceLoading = isCreditBalanceLoading,
        creditBalanceError = creditBalanceError,
        onRefreshCreditBalance = onRefreshCreditBalance,
        onSettingsClick = onSettingsClick,
        refreshable = false,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctHeaderWithRefreshableBalance(
    creditBalance: Int,
    isCreditBalanceLoading: Boolean,
    creditBalanceError: String?,
    onRefreshCreditBalance: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PluctHeaderBar(
        creditBalance = creditBalance,
        isCreditBalanceLoading = isCreditBalanceLoading,
        creditBalanceError = creditBalanceError,
        onRefreshCreditBalance = onRefreshCreditBalance,
        onSettingsClick = onSettingsClick,
        refreshable = true,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluctHeaderBar(
    creditBalance: Int,
    isCreditBalanceLoading: Boolean,
    creditBalanceError: String?,
    onRefreshCreditBalance: () -> Unit,
    onSettingsClick: () -> Unit,
    refreshable: Boolean,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pluct",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.semantics { contentDescription = "Pluct app title" }
                )
                CreditBalanceChip(
                    creditBalance = creditBalance,
                    isLoading = isCreditBalanceLoading,
                    error = creditBalanceError,
                    onRefresh = onRefreshCreditBalance,
                    refreshable = refreshable
                )
            }
        },
        actions = {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(44.dp)
                    .semantics {
                        contentDescription = "Settings button"
                        testTag = "settings_button"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        modifier = modifier.semantics {
            contentDescription = "App header with credit balance and settings"
        }
    )
}

@Composable
private fun CreditBalanceChip(
    creditBalance: Int,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    refreshable: Boolean
) {
    val lowBalance = creditBalance in 1..2
    val label = when {
        isLoading -> "..."
        error != null -> "!"
        else -> "$creditBalance credits"
    }
    val description = when {
        isLoading -> "Loading credits — wallet balance"
        error != null -> "Credit balance error: $error"
        lowBalance -> "Credit balance: $creditBalance credits - low balance"
        else -> "Credit balance: $creditBalance credits"
    } + if (refreshable) " - tap to refresh" else ""

    Surface(
        color = if (error != null || lowBalance) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .then(if (refreshable) Modifier.clickable { onRefresh() } else Modifier)
            .semantics {
                testTag = "header_credit_balance_chip"
                contentDescription = description
            }
    ) {
        Row(
            modifier = Modifier
                .width(116.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (error != null || lowBalance) Icons.Default.Warning else Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (error != null || lowBalance) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
            Spacer(modifier = Modifier.width(6.dp))
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (error != null || lowBalance) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }
        }
    }
}
