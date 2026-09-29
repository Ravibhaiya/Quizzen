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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
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
    // CSS `filter: blur(10px)`; RenderEffect blur is available on API 31+, older devices get crisp circles.
    Canvas(Modifier.fillMaxSize().blur(10.dp, BlurredEdgeTreatment.Unbounded)) {
        // .glow.a: 280px circle at top:-140 right:-120
        drawCircle(color, radius = 140.dp.toPx(), center = Offset(size.width + 120.dp.toPx() - 140.dp.toPx(), 0f))
        // .glow.b: 220px circle at bottom:-110 left:-100
        drawCircle(color, radius = 110.dp.toPx(), center = Offset(-100.dp.toPx() + 110.dp.toPx(), size.height))
    }
}
