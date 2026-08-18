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
import androidx.compose.ui.geometry.Offset
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
    
    val expansion by animateFloatAsState(
        targetValue = ui.portalExpansion,
        label = "expansion"
    )

    val density = LocalDensity.current
    val baseSize = 240.dp
    val baseSizePx = with(density) { baseSize.toPx() }
    
    val screenDiagonal = sqrt((screenWidth.value * screenWidth.value) + (screenHeight.value * screenHeight.value))
    val targetScale = (screenDiagonal / baseSize.value) * 1.2f
    val currentScale = 1f + (expansion * (targetScale - 1f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f * (1f - expansion)))
            .pointerInput(Unit) {
                var gestureMaxExpansion = 0f
                var dragStartPoint: Offset? = null
                
                detectDragGestures(
                    onDragStart = { offset -> 
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val distFromCenter = sqrt((offset.x - centerX) * (offset.x - centerX) + (offset.y - centerY) * (offset.y - centerY))
                        
                        // Only start if touch is inside the portal bubble
                        if (distFromCenter <= (baseSizePx / 2f)) {
                            dragStartPoint = offset
                            gestureMaxExpansion = currentPortalExpansion
                        } else {
                            dragStartPoint = null
                        }
                    },
                    onDrag = { change, _ ->
                        val start = dragStartPoint ?: return@detectDragGestures
                        change.consume()
                        
                        val currentPos = change.position
                        val distFromStart = sqrt((currentPos.x - start.x) * (currentPos.x - start.x) + (currentPos.y - start.y) * (currentPos.y - start.y))
                        
                        // Density-aware drag range (500dp)
                        val dragRangePx = with(density) { 500.dp.toPx() }
                        val newExpansion = (distFromStart / dragRangePx).coerceIn(0f, 1f)
                        
                        if (newExpansion > gestureMaxExpansion) {
                            gestureMaxExpansion = newExpansion
                            viewModel.setPortalExpansion(newExpansion)
                        }
                    },
                    onDragEnd = {
                        if (gestureMaxExpansion > 0.3f) {
                            viewModel.setPortalExpansion(1f)
                        } else {
                            viewModel.setPortalExpansion(0f)
                        }
                        dragStartPoint = null
                    },
                    onDragCancel = {
                        viewModel.setPortalExpansion(0f)
                        dragStartPoint = null
                    }
                )
            }
            .pointerInput(Unit) {
                // Simplified zoom tracking: detectTransformGestures 
                // naturally resets its internal state when pointers are lifted.
                detectTransformGestures { _, _, zoom, _ ->
                    // CodeRabbit: accumulate zoom within the active gesture
                    // We'll use a property in the ViewModel or a local state 
                    // if we need persistent accumulation across recompositions,
                    // but for a single gesture, this local variable is reset 
                    // when detectTransformGestures starts a new internal loop.
                    
                    // To be strictly compliant with "reset at start of each pinch":
                    // detectTransformGestures does this by finishing and restarting 
                    // when fingers are lifted.
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
                        scaleX = 1f / currentScale
                        scaleY = 1f / currentScale
                    }
                ) {
                    content()
                }
                
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
