package com.ravibhaiya.quizzen.ui.multiply

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.OptionChip
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.TimerFooter
import com.ravibhaiya.quizzen.ui.components.rememberHaptics

@Composable
fun MultiplyConfigScreen(
    hapticEnabled: Boolean,
    onBack: () -> Unit,
    onStart: (PracticeConfig.Multiply) -> Unit,
    viewModel: MultiplyConfigViewModel = viewModel(),
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
                ScreenHeader(title = stringResource(R.string.multiply_title), onBack = onBack)

                Text(
                    text = stringResource(R.string.configure_challenge),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 20.dp),
                )

                DigitSection(
                    title = stringResource(R.string.digits_first_number),
                    selected = state.firstDigits,
                    onSelect = { haptics.tick(); viewModel.selectFirstDigits(it) },
                )
                DigitSection(
                    title = stringResource(R.string.digits_second_number),
                    selected = state.secondDigits,
                    onSelect = { haptics.tick(); viewModel.selectSecondDigits(it) },
                )
            }

            TimerFooter(
                timer = timer,
                onTimerChange = viewModel.timer::onTextChanged,
                onTimerFocusLost = viewModel.timer::onFocusLost,
                startEnabled = true,
                onStart = { onStart(viewModel.buildConfig()) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DigitSection(title: String, selected: Int, onSelect: (Int) -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 14.dp),
    )
    FlowRow(
        modifier = Modifier.padding(bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MultiplyConfigViewModel.DIGIT_OPTIONS.forEach { digits ->
            OptionChip(
                label = stringResource(R.string.digits_option, digits),
                selected = digits == selected,
                onClick = { onSelect(digits) },
            )
        }
    }
}
