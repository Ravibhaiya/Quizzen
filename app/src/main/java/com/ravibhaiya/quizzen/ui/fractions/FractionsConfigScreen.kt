package com.ravibhaiya.quizzen.ui.fractions

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.FractionChallenge
import com.ravibhaiya.quizzen.domain.FractionRules
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.ChoiceCard
import com.ravibhaiya.quizzen.ui.components.PillRangeCard
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.SectionTitle
import com.ravibhaiya.quizzen.ui.components.TimerFooter
import com.ravibhaiya.quizzen.ui.components.rememberHaptics

/** Setup of Fraction & Percentage: which fractions (a two-thumb range, 1/2 to 1/50), what to answer in (one or both) and the timer. */
@Composable
fun FractionsConfigScreen(
    hapticEnabled: Boolean,
    onBack: () -> Unit,
    onStart: (PracticeConfig.Fractions) -> Unit,
    viewModel: FractionsConfigViewModel = viewModel(factory = FractionsConfigViewModel.Factory),
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
                    title = stringResource(R.string.feature_fraction_percentage),
                    onBack = { haptics.click(); onBack() },
                )
                Text(
                    text = stringResource(R.string.configure_challenge),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 20.dp),
                )

                SectionTitle(R.string.fraction_range)
                val first = "1/${FractionRules.denominatorAt(state.range.first)}"
                val last = "1/${FractionRules.denominatorAt(state.range.last)}"
                PillRangeCard(
                    range = state.range,
                    lowest = FractionRules.FIRST,
                    highest = FractionRules.SIZE,
                    firstLabel = first,
                    lastLabel = last,
                    lowestLabel = "1/${FractionRules.denominatorAt(FractionRules.FIRST)}",
                    highestLabel = "1/${FractionRules.denominatorAt(FractionRules.SIZE)}",
                    summary = stringResource(R.string.fraction_range_summary, first, last),
                    startThumbDescription = stringResource(R.string.fraction_thumb_from),
                    endThumbDescription = stringResource(R.string.fraction_thumb_to),
                    onRangeChange = viewModel::onRangeChanged,
                    onRangeChangeFinished = { haptics.tick() },
                    modifier = Modifier.padding(bottom = 28.dp),
                )

                SectionTitle(R.string.answer_in)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FractionChallenge.entries.forEach { challenge ->
                        ChoiceCard(
                            title = stringResource(challenge.titleRes()),
                            selected = challenge in state.challenges,
                            onClick = { haptics.tick(); viewModel.toggleChallenge(challenge) },
                            multiSelect = true,
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
private fun FractionChallenge.titleRes(): Int = when (this) {
    FractionChallenge.Fraction -> R.string.answer_in_fraction
    FractionChallenge.Percentage -> R.string.answer_in_percentage
}
