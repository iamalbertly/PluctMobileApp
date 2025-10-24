package app.pluct.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Pluct-UI-Component-03SearchFilter - Search and filtering capabilities for transcriptions
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    placeholder: String = "Search transcriptions...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag("search_bar"),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.testTag("search_icon")
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.testTag("clear_search")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun PluctFilterChips(
    selectedFilters: Set<TranscriptionFilter>,
    onFilterChange: (Set<TranscriptionFilter>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.testTag("filter_chips_container")
    ) {
        Text(
            text = "Filter by:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("filter_label")
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status filters
            TranscriptionFilter.STATUS_COMPLETED.takeIf { 
                selectedFilters.contains(TranscriptionFilter.STATUS_COMPLETED) 
            }?.let { filter ->
                PluctFilterChip(
                    filter = filter,
                    isSelected = true,
                    onClick = { onFilterChange(selectedFilters - filter) },
                    modifier = Modifier.testTag("filter_chip_completed")
                )
            }
            
            TranscriptionFilter.STATUS_PROCESSING.takeIf { 
                selectedFilters.contains(TranscriptionFilter.STATUS_PROCESSING) 
            }?.let { filter ->
                PluctFilterChip(
                    filter = filter,
                    isSelected = true,
                    onClick = { onFilterChange(selectedFilters - filter) },
                    modifier = Modifier.testTag("filter_chip_processing")
                )
            }
            
            TranscriptionFilter.STATUS_FAILED.takeIf { 
                selectedFilters.contains(TranscriptionFilter.STATUS_FAILED) 
            }?.let { filter ->
                PluctFilterChip(
                    filter = filter,
                    isSelected = true,
                    onClick = { onFilterChange(selectedFilters - filter) },
                    modifier = Modifier.testTag("filter_chip_failed")
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tier filters
            TranscriptionFilter.TIER_STANDARD.takeIf { 
                selectedFilters.contains(TranscriptionFilter.TIER_STANDARD) 
            }?.let { filter ->
                PluctFilterChip(
                    filter = filter,
                    isSelected = true,
                    onClick = { onFilterChange(selectedFilters - filter) },
                    modifier = Modifier.testTag("filter_chip_standard")
                )
            }
            
            TranscriptionFilter.TIER_PREMIUM.takeIf { 
                selectedFilters.contains(TranscriptionFilter.TIER_PREMIUM) 
            }?.let { filter ->
                PluctFilterChip(
                    filter = filter,
                    isSelected = true,
                    onClick = { onFilterChange(selectedFilters - filter) },
                    modifier = Modifier.testTag("filter_chip_premium")
                )
            }
            
            TranscriptionFilter.TIER_AI_ANALYSIS.takeIf { 
                selectedFilters.contains(TranscriptionFilter.TIER_AI_ANALYSIS) 
            }?.let { filter ->
                PluctFilterChip(
                    filter = filter,
                    isSelected = true,
                    onClick = { onFilterChange(selectedFilters - filter) },
                    modifier = Modifier.testTag("filter_chip_ai_analysis")
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date filters
            TranscriptionFilter.DATE_TODAY.takeIf { 
                selectedFilters.contains(TranscriptionFilter.DATE_TODAY) 
            }?.let { filter ->
                PluctFilterChip(
                    filter = filter,
                    isSelected = true,
                    onClick = { onFilterChange(selectedFilters - filter) },
                    modifier = Modifier.testTag("filter_chip_today")
                )
            }
            
            TranscriptionFilter.DATE_THIS_WEEK.takeIf { 
                selectedFilters.contains(TranscriptionFilter.DATE_THIS_WEEK) 
            }?.let { filter ->
                PluctFilterChip(
                    filter = filter,
                    isSelected = true,
                    onClick = { onFilterChange(selectedFilters - filter) },
                    modifier = Modifier.testTag("filter_chip_this_week")
                )
            }
            
            TranscriptionFilter.DATE_THIS_MONTH.takeIf { 
                selectedFilters.contains(TranscriptionFilter.DATE_THIS_MONTH) 
            }?.let { filter ->
                PluctFilterChip(
                    filter = filter,
                    isSelected = true,
                    onClick = { onFilterChange(selectedFilters - filter) },
                    modifier = Modifier.testTag("filter_chip_this_month")
                )
            }
        }
    }
}

@Composable
fun PluctFilterChip(
    filter: TranscriptionFilter,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .testTag("filter_chip_${filter.name.lowercase()}")
            .clickable { onClick() },
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = filter.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Text(
                text = filter.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun PluctSortOptions(
    selectedSort: TranscriptionSort,
    onSortChange: (TranscriptionSort) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.testTag("sort_options_container")
    ) {
        Text(
            text = "Sort by:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("sort_label")
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TranscriptionSort.values().forEach { sort ->
                PluctSortChip(
                    sort = sort,
                    isSelected = selectedSort == sort,
                    onClick = { onSortChange(sort) },
                    modifier = Modifier.testTag("sort_chip_${sort.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
fun PluctSortChip(
    sort: TranscriptionSort,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .testTag("sort_chip_${sort.name.lowercase()}")
            .clickable { onClick() },
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = sort.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Text(
                text = sort.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun PluctSearchResults(
    results: List<TranscriptionSearchResult>,
    onResultClick: (TranscriptionSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.testTag("search_results_container")
    ) {
        Text(
            text = "Search Results (${results.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("search_results_count")
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        results.forEach { result ->
            PluctSearchResultCard(
                result = result,
                onClick = { onResultClick(result) },
                modifier = Modifier.testTag("search_result_${result.id}")
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PluctSearchResultCard(
    result: TranscriptionSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("search_result_card_${result.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title and metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("result_title_${result.id}")
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "by ${result.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("result_author_${result.id}")
                    )
                }
                
                Text(
                    text = result.timestamp.format(DateTimeFormatter.ofPattern("MMM dd")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("result_timestamp_${result.id}")
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Transcript preview
            Text(
                text = result.transcriptPreview,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("result_preview_${result.id}")
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Quality and tier indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PluctQualityBadge(
                    quality = result.quality,
                    modifier = Modifier.testTag("result_quality_${result.id}")
                )
                
                Text(
                    text = result.tier.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("result_tier_${result.id}")
                )
            }
        }
    }
}

// Data classes for search and filtering
data class TranscriptionSearchResult(
    val id: String,
    val title: String,
    val author: String,
    val transcriptPreview: String,
    val timestamp: LocalDateTime,
    val quality: TranscriptionQuality,
    val tier: ProcessingTier,
    val status: ProcessingStatus
)

enum class TranscriptionFilter(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    STATUS_COMPLETED("Completed", Icons.Default.CheckCircle),
    STATUS_PROCESSING("Processing", Icons.Default.Sync),
    STATUS_FAILED("Failed", Icons.Default.Error),
    TIER_STANDARD("Standard", Icons.Default.Speed),
    TIER_PREMIUM("Premium", Icons.Default.Star),
    TIER_AI_ANALYSIS("AI Analysis", Icons.Default.Psychology),
    DATE_TODAY("Today", Icons.Default.Today),
    DATE_THIS_WEEK("This Week", Icons.Default.DateRange),
    DATE_THIS_MONTH("This Month", Icons.Default.CalendarMonth)
}

enum class TranscriptionSort(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    DATE_NEWEST("Newest", Icons.Default.ArrowUpward),
    DATE_OLDEST("Oldest", Icons.Default.ArrowDownward),
    TITLE_A_Z("Title A-Z", Icons.Default.Sort),
    TITLE_Z_A("Title Z-A", Icons.Default.Sort),
    QUALITY_HIGHEST("Quality", Icons.Default.Star),
    AUTHOR("Author", Icons.Default.Person)
}
