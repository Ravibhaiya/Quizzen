package com.ravibhaiya.quizzen.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.RangeSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val ThumbSlotWidth = 6.dp
private val ThumbHeldWidth = 4.dp
private val ThumbHeight = 40.dp
private val TrackHeight = 16.dp

/** Space between a handle and the track on both sides of it. */
private val HandleGap = 6.dp

/** Corner of a track piece where it faces a handle (the outer ends are fully round). */
private val InnerCorner = 3.dp
private val StopDotRadius = 2.dp

/**
 * Two-thumb slider for whole numbers from [lowest] to [highest], in the Material 3 Expressive shape: a thick track cut into three
 * pieces (soft peach before the first handle, primary between the handles, soft peach after the second), and a slim vertical
 * pill as each handle, with a small gap between handle and track. The handle narrows a little while held. Small dots at the two
 * ends of the track mark the limits while the range does not reach them. The end of the slider is the limit, and it snaps to whole
 * numbers.
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
        startThumb = { RangeHandle(startSource, startThumbDescription) },
        endThumb = { RangeHandle(endSource, endThumbDescription) },
        track = { state -> RangeTrack(state) },
    )
}

/** The slot keeps one fixed size (so the track never shifts); only the drawn pill inside it narrows while held. */
@Composable
private fun RangeHandle(interactionSource: MutableInteractionSource, description: String) {
    val held by interactionSource.collectIsDraggedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val width by animateDpAsState(
        targetValue = if (held || pressed) ThumbHeldWidth else ThumbSlotWidth,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "rangeHandleWidth",
    )
    Box(
        modifier = Modifier
            .size(width = ThumbSlotWidth, height = ThumbHeight)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(width)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
        )
    }
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
            .height(TrackHeight),
    ) {
        val span = (state.valueRange.endInclusive - state.valueRange.start).coerceAtLeast(MIN_SPAN)
        val startX = size.width * (state.activeRangeStart - state.valueRange.start) / span
        val endX = size.width * (state.activeRangeEnd - state.valueRange.start) / span
        val gap = (ThumbSlotWidth / 2 + HandleGap).toPx()
        val outer = size.height / 2f
        val inner = InnerCorner.toPx()

        drawPiece(0f, startX - gap, outer, inner, inactive)
        drawPiece(startX + gap, endX - gap, inner, inner, active)
        drawPiece(endX + gap, size.width, inner, outer, inactive)

        // A dot at an end of the track says "the limit is here" for as long as the range does not reach it.
        val dotRadius = StopDotRadius.toPx()
        if (startX - gap > 2f * outer) drawCircle(active, dotRadius, Offset(outer, size.height / 2f))
        if (size.width - (endX + gap) > 2f * outer) drawCircle(active, dotRadius, Offset(size.width - outer, size.height / 2f))
    }
}

/** One piece of the track from [left] to [right] (clipped to the track); a piece that would be thinner than a pixel is skipped. */
private fun DrawScope.drawPiece(left: Float, right: Float, leftRadius: Float, rightRadius: Float, color: Color) {
    val from = max(0f, left)
    val to = min(size.width, right)
    if (to - from < 1f) return
    val half = (to - from) / 2f
    val start = CornerRadius(min(leftRadius, half))
    val end = CornerRadius(min(rightRadius, half))
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = from, top = 0f, right = to, bottom = size.height,
                topLeftCornerRadius = start, topRightCornerRadius = end,
                bottomRightCornerRadius = end, bottomLeftCornerRadius = start,
            ),
        )
    }
    drawPath(path, color)
}

private const val MIN_SPAN = 0.0001f
