package com.ravibhaiya.quizzen.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.RangeSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Two-thumb slider for whole numbers from [lowest] to [highest], in the Quizzen style: a thick rounded track (primary where
 * the range is, soft peach elsewhere) and white round thumbs with a primary ring that grow a little while held. The track
 * ends at [highest], so the end of the slider is the limit. It snaps to whole numbers.
 *
 * [range] is the current selection (both ends included). [onRangeChangeFinished] fires when the finger is lifted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizzenRangeSlider(
    range: IntRange,
    lowest: Int,
    highest: Int,
    onRangeChange: (IntRange) -> Unit,
    onRangeChangeFinished: () -> Unit,
    startThumbDescription: String,
    endThumbDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val startSource = remember { MutableInteractionSource() }
    val endSource = remember { MutableInteractionSource() }
    RangeSlider(
        value = range.first.toFloat()..range.last.toFloat(),
        onValueChange = { value -> onRangeChange(value.start.roundToInt()..value.endInclusive.roundToInt()) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        valueRange = lowest.toFloat()..highest.toFloat(),
        steps = (highest - lowest - 1).coerceAtLeast(0), // one stop per whole number, ends included
        onValueChangeFinished = onRangeChangeFinished,
        startInteractionSource = startSource,
        endInteractionSource = endSource,
        startThumb = { RangeThumb(startSource, startThumbDescription) },
        endThumb = { RangeThumb(endSource, endThumbDescription) },
        track = { state -> RangeTrack(state) },
    )
}

@Composable
private fun RangeThumb(interactionSource: MutableInteractionSource, description: String) {
    val held by interactionSource.collectIsDraggedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (held || pressed) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "rangeThumbScale",
    )
    Box(
        modifier = Modifier
            .size(30.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .cssShadow(
                NeutralShadowColor.copy(alpha = 0.28f),
                offsetY = 2.dp, blur = 6.dp, spread = 0.dp, shape = CircleShape,
            )
            .background(Color.White, CircleShape)
            .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
            .semantics { contentDescription = description },
    )
}

/** The track is drawn from the slider state inside the draw phase, so dragging never recomposes it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeTrack(state: RangeSliderState) {
    val inactive = MaterialTheme.colorScheme.surfaceContainerHigh
    val active = MaterialTheme.colorScheme.primary
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(10.dp),
    ) {
        val span = state.valueRange.endInclusive - state.valueRange.start
        val start = (state.activeRangeStart - state.valueRange.start) / span
        val end = (state.activeRangeEnd - state.valueRange.start) / span
        val corner = CornerRadius(size.height / 2f)
        drawRoundRect(color = inactive, size = size, cornerRadius = corner)
        drawRoundRect(
            color = active,
            topLeft = Offset(size.width * start, 0f),
            size = Size(size.width * (end - start), size.height),
            cornerRadius = corner,
        )
    }
}
