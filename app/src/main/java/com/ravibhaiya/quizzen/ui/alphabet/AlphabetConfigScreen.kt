package com.ravibhaiya.quizzen.ui.alphabet

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.AlphabetChallenge
import com.ravibhaiya.quizzen.domain.AlphabetRules
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.ChoiceCard
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.QuizzenRangeSlider
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.SectionTitle
import com.ravibhaiya.quizzen.ui.components.TimerFooter
import com.ravibhaiya.quizzen.ui.components.cssShadow
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
                LetterRangeCard(
                    range = state.letters,
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

/**
 * The letter range, laid out like the Powers & Roots range cards: the letter picked with the left thumb in a pill at the left, the
 * letter picked with the right thumb in a pill at the right, and one two-thumb slider (A at one end, Z at the other) under them.
 */
@Composable
private fun LetterRangeCard(
    range: IntRange,
    onRangeChange: (IntRange) -> Unit,
    onRangeChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val first = AlphabetRules.letterOf(range.first).toString()
    val last = AlphabetRules.letterOf(range.last).toString()
    val summary = stringResource(R.string.letter_range_summary, first, last)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .cssShadow(
                NeutralShadowColor.copy(alpha = 0.05f),
                offsetY = 2.dp, blur = 6.dp, spread = 0.dp, shape = shape,
            )
            .background(colors.surfaceContainer, shape)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = summary },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LetterPill(first)
            LetterPill(last)
        }
        QuizzenRangeSlider(
            range = range,
            lowest = AlphabetRules.FIRST,
            highest = AlphabetRules.SIZE,
            onRangeChange = onRangeChange,
            onRangeChangeFinished = onRangeChangeFinished,
            startThumbDescription = stringResource(R.string.letter_thumb_from),
            endThumbDescription = stringResource(R.string.letter_thumb_to),
            modifier = Modifier.padding(top = 12.dp),
        )
        // The two end letters line up with the ends of the track (half a handle in from the card edge).
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val endStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
            Text(AlphabetRules.letterOf(AlphabetRules.FIRST).toString(), style = endStyle, color = colors.onSurfaceVariant)
            Text(AlphabetRules.letterOf(AlphabetRules.SIZE).toString(), style = endStyle, color = colors.onSurfaceVariant)
        }
    }
}

/** A picked letter in a pill: `primaryContainer`, fully rounded, wide enough that every letter gets the same pill. */
@Composable
private fun LetterPill(letter: String) {
    Box(
        modifier = Modifier
            .widthIn(min = 56.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@StringRes
private fun AlphabetChallenge.titleRes(): Int = when (this) {
    AlphabetChallenge.FindPosition -> R.string.challenge_find_position
    AlphabetChallenge.FindLetter -> R.string.challenge_find_letter
    AlphabetChallenge.ReverseLetter -> R.string.challenge_reverse_letter
}
