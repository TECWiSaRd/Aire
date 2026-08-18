package com.aire.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

    // Initial "Expanding from Bar" state
    val baseSize = 200.dp
    
    // Scale starts small (circle) and grows to fill screen
    val scale = 1f + (expansion * 5f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    // Pinch to go back logic
                    if (zoom < 0.7f) {
                        viewModel.navigateTo(AppScreen.HOME)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Dim the background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * (1f - expansion)))
        )

        // The Portal Bubble
        Box(
            modifier = Modifier
                .size(baseSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            
                            val currentPos = change.position
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            val dist = sqrt((currentPos.x - centerX) * (currentPos.x - centerX) + (currentPos.y - centerY) * (currentPos.y - centerY))
                            
                            // Dragging away from center increases expansion
                            val newExpansion = (dist / (size.width * 1.5f)).coerceIn(0f, 1f)
                            if (newExpansion > ui.portalExpansion) {
                                viewModel.setPortalExpansion(newExpansion)
                            }
                        },
                        onDragEnd = {
                            if (ui.portalExpansion > 0.4f) {
                                viewModel.setPortalExpansion(1f)
                            } else {
                                viewModel.setPortalExpansion(0f)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Content (The Chat Screen)
            Box(modifier = Modifier
                .requiredSize(screenWidth, screenHeight)
                .graphicsLayer {
                    scaleX = 1f / scale
                    scaleY = 1f / scale
                }
            ) {
                content()
            }
            
            // "Part of the response" preview overlay if not expanded
            if (expansion < 0.5f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (ui.isThinking) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "Pull to expand chat",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
