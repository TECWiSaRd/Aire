package com.aire.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

@Composable
fun ChatPortal(
    viewModel: MemoryViewModel,
    content: @Composable () -> Unit
) {
    val ui by viewModel.uiState.collectAsState()
    val currentPortalExpansion by rememberUpdatedState(ui.portalExpansion)
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    val expansion by animateFloatAsState(
        targetValue = ui.portalExpansion,
        label = "expansion"
    )

    val baseSize = 240.dp
    // Scale starts small (circle) and grows to fill screen
    val screenDiagonal = sqrt((screenWidth.value * screenWidth.value) + (screenHeight.value * screenHeight.value))
    val targetScale = (screenDiagonal / baseSize.value) * 1.2f
    val currentScale = 1f + (expansion * (targetScale - 1f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f * (1f - expansion)))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDrag = { change, _ ->
                        change.consume()
                        
                        val currentPos = change.position
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val dist = sqrt((currentPos.x - centerX) * (currentPos.x - centerX) + (currentPos.y - centerY) * (currentPos.y - centerY))
                        
                        // Dragging outward from center drives expansion
                        val normalizedDist = ((dist - 120f) / 500f).coerceIn(0f, 1f)
                        if (normalizedDist > currentPortalExpansion) {
                            viewModel.setPortalExpansion(normalizedDist)
                        }
                    },
                    onDragEnd = {
                        if (currentPortalExpansion > 0.3f) {
                            viewModel.setPortalExpansion(1f)
                        } else {
                            viewModel.setPortalExpansion(0f)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                var zoomAccumulator = 1f
                detectTransformGestures { _, _, zoom, _ ->
                    zoomAccumulator *= zoom
                    if (zoomAccumulator < 0.7f) {
                        viewModel.closePortal()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // The Portal Bubble
        Surface(
            modifier = Modifier
                .size(baseSize)
                .graphicsLayer {
                    scaleX = currentScale
                    scaleY = currentScale
                }
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Content (The Chat Screen)
                Box(modifier = Modifier
                    .requiredSize(screenWidth, screenHeight)
                    .graphicsLayer {
                        // Keep content at its target size while the bubble scales
                        scaleX = 1f / currentScale
                        scaleY = 1f / currentScale
                    }
                ) {
                    content()
                }
                
                // Show a preview overlay if the portal isn't fully open
                if (expansion < 0.5f) {
                    val lastAssistantMessage = ui.chatHistory.lastOrNull { !it.isUser }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f * (1f - (expansion * 2).coerceIn(0f, 1f))))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (ui.isThinking && lastAssistantMessage == null) {
                                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("Aire is thinking...", style = MaterialTheme.typography.bodyMedium)
                            } else if (lastAssistantMessage != null) {
                                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = lastAssistantMessage.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 4
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Pull outward to open",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            } else {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(16.dp))
                                Text("Pull outward to open chat", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
