package app.pluct.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pluct.R

data class ProgressItem(
    val url: String,
    val stage: String,
    val percent: Int,
    val message: String? = null
)

@Composable
fun ProcessingStatusPanel(
    items: List<ProgressItem> = emptyList(),
    modifier: Modifier = Modifier
) {
    val statusPanelCd = stringResource(R.string.cd_status_panel)
    
    Column(
        modifier = modifier
            .semantics { contentDescription = statusPanelCd }
            .testTag("panel_processing_status")
    ) {
        items.forEach { item ->
            Text(item.url)
            LinearProgressIndicator(progress = (item.percent / 100f))
            Text("${item.stage} ${item.message.orEmpty()}")
        }
    }
}