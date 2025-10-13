package app.pluct.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.VideoItem
import app.pluct.ui.utils.DateFormatter
import app.pluct.utils.PluctUrlUtils

/**
 * Pluct-Video-Card-Core - Core video card component (under 300 lines)
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Composable
fun PluctVideoCardCore(
    video: VideoItem,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Animation for card entrance
    var isVisible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and menu
            PluctVideoCardHeader(
                video = video,
                showMenu = showMenu,
                onMenuToggle = { showMenu = !showMenu }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Video thumbnail and status
            PluctVideoCardThumbnail(video = video)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Video metadata
            PluctVideoCardMetadata(video = video)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Processing status
            PluctVideoCardStatus(video = video)
        }
        
        // Menu dropdown
        if (showMenu) {
            PluctVideoCardMenu(
                video = video,
                onDismiss = { showMenu = false },
                onDelete = { showDeleteDialog = true }
            )
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        PluctVideoCardDeleteDialog(
            video = video,
            onDismiss = { showDeleteDialog = false },
            onConfirm = { 
                showDeleteDialog = false
                // Handle delete
            }
        )
    }
}
