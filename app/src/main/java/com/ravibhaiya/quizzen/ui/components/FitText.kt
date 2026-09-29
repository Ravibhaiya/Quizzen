package com.ravibhaiya.quizzen.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Single-line text that shrinks from [maxFontSize] toward [minFontSize] in [step] increments until it fits
 * the available width (port of `fitQuestionText()` in the web original). Hidden until fitted to avoid flicker.
 * Used for the practice question (centered) and the hero-card titles (start aligned), so long words such as
 * "Vocabulary" never wrap.
 */
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
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    var ready by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent { if (ready) drawContent() },
        style = style,
        fontSize = fontSize,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize.value > minFontSize.value) {
                fontSize = (fontSize.value - step.value).coerceAtLeast(minFontSize.value).sp
            } else {
                ready = true
            }
        },
    )
}
