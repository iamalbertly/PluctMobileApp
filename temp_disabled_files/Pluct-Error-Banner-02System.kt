package app.pluct.ui.error

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pluct.core.error.ErrorEnvelope
import kotlinx.coroutines.flow.collectAsState

/**
 * Pluct-Error-Banner-02System - Comprehensive error banner system
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Implements modern error display with aggregation, details, and user interaction
 */
@Composable
fun PluctErrorBannerSystem(
    errorCenter: ErrorCenter,
    modifier: Modifier = Modifier
) {
    val errors by errorCenter.errors.collectAsState()
    
    if (errors.isNotEmpty()) {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .testTag("error_banner_system"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(errors) { error ->
                PluctErrorBannerItem(
                    error = error,
                    onDismiss = { errorCenter.dismissError(error.id) },
                    onDetails = { /* TODO: Show error details modal */ }
                )
            }
        }
    }
}

@Composable
fun PluctErrorBannerItem(
    error: ErrorEnvelope,
    onDismiss: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("error_banner")
            .semantics { 
                contentDescription = "error_code:${error.code}" 
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = error.code,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Row {
                    IconButton(
                        onClick = { showDetails = !showDetails },
                        modifier = Modifier.testTag("error_details_button")
                    ) {
                        Icon(
                            imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle details"
                        )
                    }
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("error_dismiss_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss error"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            if (showDetails) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Error details
                error.details?.let { details ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Error Details:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            details.forEach { (key, value) ->
                                Text(
                                    text = "$key: $value",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PluctErrorBannerHost(
    center: ErrorCenter,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        PluctErrorBannerSystem(
            errorCenter = center,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
