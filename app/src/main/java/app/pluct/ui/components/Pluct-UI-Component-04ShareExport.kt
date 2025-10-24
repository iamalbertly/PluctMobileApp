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
import app.pluct.data.entity.ProcessingTier
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Pluct-UI-Component-04ShareExport - Sharing and export functionality for transcriptions
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */

@Composable
fun PluctShareButton(
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onShare,
        modifier = modifier.testTag("share_button")
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share transcription",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun PluctExportButton(
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onExport,
        modifier = modifier.testTag("export_button")
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Export transcription",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctShareOptionsSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onShareOptionSelected: (ShareOption) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = modifier.testTag("share_options_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Share Transcription",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("share_sheet_title")
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ShareOption.values().forEach { option ->
                    PluctShareOptionCard(
                        option = option,
                        onClick = { onShareOptionSelected(option) },
                        modifier = Modifier.testTag("share_option_${option.name.lowercase()}")
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctShareOptionCard(
    option: ShareOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("share_option_card_${option.name.lowercase()}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("share_option_title_${option.name.lowercase()}")
                )
                
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("share_option_description_${option.name.lowercase()}")
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctExportOptionsSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onExportOptionSelected: (ExportOption) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = modifier.testTag("export_options_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Export Transcription",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("export_sheet_title")
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ExportOption.values().forEach { option ->
                    PluctExportOptionCard(
                        option = option,
                        onClick = { onExportOptionSelected(option) },
                        modifier = Modifier.testTag("export_option_${option.name.lowercase()}")
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctExportOptionCard(
    option: ExportOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("export_option_card_${option.name.lowercase()}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("export_option_title_${option.name.lowercase()}")
                )
                
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("export_option_description_${option.name.lowercase()}")
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PluctTranscriptionPreview(
    transcription: TranscriptionContent,
    onShare: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transcription_preview_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = transcription.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("preview_title")
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "by ${transcription.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("preview_author")
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PluctShareButton(
                        onShare = onShare,
                        modifier = Modifier.testTag("preview_share_button")
                    )
                    
                    PluctExportButton(
                        onExport = onExport,
                        modifier = Modifier.testTag("preview_export_button")
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = transcription.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("preview_timestamp")
                )
                
                Text(
                    text = transcription.tier.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("preview_tier")
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Transcript content
            Text(
                text = transcription.content,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4,
                modifier = Modifier.testTag("preview_content")
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quality indicator
            PluctQualityBadge(
                quality = transcription.quality,
                modifier = Modifier.testTag("preview_quality_badge")
            )
        }
    }
}

@Composable
fun PluctShareConfirmationDialog(
    isVisible: Boolean,
    shareOption: ShareOption?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible && shareOption != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Share Transcription") },
            text = { Text("Share this transcription via ${shareOption.title}?") },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.testTag("share_confirm_button")
                ) {
                    Text("Share")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("share_cancel_button")
                ) {
                    Text("Cancel")
                }
            },
            modifier = modifier.testTag("share_confirmation_dialog")
        )
    }
}

@Composable
fun PluctExportConfirmationDialog(
    isVisible: Boolean,
    exportOption: ExportOption?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible && exportOption != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Export Transcription") },
            text = { Text("Export this transcription as ${exportOption.title}?") },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.testTag("export_confirm_button")
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("export_cancel_button")
                ) {
                    Text("Cancel")
                }
            },
            modifier = modifier.testTag("export_confirmation_dialog")
        )
    }
}

// Data classes for sharing and export
data class TranscriptionContent(
    val id: String,
    val title: String,
    val author: String,
    val content: String,
    val timestamp: LocalDateTime,
    val tier: ProcessingTier,
    val quality: TranscriptionQuality
)

enum class ShareOption(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    TEXT("Copy as Text", "Copy transcription to clipboard", Icons.Default.ContentCopy),
    LINK("Share Link", "Share a link to this transcription", Icons.Default.Link),
    SOCIAL("Social Media", "Share on social media platforms", Icons.Default.Share),
    EMAIL("Email", "Send via email", Icons.Default.Email),
    MESSAGES("Messages", "Send via text message", Icons.Default.Message)
}

enum class ExportOption(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    PDF("PDF Document", "Export as PDF file", Icons.Default.PictureAsPdf),
    TEXT("Text File", "Export as plain text file", Icons.Default.Description),
    WORD("Word Document", "Export as Word document", Icons.Default.Description),
    MARKDOWN("Markdown", "Export as Markdown file", Icons.Default.Code)
}
