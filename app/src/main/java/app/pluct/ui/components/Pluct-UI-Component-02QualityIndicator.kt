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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pluct-UI-Component-02QualityIndicator - Transcription quality and confidence indicators
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */

@Composable
fun PluctQualityIndicator(
    confidence: Float,
    quality: TranscriptionQuality,
    language: String = "en",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quality_indicator_card"),
        colors = CardDefaults.cardColors(
            containerColor = when (quality) {
                TranscriptionQuality.EXCELLENT -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)
                TranscriptionQuality.GOOD -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                TranscriptionQuality.FAIR -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
                TranscriptionQuality.POOR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Quality header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (quality) {
                            TranscriptionQuality.EXCELLENT -> Icons.Default.Star
                            TranscriptionQuality.GOOD -> Icons.Default.ThumbUp
                            TranscriptionQuality.FAIR -> Icons.Default.ThumbUp
                            TranscriptionQuality.POOR -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = when (quality) {
                            TranscriptionQuality.EXCELLENT -> MaterialTheme.colorScheme.tertiary
                            TranscriptionQuality.GOOD -> MaterialTheme.colorScheme.primary
                            TranscriptionQuality.FAIR -> MaterialTheme.colorScheme.secondary
                            TranscriptionQuality.POOR -> MaterialTheme.colorScheme.error
                        }
                    )
                    
                    Text(
                        text = "Transcription Quality",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("quality_title")
                    )
                }
                
                Text(
                    text = "${(confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (quality) {
                        TranscriptionQuality.EXCELLENT -> MaterialTheme.colorScheme.tertiary
                        TranscriptionQuality.GOOD -> MaterialTheme.colorScheme.primary
                        TranscriptionQuality.FAIR -> MaterialTheme.colorScheme.secondary
                        TranscriptionQuality.POOR -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.testTag("confidence_percentage")
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Quality bar
            PluctQualityBar(
                confidence = confidence,
                quality = quality,
                modifier = Modifier.testTag("quality_bar")
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Quality description
            Text(
                text = when (quality) {
                    TranscriptionQuality.EXCELLENT -> "Excellent transcription quality with high accuracy"
                    TranscriptionQuality.GOOD -> "Good transcription quality with minor errors"
                    TranscriptionQuality.FAIR -> "Fair transcription quality with some errors"
                    TranscriptionQuality.POOR -> "Poor transcription quality with many errors"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("quality_description")
            )
            
            // Language indicator
            if (language.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Language: ${language.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("language_indicator")
                    )
                }
            }
        }
    }
}

@Composable
fun PluctQualityBar(
    confidence: Float,
    quality: TranscriptionQuality,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(confidence)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when (quality) {
                        TranscriptionQuality.EXCELLENT -> MaterialTheme.colorScheme.tertiary
                        TranscriptionQuality.GOOD -> MaterialTheme.colorScheme.primary
                        TranscriptionQuality.FAIR -> MaterialTheme.colorScheme.secondary
                        TranscriptionQuality.POOR -> MaterialTheme.colorScheme.error
                    }
                )
                .testTag("quality_bar_fill")
        )
    }
}

@Composable
fun PluctConfidenceMeter(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.testTag("confidence_meter"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Confidence circle
        Box(
            modifier = Modifier
                .size(60.dp)
                .testTag("confidence_circle"),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = confidence,
                modifier = Modifier.size(60.dp),
                color = when {
                    confidence >= 0.9f -> MaterialTheme.colorScheme.tertiary
                    confidence >= 0.7f -> MaterialTheme.colorScheme.primary
                    confidence >= 0.5f -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                },
                strokeWidth = 4.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Text(
                text = "${(confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("confidence_text")
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Confidence",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("confidence_label")
        )
    }
}

@Composable
fun PluctQualityBadge(
    quality: TranscriptionQuality,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("quality_badge"),
        color = when (quality) {
            TranscriptionQuality.EXCELLENT -> MaterialTheme.colorScheme.tertiaryContainer
            TranscriptionQuality.GOOD -> MaterialTheme.colorScheme.primaryContainer
            TranscriptionQuality.FAIR -> MaterialTheme.colorScheme.secondaryContainer
            TranscriptionQuality.POOR -> MaterialTheme.colorScheme.errorContainer
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = when (quality) {
                    TranscriptionQuality.EXCELLENT -> Icons.Default.Star
                    TranscriptionQuality.GOOD -> Icons.Default.ThumbUp
                    TranscriptionQuality.FAIR -> Icons.Default.ThumbUp
                    TranscriptionQuality.POOR -> Icons.Default.Warning
                },
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = when (quality) {
                    TranscriptionQuality.EXCELLENT -> MaterialTheme.colorScheme.tertiary
                    TranscriptionQuality.GOOD -> MaterialTheme.colorScheme.primary
                    TranscriptionQuality.FAIR -> MaterialTheme.colorScheme.secondary
                    TranscriptionQuality.POOR -> MaterialTheme.colorScheme.error
                }
            )
            
            Text(
                text = when (quality) {
                    TranscriptionQuality.EXCELLENT -> "Excellent"
                    TranscriptionQuality.GOOD -> "Good"
                    TranscriptionQuality.FAIR -> "Fair"
                    TranscriptionQuality.POOR -> "Poor"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = when (quality) {
                    TranscriptionQuality.EXCELLENT -> MaterialTheme.colorScheme.tertiary
                    TranscriptionQuality.GOOD -> MaterialTheme.colorScheme.primary
                    TranscriptionQuality.FAIR -> MaterialTheme.colorScheme.secondary
                    TranscriptionQuality.POOR -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

enum class TranscriptionQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR
}

fun calculateTranscriptionQuality(confidence: Float): TranscriptionQuality {
    return when {
        confidence >= 0.9f -> TranscriptionQuality.EXCELLENT
        confidence >= 0.7f -> TranscriptionQuality.GOOD
        confidence >= 0.5f -> TranscriptionQuality.FAIR
        else -> TranscriptionQuality.POOR
    }
}
