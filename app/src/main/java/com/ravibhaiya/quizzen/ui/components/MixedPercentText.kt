package com.ravibhaiya.quizzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Size of the numerator and the denominator relative to the whole part. */
private const val FractionScale = 0.5f

/** Gap between the whole part and the fraction, and between the fraction and the percent sign, relative to the whole part. */
private const val GapScale = 0.1f

/** How far the fraction bar reaches past the digits on each side, relative to the whole part. */
private const val BarOverhangScale = 0.06f

/**
 * A percentage written as a mixed number, with the fraction stacked like in a textbook: `33` then a smaller `1` over a bar over a
 * `3`, then the percent sign. The numerator and the denominator are half the size of the whole part and the stack is centred on
 * it. Like [FitText] the single size is worked out in one pass ([fitFontSize]) so the whole thing always fits in one line; the
 * size is the one of the whole part.
 *
 * [percentColor] tints the `%` sign. Everything is measured with the same [style] that draws it.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun MixedPercentText(
    whole: String,
    numerator: String,
    denominator: String,
    style: TextStyle,
    percentColor: Color,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    step: TextUnit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val availableWidth = constraints.maxWidth
        val fontSize: Float = remember(whole, numerator, denominator, style, availableWidth, maxFontSize, minFontSize, step) {
            if (availableWidth == Constraints.Infinity) {
                maxFontSize.value
            } else {
                fun width(text: String, size: Float): Float = measurer.measure(
                    text = text,
                    style = style.copy(fontSize = size.sp),
                    softWrap = false,
                    maxLines = 1,
                    constraints = Constraints(),
                ).size.width.toFloat()

                fitFontSize(
                    maxSize = maxFontSize.value,
                    minSize = minFontSize.value,
                    step = step.value,
                    availableWidth = availableWidth.toFloat(),
                ) { size ->
                    val small = size * FractionScale
                    val stack = maxOf(width(numerator, small), width(denominator, small))
                    val extras = with(density) { ((GapScale * 2 + BarOverhangScale * 2) * size).sp.toPx() }
                    width(whole, size) + stack + extras + width("%", size)
                }
            }
        }
        val size = fontSize.sp
        val small = (fontSize * FractionScale).sp
        val gap: Dp = with(density) { (GapScale * fontSize).sp.toDp() }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = whole, style = style, fontSize = size, maxLines = 1, softWrap = false)
            Spacer(Modifier.width(gap))
            StackedFraction(
                numerator = numerator,
                denominator = denominator,
                style = style,
                fontSize = small,
                barOverhang = with(density) { (BarOverhangScale * fontSize).sp.toDp() },
            )
            Spacer(Modifier.width(gap))
            Text(text = "%", style = style, fontSize = size, color = percentColor, maxLines = 1, softWrap = false)
        }
    }
}

/** [numerator] over a bar over [denominator]; as wide as the wider of the two plus [barOverhang] on each side. */
@Composable
private fun StackedFraction(
    numerator: String,
    denominator: String,
    style: TextStyle,
    fontSize: TextUnit,
    barOverhang: Dp,
) {
    // Tight lines: the digits sit right above and below the bar instead of in full-height text boxes.
    val tight = style.copy(fontSize = fontSize, lineHeight = fontSize, textAlign = TextAlign.Center)
    val barColor = style.color
    // The bar is a bit thinner than the strokes of the digits.
    val thickness = with(LocalDensity.current) { (fontSize.value * 0.12f).sp.toDp() }.coerceAtLeast(1.dp)
    Column(
        modifier = Modifier.width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The padding makes the bar reach a little past the digits on both sides.
        Text(text = numerator, style = tight, maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = barOverhang))
        Spacer(Modifier.height(thickness / 2))
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(thickness)
                .background(barColor),
        )
        Spacer(Modifier.height(thickness / 2))
        Text(text = denominator, style = tight, maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = barOverhang))
    }
}
