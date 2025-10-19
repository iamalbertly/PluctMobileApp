package app.pluct.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingTier
import app.pluct.R

/**
 * Tier selection component for Pluct capture sheet
 */
@Composable
fun PluctCaptureTierSelection(
    url: String?,
    credits: Int,
    onTierSelected: (ProcessingTier, String?) -> Unit
) {
    android.util.Log.i("PluctCaptureTierSelection", "🎯 RENDERING TIER SELECTION COMPONENT url=$url credits=$credits")
    val cdProcess = stringResource(R.string.cd_process)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Scan Option - using dedicated QuickScanCard
        QuickScanCard(
            url = url,
            credits = credits,
            onStart = { clientRequestId ->
                android.util.Log.i("PluctCaptureTierSelection", "🎯 QUICK SCAN SELECTED clientRequestId=$clientRequestId")
                android.util.Log.i("pluct-http", """PLUCT_HTTP>OUT {"event":"tier_selected","tier":"QUICK_SCAN","clientRequestId":"$clientRequestId"}""")
                onTierSelected(ProcessingTier.QUICK_SCAN, clientRequestId)
            }
        )
        
        // AI Analysis Option
        PluctTierSelectionCard(
            title = "AI Analysis",
            subtitle = "Premium • Deep insights",
            icon = "✨",
            description = "AI-powered summary, key takeaways & actionable steps",
            onClick = { 
                android.util.Log.i("PluctCaptureTierSelection", "🎯 AI ANALYSIS SELECTED")
                onTierSelected(ProcessingTier.AI_ANALYSIS, null) 
            },
            isRecommended = true,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = cdProcess }
                .testTag("btn_process_ai")
        )
    }
}

@Composable
private fun PluctTierSelectionCard(
    title: String,
    subtitle: String,
    icon: String,
    description: String,
    onClick: () -> Unit,
    isRecommended: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .clickable(role = Role.Button) { 
                isPressed = true
                // Emit telemetry for UI clicks
                android.util.Log.i("pluct-http", """PLUCT_HTTP>OUT {"event":"ui_click","target":"$title","timestamp":${System.currentTimeMillis()}}""")
                android.util.Log.i("PluctTierSelectionCard", "🎯 CARD CLICKED: $title")
                onClick()
            }
            .semantics { 
                contentDescription = title
                role = Role.Button
            }
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isRecommended) 6.dp else 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 16.dp)
            )
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Recommended",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3
                )
            }
        }
    }
}
