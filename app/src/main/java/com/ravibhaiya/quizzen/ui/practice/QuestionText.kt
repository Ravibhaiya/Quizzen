package com.ravibhaiya.quizzen.ui.practice

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.AlphabetChallenge
import com.ravibhaiya.quizzen.domain.AlphabetQuestion
import com.ravibhaiya.quizzen.domain.PowerQuestion
import com.ravibhaiya.quizzen.domain.ProductQuestion
import com.ravibhaiya.quizzen.domain.Question
import com.ravibhaiya.quizzen.domain.RootQuestion

/** Size of the exponent / root index relative to the surrounding text, so it keeps shrinking together with `FitText`. */
private val ScriptSize = 0.55.em

/**
 * Pulls the root sign towards the cube-root index, which moves the index to the right so it sits against the sign like a
 * printed cube root. Measured on the font: at this value the gap shrinks from about 4.9 sp to about 0.7 sp at 49.6 sp.
 */
private val RootIndexNudge = (-0.15).em

/**
 * How a question is drawn on the Practice screen. The operator (the multiplication sign, the exponent, the root sign) is
 * tinted with [accent], exactly like the orange "x" in the original design:
 *  - product: `710 x 60`
 *  - square / cube: `17` with a raised `2` / `3`
 *  - square root / cube root: `√784` and a raised `3` in front of the root sign for cube roots
 *  - alphabet: just the letter (`C`) or the place (`3`), in the text colour
 *
 * Exponents use a real superscript style instead of the `²` `³` characters so they scale with the text.
 */
fun Question.toDisplayText(accent: Color): AnnotatedString = buildAnnotatedString {
    val script = SpanStyle(color = accent, baselineShift = BaselineShift.Superscript, fontSize = ScriptSize)
    when (val question = this@toDisplayText) {
        is ProductQuestion -> {
            append(question.left.toString())
            append(" ")
            withStyle(SpanStyle(color = accent)) { append("\u00D7") }
            append(" ")
            append(question.right.toString())
        }
        is AlphabetQuestion -> append(question.shown)
        is PowerQuestion -> {
            append(question.base.toString())
            withStyle(script) { append(question.exponent.toString()) }
        }
        is RootQuestion -> {
            if (question.degree != 2) {
                withStyle(script.copy(letterSpacing = RootIndexNudge)) { append(question.degree.toString()) }
            }
            withStyle(SpanStyle(color = accent)) { append("\u221A") }
            append(question.radicand.toString())
        }
    }
}

@Composable
fun questionText(question: Question): AnnotatedString {
    val accent = MaterialTheme.colorScheme.primary
    return remember(question, accent) { question.toDisplayText(accent) }
}

/** What a screen reader says: "710 times 60", "17 squared", "cube root of 1728" (not "172" or "31728"). */
@Composable
fun spokenQuestion(question: Question): String = when (question) {
    is ProductQuestion -> stringResource(R.string.question_spoken_product, question.left, question.right)
    is PowerQuestion -> when (question.exponent) {
        2 -> stringResource(R.string.question_spoken_square, question.base)
        else -> stringResource(R.string.question_spoken_cube, question.base)
    }
    is RootQuestion -> when (question.degree) {
        2 -> stringResource(R.string.question_spoken_square_root, question.radicand)
        else -> stringResource(R.string.question_spoken_cube_root, question.radicand)
    }
    is AlphabetQuestion -> when (question.challenge) {
        AlphabetChallenge.FindPosition -> stringResource(R.string.question_spoken_letter, question.shown)
        AlphabetChallenge.FindLetter -> stringResource(R.string.question_spoken_position, question.position)
        AlphabetChallenge.ReverseLetter -> stringResource(R.string.question_spoken_opposite, question.shown)
    }
}

/** The placeholder of the answer field: tells Alphabet players what to type; every other quiz just says "Answer". */
@Composable
fun answerHint(question: Question): String = when (question) {
    is AlphabetQuestion -> stringResource(
        when (question.challenge) {
            AlphabetChallenge.FindPosition -> R.string.answer_hint_position
            AlphabetChallenge.FindLetter -> R.string.answer_hint_letter
            AlphabetChallenge.ReverseLetter -> R.string.answer_hint_opposite
        },
    )
    else -> stringResource(R.string.answer_hint)
}
