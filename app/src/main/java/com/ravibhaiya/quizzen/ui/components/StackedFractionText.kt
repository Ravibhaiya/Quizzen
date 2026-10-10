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

/** Gap between the whole part and the fraction, and between the fraction and the percent sign, relative to the font size. */
private const val GapScale = 0.1f

/** How far the fraction bar reaches past the digits on each side, relative to the size of the fraction. */
private const val BarOverhangScale = 0.12f

/**
 * A fraction written the textbook way: [numerator] over a bar over [denominator], optionally with a [whole] part in front of it (a
 * mixed number such as `33` + `1/3`) and a [suffix] after it (the `%` of a percentage).
 *
 *  - `1/9` as a question: only the fraction, as big as the text ([fractionScale] 1).
 *  - `33 1/3%`: the whole part at full size, the fraction at half size ([fractionScale] 0.5) centred on it, then the `%` sign.
 *
 * Like [FitText] the single size is worked out in one pass ([fitFontSize]) so everything always fits in one line. The size it
 * finds is the one of the whole part and the suffix; the fraction is [fractionScale] times that. [suffixColor] tints the suffix.
 * Everything is measured with the same [style] that draws it.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun StackedFractionText(
    numerator: String,
    denominator: String,
    style: TextStyle,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    step: TextUnit,
    modifier: Modifier = Modifier,
    whole: String? = null,
    suffix: String? = null,
    suffixColor: Color = Color.Unspecified,
    fractionScale: Float = 1f,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val availableWidth = constraints.maxWidth
        val fontSize: Float = remember(
            whole, numerator, denominator, suffix, style, availableWidth, maxFontSize, minFontSize, step, fractionScale,
        ) {
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

                val gaps = (if (whole != null) 1 else 0) + (if (suffix != null) 1 else 0)
                fitFontSize(
                    maxSize = maxFontSize.value,
                    minSize = minFontSize.value,
                    step = step.value,
                    availableWidth = availableWidth.toFloat(),
                ) { size ->
                    val small = size * fractionScale
                    val stack = maxOf(width(numerator, small), width(denominator, small))
                    val extras = with(density) { ((GapScale * gaps + BarOverhangScale * fractionScale * 2) * size).sp.toPx() }
                    val before = if (whole != null) width(whole, size) else 0f
                    val after = if (suffix != null) width(suffix, size) else 0f
                    before + stack + extras + after
                }
            }
        }
        val size = fontSize.sp
        val gap: Dp = with(density) { (GapScale * fontSize).sp.toDp() }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (whole != null) {
                Text(text = whole, style = style, fontSize = size, maxLines = 1, softWrap = false)
                Spacer(Modifier.width(gap))
            }
            StackedFraction(
                numerator = numerator,
                denominator = denominator,
                style = style,
                fontSize = (fontSize * fractionScale).sp,
                barOverhang = with(density) { (BarOverhangScale * fontSize * fractionScale).sp.toDp() },
            )
            if (suffix != null) {
                Spacer(Modifier.width(gap))
                Text(text = suffix, style = style, fontSize = size, color = suffixColor, maxLines = 1, softWrap = false)
            }
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
    // The bar is a bit thinner than the strokes of the digits.
    val thickness = with(LocalDensity.current) { (fontSize.value * 0.1f).sp.toDp() }.coerceAtLeast(1.dp)
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
                .background(style.color),
        )
        Spacer(Modifier.height(thickness / 2))
        Text(text = denominator, style = tight, maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = barOverhang))
    }
}
