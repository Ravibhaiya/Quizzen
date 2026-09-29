package com.ravibhaiya.quizzen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/** Max content width. Mirrors the web original's 440px column, keeping tablets/foldables tidy. */
val ContentMaxWidth = 440.dp

/**
 * Base container for every screen: surface background, centered max-width column, and the two
 * soft decorative "glow" circles from the original design.
 */
@Composable
fun QuizzenScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .widthIn(max = ContentMaxWidth)
                    .clipToBounds(),
            ) {
                GlowBackground()
                content()
            }
        }
    }
}

@Composable
private fun GlowBackground() {
    val color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    Canvas(Modifier.fillMaxSize()) {
        // .glow.a: 280px circle at top:-140 right:-120 -> centre 20dp inside the right edge, on the top edge
        drawSoftDisc(color, radius = 140.dp.toPx(), center = Offset(size.width - 20.dp.toPx(), 0f))
        // .glow.b: 220px circle at bottom:-110 left:-100 -> centre 10dp inside the left edge, on the bottom edge
        drawSoftDisc(color, radius = 110.dp.toPx(), center = Offset(10.dp.toPx(), size.height))
    }
}

/**
 * Disc with the soft edge of CSS `filter: blur(10px)` (Gaussian sigma 10). Drawn as a radial gradient following the
 * Gaussian edge profile, so it looks the same on every API level (`Modifier.blur` only exists on API 31+, and older
 * phones showed a crisp, much more visible circle).
 */
private fun DrawScope.drawSoftDisc(color: Color, radius: Float, center: Offset) {
    val sigma = 10.dp.toPx()
    val outer = radius + 2f * sigma
    // Alpha of a Gaussian-blurred edge at -2s, -1s, 0, +1s from the original edge: 97.7%, 84.1%, 50%, 15.9%, then 0.
    val brush = Brush.radialGradient(
        colorStops = arrayOf(
            0f to color,
            (radius - 2f * sigma) / outer to color.copy(alpha = color.alpha * 0.977f),
            (radius - sigma) / outer to color.copy(alpha = color.alpha * 0.841f),
            radius / outer to color.copy(alpha = color.alpha * 0.5f),
            (radius + sigma) / outer to color.copy(alpha = color.alpha * 0.159f),
            1f to color.copy(alpha = 0f),
        ),
        center = center,
        radius = outer,
    )
    drawCircle(brush = brush, radius = outer, center = center)
}
