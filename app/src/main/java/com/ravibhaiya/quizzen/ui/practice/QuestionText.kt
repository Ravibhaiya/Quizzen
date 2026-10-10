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
import com.ravibhaiya.quizzen.domain.FractionChallenge
import com.ravibhaiya.quizzen.domain.FractionQuestion
import com.ravibhaiya.quizzen.domain.FractionRules
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
 *  - fraction: `1/25` for a fraction to turn into a percentage; for a percentage to turn into a fraction a mixed number
 *    (`33 1/3%`, drawn stacked by StackedFractionText on the Practice screen; this is its plain-text form) or the decimal (`33.33%`),
 *    as the question was worded
 *  The slash and the percent sign are tinted with [accent], like the other operators.
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
        is FractionQuestion -> when (question.challenge) {
            FractionChallenge.Percentage -> {
                append("1")
                withStyle(SpanStyle(color = accent)) { append("/") }
                append(question.denominator.toString())
            }
            FractionChallenge.Fraction -> {
                val percent = question.percent
                if (question.decimal) {
                    append(FractionRules.decimalOf(question.denominator))
                } else {
                    // Drawn as a stacked fraction by StackedFractionText; this is its plain-text form.
                    append(percent.whole.toString())
                    if (percent.hasFraction) append(" ${percent.numerator}/${percent.denominator}")
                }
                withStyle(SpanStyle(color = accent)) { append("%") }
            }
        }
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
    is FractionQuestion -> {
        val percent = question.percent
        when {
            question.challenge == FractionChallenge.Percentage ->
                stringResource(R.string.question_spoken_fraction, question.denominator)
            question.decimal ->
                stringResource(R.string.question_spoken_percent_decimal, FractionRules.decimalOf(question.denominator))
            percent.hasFraction ->
                stringResource(R.string.question_spoken_percent_mixed, percent.whole, percent.numerator, percent.denominator)
            else -> stringResource(R.string.question_spoken_percent, percent.whole)
        }
    }
}

/** The placeholder of the answer field: tells Alphabet and Fraction & Percentage players what to type; every other quiz just says "Answer". */
@Composable
fun answerHint(question: Question): String = when (question) {
    is AlphabetQuestion -> stringResource(
        when (question.challenge) {
            AlphabetChallenge.FindPosition -> R.string.answer_hint_position
            AlphabetChallenge.FindLetter -> R.string.answer_hint_letter
            AlphabetChallenge.ReverseLetter -> R.string.answer_hint_opposite
        },
    )
    is FractionQuestion -> stringResource(
        if (question.challenge == FractionChallenge.Fraction) R.string.answer_hint_fraction else R.string.answer_hint_percentage,
    )
    else -> stringResource(R.string.answer_hint)
}
