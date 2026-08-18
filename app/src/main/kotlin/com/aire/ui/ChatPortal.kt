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
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

@Composable
fun ChatPortal(
    viewModel: MemoryViewModel,
    content: @Composable () -> Unit
) {
    val ui by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    val expansion by animateFloatAsState(
        targetValue = ui.portalExpansion,
        label = "expansion"
    )

    val baseSize = 240.dp
    // Scale starts small (circle) and grows to fill screen
    // We need the scale to reach a point where the circle covers the entire diagonal
    val screenDiagonal = sqrt((screenWidth.value * screenWidth.value) + (screenHeight.value * screenHeight.value))
    val targetScale = (screenDiagonal / baseSize.value) * 1.2f
    val currentScale = 1f + (expansion * (targetScale - 1f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f * (1f - expansion)))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDrag = { change, _ ->
                        change.consume()
                        
                        val currentPos = change.position
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val dist = sqrt((currentPos.x - centerX) * (currentPos.x - centerX) + (currentPos.y - centerY) * (currentPos.y - centerY))
                        
                        // Normalized distance from center to drive expansion
                        // Start expanding when dragging away from the center area
                        val normalizedDist = ((dist - 100f) / 600f).coerceIn(0f, 1f)
                        if (normalizedDist > ui.portalExpansion) {
                            viewModel.setPortalExpansion(normalizedDist)
                        }
                    },
                    onDragEnd = {
                        if (ui.portalExpansion > 0.3f) {
                            viewModel.setPortalExpansion(1f)
                        } else {
                            viewModel.setPortalExpansion(0f)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom < 0.7f) {
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
                        // Counter-scale to keep content looking normal size
                        scaleX = 1f / currentScale
                        scaleY = 1f / currentScale
                    }
                ) {
                    content()
                }
                
                // Interactive overlay guidance
                if (expansion < 0.4f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f * (1f - expansion))),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (ui.isThinking && ui.chatHistory.isEmpty()) {
                                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            } else {
                                Icon(
                                    Icons.Default.AutoAwesome, 
                                    null, 
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Pull outward to open chat",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
