package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Pluct-UI-Screen-VideoDetail-02TopBar - Top bar component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single source of truth for top bar functionality
 */
@Composable
fun PluctVideoDetailTopBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = { Text("Video Details") },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.semantics {
                    contentDescription = "Back button"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    )
}
