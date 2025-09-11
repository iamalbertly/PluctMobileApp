package app.pluct.ui.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.pluct.viewmodel.HomeViewModel

/**
 * Recent transcripts section displaying saved videos
 */
@Composable
fun RecentTranscriptsSection(
    videos: List<app.pluct.data.entity.VideoItem>,
    navController: NavController,
    viewModel: HomeViewModel
) {
    if (videos.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transcripts",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        text = "${videos.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(videos.take(10)) { video ->
                        RecentTranscriptItem(
                            video = video,
                            navController = navController,
                            viewModel = viewModel
                        )
                    }
                }
                
                if (videos.size > 10) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { /* TODO: Navigate to full list */ },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("View All (${videos.size})")
                    }
                }
            }
        }
    }
}
