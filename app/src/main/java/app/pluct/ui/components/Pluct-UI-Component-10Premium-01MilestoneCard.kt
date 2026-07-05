package app.pluct.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp

@Composable
fun PluctUIComponent10Premium01MilestoneCard(count: Int, onSeePremium: () -> Unit, onLater: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().semantics { testTag = "premium_milestone_card"; contentDescription = "Premium offer after $count successful Plucts" },
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("You've Plucted $count videos.", style = MaterialTheme.typography.titleMedium)
            Text("Treat yourself to more uses, a faster queue, no ads, and longer history.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSeePremium, modifier = Modifier.semantics { contentDescription = "See Premium" }) { Text("See Premium") }
                OutlinedButton(onClick = onLater, modifier = Modifier.semantics { contentDescription = "Maybe later" }) { Text("Maybe later") }
            }
        }
    }
}
