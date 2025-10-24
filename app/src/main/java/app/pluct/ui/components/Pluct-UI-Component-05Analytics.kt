package app.pluct.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingTier
import kotlin.math.max

/**
 * Pluct-UI-Component-05Analytics - Analytics and insights dashboard for transcriptions
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */

@Composable
fun PluctAnalyticsDashboard(
    analytics: TranscriptionAnalytics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("analytics_dashboard")
    ) {
        // Header
        Text(
            text = "Transcription Analytics",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("analytics_title")
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Overview cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PluctAnalyticsCard(
                title = "Total Transcriptions",
                value = analytics.totalTranscriptions.toString(),
                icon = Icons.Default.VideoLibrary,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("total_transcriptions_card")
            )
            
            PluctAnalyticsCard(
                title = "Success Rate",
                value = "${(analytics.successRate * 100).toInt()}%",
                icon = Icons.Default.CheckCircle,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("success_rate_card")
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quality distribution
        PluctQualityDistributionChart(
            qualityDistribution = analytics.qualityDistribution,
            modifier = Modifier.testTag("quality_distribution_chart")
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tier usage
        PluctTierUsageChart(
            tierUsage = analytics.tierUsage,
            modifier = Modifier.testTag("tier_usage_chart")
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Recent activity
        PluctRecentActivityList(
            recentActivity = analytics.recentActivity,
            modifier = Modifier.testTag("recent_activity_list")
        )
    }
}

@Composable
fun PluctAnalyticsCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("analytics_card"),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = color
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.testTag("analytics_value")
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("analytics_title")
            )
        }
    }
}

@Composable
fun PluctQualityDistributionChart(
    qualityDistribution: Map<TranscriptionQuality, Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quality_distribution_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quality Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("quality_chart_title")
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val total = qualityDistribution.values.sum()
            if (total > 0) {
                qualityDistribution.entries.forEach { (quality, count) ->
                    val percentage = (count.toFloat() / total * 100).toInt()
                    
                    PluctQualityBar(
                        quality = quality,
                        count = count,
                        percentage = percentage,
                        modifier = Modifier.testTag("quality_bar_${quality.name.lowercase()}")
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("no_quality_data")
                )
            }
        }
    }
}

@Composable
fun PluctQualityBar(
    quality: TranscriptionQuality,
    count: Int,
    percentage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quality_bar_${quality.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quality label
        Text(
            text = quality.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .width(80.dp)
                .testTag("quality_label_${quality.name.lowercase()}")
        )
        
        // Progress bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage / 100f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when (quality) {
                            TranscriptionQuality.EXCELLENT -> MaterialTheme.colorScheme.tertiary
                            TranscriptionQuality.GOOD -> MaterialTheme.colorScheme.primary
                            TranscriptionQuality.FAIR -> MaterialTheme.colorScheme.secondary
                            TranscriptionQuality.POOR -> MaterialTheme.colorScheme.error
                        }
                    )
                    .testTag("quality_bar_fill_${quality.name.lowercase()}")
            )
        }
        
        // Count and percentage
        Text(
            text = "$count ($percentage%)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("quality_stats_${quality.name.lowercase()}")
        )
    }
}

@Composable
fun PluctTierUsageChart(
    tierUsage: Map<ProcessingTier, Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tier_usage_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Tier Usage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("tier_chart_title")
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val total = tierUsage.values.sum()
            if (total > 0) {
                tierUsage.entries.forEach { (tier, count) ->
                    val percentage = (count.toFloat() / total * 100).toInt()
                    
                    PluctTierBar(
                        tier = tier,
                        count = count,
                        percentage = percentage,
                        modifier = Modifier.testTag("tier_bar_${tier.name.lowercase()}")
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("no_tier_data")
                )
            }
        }
    }
}

@Composable
fun PluctTierBar(
    tier: ProcessingTier,
    count: Int,
    percentage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tier_bar_${tier.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tier label
        Text(
            text = tier.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .width(100.dp)
                .testTag("tier_label_${tier.name.lowercase()}")
        )
        
        // Progress bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage / 100f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("tier_bar_fill_${tier.name.lowercase()}")
            )
        }
        
        // Count and percentage
        Text(
            text = "$count ($percentage%)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("tier_stats_${tier.name.lowercase()}")
        )
    }
}

@Composable
fun PluctRecentActivityList(
    recentActivity: List<TranscriptionActivity>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recent_activity_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("activity_title")
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (recentActivity.isNotEmpty()) {
                recentActivity.forEach { activity ->
                    PluctActivityItem(
                        activity = activity,
                        modifier = Modifier.testTag("activity_item_${activity.id}")
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text(
                    text = "No recent activity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("no_activity")
                )
            }
        }
    }
}

@Composable
fun PluctActivityItem(
    activity: TranscriptionActivity,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("activity_item_${activity.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = activity.icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = activity.color
        )
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = activity.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("activity_title_${activity.id}")
            )
            
            Text(
                text = activity.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("activity_description_${activity.id}")
            )
        }
        
        Text(
            text = activity.timestamp,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("activity_timestamp_${activity.id}")
        )
    }
}

// Data classes for analytics
data class TranscriptionAnalytics(
    val totalTranscriptions: Int,
    val successRate: Float,
    val qualityDistribution: Map<TranscriptionQuality, Int>,
    val tierUsage: Map<ProcessingTier, Int>,
    val recentActivity: List<TranscriptionActivity>
)

data class TranscriptionActivity(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)
