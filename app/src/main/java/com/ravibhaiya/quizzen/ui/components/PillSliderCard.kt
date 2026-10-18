package com.ravibhaiya.quizzen.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ravibhaiya.quizzen.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val ThumbSlotWidth = 6.dp
private val ThumbHeldWidth = 4.dp
private val ThumbHeight = 40.dp
private val TrackHeight = 16.dp
private val HandleGap = 6.dp
private val InnerCorner = 3.dp
private val StopDotRadius = 2.dp
private const val MIN_SPAN = 0.0001f

/**
 * Single-value slider card matching [PillRangeCard], used for selecting the number of words to attempt (5..25).
 */
@Composable
fun PillSliderCard(
    value: Int,
    lowest: Int,
    highest: Int,
    valueLabel: String,
    lowestLabel: String,
    highestLabel: String,
    summary: String,
    thumbDescription: String,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .cssShadow(
                NeutralShadowColor.copy(alpha = 0.05f),
                offsetY = 2.dp,
                blur = 6.dp,
                spread = 0.dp,
                shape = shape,
            )
            .background(colors.surfaceContainer, shape)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = summary },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .widthIn(min = 56.dp)
                    .background(colors.primaryContainer, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onPrimaryContainer,
                )
            }
        }
        QuizzenSingleSlider(
            value = value,
            lowest = lowest,
            highest = highest,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            thumbDescription = thumbDescription,
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(lowestLabel, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(highestLabel, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizzenSingleSlider(
    value: Int,
    lowest: Int,
    highest: Int,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
    thumbDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Slider(
        value = value.coerceIn(lowest, highest).toFloat(),
        onValueChange = { raw -> onValueChange(raw.roundToInt()) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        valueRange = lowest.toFloat()..highest.toFloat(),
        steps = (highest - lowest - 1).coerceAtLeast(0),
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        thumb = { SingleSliderHandle(interactionSource, thumbDescription) },
        track = { state -> SingleSliderTrack(state) },
    )
}

@Composable
private fun SingleSliderHandle(interactionSource: MutableInteractionSource, description: String) {
    val held by interactionSource.collectIsDraggedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val width by animateDpAsState(
        targetValue = if (held || pressed) ThumbHeldWidth else ThumbSlotWidth,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "singleSliderHandleWidth",
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
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleSliderTrack(state: SliderState) {
    val inactive = MaterialTheme.colorScheme.surfaceContainerHigh
    val active = MaterialTheme.colorScheme.primary
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(TrackHeight),
    ) {
        val span = (state.valueRange.endInclusive - state.valueRange.start).coerceAtLeast(MIN_SPAN)
        val thumbX = size.width * (state.value - state.valueRange.start) / span
        val gap = (ThumbSlotWidth / 2 + HandleGap).toPx()
        val outer = size.height / 2f
        val inner = InnerCorner.toPx()

        drawSinglePiece(0f, thumbX - gap, outer, inner, active)
        drawSinglePiece(thumbX + gap, size.width, inner, outer, inactive)

        val dotRadius = StopDotRadius.toPx()
        if (size.width - (thumbX + gap) > 2f * outer) {
            drawCircle(active, dotRadius, Offset(size.width - outer, size.height / 2f))
        }
    }
}

private fun DrawScope.drawSinglePiece(
    left: Float,
    right: Float,
    leftRadius: Float,
    rightRadius: Float,
    color: Color,
) {
    val from = max(0f, left)
    val to = min(size.width, right)
    if (to - from < 1f) return
    val half = (to - from) / 2f
    val start = CornerRadius(min(leftRadius, half))
    val end = CornerRadius(min(rightRadius, half))
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = from,
                top = 0f,
                right = to,
                bottom = size.height,
                topLeftCornerRadius = start,
                topRightCornerRadius = end,
                bottomRightCornerRadius = end,
                bottomLeftCornerRadius = start,
            ),
        )
    }
    drawPath(path, color)
}

/**
 * Shared countdown chip used across practice and vocabulary quiz headers.
 * While [pulsing] it breathes between 100% and 110% inside `graphicsLayer`; otherwise nothing animates.
 */
@Composable
fun TimerChip(seconds: Int, pulsing: Boolean, modifier: Modifier = Modifier) {
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(pulsing) {
        if (pulsing) {
            pulse.animateTo(1.1f, infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse))
        } else {
            pulse.snapTo(1f)
        }
    }
    Text(
        text = stringResource(R.string.seconds_short, seconds),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            }
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            .padding(horizontal = 15.dp, vertical = 9.dp),
    )
}
