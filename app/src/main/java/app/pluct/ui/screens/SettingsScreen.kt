package app.pluct.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.pluct.R
import app.pluct.ui.components.ApiKeyBanner
import app.pluct.ui.navigation.Screen
import app.pluct.ui.utils.ProviderSettings
import app.pluct.ui.utils.TranscriptProvider

/**
 * Settings screen with stub for future API key entry.
 * 
 * Why stub: Provides placeholder for future functionality while maintaining
 * app structure and navigation consistency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedProvider by remember { mutableStateOf(ProviderSettings.getSelectedProvider(context)) }
    var tokauditApiKey by remember { mutableStateOf(ProviderSettings.getApiKey(context, TranscriptProvider.TOKAUDIT) ?: "") }
    var getTranscribeApiKey by remember { mutableStateOf(ProviderSettings.getApiKey(context, TranscriptProvider.GETTRANSCRIBE) ?: "") }
    var tokauditEnabled by remember { mutableStateOf(ProviderSettings.isProviderEnabled(context, TranscriptProvider.TOKAUDIT)) }
    var getTranscribeEnabled by remember { mutableStateOf(ProviderSettings.isProviderEnabled(context, TranscriptProvider.GETTRANSCRIBE)) }

    LaunchedEffect(selectedProvider, tokauditApiKey, getTranscribeApiKey, tokauditEnabled, getTranscribeEnabled) {
        ProviderSettings.setSelectedProvider(context, selectedProvider)
        ProviderSettings.setApiKey(context, TranscriptProvider.TOKAUDIT, tokauditApiKey.ifBlank { null })
        ProviderSettings.setApiKey(context, TranscriptProvider.GETTRANSCRIBE, getTranscribeApiKey.ifBlank { null })
        ProviderSettings.setProviderEnabled(context, TranscriptProvider.TOKAUDIT, tokauditEnabled)
        ProviderSettings.setProviderEnabled(context, TranscriptProvider.GETTRANSCRIBE, getTranscribeEnabled)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = false,
                    onClick = { navController.navigate(Screen.Home.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = true,
                    onClick = { /* Already on settings */ }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ApiKeyBanner()
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    text = "Transcript Providers",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.padding(4.dp))
                
                // Provider Enable/Disable Toggles
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Enable/Disable Providers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TokAudit")
                            Switch(
                                checked = tokauditEnabled,
                                onCheckedChange = { tokauditEnabled = it }
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("GetTranscribe")
                            Switch(
                                checked = getTranscribeEnabled,
                                onCheckedChange = { getTranscribeEnabled = it }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.padding(8.dp))
                
                // Provider Selection (only show enabled providers)
                Text(
                    text = "Default Provider",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (tokauditEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedProvider == TranscriptProvider.TOKAUDIT,
                                onClick = { selectedProvider = TranscriptProvider.TOKAUDIT }
                            )
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("TokAudit")
                        }
                    }
                    if (getTranscribeEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedProvider == TranscriptProvider.GETTRANSCRIBE,
                                onClick = { selectedProvider = TranscriptProvider.GETTRANSCRIBE }
                            )
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("GetTranscribe")
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(12.dp))
                Text(
                    text = "API Keys (optional)",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.padding(4.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = tokauditApiKey,
                    onValueChange = { tokauditApiKey = it },
                    label = { Text("TokAudit API Key") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.padding(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = getTranscribeApiKey,
                    onValueChange = { getTranscribeApiKey = it },
                    label = { Text("GetTranscribe API Key") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
