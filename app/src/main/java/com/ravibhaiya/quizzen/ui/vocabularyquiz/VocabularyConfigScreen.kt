package com.ravibhaiya.quizzen.ui.vocabularyquiz

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.VocabularyQuizConfig
import com.ravibhaiya.quizzen.domain.VocabularyQuizMode
import com.ravibhaiya.quizzen.domain.VocabularyQuizRules
import com.ravibhaiya.quizzen.ui.components.ChoiceCard
import com.ravibhaiya.quizzen.ui.components.PillSliderCard
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.SectionTitle
import com.ravibhaiya.quizzen.ui.components.TimerFooter
import com.ravibhaiya.quizzen.ui.components.rememberHaptics

@Composable
fun VocabularyConfigScreen(
    hapticEnabled: Boolean,
    onBack: () -> Unit,
    onStart: (VocabularyQuizConfig) -> Unit,
    viewModel: VocabularyConfigViewModel = viewModel(factory = VocabularyConfigViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val timer by viewModel.timer.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(hapticEnabled)

    QuizzenScreen {
        val shown by animateFloatAsState(if (state.loaded) 1f else 0f, tween(120), label = "vocabConfigShown")
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = shown }
                .statusBarsPadding()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 34.dp),
            ) {
                ScreenHeader(
                    title = stringResource(R.string.feature_vocabulary),
                    onBack = { haptics.click(); onBack() },
                )
                Text(
                    text = stringResource(R.string.configure_challenge),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 20.dp),
                )

                SectionTitle(R.string.vocabulary_word_count_title)
                PillSliderCard(
                    value = state.wordCount,
                    lowest = VocabularyQuizRules.MIN_WORDS,
                    highest = VocabularyQuizRules.MAX_WORDS,
                    valueLabel = state.wordCount.toString(),
                    lowestLabel = VocabularyQuizRules.MIN_WORDS.toString(),
                    highestLabel = VocabularyQuizRules.MAX_WORDS.toString(),
                    summary = stringResource(R.string.vocabulary_word_count_summary, state.wordCount),
                    thumbDescription = stringResource(R.string.vocabulary_word_count_thumb),
                    onValueChange = viewModel::onWordCountChanged,
                    onValueChangeFinished = { haptics.tick() },
                    modifier = Modifier
                        .padding(bottom = 28.dp)
                        .testTag("vocabulary_word_count_slider"),
                )

                SectionTitle(R.string.vocabulary_quiz_mode_title)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    VocabularyQuizMode.entries.forEach { mode ->
                        ChoiceCard(
                            title = stringResource(mode.titleRes()),
                            selected = mode in state.modes,
                            onClick = { haptics.tick(); viewModel.toggleMode(mode) },
                            multiSelect = true,
                            modifier = Modifier.testTag("vocabulary_mode_${mode.code}"),
                        )
                    }
                }
            }

            TimerFooter(
                timer = timer,
                onTimerChange = viewModel::onTimerChanged,
                onTimerFocusLost = viewModel.timer::onFocusLost,
                startEnabled = true,
                onStart = { haptics.heavyClick(); onStart(viewModel.startQuiz()) },
            )
        }
    }
}

@StringRes
private fun VocabularyQuizMode.titleRes(): Int = when (this) {
    VocabularyQuizMode.Synonyms -> R.string.vocabulary_mode_synonyms
    VocabularyQuizMode.Antonyms -> R.string.vocabulary_mode_antonyms
}
