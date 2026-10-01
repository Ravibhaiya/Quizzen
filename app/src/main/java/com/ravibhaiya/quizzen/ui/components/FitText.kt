package com.ravibhaiya.quizzen.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Single-line text that shrinks from [maxFontSize] toward [minFontSize] (in [step] increments) until it fits the available
 * width (port of `fitQuestionText()` in the web original). The size is worked out in one pass with a text measurer
 * ([fitFontSize]) before the text is drawn, so there is no flicker and no repeated relayout.
 * Used for the practice question (centered) and the hero-card titles (start aligned), so long words such as
 * "Vocabulary" never wrap.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun FitText(
    text: AnnotatedString,
    style: TextStyle,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    step: TextUnit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val availableWidth = constraints.maxWidth
        val fontSize = remember(text, style, availableWidth, maxFontSize, minFontSize, step) {
            if (availableWidth == Constraints.Infinity) {
                maxFontSize
            } else {
                fitFontSize(
                    maxSize = maxFontSize.value,
                    minSize = minFontSize.value,
                    step = step.value,
                    availableWidth = availableWidth.toFloat(),
                ) { size ->
                    measurer.measure(
                        text = text,
                        style = style.copy(fontSize = size.sp),
                        softWrap = false,
                        maxLines = 1,
                        constraints = Constraints(),
                    ).size.width.toFloat()
                }.sp
            }
        }
        Text(
            text = text,
            style = style,
            fontSize = fontSize,
            textAlign = textAlign,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
