package com.ravibhaiya.quizzen.ui.alphabet

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.AlphabetChallenge
import com.ravibhaiya.quizzen.domain.AlphabetRules
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.ChoiceCard
import com.ravibhaiya.quizzen.ui.components.PillRangeCard
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.SectionTitle
import com.ravibhaiya.quizzen.ui.components.TimerFooter
import com.ravibhaiya.quizzen.ui.components.rememberHaptics

@Composable
fun AlphabetConfigScreen(
    hapticEnabled: Boolean,
    onBack: () -> Unit,
    onStart: (PracticeConfig.Alphabet) -> Unit,
    viewModel: AlphabetConfigViewModel = viewModel(factory = AlphabetConfigViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val timer by viewModel.timer.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(hapticEnabled)

    QuizzenScreen {
        // Wait (a few milliseconds) for the saved setting so the screen never flashes the defaults first.
        val shown by animateFloatAsState(if (state.loaded) 1f else 0f, tween(120), label = "configShown")
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
                    title = stringResource(R.string.alphabet_title),
                    onBack = { haptics.click(); onBack() },
                )
                Text(
                    text = stringResource(R.string.configure_challenge),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 20.dp),
                )

                SectionTitle(R.string.letter_range)
                val first = AlphabetRules.letterOf(state.letters.first).toString()
                val last = AlphabetRules.letterOf(state.letters.last).toString()
                PillRangeCard(
                    range = state.letters,
                    lowest = AlphabetRules.FIRST,
                    highest = AlphabetRules.SIZE,
                    firstLabel = first,
                    lastLabel = last,
                    lowestLabel = AlphabetRules.letterOf(AlphabetRules.FIRST).toString(),
                    highestLabel = AlphabetRules.letterOf(AlphabetRules.SIZE).toString(),
                    summary = stringResource(R.string.letter_range_summary, first, last),
                    startThumbDescription = stringResource(R.string.letter_thumb_from),
                    endThumbDescription = stringResource(R.string.letter_thumb_to),
                    onRangeChange = viewModel::onLettersChanged,
                    onRangeChangeFinished = { haptics.tick() },
                    modifier = Modifier.padding(bottom = 28.dp),
                )

                SectionTitle(R.string.challenge_type)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AlphabetChallenge.entries.forEach { challenge ->
                        ChoiceCard(
                            title = stringResource(challenge.titleRes()),
                            selected = challenge == state.challenge,
                            onClick = { haptics.tick(); viewModel.selectChallenge(challenge) },
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
private fun AlphabetChallenge.titleRes(): Int = when (this) {
    AlphabetChallenge.FindPosition -> R.string.challenge_find_position
    AlphabetChallenge.FindLetter -> R.string.challenge_find_letter
    AlphabetChallenge.ReverseLetter -> R.string.challenge_reverse_letter
}
