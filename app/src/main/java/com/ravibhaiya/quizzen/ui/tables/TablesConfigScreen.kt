package com.ravibhaiya.quizzen.ui.tables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.NumberCell
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.SelectAllButton
import com.ravibhaiya.quizzen.ui.components.TimerFooter
import com.ravibhaiya.quizzen.ui.components.rememberHaptics

private const val GRID_COLUMNS = 4

@Composable
fun TablesConfigScreen(
    hapticEnabled: Boolean,
    onBack: () -> Unit,
    onStart: (PracticeConfig.Tables) -> Unit,
    viewModel: TablesConfigViewModel = viewModel(),
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
                    title = stringResource(R.string.tables_title),
                    onBack = { haptics.click(); onBack() },
                )

                Text(
                    text = stringResource(R.string.select_numbers_to_practice),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 20.dp),
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SelectAllButton(
                        label = stringResource(R.string.select_all),
                        active = state.allSelected,
                        onClick = { haptics.click(); viewModel.toggleAll() },
                    )
                }

                Column(
                    modifier = Modifier.padding(top = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PracticeConfig.Tables.AVAILABLE_NUMBERS.chunked(GRID_COLUMNS).forEach { rowNumbers ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowNumbers.forEach { number ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    NumberCell(
                                        number = number,
                                        selected = number in state.selected,
                                        onClick = { haptics.tick(); viewModel.toggle(number) },
                                    )
                                }
                            }
                            // Keep the last, shorter row aligned to the same column widths.
                            repeat(GRID_COLUMNS - rowNumbers.size) { Box(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            TimerFooter(
                timer = timer,
                onTimerChange = viewModel.timer::onTextChanged,
                onTimerFocusLost = viewModel.timer::onFocusLost,
                startEnabled = state.selected.isNotEmpty(),
                onStart = { haptics.heavyClick(); onStart(viewModel.buildConfig()) },
            )
        }
    }
}
