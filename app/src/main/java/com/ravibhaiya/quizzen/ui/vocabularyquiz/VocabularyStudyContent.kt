package com.ravibhaiya.quizzen.ui.vocabularyquiz

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.WordDetails
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.PrimaryButton
import com.ravibhaiya.quizzen.ui.components.QuizzenIcons
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.bouncyClickable
import com.ravibhaiya.quizzen.ui.components.softShadow
import com.ravibhaiya.quizzen.ui.theme.QuizzenShapes
import com.ravibhaiya.quizzen.ui.theme.quizzen

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VocabularyStudyView(
    entries: List<VocabularyEntry>,
    onBack: () -> Unit,
    onShuffleWords: () -> Unit,
    onStartQuiz: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val extra = MaterialTheme.quizzen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 24.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.vocabulary_study_title),
                onBack = onBack,
                modifier = Modifier.padding(bottom = 12.dp),
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .background(colors.surfaceContainerHigh, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.vocabulary_word_count_summary, entries.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurface,
                            )
                        }
                        if (entries.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .bouncyClickable(
                                        shape = CircleShape,
                                        pressedScale = 0.88f,
                                        onClick = onShuffleWords,
                                    )
                                    .background(colors.primaryContainer, CircleShape)
                                    .testTag("vocabulary_shuffle_study_button"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = QuizzenIcons.Shuffle,
                                    contentDescription = stringResource(R.string.cd_shuffle_study_words),
                                    tint = colors.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                },
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("vocabulary_study_list"),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 0.dp, bottom = 24.dp),
        ) {
            if (entries.isEmpty()) {
                item(key = "study_empty_state") {
                    StudyEmptyStateCard()
                }
            } else {
                entries.forEachIndexed { entryIndex, entry ->
                    val index = entryIndex + 1
                    val entryKey = entry.id.ifBlank { "${entry.wordDetails.word}_$entryIndex" }
                    val hasSynonyms = entry.synonyms.isNotEmpty()
                    val hasAntonyms = entry.antonyms.isNotEmpty()
                    val mainDetailsIsLast = !hasSynonyms && !hasAntonyms

                    stickyHeader(key = "study_sticky_header_$entryKey") {
                        StudyMainWordStickyHeader(
                            index = index,
                            entry = entry,
                        )
                    }

                    item(key = "study_main_details_$entryKey") {
                        StudyClusterBodySlice(
                            isFirst = true,
                            isLast = mainDetailsIsLast,
                        ) {
                            StudyDetailsBlock(
                                details = entry.wordDetails,
                                showWordHeading = false,
                            )
                        }
                    }

                    if (hasSynonyms) {
                        item(key = "study_syn_header_$entryKey") {
                            StudyClusterBodySlice(
                                isFirst = false,
                                isLast = false,
                                topPadding = 12.dp,
                                bottomPadding = 6.dp,
                            ) {
                                StudyRelationHeaderRow(
                                    label = stringResource(R.string.vocabulary_synonyms_label),
                                    count = entry.synonyms.size,
                                    badgeContainer = extra.synonymContainer,
                                    badgeContent = extra.onSynonymContainer,
                                    accentColor = extra.synonym,
                                )
                            }
                        }

                        itemsIndexed(
                            items = entry.synonyms,
                            key = { synIdx, syn -> "study_syn_${entryKey}_${synIdx}_${syn.word}" },
                        ) { synIdx, synDetail ->
                            val isLastInEntry = !hasAntonyms && synIdx == entry.synonyms.lastIndex
                            StudyClusterBodySlice(
                                isFirst = false,
                                isLast = isLastInEntry,
                                topPadding = 4.dp,
                                bottomPadding = if (isLastInEntry) 18.dp else 4.dp,
                            ) {
                                StudyDetailsBlock(
                                    details = synDetail,
                                    showWordHeading = true,
                                    headingColor = extra.synonym,
                                    borderColor = extra.synonym.copy(alpha = 0.25f),
                                )
                            }
                        }
                    }

                    if (hasAntonyms) {
                        item(key = "study_ant_header_$entryKey") {
                            StudyClusterBodySlice(
                                isFirst = false,
                                isLast = false,
                                topPadding = 12.dp,
                                bottomPadding = 6.dp,
                            ) {
                                StudyRelationHeaderRow(
                                    label = stringResource(R.string.vocabulary_antonyms_label),
                                    count = entry.antonyms.size,
                                    badgeContainer = extra.antonymContainer,
                                    badgeContent = extra.onAntonymContainer,
                                    accentColor = extra.antonym,
                                )
                            }
                        }

                        itemsIndexed(
                            items = entry.antonyms,
                            key = { antIdx, ant -> "study_ant_${entryKey}_${antIdx}_${ant.word}" },
                        ) { antIdx, antDetail ->
                            val isLastInEntry = antIdx == entry.antonyms.lastIndex
                            StudyClusterBodySlice(
                                isFirst = false,
                                isLast = isLastInEntry,
                                topPadding = 4.dp,
                                bottomPadding = if (isLastInEntry) 18.dp else 4.dp,
                            ) {
                                StudyDetailsBlock(
                                    details = antDetail,
                                    showWordHeading = true,
                                    headingColor = extra.antonym,
                                    borderColor = extra.antonym.copy(alpha = 0.25f),
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .softShadow(
                    NeutralShadowColor.copy(alpha = 0.2f),
                    offsetY = (-6).dp,
                    blur = 18.dp,
                    spread = (-14).dp,
                    shape = RectangleShape,
                ),
            color = colors.surface,
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
private fun StudyMainWordStickyHeader(
    index: Int,
    entry: VocabularyEntry,
) {
    val colors = MaterialTheme.colorScheme
    val extra = MaterialTheme.quizzen
    val resources = LocalContext.current.resources
    val mainPartOfSpeech = entry.wordDetails.partOfSpeech.trim()
    val headerShape = RoundedCornerShape(22.dp)

    // Solid surface backdrop ensures scrolling synonym/antonym cards underneath never bleed through.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(top = 4.dp, bottom = 6.dp)
            .testTag("study_sticky_main_word_$index"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .softShadow(
                    NeutralShadowColor.copy(alpha = 0.08f),
                    offsetY = 3.dp,
                    blur = 10.dp,
                    spread = 0.dp,
                    shape = headerShape,
                )
                .background(colors.surfaceContainer, headerShape)
                .border(1.5.dp, colors.primary.copy(alpha = 0.28f), headerShape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.vocabulary_main_word_label).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp,
                            ),
                            color = colors.primary,
                        )
                        Text(
                            text = entry.wordDetails.word,
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.onSurface,
                        )
                    }
                    if (mainPartOfSpeech.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(colors.primaryContainer, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("study_word_pos_$index"),
                        ) {
                            Text(
                                text = mainPartOfSpeech,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.onPrimaryContainer,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StudyRelationCountPill(
                        text = "≈ " + resources.getQuantityString(
                            R.plurals.synonym_count,
                            entry.synonyms.size,
                            entry.synonyms.size,
                        ),
                        containerColor = extra.synonymContainer,
                        contentColor = extra.onSynonymContainer,
                    )
                    StudyRelationCountPill(
                        text = "⇄ " + resources.getQuantityString(
                            R.plurals.antonym_count,
                            entry.antonyms.size,
                            entry.antonyms.size,
                        ),
                        containerColor = extra.antonymContainer,
                        contentColor = extra.onAntonymContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyClusterBodySlice(
    isFirst: Boolean,
    isLast: Boolean,
    topPadding: Dp = if (isFirst) 14.dp else 4.dp,
    bottomPadding: Dp = if (isLast) 18.dp else 4.dp,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(
        topStart = if (isFirst) 22.dp else 0.dp,
        topEnd = if (isFirst) 22.dp else 0.dp,
        bottomStart = if (isLast) 22.dp else 0.dp,
        bottomEnd = if (isLast) 22.dp else 0.dp,
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceContainer, shape)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = topPadding,
                    bottom = bottomPadding,
                ),
        ) {
            content()
        }
        if (isLast) {
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun StudyEmptyStateCard() {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(
                NeutralShadowColor.copy(alpha = 0.05f),
                offsetY = 2.dp,
                blur = 8.dp,
                spread = 0.dp,
                shape = shape,
            )
            .background(colors.surfaceContainer, shape)
            .padding(24.dp)
            .testTag("vocabulary_study_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(colors.primaryContainer, QuizzenShapes.BlobA),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = QuizzenIcons.BookOpen,
                contentDescription = null,
                tint = colors.onPrimaryContainer,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(
            text = stringResource(R.string.vocabulary_study_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
        )
        Text(
            text = stringResource(R.string.vocabulary_study_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun StudyRelationCountPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(containerColor, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun StudyRelationHeaderRow(
    label: String,
    count: Int,
    badgeContainer: Color,
    badgeContent: Color,
    accentColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .background(badgeContainer, CircleShape)
                .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = badgeContent,
            )
        }
        Box(
            modifier = Modifier
                .background(badgeContainer, CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.word_count_badge, count),
                style = MaterialTheme.typography.labelSmall,
                color = badgeContent,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun StudyDetailsBlock(
    details: WordDetails,
    showWordHeading: Boolean,
    headingColor: Color = MaterialTheme.colorScheme.primary,
    borderColor: Color = Color.Transparent,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val pos = details.partOfSpeech.trim()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, shape)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.dp, borderColor, shape)
                } else {
                    Modifier
                },
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (showWordHeading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = details.word,
                    style = MaterialTheme.typography.titleMedium,
                    color = headingColor,
                    modifier = Modifier.weight(1f),
                )
                if (pos.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(colors.surfaceContainer, CircleShape)
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = pos,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
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
