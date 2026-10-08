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
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.ui.components.FitText
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.PrimaryButton
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
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
                FitText(
                    text = questionText(state.question),
                    style = MaterialTheme.typography.displayLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    maxFontSize = 49.6.sp,
                    minFontSize = 24.sp,
                    step = 1.6.sp,
                    modifier = Modifier
                        .padding(top = 46.dp, bottom = 46.dp)
                        .clearAndSetSemantics { contentDescription = spoken },
                )

                AnswerField(
                    value = state.answer,
                    hint = answerHint(state.question),
                    kind = state.question.answerKind,
                    onValueChange = viewModel::onAnswerChanged,
                    onDone = viewModel::onCheck,
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

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
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
        keyboardOptions = if (kind == AnswerKind.Letter) {
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            )
        } else {
            KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
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
        },
    )
}
