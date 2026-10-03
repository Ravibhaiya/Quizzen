package com.ravibhaiya.quizzen.ui.powers

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.PowerRootType
import com.ravibhaiya.quizzen.domain.PowersRootsRules
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.NumberField
import com.ravibhaiya.quizzen.ui.components.OptionChip
import com.ravibhaiya.quizzen.ui.components.QuizzenIcons
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.TimerFooter
import com.ravibhaiya.quizzen.ui.components.rememberHaptics
import com.ravibhaiya.quizzen.ui.theme.quizzen

@Composable
fun PowersRootsConfigScreen(
    hapticEnabled: Boolean,
    onBack: () -> Unit,
    onStart: (PracticeConfig.PowersRoots) -> Unit,
    viewModel: PowersRootsConfigViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val timer by viewModel.timer.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(hapticEnabled)

    QuizzenScreen {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
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
                NumberRange(
                    state = state,
                    onMinChanged = viewModel::onMinChanged,
                    onMaxChanged = viewModel::onMaxChanged,
                    onMinFocusLost = viewModel::onMinFocusLost,
                    onMaxFocusLost = viewModel::onMaxFocusLost,
                )
            }

            TimerFooter(
                timer = timer,
                onTimerChange = viewModel.timer::onTextChanged,
                onTimerFocusLost = viewModel.timer::onFocusLost,
                startEnabled = state.canStart,
                onStart = { haptics.heavyClick(); onStart(viewModel.buildConfig()) },
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
private fun NumberRange(
    state: PowersRootsConfigUiState,
    onMinChanged: (String) -> Unit,
    onMaxChanged: (String) -> Unit,
    onMinFocusLost: () -> Unit,
    onMaxFocusLost: () -> Unit,
) {
    Text(
        text = stringResource(R.string.number_range),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 14.dp),
    )
    val hasError = state.limitExceeded || state.showIssue
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        NumberField(
            label = stringResource(R.string.min_number),
            value = state.minText,
            onValueChange = onMinChanged,
            onFocusLost = onMinFocusLost,
            isError = hasError,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = QuizzenIcons.ArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 24.dp).size(24.dp),
        )
        NumberField(
            label = stringResource(R.string.max_number),
            value = state.maxText,
            onValueChange = onMaxChanged,
            onFocusLost = onMaxFocusLost,
            imeAction = ImeAction.Done,
            isError = hasError,
            modifier = Modifier.weight(1f),
        )
    }

    val error = rangeError(state)
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn(tween(150)) + expandVertically(tween(150)),
        exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
    ) {
        Text(
            text = error.orEmpty(),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.6.sp),
            color = MaterialTheme.quizzen.error,
            modifier = Modifier.padding(start = 4.dp, top = 12.dp),
        )
    }
    LimitCards(
        models = limitCardModels(state.types, state.min, state.max),
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun rangeError(state: PowersRootsConfigUiState): String? = when {
    state.limitExceeded -> stringResource(R.string.range_error_limit, PowersRootsRules.MAX_ALLOWED)
    !state.showIssue -> null
    else -> when (state.issue) {
        PowersRootsRules.Issue.InvalidNumber ->
            stringResource(R.string.range_error_invalid, PowersRootsRules.MIN_ALLOWED, PowersRootsRules.MAX_ALLOWED)
        PowersRootsRules.Issue.MinAboveMax -> stringResource(R.string.range_error_min_above_max)
        PowersRootsRules.Issue.BeyondCubeLimit -> stringResource(R.string.range_error_cube_limit, PowersRootsRules.CUBE_LIMIT)
        PowersRootsRules.Issue.None -> null
    }
}

@StringRes
private fun PowerRootType.labelRes(): Int = when (this) {
    PowerRootType.Squares -> R.string.type_squares
    PowerRootType.Cubes -> R.string.type_cubes
    PowerRootType.SquareRoots -> R.string.type_square_roots
    PowerRootType.CubeRoots -> R.string.type_cube_roots
}
