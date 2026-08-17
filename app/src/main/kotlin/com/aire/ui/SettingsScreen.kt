package com.aire.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MemoryViewModel) {
    val ui by viewModel.uiState.collectAsState()
    var apiKeyVisible by remember { mutableStateOf(false) }
    var googleKeyVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- API Key Section ---
            SettingsSection(title = "AI Configuration") {
                var tempKey by remember { mutableStateOf("") }
                
                OutlinedTextField(
                    value = tempKey,
                    onValueChange = { tempKey = it },
                    label = { Text("Anthropic API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    placeholder = { Text("sk-ant-...") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                    if (ui.isAiAvailable) {
                        OutlinedButton(onClick = { viewModel.updateApiKey("") }) {
                            Text("Remove")
                        }
                    }
                    Button(
                        onClick = { viewModel.updateApiKey(tempKey); tempKey = "" },
                        enabled = tempKey.isNotBlank()
                    ) {
                        Text("Save Key")
                    }
                }
                if (ui.isAiAvailable) {
                    Text("Key is active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("No key saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }

            // --- Google Cloud Integration ---
            SettingsSection(title = "Google Cloud Integration") {
                var tempGoogleKey by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = tempGoogleKey,
                    onValueChange = { tempGoogleKey = it },
                    label = { Text("Google API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (googleKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { googleKeyVisible = !googleKeyVisible }) {
                            Icon(if (googleKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    placeholder = { Text("AIza...") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                    Button(
                        onClick = { viewModel.updateGoogleApiKey(tempGoogleKey); tempGoogleKey = "" },
                        enabled = tempGoogleKey.isNotBlank()
                    ) {
                        Text("Save Google Key")
                    }
                }
                Text("Required for advanced Maps & Places features.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }

            // --- Model Switcher ---
            SettingsSection(title = "AI Model") {
                val models = listOf(
                    "claude-3-5-haiku-latest" to "Haiku (Fastest)",
                    "claude-3-5-sonnet-latest" to "Sonnet (Smart)",
                    "claude-3-opus-latest" to "Opus (Powerful)"
                )
                models.forEach { (id, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = ui.aiModel == id,
                            onClick = { viewModel.updateModel(id) }
                        )
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            // --- Appearance ---
            SettingsSection(title = "Appearance") {
                val modes = listOf("Light", "Dark", "System")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    modes.forEach { mode ->
                        FilterChip(
                            selected = ui.appearance == mode,
                            onClick = { viewModel.updateAppearance(mode) },
                            label = { Text(mode) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
            Text(
                "Aire Assistant v0.1.0",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
