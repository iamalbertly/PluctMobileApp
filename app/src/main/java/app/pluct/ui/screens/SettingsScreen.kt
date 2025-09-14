package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.pluct.R
import app.pluct.ui.components.ApiKeyBanner
import app.pluct.ui.components.ModernBottomNavigation
import app.pluct.ui.navigation.Screen
import app.pluct.ui.utils.ProviderSettings
import app.pluct.ui.utils.TranscriptProvider

/**
 * Settings screen for configuring transcription providers and API keys
 * 
 * Features:
 * - Provider enable/disable toggles
 * - Default provider selection
 * - API key management
 * - Clean, modern UI with emoji icons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedProvider by remember { mutableStateOf(ProviderSettings.getSelectedProvider(context)) }
    var tokauditApiKey by remember { mutableStateOf(ProviderSettings.getApiKey(context, TranscriptProvider.TOKAUDIT) ?: "") }
    var getTranscribeApiKey by remember { mutableStateOf(ProviderSettings.getApiKey(context, TranscriptProvider.GETTRANSCRIBE) ?: "") }
    var openaiApiKey by remember { mutableStateOf(ProviderSettings.getApiKey(context, TranscriptProvider.OPENAI) ?: "") }
    var tokauditEnabled by remember { mutableStateOf(ProviderSettings.isProviderEnabled(context, TranscriptProvider.TOKAUDIT)) }
    var getTranscribeEnabled by remember { mutableStateOf(ProviderSettings.isProviderEnabled(context, TranscriptProvider.GETTRANSCRIBE)) }
    var openaiEnabled by remember { mutableStateOf(ProviderSettings.isProviderEnabled(context, TranscriptProvider.OPENAI)) }
    var showOpenAiApiKeyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedProvider, tokauditApiKey, getTranscribeApiKey, openaiApiKey, tokauditEnabled, getTranscribeEnabled, openaiEnabled) {
        ProviderSettings.setSelectedProvider(context, selectedProvider)
        ProviderSettings.setApiKey(context, TranscriptProvider.TOKAUDIT, tokauditApiKey.ifBlank { null })
        ProviderSettings.setApiKey(context, TranscriptProvider.GETTRANSCRIBE, getTranscribeApiKey.ifBlank { null })
        ProviderSettings.setApiKey(context, TranscriptProvider.OPENAI, openaiApiKey.ifBlank { null })
        ProviderSettings.setProviderEnabled(context, TranscriptProvider.TOKAUDIT, tokauditEnabled)
        ProviderSettings.setProviderEnabled(context, TranscriptProvider.GETTRANSCRIBE, getTranscribeEnabled)
        ProviderSettings.setProviderEnabled(context, TranscriptProvider.OPENAI, openaiEnabled)
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
            ModernBottomNavigation(navController = navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Primary Section: Transcription Providers
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🎯 Transcription Providers",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose which services to use for transcript extraction",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Provider Toggles
                    ProviderToggleItem(
                        name = "TokAudit.io",
                        description = "Fast TikTok transcript extraction",
                        enabled = tokauditEnabled,
                        onToggle = { tokauditEnabled = it }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ProviderToggleItem(
                        name = "GetTranscribe.ai",
                        description = "Multi-platform transcript service",
                        enabled = getTranscribeEnabled,
                        onToggle = { getTranscribeEnabled = it }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ProviderToggleItem(
                        name = "OpenAI",
                        description = "AI-powered transcript generation",
                        enabled = openaiEnabled,
                        onToggle = { enabled ->
                            if (enabled && openaiApiKey.isBlank()) {
                                showOpenAiApiKeyDialog = true
                            } else {
                                openaiEnabled = enabled
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Default Provider Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "⚙️ Default Provider",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select your preferred transcription service",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (tokauditEnabled) {
                        ProviderRadioItem(
                            name = "TokAudit.io",
                            selected = selectedProvider == TranscriptProvider.TOKAUDIT,
                            onClick = { selectedProvider = TranscriptProvider.TOKAUDIT }
                        )
                    }
                    
                    if (getTranscribeEnabled) {
                        ProviderRadioItem(
                            name = "GetTranscribe.ai",
                            selected = selectedProvider == TranscriptProvider.GETTRANSCRIBE,
                            onClick = { selectedProvider = TranscriptProvider.GETTRANSCRIBE }
                        )
                    }
                    
                    if (openaiEnabled) {
                        ProviderRadioItem(
                            name = "OpenAI",
                            selected = selectedProvider == TranscriptProvider.OPENAI,
                            onClick = { selectedProvider = TranscriptProvider.OPENAI }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // API Keys Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🔑 API Keys",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Optional: Add API keys for enhanced features",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (tokauditEnabled) {
                        OutlinedTextField(
                            value = tokauditApiKey,
                            onValueChange = { tokauditApiKey = it },
                            label = { Text("TokAudit API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = tokauditEnabled
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    if (getTranscribeEnabled) {
                        OutlinedTextField(
                            value = getTranscribeApiKey,
                            onValueChange = { getTranscribeApiKey = it },
                            label = { Text("GetTranscribe API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = getTranscribeEnabled
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    if (openaiEnabled) {
                        OutlinedTextField(
                            value = openaiApiKey,
                            onValueChange = { openaiApiKey = it },
                            label = { Text("OpenAI API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = openaiEnabled
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info Banner
            ApiKeyBanner()
        }
        
        // OpenAI API Key Dialog
        if (showOpenAiApiKeyDialog) {
            AlertDialog(
                onDismissRequest = { showOpenAiApiKeyDialog = false },
                title = { Text("OpenAI API Key Required") },
                text = { 
                    Text("To use OpenAI transcription, please enter your API key. You can get one from https://platform.openai.com/api-keys")
                },
                confirmButton = {
                    TextButton(
                        onClick = { 
                            showOpenAiApiKeyDialog = false
                            openaiEnabled = true
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showOpenAiApiKeyDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ProviderToggleItem(
    name: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun ProviderRadioItem(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}