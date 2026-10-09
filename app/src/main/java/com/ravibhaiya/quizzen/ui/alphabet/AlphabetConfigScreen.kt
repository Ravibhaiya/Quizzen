package com.ravibhaiya.quizzen.ui.alphabet

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.TimerFooter
import com.ravibhaiya.quizzen.ui.components.cssShadow
import com.ravibhaiya.quizzen.ui.components.pressScale
import com.ravibhaiya.quizzen.ui.components.rememberHaptics

private const val GRID_COLUMNS = 7

/** The quick ranges under the letter grid: the whole alphabet and its two halves. */
private val LetterPresets = listOf(AlphabetRules.FULL, 1..13, 14..26)

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
                    onLetterTap = { haptics.tick(); viewModel.onLetterTapped(it) },
                    onPreset = { haptics.tick(); viewModel.onLettersChanged(it) },
                    modifier = Modifier.padding(bottom = 28.dp),
                )

                SectionTitle(R.string.challenge_type)
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AlphabetChallenge.entries.forEach { challenge ->
                        ChallengeTile(
                            glyph = challenge.glyph(),
                            title = stringResource(challenge.titleRes()),
                            selected = challenge == state.challenge,
                            onClick = { haptics.tick(); viewModel.selectChallenge(challenge) },
                            modifier = Modifier.weight(1f),
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
 * The letter range as a picture of the alphabet: a big "A -> X" summary with the number of letters, the 26 letters as a grid where
 * the chosen range is lit up (the two ends solid, the letters between them soft), and three quick presets. Tapping a letter moves
 * an end of the range (see [AlphabetRules.pick]), so the screen is the control.
 */
@Composable
private fun LetterRangeCard(
    range: IntRange,
    onLetterTap: (Int) -> Unit,
    onPreset: (IntRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val first = AlphabetRules.letterOf(range.first).toString()
    val last = AlphabetRules.letterOf(range.last).toString()
    val count = range.last - range.first + 1
    val summary = stringResource(R.string.letter_range_summary, first, last)
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .cssShadow(
                NeutralShadowColor.copy(alpha = 0.05f),
                offsetY = 2.dp, blur = 6.dp, spread = 0.dp, shape = shape,
            )
            .background(colors.surfaceContainer, shape)
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = summary },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val big = MaterialTheme.typography.headlineMedium.copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text(first, style = big, color = colors.onSurface)
                Icon(
                    imageVector = QuizzenIcons.ArrowRight,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp),
                )
                Text(last, style = big, color = colors.onSurface)
            }
            Text(
                text = LocalContext.current.resources.getQuantityString(R.plurals.letters_count, count, count),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.onPrimaryContainer,
                modifier = Modifier
                    .background(colors.primaryContainer, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        LetterGrid(range = range, onLetterTap = onLetterTap)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LetterPresets.forEach { preset ->
                PresetPill(
                    text = "${AlphabetRules.letterOf(preset.first)}–${AlphabetRules.letterOf(preset.last)}",
                    selected = preset == range,
                    onClick = { onPreset(preset) },
                )
            }
        }
    }
}

@Composable
private fun LetterGrid(range: IntRange, onLetterTap: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AlphabetRules.FULL.chunked(GRID_COLUMNS).forEach { rowPositions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowPositions.forEach { position ->
                    LetterCell(position = position, range = range, onClick = { onLetterTap(position) }, modifier = Modifier.weight(1f))
                }
                // The last row is shorter: keep its cells the same size as the rows above.
                repeat(GRID_COLUMNS - rowPositions.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** One letter. Ends of the range: primary fill with a soft glow. Between them: `primaryContainer`. Outside: quiet. */
@Composable
private fun LetterCell(position: Int, range: IntRange, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    val isEnd = position == range.first || position == range.last
    val isInside = position in range
    val container by animateColorAsState(
        targetValue = when {
            isEnd -> colors.primary
            isInside -> colors.primaryContainer
            else -> colors.surfaceContainerHigh
        },
        animationSpec = tween(160),
        label = "letterContainer",
    )
    val content by animateColorAsState(
        targetValue = when {
            isEnd -> colors.onPrimary
            isInside -> colors.onPrimaryContainer
            else -> colors.onSurfaceVariant
        },
        animationSpec = tween(160),
        label = "letterContent",
    )
    val glow by animateColorAsState(
        targetValue = colors.primary.copy(alpha = if (isEnd) 0.5f else 0f),
        animationSpec = tween(160),
        label = "letterGlow",
    )
    val source = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .pressScale(source)
            .cssShadow(glow, offsetY = 6.dp, blur = 12.dp, spread = (-4).dp, shape = shape)
            .clip(shape)
            .background(container)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { this.selected = isInside },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = AlphabetRules.letterOf(position).toString(),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            color = content,
        )
    }
}

@Composable
private fun PresetPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.surfaceContainerHigh,
        animationSpec = tween(160),
        label = "presetContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        animationSpec = tween(160),
        label = "presetContent",
    )
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(50)
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = content,
        modifier = Modifier
            .pressScale(source)
            .clip(shape)
            .background(container)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * One of the three challenges as a tile: a big symbol of what it does on top, its name under it. Single select, so the chosen tile
 * uses the chip language (primary fill, `onPrimary` text, soft coloured shadow) and the others stay plain `surfaceContainer`.
 */
@Composable
private fun ChallengeTile(
    glyph: String,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
    val glyphBadge by animateColorAsState(
        targetValue = if (selected) colors.onPrimary.copy(alpha = 0.2f) else colors.surfaceContainerHigh,
        animationSpec = tween(200),
        label = "challengeBadge",
    )
    val glow by animateColorAsState(
        targetValue = colors.primary.copy(alpha = if (selected) 0.55f else 0f),
        animationSpec = tween(150),
        label = "challengeShadow",
    )
    val source = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
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
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .background(glyphBadge, CircleShape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = glyph,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold),
                color = content,
                maxLines = 1,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.6.sp, fontWeight = FontWeight.Bold),
            color = content,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
        )
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

/** A tiny symbol of the challenge (what you see -> what you type). Symbols, not words, so they need no translation. */
private fun AlphabetChallenge.glyph(): String = when (this) {
    AlphabetChallenge.FindPosition -> "A→1"
    AlphabetChallenge.FindLetter -> "1→A"
    AlphabetChallenge.ReverseLetter -> "A→Z"
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
