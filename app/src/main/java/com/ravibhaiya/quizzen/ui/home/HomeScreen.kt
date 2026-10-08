package com.ravibhaiya.quizzen.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.ui.components.EmphasizedEasing
import com.ravibhaiya.quizzen.ui.components.QuizzenIconButton
import com.ravibhaiya.quizzen.ui.components.QuizzenIcons
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.SegmentItem
import com.ravibhaiya.quizzen.ui.components.SegmentedControl
import com.ravibhaiya.quizzen.ui.components.rememberHaptics
import com.ravibhaiya.quizzen.ui.theme.QuizzenShapes
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    hapticEnabled: Boolean,
    onHapticChange: (Boolean) -> Unit,
    onOpenMultiply: () -> Unit,
    onOpenTables: () -> Unit,
    onOpenPowers: () -> Unit,
    onOpenAlphabet: () -> Unit,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = rememberHaptics(hapticEnabled)
    val confirmHaptics = rememberHaptics(true)
    val comingSoon = stringResource(R.string.coming_soon)
    val notify: () -> Unit = {
        haptics.tick()
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(comingSoon)
        }
    }

    val tabs = listOf(
        SegmentItem(stringResource(R.string.tab_math), QuizzenIcons.Calculator),
        SegmentItem(stringResource(R.string.tab_language), QuizzenIcons.Globe),
    )

    QuizzenScreen {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 24.dp)) {
                HomeHeader(onSettings = { haptics.click(); showSettings = true })
                Spacer(Modifier.height(22.dp))
                SegmentedControl(
                    items = tabs,
                    selectedIndex = pagerState.currentPage,
                    position = { (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f) },
                    onSelect = { index ->
                        haptics.tick()
                        scope.launch {
                            pagerState.animateScrollToPage(index, animationSpec = tween(400, easing = EmphasizedEasing))
                        }
                    },
                )
                Spacer(Modifier.height(22.dp))
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp),
                pageSpacing = 18.dp,
                verticalAlignment = Alignment.Top,
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 34.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (page == 0) {
                        MathPage(
                            onOpenMultiply = { haptics.click(); onOpenMultiply() },
                            onOpenTables = { haptics.click(); onOpenTables() },
                            onOpenPowers = { haptics.click(); onOpenPowers() },
                            onOpenAlphabet = { haptics.click(); onOpenAlphabet() },
                            onComingSoon = notify,
                        )
                    } else {
                        LanguagePage(notify)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars),
        )
    }

    if (showSettings) {
        SettingsSheet(
            hapticEnabled = hapticEnabled,
            onHapticChange = { enabled ->
                onHapticChange(enabled)
                if (enabled) confirmHaptics.click() else haptics.tick()
            },
            onDatabaseManager = {
                showSettings = false
                notify()
            },
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun HomeHeader(onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = (-0.03).em),
            color = MaterialTheme.colorScheme.onSurface,
        )
        QuizzenIconButton(
            icon = QuizzenIcons.Settings,
            contentDescription = stringResource(R.string.settings),
            onClick = onSettings,
        )
    }
}

@Composable
private fun MathPage(
    onOpenMultiply: () -> Unit,
    onOpenTables: () -> Unit,
    onOpenPowers: () -> Unit,
    onOpenAlphabet: () -> Unit,
    onComingSoon: () -> Unit,
) {
    HeroCard(
        letter = "M",
        title = stringResource(R.string.feature_multiply),
        onClick = onOpenMultiply,
    )
    TileRow {
        FeatureTile("T", stringResource(R.string.feature_tables), QuizzenShapes.BlobA, onOpenTables, Modifier.weight(1f))
        FeatureTile("P", stringResource(R.string.feature_powers_roots), QuizzenShapes.BlobB, onOpenPowers, Modifier.weight(1f))
    }
    TileRow {
        FeatureTile("F", stringResource(R.string.feature_fraction_percentage), QuizzenShapes.BlobB, onComingSoon, Modifier.weight(1f))
        FeatureTile("A", stringResource(R.string.feature_alphabet_reasoning), QuizzenShapes.BlobA, onOpenAlphabet, Modifier.weight(1f))
    }
}

@Composable
private fun LanguagePage(onComingSoon: () -> Unit) {
    HeroCard(
        letter = "V",
        title = stringResource(R.string.feature_vocabulary),
        onClick = onComingSoon,
    )
    TileRow {
        FeatureTile("F", stringResource(R.string.feature_fixed_preposition), QuizzenShapes.BlobA, onComingSoon, Modifier.weight(1f))
        FeatureTile("P", stringResource(R.string.feature_phrasal_verb), QuizzenShapes.BlobB, onComingSoon, Modifier.weight(1f))
    }
}

@Composable
private fun TileRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}
