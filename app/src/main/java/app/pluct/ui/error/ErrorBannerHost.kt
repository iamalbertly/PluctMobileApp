package app.pluct.ui.error

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ErrorBannerHost(center: ErrorCenter) {
    val errors by center.errors.collectAsState(initial = null)
    
    errors?.let { error ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("error_banner"),
            color = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ) {
            Text(
                text = "${error.code}: ${error.message}",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
