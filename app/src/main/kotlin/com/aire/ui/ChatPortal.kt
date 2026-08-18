package com.aire.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
    val density = LocalDensity.current
    
    // Spring animation for much smoother physical feel
    val expansion by animateFloatAsState(
        targetValue = ui.portalExpansion,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "expansion"
    )

    val baseSize = 240.dp
    val baseSizePx = with(density) { baseSize.toPx() }
    
    // Scale starts small and grows to cover the entire screen
    val screenDiagonal = sqrt((screenWidth.value * screenWidth.value) + (screenHeight.value * screenHeight.value))
    val targetScale = (screenDiagonal / baseSize.value) * 1.5f
    val currentScale = 1f + (expansion * (targetScale - 1f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f * (1f - expansion))),
        contentAlignment = Alignment.Center
    ) {
        // --- The Portal Bubble ---
        Surface(
            modifier = Modifier
                .size(baseSize)
                .graphicsLayer {
                    scaleX = currentScale
                    scaleY = currentScale
                    // Morph from circle to square as we expand
                    shape = CircleShape
                    clip = true
                }
                .clickable { viewModel.setPortalExpansion(1f) },
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Fixed-size content container that doesn't "stretch" with the bubble
                Box(modifier = Modifier
                    .requiredSize(screenWidth, screenHeight)
                    .graphicsLayer {
                        // Counter-scale so content looks perfectly normal at all times
                        scaleX = 1f / currentScale
                        scaleY = 1f / currentScale
                        // Alpha-blend the real chat content as we expand
                        alpha = (expansion * 2f).coerceIn(0f, 1f)
                    }
                ) {
                    content()
                }
                
                // --- Preview / Guidance Overlay ---
                // Fades out as you expand
                val overlayAlpha = (1f - (expansion * 2f)).coerceIn(0f, 1f)
                if (overlayAlpha > 0f) {
                    val lastAssistantMessage = ui.chatHistory.lastOrNull { !it.isUser }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(overlayAlpha)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
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
                                    maxLines = 5
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "PULL TO OPEN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(16.dp))
                                Text("Pull outward to open", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        // --- GESTURE LAYER ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var cumulativeZoom = 1f
                    var gestureMaxExpansion = 0f
                    
                    detectTransformGestures { centroid, _, zoom, _ ->
                        // 1. PINCH TO EXIT
                        if (zoom != 1f) {
                            cumulativeZoom *= zoom
                            if (cumulativeZoom < 0.7f) {
                                viewModel.closePortal()
                                cumulativeZoom = 1f
                            }
                        }

                        // 2. RADIAL PULL TO EXPAND
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = centroid.x - centerX
                        val dy = centroid.y - centerY
                        val dist = sqrt(dx * dx + dy * dy)
                        
                        val hitRadius = baseSizePx * 1.2f
                        if (dist <= hitRadius || currentPortalExpansion > 0f) {
                            val startRadius = baseSizePx / 4f
                            val endRange = with(density) { 320.dp.toPx() }
                            val newExpansion = ((dist - startRadius) / (endRange - startRadius)).coerceIn(0f, 1f)
                            
                            if (newExpansion > gestureMaxExpansion) {
                                gestureMaxExpansion = newExpansion
                                viewModel.setPortalExpansion(newExpansion)
                            }
                        }
                    }
                }
        )
    }
}
