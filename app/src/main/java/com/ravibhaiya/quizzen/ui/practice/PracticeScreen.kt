package com.ravibhaiya.quizzen.ui.practice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.AnswerKind
import com.ravibhaiya.quizzen.domain.FractionQuestion
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.ui.components.FitText
import com.ravibhaiya.quizzen.ui.components.StackedFractionText
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.PrimaryButton
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.bouncyClickable
import com.ravibhaiya.quizzen.ui.components.cssShadow
import com.ravibhaiya.quizzen.ui.components.rememberHaptics

@Composable
fun PracticeScreen(
    hapticEnabled: Boolean,
    onBack: () -> Unit,
    viewModel: PracticeViewModel = viewModel(factory = PracticeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(hapticEnabled)
    val focusRequester = remember { FocusRequester() }
    val shake = remember { Animatable(0f) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onPause() }

    // Haptic on result.
    LaunchedEffect(state.feedback) {
        when (state.feedback?.type) {
            FeedbackType.Correct -> haptics.success()
            FeedbackType.Incorrect -> haptics.error()
            FeedbackType.Timeout -> haptics.timeout()
            null -> Unit
        }
    }

    // Shake on every incorrect answer (400ms, same keyframes as the CSS).
    LaunchedEffect(state.shakeCount) {
        if (state.shakeCount > 0) {
            shake.snapTo(0f)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -3f at 40
                    5f at 80
                    -9f at 120
                    9f at 160
                    -9f at 200
                    9f at 240
                    -9f at 280
                    5f at 320
                    -3f at 360
                    0f at 400
                },
            )
        }
    }

    // Focus the field for every new question so the number pad stays up.
    LaunchedEffect(state.question) {
        // The node may not be attached yet on the very first frame; ignoring that failure is safe.
        runCatching { focusRequester.requestFocus() }
    }

    QuizzenScreen {
        Box(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 34.dp),
            ) {
                ScreenHeader(
                    title = stringResource(R.string.practice_title),
                    onBack = { haptics.click(); onBack() },
                    trailing = { TimerChip(seconds = state.remainingSeconds, pulsing = state.isTimerLow && !state.isLocked) },
                )

                val spoken = spokenQuestion(state.question)
                val questionStyle = MaterialTheme.typography.displayLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                val questionModifier = Modifier
                    .padding(top = 46.dp, bottom = 46.dp)
                    .clearAndSetSemantics { contentDescription = spoken }
                val question = state.question
                if (question is FractionQuestion && question.showsMixedNumber) {
                    // A mixed number such as 33 1/3% is drawn with the fraction stacked (number over bar over number).
                    StackedFractionText(
                        whole = question.percent.whole.toString(),
                        numerator = question.percent.numerator.toString(),
                        denominator = question.percent.denominator.toString(),
                        suffix = "%",
                        suffixColor = MaterialTheme.colorScheme.primary,
                        fractionScale = 0.5f,
                        style = questionStyle,
                        maxFontSize = 49.6.sp,
                        minFontSize = 24.sp,
                        step = 1.6.sp,
                        modifier = questionModifier,
                    )
                } else if (question is FractionQuestion && question.showsFraction) {
                    // The fraction to turn into a percentage: 1 over the denominator, as big as the other questions.
                    StackedFractionText(
                        numerator = "1",
                        denominator = question.denominator.toString(),
                        style = questionStyle,
                        maxFontSize = 49.6.sp,
                        minFontSize = 24.sp,
                        step = 1.6.sp,
                        modifier = questionModifier,
                    )
                } else {
                    FitText(
                        text = questionText(question),
                        style = questionStyle,
                        maxFontSize = 49.6.sp,
                        minFontSize = 24.sp,
                        step = 1.6.sp,
                        modifier = questionModifier,
                    )
                }

                AnswerField(
                    value = state.answer,
                    hint = answerHint(state.question),
                    kind = state.question.answerKind,
                    onValueChange = viewModel::onAnswerChanged,
                    onDone = viewModel::onCheck,
                    // The number keyboard has no slash: fraction answers get an on-screen "/" key.
                    onSlash = if (state.question.answerKind == AnswerKind.Fraction) {
                        {
                            haptics.tick()
                            viewModel.onSlash()
                            runCatching { focusRequester.requestFocus() }
                        }
                    } else {
                        null
                    },
                    readOnly = state.isLocked,
                    shakeOffsetDp = { shake.value },
                    modifier = Modifier.focusRequester(focusRequester),
                )

                PrimaryButton(
                    text = stringResource(R.string.check),
                    onClick = viewModel::onCheck,
                    enabled = !state.isLocked,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }

            FeedbackSheet(
                feedback = state.feedback,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * Countdown chip. While [pulsing] it breathes between 100% and 110%; otherwise nothing animates, so an idle Practice screen
 * schedules no frames at all (an always-running infinite transition kept redrawing every frame, draining battery and
 * stealing time from slow phones). The scale is read inside `graphicsLayer`, so pulsing never recomposes the chip.
 */
@Composable
private fun TimerChip(seconds: Int, pulsing: Boolean) {
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
        modifier = Modifier
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            }
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            .padding(horizontal = 15.dp, vertical = 9.dp),
    )
}

@Composable
private fun AnswerField(
    value: String,
    hint: String,
    kind: AnswerKind,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    readOnly: Boolean,
    shakeOffsetDp: () -> Float,
    modifier: Modifier = Modifier,
    /** When given, a "/" key is shown at the end of the field and calls this. */
    onSlash: (() -> Unit)? = null,
) {
    val primary = MaterialTheme.colorScheme.primary
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = RoundedCornerShape(28.dp)
    val container by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(200),
        label = "answerContainer",
    )
    val ringAlpha by animateFloatAsState(if (focused) 1f else 0f, tween(200), label = "answerRing")
    val percentSign = MaterialTheme.colorScheme.onSurfaceVariant
    val visualTransformation = remember(kind, percentSign) {
        if (kind == AnswerKind.Percent) PercentSuffix(SpanStyle(color = percentSign)) else VisualTransformation.None
    }

    BasicTextField(
        // The cursor always stays at the end of the answer, also after the on-screen "/" key added a character: otherwise the
        // next digit would be typed before the slash (1, /, 3 must give 1/3, not 13/).
        value = TextFieldValue(text = value, selection = TextRange(value.length)),
        onValueChange = { onValueChange(it.text) },
        readOnly = readOnly,
        singleLine = true,
        interactionSource = source,
        textStyle = MaterialTheme.typography.headlineLarge.copy(
            fontSize = 22.4.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp,
        ),
        cursorBrush = SolidColor(primary),
        visualTransformation = visualTransformation,
        keyboardOptions = when (kind) {
            AnswerKind.Letter -> KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            )
            // A percentage needs the decimal point (33.33); the % sign is added by PercentSuffix.
            AnswerKind.Percent -> KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
            AnswerKind.Number, AnswerKind.Fraction -> KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
        },
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = shakeOffsetDp().dp.toPx() }
            .drawBehind {
                if (ringAlpha > 0f) {
                    val inflate = 2.dp.toPx()
                    drawRoundRect(
                        color = primary.copy(alpha = 0.2f * ringAlpha),
                        topLeft = Offset(-inflate, -inflate),
                        size = Size(size.width + inflate * 2, size.height + inflate * 2),
                        cornerRadius = CornerRadius(28.dp.toPx() + inflate),
                        style = Stroke(width = 4.dp.toPx()),
                    )
                }
            }
            .cssShadow(NeutralShadowColor.copy(alpha = 0.05f), offsetY = 2.dp, blur = 8.dp, spread = 0.dp, shape = shape)
            .cssShadow(primary.copy(alpha = 0.4f * ringAlpha), offsetY = 10.dp, blur = 20.dp, spread = (-10).dp, shape = shape)
            .background(container, shape),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 22.4.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                    }
                    inner()
                }
                if (onSlash != null) SlashKey(onClick = onSlash, modifier = Modifier.padding(end = 12.dp))
            }
        },
    )
}

/** The "/" key at the end of the answer field of a fraction question (48 dp touch target, like the other round buttons). */
@Composable
private fun SlashKey(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_insert_slash)
    Box(
        modifier = modifier
            .size(48.dp)
            .bouncyClickable(CircleShape, pressedScale = 0.88f, onClick = onClick)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.slash_key),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp, letterSpacing = 0.sp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/** Shows a `%` after whatever is typed, so the answer `33.33` reads `33.33%` without the player typing the sign. */
private class PercentSuffix(private val sign: SpanStyle) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val shown = buildAnnotatedString {
            append(text)
            pushStyle(sign)
            append("%")
            pop()
        }
        return TransformedText(
            text = shown,
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = offset

                // The sign is only ever at the very end, so a cursor on it belongs to the end of what was typed.
                override fun transformedToOriginal(offset: Int): Int = offset.coerceAtMost(text.length)
            },
        )
    }
}
