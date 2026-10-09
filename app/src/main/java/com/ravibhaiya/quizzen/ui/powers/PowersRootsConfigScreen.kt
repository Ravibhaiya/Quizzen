package com.ravibhaiya.quizzen.ui.powers

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.PowerRootType
import com.ravibhaiya.quizzen.domain.PowersRootsRules
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.OptionChip
import com.ravibhaiya.quizzen.ui.components.QuizzenRangeSlider
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.TimerFooter
import com.ravibhaiya.quizzen.ui.components.cssShadow
import com.ravibhaiya.quizzen.ui.components.rememberHaptics
import com.ravibhaiya.quizzen.ui.theme.quizzen

@Composable
fun PowersRootsConfigScreen(
    hapticEnabled: Boolean,
    onBack: () -> Unit,
    onStart: (PracticeConfig.PowersRoots) -> Unit,
    viewModel: PowersRootsConfigViewModel = viewModel(factory = PowersRootsConfigViewModel.Factory),
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
                    title = stringResource(R.string.feature_powers_roots),
                    onBack = { haptics.click(); onBack() },
                )
                Text(
                    text = stringResource(R.string.powers_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 20.dp),
                )

                PracticeTypes(
                    selected = state.types,
                    onToggle = { haptics.tick(); viewModel.toggleType(it) },
                )
                NumberRanges(
                    state = state,
                    onRangeChanged = viewModel::onRangeChanged,
                    onRangeChangeFinished = { haptics.tick() },
                )
            }

            TimerFooter(
                timer = timer,
                onTimerChange = viewModel::onTimerChanged,
                onTimerFocusLost = viewModel.timer::onFocusLost,
                startEnabled = state.canStart,
                onStart = { haptics.heavyClick(); onStart(viewModel.startQuiz()) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeTypes(selected: Set<PowerRootType>, onToggle: (PowerRootType) -> Unit) {
    Text(
        text = stringResource(R.string.practice_types),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 14.dp),
    )
    FlowRow(
        modifier = Modifier.padding(bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PowerRootType.entries.forEach { type ->
            OptionChip(
                label = stringResource(type.labelRes()),
                selected = type in selected,
                onClick = { onToggle(type) },
                role = Role.Checkbox,
            )
        }
    }
}

@Composable
private fun NumberRanges(
    state: PowersRootsConfigUiState,
    onRangeChanged: (PowerRootType.Family, IntRange) -> Unit,
    onRangeChangeFinished: () -> Unit,
) {
    Text(
        text = stringResource(R.string.number_range),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 14.dp),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(PowerRootType.Family.Square, PowerRootType.Family.Cube).forEach { family ->
            RangeCard(
                family = family,
                range = state.rangeOf(family),
                inUse = state.isInUse(family),
                onRangeChange = { onRangeChanged(family, it) },
                onRangeChangeFinished = onRangeChangeFinished,
            )
        }
    }
}

/**
 * One slider for one kind of question. The slider's own ends are the limit (shown as the two small numbers under it), and
 * the pill shows the range in use, so nothing else is needed to explain the limits. When other kinds are chosen but not this
 * one the card fades and cannot be dragged.
 */
@Composable
private fun RangeCard(
    family: PowerRootType.Family,
    range: IntRange,
    inUse: Boolean,
    onRangeChange: (IntRange) -> Unit,
    onRangeChangeFinished: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val limit = PowersRootsRules.limitOf(family)
    val fade by animateFloatAsState(if (inUse) 1f else 0.45f, tween(200), label = "rangeCardFade")
    val title = stringResource(
        if (family == PowerRootType.Family.Square) R.string.range_title_squares else R.string.range_title_cubes,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = fade }
            .cssShadow(
                NeutralShadowColor.copy(alpha = 0.05f),
                offsetY = 2.dp, blur = 6.dp, spread = 0.dp, shape = shape,
            )
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.range_value, range.first, range.last),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        QuizzenRangeSlider(
            range = range,
            lowest = PowersRootsRules.MIN_ALLOWED,
            highest = limit,
            onRangeChange = onRangeChange,
            onRangeChangeFinished = onRangeChangeFinished,
            startThumbDescription = stringResource(R.string.range_thumb_min, title),
            endThumbDescription = stringResource(R.string.range_thumb_max, title),
            enabled = inUse,
            modifier = Modifier.padding(top = 6.dp),
        )
        // The two end numbers line up with the ends of the track (half a handle in from the card edge).
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val endStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
            Text(PowersRootsRules.MIN_ALLOWED.toString(), style = endStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(limit.toString(), style = endStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@StringRes
private fun PowerRootType.labelRes(): Int = when (this) {
    PowerRootType.Squares -> R.string.type_squares
    PowerRootType.Cubes -> R.string.type_cubes
    PowerRootType.SquareRoots -> R.string.type_square_roots
    PowerRootType.CubeRoots -> R.string.type_cube_roots
}
