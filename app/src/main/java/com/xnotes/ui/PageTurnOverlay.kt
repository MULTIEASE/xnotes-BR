package com.xnotes.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale

/**
 * xnotes BR: the notebook page turn. A picture of the page that was showing lifts from the
 * binding and swings away like a paper leaf, its far side darkening and a soft shadow falling on
 * the new page beneath, then disappears.
 */
@Composable
fun PageTurnOverlay(editor: Editor) {
    val turn = editor.pageTurn ?: return
    val progress = remember(turn.id) { Animatable(0f) }
    LaunchedEffect(turn.id) {
        progress.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
        if (editor.pageTurn?.id == turn.id) editor.pageTurn = null
    }
    val p = progress.value
    Box(Modifier.fillMaxSize()) {
        // Shadow the turning leaf throws on the page underneath, strongest at the start.
        Canvas(Modifier.fillMaxSize()) {
            val shade = (1f - p) * 0.35f
            val edge = if (turn.forward) size.width * (1f - p) else size.width * p
            val brush = if (turn.forward) {
                Brush.horizontalGradient(listOf(Color.Black.copy(alpha = shade), Color.Transparent), startX = edge, endX = edge + size.width * 0.25f)
            } else {
                Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = shade)), startX = edge - size.width * 0.25f, endX = edge)
            }
            drawRect(brush)
        }
        // The leaf itself: hinged on the left edge going forward, on the right edge going back.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(if (turn.forward) 0f else 1f, 0.5f)
                    cameraDistance = 14f * density
                    rotationY = (if (turn.forward) -1f else 1f) * 90f * p
                    alpha = if (p >= 0.98f) 0f else 1f
                },
        ) {
            Image(turn.picture, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            // The leaf darkens as it turns away from the light.
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f * p)))
            // A bright crease along the binding edge.
            Canvas(Modifier.fillMaxSize()) {
                val x = if (turn.forward) 0f else size.width
                drawLine(Color.White.copy(alpha = 0.5f * (1f - p)), Offset(x, 0f), Offset(x, size.height), strokeWidth = 6f)
            }
        }
    }
}
