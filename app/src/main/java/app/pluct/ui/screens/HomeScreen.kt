package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.pluct.ui.components.EmptyStateView
import app.pluct.ui.screens.components.*
import app.pluct.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    
    val focusRequester = remember { FocusRequester() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pluct - TikTok Transcripts") },
                actions = {
                    IconButton(onClick = { navController.navigate(app.pluct.ui.navigation.Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Manual URL Input Section
            ManualUrlInput(
                navController = navController,
                focusRequester = focusRequester
            )
            
            // Recent Transcripts Section
            RecentTranscriptsSection(
                videos = videos,
                navController = navController,
                viewModel = viewModel
            )
            
            // Content
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (videos.isEmpty()) {
                EmptyStateView()
            }
        }
    }
    
    // Auto-focus the URL input field
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}