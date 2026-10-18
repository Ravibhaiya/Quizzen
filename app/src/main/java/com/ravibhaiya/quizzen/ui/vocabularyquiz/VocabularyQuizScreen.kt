package com.ravibhaiya.quizzen.ui.vocabularyquiz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.Feedback
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.VocabularyQuizItem
import com.ravibhaiya.quizzen.domain.VocabularyQuizMode
import com.ravibhaiya.quizzen.domain.WordDetails
import com.ravibhaiya.quizzen.ui.components.FitText
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.PrimaryButton
import com.ravibhaiya.quizzen.ui.components.QuizzenIconButton
import com.ravibhaiya.quizzen.ui.components.QuizzenIcons
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.TimerChip
import com.ravibhaiya.quizzen.ui.components.bouncyClickable
import com.ravibhaiya.quizzen.ui.components.cssShadow
import com.ravibhaiya.quizzen.ui.components.pressScale
import com.ravibhaiya.quizzen.ui.components.rememberHaptics
import com.ravibhaiya.quizzen.ui.theme.QuizzenShapes
import com.ravibhaiya.quizzen.ui.theme.quizzen

private val OPTION_LETTERS = listOf("A", "B", "C", "D")

@Composable
fun VocabularyQuizScreen(
    hapticEnabled: Boolean,
    onBack: () -> Unit,
    viewModel: VocabularyQuizViewModel = viewModel(factory = VocabularyQuizViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(hapticEnabled)
    val shake = remember { Animatable(0f) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onPause() }

    BackHandler(enabled = state.phase != VocabularyQuizPhase.Study) {
        haptics.click()
        onBack()
    }

    LaunchedEffect(state.feedback) {
        when (state.feedback?.type) {
            FeedbackType.Correct -> haptics.success()
            FeedbackType.Incorrect -> haptics.error()
            FeedbackType.Timeout -> haptics.timeout()
            null -> Unit
        }
    }

    LaunchedEffect(state.shakeCount) {
        if (state.shakeCount > 0) {
            shake.snapTo(0f)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -3f at 40
                    5f at 80
                    -9f at 120
                    9f at 160
                    -9f at 200
                    9f at 240
                    -9f at 280
                    5f at 320
                    -3f at 360
                    0f at 400
                },
            )
        }
    }

    QuizzenScreen {
        when (state.phase) {
            VocabularyQuizPhase.Study -> {
                VocabularyStudyView(
                    entries = state.studyEntries,
                    onBack = { haptics.click(); onBack() },
                    onShuffle = { haptics.tick(); viewModel.shuffleStudyWords() },
                    onStartQuiz = { haptics.heavyClick(); viewModel.startQuiz() },
                )
            }

            VocabularyQuizPhase.Quiz -> {
                VocabularyActiveQuizView(
                    state = state,
                    shakeOffsetDp = { shake.value },
                    onBack = { haptics.click(); onBack() },
                    onSelectOption = { option ->
                        haptics.click()
                        viewModel.selectOption(option)
                    },
                    onNextQuestion = {
                        haptics.click()
                        viewModel.nextQuestion()
                    },
                )
            }

            VocabularyQuizPhase.Finished -> {
                VocabularyFinishedView(
                    state = state,
                    onBack = { haptics.click(); onBack() },
                    onRetrySameWords = { haptics.heavyClick(); viewModel.retryQuiz() },
                    onReviewStudyWords = { haptics.click(); viewModel.backToStudy() },
                )
            }
        }
    }
}

@Composable
private fun VocabularyStudyView(
    entries: List<VocabularyEntry>,
    onBack: () -> Unit,
    onShuffle: () -> Unit,
    onStartQuiz: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 12.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.vocabulary_study_title),
                onBack = onBack,
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = entries.size.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        QuizzenIconButton(
                            icon = QuizzenIcons.Shuffle,
                            contentDescription = stringResource(R.string.cd_shuffle_study_words),
                            onClick = onShuffle,
                        )
                    }
                },
            )
            Text(
                text = stringResource(R.string.vocabulary_study_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("vocabulary_study_list"),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(entries, key = { index, item -> item.id.ifBlank { "study_$index" } }) { index, entry ->
                StudyWordCard(index = index + 1, entry = entry)
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
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 18.dp),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.vocabulary_start_quiz),
                    onClick = onStartQuiz,
                    enabled = entries.isNotEmpty(),
                    modifier = Modifier.testTag("vocabulary_start_quiz_button"),
                )
            }
        }
    }
}

@Composable
private fun StudyWordCard(
    index: Int,
    entry: VocabularyEntry,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val resources = androidx.compose.ui.platform.LocalContext.current.resources

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cssShadow(
                NeutralShadowColor.copy(alpha = 0.05f),
                offsetY = 2.dp,
                blur = 8.dp,
                spread = 0.dp,
                shape = shape,
            )
            .background(colors.surfaceContainer, shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.primaryContainer, QuizzenShapes.BlobA),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onPrimaryContainer,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = entry.wordDetails.word,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StudyRelationCountPill(
                        text = resources.getQuantityString(
                            R.plurals.synonym_count,
                            entry.synonyms.size,
                            entry.synonyms.size,
                        ),
                    )
                    StudyRelationCountPill(
                        text = resources.getQuantityString(
                            R.plurals.antonym_count,
                            entry.antonyms.size,
                            entry.antonyms.size,
                        ),
                    )
                }
            }
        }

        StudyDetailsBlock(details = entry.wordDetails, showWordHeading = false)

        if (entry.synonyms.isNotEmpty()) {
            StudyRelationSection(
                label = stringResource(R.string.vocabulary_synonyms_label),
                items = entry.synonyms,
                badgeContainer = colors.primaryContainer,
                badgeContent = colors.onPrimaryContainer,
            )
        }

        if (entry.antonyms.isNotEmpty()) {
            StudyRelationSection(
                label = stringResource(R.string.vocabulary_antonyms_label),
                items = entry.antonyms,
                badgeContainer = colors.surfaceContainerHigh,
                badgeContent = colors.onSurface,
            )
        }
    }
}

@Composable
private fun StudyRelationCountPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun StudyRelationSection(
    label: String,
    items: List<WordDetails>,
    badgeContainer: Color,
    badgeContent: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(badgeContainer, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeContent,
                )
            }
            Box(
                modifier = Modifier
                    .background(badgeContainer, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.word_count_badge, items.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeContent,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        items.forEach { detail ->
            StudyDetailsBlock(details = detail, showWordHeading = true)
        }
    }
}

@Composable
private fun StudyDetailsBlock(
    details: WordDetails,
    showWordHeading: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (showWordHeading) {
            Text(
                text = details.word,
                style = MaterialTheme.typography.titleMedium,
                color = colors.primary,
            )
        }
        if (details.meaningEnglish.isNotBlank()) {
            StudyFieldRow(
                label = stringResource(R.string.vocabulary_field_english),
                value = details.meaningEnglish,
            )
        }
        if (details.meaningHinglish.isNotBlank()) {
            StudyFieldRow(
                label = stringResource(R.string.vocabulary_field_hinglish),
                value = details.meaningHinglish,
            )
        }
        if (details.exampleSentence.isNotBlank()) {
            StudyFieldRow(
                label = stringResource(R.string.vocabulary_field_example),
                value = details.exampleSentence,
                italic = true,
            )
        }
    }
}

@Composable
private fun StudyFieldRow(
    label: String,
    value: String,
    italic: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            ),
            color = colors.onSurface,
        )
    }
}

@Composable
private fun VocabularyActiveQuizView(
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
            text = androidx.compose.ui.text.AnnotatedString(question.promptWord),
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

@Composable
private fun VocabularyFinishedView(
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
