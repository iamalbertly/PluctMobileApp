package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("settings_title")
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.testTag("back_button")
                    )
                }
            },
            modifier = Modifier.testTag("settings_top_bar")
        )
        
        // Settings Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Provider Configuration Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("provider_configuration_section")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Provider Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // HuggingFace Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("HuggingFace")
                        Switch(
                            checked = true,
                            onCheckedChange = { /* TODO: Implement */ }
                        )
                    }
                    
                    // TokAudit Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TokAudit")
                        Switch(
                            checked = true,
                            onCheckedChange = { /* TODO: Implement */ }
                        )
                    }
                    
                    // GetTranscribe Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GetTranscribe")
                        Switch(
                            checked = false,
                            onCheckedChange = { /* TODO: Implement */ }
                        )
                    }
                    
                    // OpenAI Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OpenAI")
                        Switch(
                            checked = false,
                            onCheckedChange = { /* TODO: Implement */ }
                        )
                    }
                }
            }
            
            // API Key Management Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_management_section")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "API Key Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // OpenAI API Key
                    OutlinedTextField(
                        value = "",
                        onValueChange = { /* TODO: Implement */ },
                        label = { Text("OpenAI API Key") },
                        placeholder = { Text("Enter your OpenAI API key") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("openai_api_key_input"),
                        singleLine = true
                    )
                }
            }
            
            // App Information Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_information_section")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "App Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Version: 1.0.0")
                    Text("Build: 2025.10.22")
                    Text("Business Engine: Connected")
                }
            }
        }
    }
}
