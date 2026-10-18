package com.ravibhaiya.quizzen.ui.vocabularyquiz

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.rememberHaptics

@Composable
fun VocabularyQuizScreen(
    hapticEnabled: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VocabularyQuizViewModel = viewModel(factory = VocabularyQuizViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(hapticEnabled)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onResume()
                Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onPause()
        }
    }

    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(state.shakeCount) {
        if (state.shakeCount > 0) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    (-6f) at 70
                    6f at 140
                    (-4f) at 210
                    4f at 280
                    0f at 350
                },
            )
        }
    }

    LaunchedEffect(state.feedback?.type, state.currentIndex) {
        when (state.feedback?.type) {
            FeedbackType.Correct -> haptics.success()
            FeedbackType.Incorrect -> haptics.error()
            FeedbackType.Timeout -> haptics.timeout()
            null -> Unit
        }
    }

    LaunchedEffect(state.remainingSeconds, state.isTimerLow, state.isLocked, state.phase) {
        if (state.phase == VocabularyQuizPhase.Quiz &&
            !state.isLocked &&
            state.isTimerLow &&
            state.remainingSeconds > 0
        ) {
            haptics.tick()
        }
    }

    QuizzenScreen(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state.phase) {
                VocabularyQuizPhase.Study -> VocabularyStudyView(
                    entries = state.studyEntries,
                    onBack = {
                        haptics.click()
                        onBack()
                    },
                    onShuffleWords = {
                        haptics.click()
                        viewModel.shuffleStudyWords()
                    },
                    onStartQuiz = {
                        haptics.heavyClick()
                        viewModel.startQuiz()
                    },
                )

                VocabularyQuizPhase.Quiz -> VocabularyActiveQuizView(
                    state = state,
                    shakeOffsetDp = { shakeOffset.value },
                    onBack = {
                        haptics.click()
                        onBack()
                    },
                    onSelectOption = { option ->
                        haptics.click()
                        viewModel.selectOption(option)
                    },
                    onNextQuestion = {
                        haptics.click()
                        viewModel.nextQuestion()
                    },
                )

                VocabularyQuizPhase.Finished -> VocabularyFinishedView(
                    state = state,
                    onBack = {
                        haptics.click()
                        onBack()
                    },
                    onRetrySameWords = {
                        haptics.heavyClick()
                        viewModel.retryQuiz()
                    },
                    onReviewStudyWords = {
                        haptics.click()
                        viewModel.backToStudy()
                    },
                )
            }
        }
    }
}
