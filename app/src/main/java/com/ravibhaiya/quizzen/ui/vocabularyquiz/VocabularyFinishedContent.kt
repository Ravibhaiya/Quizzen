package com.ravibhaiya.quizzen.ui.vocabularyquiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.domain.VocabularyQuizItem
import com.ravibhaiya.quizzen.domain.VocabularyQuizMode
import com.ravibhaiya.quizzen.domain.WordDetails
import com.ravibhaiya.quizzen.ui.components.EmphasizedEasing
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.PrimaryButton
import com.ravibhaiya.quizzen.ui.components.QuizzenIcons
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.bouncyClickable
import com.ravibhaiya.quizzen.ui.components.softShadow
import com.ravibhaiya.quizzen.ui.theme.quizzen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val HIGH_SCORE_THRESHOLD = 80
private const val MID_SCORE_THRESHOLD = 50
private const val CELEBRATION_PARTICLE_COUNT = 14

@Composable
internal fun VocabularyFinishedView(
    state: VocabularyQuizUiState,
    onBack: () -> Unit,
    onRetryMissedWords: () -> Unit,
    onRetrySameWords: () -> Unit,
    onReviewStudyWords: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val extra = MaterialTheme.quizzen
    val cardShape = RoundedCornerShape(28.dp)
    val accuracy = state.accuracyPercent.coerceIn(0, 100)
    val isHighScore = accuracy >= HIGH_SCORE_THRESHOLD
    val missedQuestions = remember(state.questions, state.outcomes) { state.missedQuestions }
    val missedStudyEntries = remember(state.studyEntries, state.questions, state.outcomes) { state.missedStudyEntries }
    val incorrectTotal = state.wrongCount + state.timeoutCount

    val accentColor = when {
        accuracy >= HIGH_SCORE_THRESHOLD -> extra.success
        accuracy >= MID_SCORE_THRESHOLD -> colors.primary
        else -> extra.error
    }
    val headlineRes = when {
        accuracy == 100 && state.totalCount > 0 -> R.string.vocabulary_score_headline_perfect
        accuracy >= HIGH_SCORE_THRESHOLD -> R.string.vocabulary_score_headline_great
        accuracy >= MID_SCORE_THRESHOLD -> R.string.vocabulary_score_headline_good
        else -> R.string.vocabulary_score_headline_practice
    }

    // One-shot finite entrance animations (strictly idle once finished — zero background redraws).
    val cardEntrance = remember { Animatable(0f) }
    val ringProgress = remember { Animatable(0f) }
    val countProgress = remember { Animatable(0f) }
    val badgeScale = remember { Animatable(0.76f) }
    val burstProgress = remember { Animatable(0f) }
    val footerEntrance = remember { Animatable(0f) }

    LaunchedEffect(state.totalCount, state.correctCount, state.wrongCount, state.timeoutCount) {
        cardEntrance.snapTo(0f)
        ringProgress.snapTo(0f)
        countProgress.snapTo(0f)
        badgeScale.snapTo(0.76f)
        burstProgress.snapTo(0f)
        footerEntrance.snapTo(0f)

        launch {
            cardEntrance.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 380, easing = EmphasizedEasing),
            )
        }
        launch {
            delay(80)
            ringProgress.animateTo(
                targetValue = accuracy / 100f,
                animationSpec = tween(durationMillis = 900, easing = EmphasizedEasing),
            )
        }
        launch {
            delay(80)
            countProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 820, easing = EmphasizedEasing),
            )
        }
        launch {
            delay(180)
            badgeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.56f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        if (isHighScore) {
            launch {
                delay(240)
                burstProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 850, easing = EmphasizedEasing),
                )
            }
        }
        launch {
            delay(180)
            footerEntrance.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = EmphasizedEasing),
            )
        }
    }

    val animatedAccuracy = (accuracy * countProgress.value).roundToInt()
    val animatedTotal = (state.totalCount * countProgress.value).roundToInt()
    val animatedAttempted = (state.attemptedCount * countProgress.value).roundToInt()
    val animatedCorrect = (state.correctCount * countProgress.value).roundToInt()
    val animatedIncorrect = (incorrectTotal * countProgress.value).roundToInt()

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
                .padding(top = 8.dp, bottom = 18.dp)
                .graphicsLayer {
                    val p = cardEntrance.value
                    alpha = p
                    translationY = (1f - p) * 20.dp.toPx()
                }
                .softShadow(
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
                    .graphicsLayer {
                        val s = badgeScale.value
                        scaleX = s
                        scaleY = s
                    }
                    .background(accentColor.copy(alpha = 0.14f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("vocabulary_score_headline"),
            ) {
                Text(
                    text = stringResource(headlineRes),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = accentColor,
                )
            }

            ScoreProgressRing(
                progressProvider = { ringProgress.value },
                burstProvider = { burstProgress.value },
                showBurst = isHighScore,
                displayPercent = animatedAccuracy,
                accentColor = accentColor,
                trackColor = colors.surfaceContainerHigh,
                burstPalette = listOf(
                    extra.success,
                    colors.primary,
                    extra.synonym,
                    extra.antonym,
                    extra.tone30,
                ),
                modifier = Modifier
                    .size(164.dp)
                    .testTag("vocabulary_score_ring"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetricTile(
                    label = stringResource(R.string.vocabulary_quiz_total_label),
                    value = animatedTotal.toString(),
                    valueColor = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                SummaryMetricTile(
                    label = stringResource(R.string.vocabulary_quiz_attempted_label),
                    value = animatedAttempted.toString(),
                    valueColor = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetricTile(
                    label = stringResource(R.string.feedback_correct_title),
                    value = animatedCorrect.toString(),
                    valueColor = extra.success,
                    modifier = Modifier.weight(1f),
                )
                SummaryMetricTile(
                    label = stringResource(R.string.feedback_incorrect_title),
                    value = animatedIncorrect.toString(),
                    valueColor = if (incorrectTotal > 0) extra.error else colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val p = footerEntrance.value
                    alpha = p
                    translationY = (1f - p) * 18.dp.toPx()
                },
        ) {
            if (missedQuestions.isNotEmpty()) {
                MissedQuestionsReviewCard(
                    missedQuestions = missedQuestions,
                    modifier = Modifier
                        .padding(bottom = 18.dp)
                        .testTag("vocabulary_missed_questions_card"),
                )
            }

            if (missedStudyEntries.isNotEmpty()) {
                PrimaryButton(
                    text = stringResource(R.string.vocabulary_retry_missed_words, missedStudyEntries.size),
                    onClick = onRetryMissedWords,
                    modifier = Modifier.testTag("vocabulary_retry_missed_button"),
                )
                Spacer(Modifier.height(12.dp))
                SecondaryActionButton(
                    text = stringResource(R.string.vocabulary_retry_quiz),
                    onClick = onRetrySameWords,
                    modifier = Modifier.testTag("vocabulary_retry_button"),
                )
            } else {
                PrimaryButton(
                    text = stringResource(R.string.vocabulary_retry_quiz),
                    onClick = onRetrySameWords,
                    modifier = Modifier.testTag("vocabulary_retry_button"),
                )
            }

            Spacer(Modifier.height(12.dp))

            SecondaryActionButton(
                text = stringResource(R.string.vocabulary_review_words),
                onClick = onReviewStudyWords,
                modifier = Modifier.testTag("vocabulary_review_words_button"),
            )
        }
    }
}

@Composable
private fun ScoreProgressRing(
    progressProvider: () -> Float,
    burstProvider: () -> Float,
    showBurst: Boolean,
    displayPercent: Int,
    accentColor: Color,
    trackColor: Color,
    burstPalette: List<Color>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 14.dp.toPx()
            val inset = strokeWidthPx / 2f + 12.dp.toPx()
            val arcSize = Size(
                width = (size.width - inset * 2f).coerceAtLeast(1f),
                height = (size.height - inset * 2f).coerceAtLeast(1f),
            )
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )

            val sweep = 360f * progressProvider().coerceIn(0f, 1f)
            if (sweep > 0.5f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                )
            }

            if (showBurst) {
                val t = burstProvider().coerceIn(0f, 1f)
                if (t > 0.01f && t < 0.99f) {
                    val alpha = ((1f - t) * (t * 4f).coerceAtMost(1f)).coerceIn(0f, 1f)
                    val baseRadius = arcSize.minDimension / 2f + strokeWidthPx * 0.45f
                    val travel = 14.dp.toPx() * t
                    val dotBaseRadius = 4.dp.toPx() * (1f - t * 0.45f)
                    for (i in 0 until CELEBRATION_PARTICLE_COUNT) {
                        val angleRad = (2.0 * PI * i / CELEBRATION_PARTICLE_COUNT) - (PI / 2.0)
                        val staggerRadius = if (i % 2 == 0) travel else travel * 0.7f
                        val r = baseRadius + staggerRadius
                        val x = center.x + (cos(angleRad) * r).toFloat()
                        val y = center.y + (sin(angleRad) * r).toFloat()
                        val dotColor = burstPalette[i % burstPalette.size].copy(alpha = alpha)
                        drawCircle(
                            color = dotColor,
                            radius = if (i % 2 == 0) dotBaseRadius else dotBaseRadius * 0.72f,
                            center = Offset(x, y),
                        )
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.vocabulary_score_percent, displayPercent),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = accentColor,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.vocabulary_quiz_accuracy_badge),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MissedQuestionsReviewCard(
    missedQuestions: List<Pair<VocabularyQuizItem, FeedbackType>>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val extra = MaterialTheme.quizzen
    val cardShape = RoundedCornerShape(24.dp)
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 220, easing = EmphasizedEasing),
        label = "missedChevronRotation",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .softShadow(
                NeutralShadowColor.copy(alpha = 0.05f),
                offsetY = 2.dp,
                blur = 10.dp,
                spread = 0.dp,
                shape = cardShape,
            )
            .background(colors.surfaceContainer, cardShape)
            .border(1.dp, extra.error.copy(alpha = 0.28f), cardShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .bouncyClickable(shape = cardShape, onClick = { expanded = !expanded })
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .testTag("vocabulary_missed_questions_toggle"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(extra.error.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = QuizzenIcons.CloseBold,
                        contentDescription = null,
                        tint = extra.error,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.vocabulary_missed_questions_title, missedQuestions.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
            }
            Icon(
                imageVector = QuizzenIcons.ChevronDown,
                contentDescription = stringResource(
                    if (expanded) R.string.cd_collapse_missed_questions else R.string.cd_expand_missed_questions,
                ),
                tint = colors.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = chevronRotation },
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(240, easing = EmphasizedEasing)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200, easing = EmphasizedEasing)) + fadeOut(tween(150)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HorizontalDivider(color = colors.surfaceContainerHigh)
                missedQuestions.forEachIndexed { index, (question, outcome) ->
                    MissedQuestionRow(
                        question = question,
                        outcome = outcome,
                        modifier = Modifier.testTag("vocabulary_missed_item_$index"),
                    )
                }
            }
        }
    }
}

@Composable
private fun MissedQuestionRow(
    question: VocabularyQuizItem,
    outcome: FeedbackType,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val extra = MaterialTheme.quizzen
    val isSynonym = question.mode == VocabularyQuizMode.Synonyms
    val badgeBg = if (isSynonym) extra.synonymContainer else extra.antonymContainer
    val badgeFg = if (isSynonym) extra.onSynonymContainer else extra.onAntonymContainer
    val badgeLabel = stringResource(
        if (isSynonym) R.string.vocabulary_mode_synonym_badge else R.string.vocabulary_mode_antonym_badge,
    )
    val answerDetails = question.detailsForOption(question.correctAnswer)
    val meaningText = formatMissedWordMeaning(answerDetails)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(badgeBg, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = badgeFg,
                )
            }
            if (outcome == FeedbackType.Timeout) {
                Box(
                    modifier = Modifier
                        .background(extra.timeout.copy(alpha = 0.14f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = stringResource(R.string.vocabulary_missed_timeout_tag),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = extra.timeout,
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = question.promptWord,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.onSurface,
            )
            Text(
                text = stringResource(R.string.vocabulary_missed_arrow),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = question.correctAnswer,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = extra.success,
            )
        }

        if (meaningText.isNotEmpty()) {
            Text(
                text = meaningText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = colors.onSurfaceVariant,
            )
        }
    }
}

private fun formatMissedWordMeaning(details: WordDetails): String {
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
private fun SummaryMetricTile(
    label: String,
    value: String,
    valueColor: Color,
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
            color = valueColor,
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
