package com.ravibhaiya.quizzen.ui.vocabularyquiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.PrimaryButton
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.bouncyClickable
import com.ravibhaiya.quizzen.ui.components.cssShadow

@Composable
internal fun VocabularyFinishedView(
    state: VocabularyQuizUiState,
    onBack: () -> Unit,
    onRetrySameWords: () -> Unit,
    onReviewStudyWords: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(28.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 34.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.vocabulary_quiz_complete_title),
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 22.dp)
                .cssShadow(
                    NeutralShadowColor.copy(alpha = 0.06f),
                    offsetY = 4.dp,
                    blur = 12.dp,
                    spread = 0.dp,
                    shape = cardShape,
                )
                .background(colors.surfaceContainer, cardShape)
                .padding(24.dp)
                .testTag("vocabulary_quiz_summary_card"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(colors.primaryContainer, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(
                    text = stringResource(R.string.vocabulary_quiz_accuracy_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onPrimaryContainer,
                )
            }

            Text(
                text = "${state.accuracyPercent}%",
                style = MaterialTheme.typography.displayLarge,
                color = colors.primary,
                textAlign = TextAlign.Center,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetricTile(
                    label = stringResource(R.string.vocabulary_quiz_total_label),
                    value = state.totalCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                SummaryMetricTile(
                    label = stringResource(R.string.vocabulary_quiz_attempted_label),
                    value = state.attemptedCount.toString(),
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetricTile(
                    label = stringResource(R.string.feedback_correct_title),
                    value = state.correctCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                SummaryMetricTile(
                    label = stringResource(R.string.feedback_incorrect_title),
                    value = (state.wrongCount + state.timeoutCount).toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        PrimaryButton(
            text = stringResource(R.string.vocabulary_retry_quiz),
            onClick = onRetrySameWords,
            modifier = Modifier.testTag("vocabulary_retry_button"),
        )

        Spacer(Modifier.height(12.dp))

        SecondaryActionButton(
            text = stringResource(R.string.vocabulary_review_words),
            onClick = onReviewStudyWords,
        )
    }
}

@Composable
private fun SummaryMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .background(colors.surface, RoundedCornerShape(20.dp))
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onSurface,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .bouncyClickable(shape = shape, onClick = onClick)
            .border(2.dp, colors.outlineVariant, shape)
            .background(colors.surfaceContainer, shape)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
        )
    }
}
