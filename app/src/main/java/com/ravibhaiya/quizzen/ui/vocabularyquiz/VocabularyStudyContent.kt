package com.ravibhaiya.quizzen.ui.vocabularyquiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.WordDetails
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.PrimaryButton
import com.ravibhaiya.quizzen.ui.components.QuizzenIcons
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.bouncyClickable
import com.ravibhaiya.quizzen.ui.components.cssShadow
import com.ravibhaiya.quizzen.ui.theme.QuizzenShapes

@Composable
internal fun VocabularyStudyView(
    entries: List<VocabularyEntry>,
    onBack: () -> Unit,
    onShuffleWords: () -> Unit,
    onStartQuiz: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

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
                trailing = {
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
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.vocabulary_study_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .background(colors.surfaceContainerHigh, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.vocabulary_word_count_summary, entries.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurface,
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("vocabulary_study_list"),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(
                items = entries,
                key = { index, item -> item.id.ifBlank { "${item.wordDetails.word}_$index" } },
            ) { index, entry ->
                StudyWordCard(
                    index = index + 1,
                    entry = entry,
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .cssShadow(
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
private fun StudyWordCard(
    index: Int,
    entry: VocabularyEntry,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val resources = LocalContext.current.resources

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
