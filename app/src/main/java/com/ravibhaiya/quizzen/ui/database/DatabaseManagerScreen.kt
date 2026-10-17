package com.ravibhaiya.quizzen.ui.database

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.rememberHaptics
import com.ravibhaiya.quizzen.ui.home.FeatureTile
import com.ravibhaiya.quizzen.ui.home.HeroCard
import com.ravibhaiya.quizzen.ui.theme.QuizzenShapes
import kotlinx.coroutines.launch

@Composable
fun DatabaseManagerScreen(
    hapticEnabled: Boolean,
    onBack: () -> Unit,
    onOpenVocabulary: () -> Unit,
) {
    val haptics = rememberHaptics(hapticEnabled)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val comingSoon = stringResource(R.string.coming_soon)

    val notifyComingSoon: () -> Unit = {
        haptics.tick()
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(comingSoon)
        }
    }

    QuizzenScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 24.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.database_manager),
                onBack = {
                    haptics.click()
                    onBack()
                },
            )

            Text(
                text = stringResource(R.string.database_manager_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(22.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                HeroCard(
                    letter = "V",
                    title = stringResource(R.string.feature_vocabulary),
                    onClick = {
                        haptics.click()
                        onOpenVocabulary()
                    },
                    modifier = Modifier.testTag("db_vocabulary_card"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    FeatureTile(
                        letter = "F",
                        title = stringResource(R.string.feature_fixed_preposition),
                        blobShape = QuizzenShapes.BlobA,
                        onClick = notifyComingSoon,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("db_fixed_preposition_tile"),
                    )
                    FeatureTile(
                        letter = "P",
                        title = stringResource(R.string.feature_phrasal_verb),
                        blobShape = QuizzenShapes.BlobB,
                        onClick = notifyComingSoon,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("db_phrasal_verb_tile"),
                    )
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
}
