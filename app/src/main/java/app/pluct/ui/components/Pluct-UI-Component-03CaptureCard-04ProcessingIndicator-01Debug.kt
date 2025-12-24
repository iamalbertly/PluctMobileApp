package app.pluct.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pluct.services.OperationStep
import app.pluct.services.TranscriptionDebugInfo

@Composable
fun PluctProcessingDebugDetails(debugInfo: TranscriptionDebugInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PluctProcessingDebugInfoRow("Current Step", debugInfo.getCurrentOperationDescription())
        PluctProcessingDebugInfoRow("Flow ID", debugInfo.flowRequestId.takeLast(12))
        debugInfo.jobId?.let { PluctProcessingDebugInfoRow("Job ID", it) }

        if (debugInfo.currentStep == OperationStep.POLLING && debugInfo.pollingAttempt != null) {
            PluctProcessingDebugInfoRow(
                "Polling",
                "${debugInfo.pollingAttempt}/${debugInfo.maxPollingAttempts}"
            )
        }

        debugInfo.getLatestRequest()?.let { request ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Latest Request:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            PluctProcessingDebugInfoRow("Method", request.method)
            PluctProcessingDebugInfoRow("Endpoint", request.endpoint)
            request.payload?.let { payload ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Payload:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = payload,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        debugInfo.getLatestResponse()?.let { response ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Latest Response:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            PluctProcessingDebugInfoRow("Status", "${response.statusCode} ${response.statusMessage}")
            PluctProcessingDebugInfoRow("Duration", "${response.duration}ms")
            response.body?.let { body ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Response Body:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        // Expectation vs actual and next actions
        val latest = debugInfo.timeline.lastOrNull()
        latest?.expected?.let { expected ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Expectation:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = expected,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        latest?.received?.let { received ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Received:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = received,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        latest?.nextAction?.let { action ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Next Action:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = action,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        latest?.correlationId?.let { corr ->
            Spacer(modifier = Modifier.height(2.dp))
            PluctProcessingDebugInfoRow("Correlation ID", corr)
        }

        latest?.retryCount?.let { retries ->
            PluctProcessingDebugInfoRow("Retries", retries.toString())
        }

        if (debugInfo.timeline.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Timeline (${debugInfo.timeline.size} steps):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            debugInfo.timeline.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = entry.step.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = entry.duration?.let { "${it}ms" } ?: "...",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PluctProcessingDebugInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.weight(0.6f)
        )
    }
}
