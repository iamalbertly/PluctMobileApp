package app.pluct.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.pluct.data.entity.ProcessingStatus

/**
 * Status pill component for showing processing status
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Composable
fun PluctStatusPill(
    status: ProcessingStatus,
    modifier: Modifier = Modifier
) {
    val (label, color, contentDesc) = when (status) {
        ProcessingStatus.PENDING -> Triple("Pending", Color.Gray, "status_pending")
        ProcessingStatus.TRANSCRIBING -> Triple("Transcribing", Color(0xFF7AA3FF), "status_processing")
        ProcessingStatus.ANALYZING -> Triple("Analyzing", Color(0xFF7AA3FF), "status_processing")
        ProcessingStatus.COMPLETED -> Triple("Completed", Color(0xFF34C759), "status_completed")
        ProcessingStatus.FAILED -> Triple("Failed", Color(0xFFFF3B30), "status_error")
    }
    
    Surface(
        modifier = modifier
            .semantics { contentDescription = contentDesc }
            .padding(4.dp),
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
