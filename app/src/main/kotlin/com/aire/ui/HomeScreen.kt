package com.aire.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aire.domain.MemoryRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MemoryViewModel) {
    val ui by viewModel.uiState.collectAsState()
    val records by viewModel.records.collectAsState()
    val context = LocalContext.current
    
    var inputText by remember { mutableStateOf("") }

    val recordAudioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.startListening(context)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(CircleShape).clickable { viewModel.navigateTo(AppScreen.VOICE_MODE) }.padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Aire", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.VAULT) }) {
                        Icon(Icons.Default.Dns, contentDescription = "Memories")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- Welcome Section ---
            Text(
                "Good afternoon.",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(32.dp))

            // --- Centered "Ask Aire" Bar ---
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask Aire anything...") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                maxLines = 3,
                trailingIcon = {
                    Row(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(onClick = {
                            recordAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                        }) {
                            Icon(Icons.Default.Mic, "Voice", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.navigateTo(AppScreen.LENS) }) {
                            Icon(Icons.Default.CameraAlt, "Lens", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            },
                            enabled = inputText.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                    }
                }
            )

            Spacer(Modifier.height(48.dp))

            // --- Recent Memories Section ---
            if (records.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Recent History",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    records.take(3).forEach { record ->
                        RecentItem(record) {
                            // In the future, tapping could open the vault detail
                            viewModel.navigateTo(AppScreen.VAULT)
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            } else {
                Text(
                    "Your Memory Vault is ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RecentItem(record: MemoryRecord, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = record.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
        }
    }
}
