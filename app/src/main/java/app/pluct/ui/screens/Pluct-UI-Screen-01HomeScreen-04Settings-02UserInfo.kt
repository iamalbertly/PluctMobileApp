package app.pluct.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.core.user.PluctCoreUser01Display01Formatter

/**
 * Pluct-UI-Screen-01HomeScreen-04Settings-02UserInfo - User info section in settings
 * Follows naming convention: [Project]-[UI]-[Screen]-[HomeScreen]-[Settings]-[Sequence][UserInfo]
 * 7 scope layers: Project, UI, Screen, HomeScreen, Settings, Sequence, UserInfo
 */
@Composable
fun PluctUIScreen01HomeScreen04Settings02UserInfoSection(
    userName: String,
    creditBalance: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = PluctCoreUser01Display01Formatter.friendlyDeviceLabel(userName),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Balance: $creditBalance",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
