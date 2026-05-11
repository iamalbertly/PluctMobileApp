package app.pluct.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import app.pluct.ui.readiness.PluctUIReadiness01Kind

/**
 * Pluct-UI-Component-09Readiness-01Strip - One-glance readiness above main content
 * Scopes: Project, UI, Component, Readiness, Strip
 */
@Composable
fun PluctUIComponent09Readiness01Strip(
    kind: PluctUIReadiness01Kind,
    onOpenMoney: () -> Unit,
    onOpenNetworkSettings: () -> Unit,
    onRetryBalance: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (kind == PluctUIReadiness01Kind.READY) return

    val icon: ImageVector
    val title: String
    val subtitle: String
    val primaryLabel: String?
    val onPrimary: (() -> Unit)?
    when (kind) {
        PluctUIReadiness01Kind.CHECKING -> {
            icon = Icons.Default.Schedule
            title = "One moment"
            subtitle = "Checking account"
            primaryLabel = null
            onPrimary = null
        }
        PluctUIReadiness01Kind.VERIFY_FAILED -> {
            icon = Icons.Default.Schedule
            title = "Can't verify"
            subtitle = "Internet issue? Tap Retry"
            primaryLabel = "Retry"
            onPrimary = onRetryBalance
        }
        PluctUIReadiness01Kind.NO_NETWORK -> {
            icon = Icons.Default.WifiOff
            title = "No internet"
            subtitle = "Wi‑Fi or mobile data"
            primaryLabel = "Open settings"
            onPrimary = onOpenNetworkSettings
        }
        PluctUIReadiness01Kind.NO_CREDITS -> {
            icon = Icons.Default.AccountBalanceWallet
            title = "No balance"
            subtitle = "Add to continue"
            primaryLabel = "Add"
            onPrimary = onOpenMoney
        }
        PluctUIReadiness01Kind.SERVICE_DEGRADED -> {
            icon = Icons.Default.CloudOff
            title = "Busy"
            subtitle = "Try again soon"
            primaryLabel = null
            onPrimary = null
        }
        PluctUIReadiness01Kind.SERVICE_SLOW -> {
            icon = Icons.Default.CloudOff
            title = "Slower"
            subtitle = "Please wait"
            primaryLabel = null
            onPrimary = null
        }
        PluctUIReadiness01Kind.READY -> return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                testTag = "readiness_strip"
                contentDescription = "$title. $subtitle"
            },
        color = when (kind) {
            PluctUIReadiness01Kind.NO_CREDITS -> MaterialTheme.colorScheme.errorContainer
            PluctUIReadiness01Kind.NO_NETWORK -> MaterialTheme.colorScheme.tertiaryContainer
            PluctUIReadiness01Kind.SERVICE_SLOW -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.secondaryContainer
        },
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            if (primaryLabel != null && onPrimary != null) {
                Button(
                    onClick = onPrimary,
                    modifier = Modifier.semantics { contentDescription = primaryLabel }
                ) { Text(primaryLabel) }
            }
        }
    }
}
