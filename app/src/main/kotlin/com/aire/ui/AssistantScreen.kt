package com.aire.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aire.claude.AssistantResponse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: MemoryViewModel) {
    val ui by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }

    val recordAudioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.startListening(context)
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.refreshLocation()
        }
    }

    LaunchedEffect(Unit) {
        locationPermission.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(ui.chatHistory.size) {
        if (ui.chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(ui.chatHistory.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Aire", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { /* Navigate to Memory Vault */ }) {
                        Icon(Icons.Default.Dns, contentDescription = "Memories")
                    }
                }
            )
        },
        bottomBar = {
            // --- Input Bar ---
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.navigationBarsPadding().imePadding()) {
                    // Context Preview (Captured Image)
                    ui.capturedImage?.let { bitmap ->
                        Box(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp).size(80.dp)) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.clearCapturedImage() },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).offset(x = 8.dp, y = (-8).dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Transcription UI
                    if (ui.isListening) {
                        Text(
                            text = ui.partialTranscription.ifBlank { "Listening..." },
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { viewModel.navigateTo(AppScreen.LENS) }) {
                            Icon(Icons.Default.CameraAlt, "Lens", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            if (ui.isListening) viewModel.stopListening()
                            else recordAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                        }) {
                            Icon(
                                if (ui.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                "Voice",
                                tint = if (ui.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask Aire...") },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.extraLarge,
                            maxLines = 4,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        viewModel.sendMessage(inputText)
                                        inputText = ""
                                    },
                                    enabled = inputText.isNotBlank() || ui.capturedImage != null
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, "Send")
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        // --- Chat History ---
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            )
        ) {
            items(ui.chatHistory) { message ->
                ChatBubble(message, onActionClick = { action ->
                    viewModel.onActionClicked(action, message.response!!)
                })
            }
            if (ui.isThinking) {
                item { ThinkingIndicator() }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, onActionClick: (com.aire.claude.AssistantAction) -> Unit) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val containerColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (message.isUser) 
        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp) 
    else 
        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = shape
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                message.image?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        null,
                        modifier = Modifier.sizeIn(maxWidth = 200.dp, maxHeight = 200.dp).clip(MaterialTheme.shapes.small).padding(bottom = 8.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Text(message.text)
            }
        }

        // Action Chips for Assistant
        message.response?.suggestedActions?.let { actions ->
            if (actions.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actions.forEach { action ->
                        SuggestionChip(
                            onClick = { onActionClick(action) },
                            label = { Text(action.label) },
                            icon = { 
                                val icon = when(action.type) {
                                    "SAVE_MEMORY" -> Icons.Default.Bookmark
                                    "ADD_CALENDAR" -> Icons.Default.Event
                                    "ADD_CONTACT" -> Icons.Default.Person
                                    else -> Icons.Default.Bolt
                                }
                                Icon(icon, null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThinkingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text("Aire is thinking...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}
