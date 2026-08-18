package com.aire.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun VoiceModeScreen(viewModel: MemoryViewModel) {
    val ui by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val recordAudioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.startListening(context)
    }

    LaunchedEffect(Unit) {
        recordAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.stopListening()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)) // Deep dark background
    ) {
        // --- Aura Animation ---
        val infiniteTransition = rememberInfiniteTransition(label = "aura")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (ui.isListening || ui.isSpeaking) 1.5f else 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = if (ui.isListening || ui.isSpeaking) 0.6f else 0.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(200.dp)
                .scale(scale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // --- Content ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (ui.isListening) "Aire is listening..." else if (ui.isSpeaking) "Aire is speaking..." else "Thinking...",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(24.dp))

            if (ui.isListening && ui.partialTranscription.isNotBlank()) {
                Text(
                    text = ui.partialTranscription,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Close Button
        IconButton(
            onClick = { 
                viewModel.stopListening()
                viewModel.navigateTo(AppScreen.HOME) 
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}
