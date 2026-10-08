package com.ravibhaiya.quizzen.ui.alphabet

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.AlphabetChallenge
import com.ravibhaiya.quizzen.domain.AlphabetRules
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.QuizzenIcons
import com.ravibhaiya.quizzen.ui.components.QuizzenRangeSlider
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.TimerFooter
import com.ravibhaiya.quizzen.ui.components.cssShadow
import com.ravibhaiya.quizzen.ui.components.pressScale
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
                Column(
                    modifier = Modifier.padding(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AlphabetChallenge.entries.forEach { challenge ->
                        ChallengeCard(
                            title = stringResource(challenge.titleRes()),
                            selected = challenge == state.challenge,
                            onClick = { haptics.tick(); viewModel.selectChallenge(challenge) },
                        )
                    }
                }

                TipCard(text = stringResource(state.challenge.tipRes()))
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

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 14.dp),
    )
}

/**
 * The letter range: the first letter at the left and the last letter at the right as two small tiles with an arrow between them, and one two-thumb slider under
 * them that moves both. The slider's ends are A and Z (the small letters under it), the tiles show what is chosen right now.
 */
@Composable
private fun LetterRangeCard(
    range: IntRange,
    onRangeChange: (IntRange) -> Unit,
    onRangeChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .cssShadow(
                NeutralShadowColor.copy(alpha = 0.05f),
                offsetY = 2.dp, blur = 6.dp, spread = 0.dp, shape = shape,
            )
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LetterTile(
                label = stringResource(R.string.letter_from),
                letter = AlphabetRules.letterOf(range.first),
            )
            Icon(
                imageVector = QuizzenIcons.ArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            LetterTile(
                label = stringResource(R.string.letter_to),
                letter = AlphabetRules.letterOf(range.last),
            )
        }
        QuizzenRangeSlider(
            range = range,
            lowest = AlphabetRules.FIRST,
            highest = AlphabetRules.SIZE,
            onRangeChange = onRangeChange,
            onRangeChangeFinished = onRangeChangeFinished,
            startThumbDescription = stringResource(R.string.letter_thumb_from),
            endThumbDescription = stringResource(R.string.letter_thumb_to),
            modifier = Modifier.padding(top = 10.dp),
        )
        // The two end letters line up with the ends of the track (half a thumb in from the card edge).
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val endStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
            Text(AlphabetRules.letterOf(AlphabetRules.FIRST).toString(), style = endStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(AlphabetRules.letterOf(AlphabetRules.SIZE).toString(), style = endStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The chosen letter in the same small box as the timer's value (letter on top, FROM / TO under it). Read as one item. */
@Composable
private fun LetterTile(label: String, letter: Char, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(min = 64.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * One full-width choice (single select), just its name. Same selection language as the option chips and the table cells: primary
 * fill with a soft coloured shadow when chosen, plain `surfaceContainer` otherwise; a radio mark on the left says "pick one".
 */
@Composable
private fun ChallengeCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val container by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.surfaceContainer,
        animationSpec = tween(200),
        label = "challengeContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.onSurface,
        animationSpec = tween(200),
        label = "challengeContent",
    )
    val glow by animateColorAsState(
        targetValue = colors.primary.copy(alpha = if (selected) 0.55f else 0f),
        animationSpec = tween(150),
        label = "challengeShadow",
    )
    val source = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source)
            .cssShadow(glow, offsetY = 8.dp, blur = 16.dp, spread = (-6).dp, shape = shape)
            .clip(shape)
            .background(container)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { this.selected = selected }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RadioMark(selected = selected, color = content)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.32.sp, fontWeight = FontWeight.Bold),
            color = content,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RadioMark(selected: Boolean, color: Color) {
    Box(
        modifier = Modifier.size(24.dp).border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(12.dp).background(color, CircleShape))
    }
}

/** A short hint for the chosen challenge, so the setup screen explains the rule without a separate help page. */
@Composable
private fun TipCard(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.tip_title).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.4.sp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@StringRes
private fun AlphabetChallenge.titleRes(): Int = when (this) {
    AlphabetChallenge.FindPosition -> R.string.challenge_find_position
    AlphabetChallenge.FindLetter -> R.string.challenge_find_letter
    AlphabetChallenge.ReverseLetter -> R.string.challenge_reverse_letter
}

@StringRes
private fun AlphabetChallenge.tipRes(): Int = when (this) {
    AlphabetChallenge.FindPosition -> R.string.tip_find_position
    AlphabetChallenge.FindLetter -> R.string.tip_find_letter
    AlphabetChallenge.ReverseLetter -> R.string.tip_reverse_letter
}
