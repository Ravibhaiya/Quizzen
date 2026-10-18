package com.ravibhaiya.quizzen.ui.vocabularyquiz

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.Feedback
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.domain.VocabularyQuizItem
import com.ravibhaiya.quizzen.domain.VocabularyQuizMode
import com.ravibhaiya.quizzen.domain.WordDetails
import com.ravibhaiya.quizzen.ui.components.FitText
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.PrimaryButton
import com.ravibhaiya.quizzen.ui.components.QuizzenIcons
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.TimerChip
import com.ravibhaiya.quizzen.ui.components.cssShadow
import com.ravibhaiya.quizzen.ui.components.pressScale
import com.ravibhaiya.quizzen.ui.theme.QuizzenShapes
import com.ravibhaiya.quizzen.ui.theme.quizzen

private val OPTION_LETTERS = listOf("A", "B", "C", "D")

@Composable
internal fun VocabularyActiveQuizView(
    state: VocabularyQuizUiState,
    shakeOffsetDp: () -> Float,
    onBack: () -> Unit,
    onSelectOption: (String) -> Unit,
    onNextQuestion: () -> Unit,
) {
    val question = state.currentQuestion ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.feature_vocabulary),
                onBack = onBack,
                trailing = {
                    TimerChip(
                        seconds = state.remainingSeconds,
                        pulsing = state.isTimerLow && !state.isLocked,
                    )
                },
            )

            QuizProgressTopBar(
                totalCount = state.totalCount,
                attemptedCount = state.attemptedCount,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            QuestionPromptCard(
                question = question,
                feedback = state.feedback,
                showMeaning = state.isLocked,
                shakeOffsetDp = shakeOffsetDp,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.options.forEachIndexed { index, option ->
                    val badge = OPTION_LETTERS.getOrElse(index) { (index + 1).toString() }
                    VocabularyOptionCard(
                        badge = badge,
                        optionIndex = index,
                        optionText = option,
                        optionDetails = question.detailsForOption(option),
                        showMeaning = state.isLocked,
                        isSelected = state.selectedOption != null &&
                            state.selectedOption.equals(option, ignoreCase = true),
                        isCorrectOption = question.isCorrect(option),
                        isLocked = state.isLocked,
                        onClick = { onSelectOption(option) },
                        modifier = Modifier.testTag("vocabulary_option_$index"),
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .cssShadow(
                    NeutralShadowColor.copy(alpha = 0.18f),
                    offsetY = (-6).dp,
                    blur = 18.dp,
                    spread = (-14).dp,
                    shape = RectangleShape,
                ),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 16.dp),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.vocabulary_next_question),
                    onClick = onNextQuestion,
                    enabled = state.isLocked,
                    modifier = Modifier.testTag("vocabulary_next_button"),
                )
            }
        }
    }
}

@Composable
private fun QuizProgressTopBar(
    totalCount: Int,
    attemptedCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vocabulary_quiz_progress_bar"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProgressStatPill(
            label = stringResource(R.string.vocabulary_quiz_total_label),
            value = totalCount.toString(),
            containerColor = colors.surfaceContainer,
            contentColor = colors.onSurface,
            modifier = Modifier
                .weight(1f)
                .testTag("vocabulary_total_badge"),
        )
        ProgressStatPill(
            label = stringResource(R.string.vocabulary_quiz_attempted_label),
            value = attemptedCount.toString(),
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            modifier = Modifier
                .weight(1f)
                .testTag("vocabulary_attempted_badge"),
        )
    }
}

@Composable
private fun ProgressStatPill(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .background(containerColor, shape)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.78f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
        )
    }
}

@Composable
private fun QuestionPromptCard(
    question: VocabularyQuizItem,
    feedback: Feedback?,
    showMeaning: Boolean,
    shakeOffsetDp: () -> Float,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val relationLabel = when (question.mode) {
        VocabularyQuizMode.Synonyms -> stringResource(R.string.vocabulary_find_synonym)
        VocabularyQuizMode.Antonyms -> stringResource(R.string.vocabulary_find_antonym)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = shakeOffsetDp().dp.toPx() }
            .cssShadow(
                NeutralShadowColor.copy(alpha = 0.05f),
                offsetY = 2.dp,
                blur = 8.dp,
                spread = 0.dp,
                shape = shape,
            )
            .background(colors.surfaceContainer, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(colors.primaryContainer, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
            ) {
                Text(
                    text = relationLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onPrimaryContainer,
                )
            }

            if (feedback != null) {
                McqFeedbackStatusPill(feedback = feedback)
            }
        }

        FitText(
            text = AnnotatedString(question.promptWord),
            style = MaterialTheme.typography.headlineLarge.copy(color = colors.onSurface),
            maxFontSize = MaterialTheme.typography.headlineLarge.fontSize * 1.35f,
            minFontSize = MaterialTheme.typography.headlineMedium.fontSize,
            step = MaterialTheme.typography.labelSmall.fontSize * 0.15f,
            modifier = Modifier
                .semantics { contentDescription = "$relationLabel: ${question.promptWord}" }
                .testTag("vocabulary_prompt_word"),
        )

        if (showMeaning) {
            PromptWordMeaningBox(
                details = question.promptDetails,
                modifier = Modifier.testTag("vocabulary_prompt_meaning"),
            )
        }
    }
}

@Composable
private fun McqFeedbackStatusPill(
    feedback: Feedback,
    modifier: Modifier = Modifier,
) {
    val extra = MaterialTheme.quizzen
    val background = when (feedback.type) {
        FeedbackType.Correct -> extra.success
        FeedbackType.Incorrect -> extra.error
        FeedbackType.Timeout -> extra.timeout
    }
    val icon = when (feedback.type) {
        FeedbackType.Correct -> QuizzenIcons.CheckBold
        FeedbackType.Incorrect -> QuizzenIcons.CloseBold
        FeedbackType.Timeout -> QuizzenIcons.TimerBold
    }
    val label = when (feedback.type) {
        FeedbackType.Correct -> stringResource(R.string.feedback_correct_title)
        FeedbackType.Incorrect -> stringResource(R.string.feedback_incorrect_title)
        FeedbackType.Timeout -> stringResource(R.string.feedback_timeout_title)
    }

    Row(
        modifier = modifier
            .background(background, CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive }
            .testTag("vocabulary_feedback_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun PromptWordMeaningBox(
    details: WordDetails,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val meaningLine = formatWordMeaning(details)
    if (meaningLine.isBlank()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (details.meaningEnglish.isNotBlank()) {
            Text(
                text = details.meaningEnglish,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        if (details.meaningHinglish.isNotBlank()) {
            Text(
                text = details.meaningHinglish,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun VocabularyOptionCard(
    badge: String,
    optionIndex: Int,
    optionText: String,
    optionDetails: WordDetails,
    showMeaning: Boolean,
    isSelected: Boolean,
    isCorrectOption: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val extra = MaterialTheme.quizzen
    val shape = RoundedCornerShape(22.dp)

    val targetContainer = when {
        isLocked && (isCorrectOption || isSelected) -> colors.surface
        else -> colors.surfaceContainer
    }
    val targetBorder = when {
        isLocked && isCorrectOption -> extra.success
        isLocked && isSelected && !isCorrectOption -> extra.error
        else -> colors.outlineVariant
    }
    val targetTitleColor = when {
        isLocked && isCorrectOption -> extra.success
        isLocked && isSelected && !isCorrectOption -> extra.error
        else -> colors.onSurface
    }
    val targetBadgeContainer = when {
        isLocked && isCorrectOption -> extra.success
        isLocked && isSelected && !isCorrectOption -> extra.error
        isLocked -> colors.surfaceContainerHigh
        else -> colors.primaryContainer
    }
    val targetBadgeContent = when {
        isLocked && (isCorrectOption || isSelected) -> Color.White
        isLocked -> colors.onSurfaceVariant
        else -> colors.onPrimaryContainer
    }

    val container by animateColorAsState(targetContainer, tween(200), label = "vocabOptContainer")
    val borderColor by animateColorAsState(targetBorder, tween(200), label = "vocabOptBorder")
    val titleColor by animateColorAsState(targetTitleColor, tween(200), label = "vocabOptTitle")
    val badgeBg by animateColorAsState(targetBadgeContainer, tween(200), label = "vocabOptBadgeBg")
    val badgeFg by animateColorAsState(targetBadgeContent, tween(200), label = "vocabOptBadgeFg")
    val interactionSource = remember { MutableInteractionSource() }
    val borderWidth = if (isLocked && (isCorrectOption || isSelected)) 2.dp else 1.5.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .cssShadow(
                NeutralShadowColor.copy(alpha = 0.05f),
                offsetY = 2.dp,
                blur = 6.dp,
                spread = 0.dp,
                shape = shape,
            )
            .clip(shape)
            .background(container)
            .border(borderWidth, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = !isLocked,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(badgeBg, QuizzenShapes.BlobB),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.titleSmall,
                color = badgeFg,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = optionText,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
            )

            if (showMeaning) {
                val meaningLine = formatWordMeaning(optionDetails)
                if (meaningLine.isNotBlank()) {
                    Text(
                        text = meaningLine,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.testTag("vocabulary_option_meaning_$optionIndex"),
                    )
                }
            }
        }

        if (isLocked && (isCorrectOption || isSelected)) {
            val statusColor = if (isCorrectOption) extra.success else extra.error
            val statusIcon = if (isCorrectOption) QuizzenIcons.CheckBold else QuizzenIcons.CloseBold
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(statusColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

private fun formatWordMeaning(details: WordDetails): String {
    val english = details.meaningEnglish.trim()
    val hinglish = details.meaningHinglish.trim()
    return when {
        english.isNotEmpty() && hinglish.isNotEmpty() -> "$english • $hinglish"
        english.isNotEmpty() -> english
        hinglish.isNotEmpty() -> hinglish
        else -> ""
    }
}
