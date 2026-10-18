package com.ravibhaiya.quizzen.ui.vocabulary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.WordDetails
import com.ravibhaiya.quizzen.ui.components.BlobLetter
import com.ravibhaiya.quizzen.ui.components.EmphasizedEasing
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.QuizzenIcons
import com.ravibhaiya.quizzen.ui.components.bouncyClickable
import com.ravibhaiya.quizzen.ui.components.cssShadow
import com.ravibhaiya.quizzen.ui.theme.QuizzenShapes
import com.ravibhaiya.quizzen.ui.theme.quizzen

@Composable
internal fun VocabularyWordCard(
    entry: VocabularyEntry,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250, easing = EmphasizedEasing),
        label = "chevronRotation",
    )
    val resources = LocalContext.current.resources
    val initial = entry.wordDetails.word.firstOrNull()?.uppercaseChar()?.toString() ?: "V"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("word_card_${entry.id}")
            .cssShadow(NeutralShadowColor.copy(alpha = 0.05f), offsetY = 2.dp, blur = 6.dp, spread = 0.dp, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BlobLetter(
                letter = initial,
                shape = QuizzenShapes.BlobA,
                size = 44.dp,
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = entry.wordDetails.word,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CardActionIconButton(
                            icon = QuizzenIcons.Edit,
                            contentDescription = stringResource(R.string.cd_edit_word, entry.wordDetails.word),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = onEdit,
                            modifier = Modifier.testTag("edit_word_${entry.id}"),
                        )
                        CardActionIconButton(
                            icon = QuizzenIcons.Trash,
                            contentDescription = stringResource(R.string.cd_delete_word, entry.wordDetails.word),
                            tint = MaterialTheme.quizzen.error,
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_word_${entry.id}"),
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("toggle_expand_word_${entry.id}")
                                .bouncyClickable(
                                    shape = CircleShape,
                                    pressedScale = 0.88f,
                                    role = Role.Button,
                                    onClick = onToggleExpand,
                                )
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = QuizzenIcons.ChevronDown,
                                contentDescription = stringResource(
                                    if (expanded) R.string.cd_collapse_word else R.string.cd_expand_word,
                                    entry.wordDetails.word,
                                ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = chevronRotation },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RelationCountPill(
                        text = resources.getQuantityString(
                            R.plurals.synonym_count,
                            entry.synonyms.size,
                            entry.synonyms.size,
                        ),
                        modifier = Modifier.testTag("synonym_count_${entry.id}"),
                    )
                    RelationCountPill(
                        text = resources.getQuantityString(
                            R.plurals.antonym_count,
                            entry.antonyms.size,
                            entry.antonyms.size,
                        ),
                        modifier = Modifier.testTag("antonym_count_${entry.id}"),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(220)) + expandVertically(tween(260, easing = EmphasizedEasing)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

                Text(
                    text = stringResource(R.string.section_word_details),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                WordDetailsDisplayBox(details = entry.wordDetails)

                RelationDisplaySection(
                    title = stringResource(R.string.section_synonyms),
                    emptyText = stringResource(R.string.no_synonyms_added),
                    items = entry.synonyms,
                )

                RelationDisplaySection(
                    title = stringResource(R.string.section_antonyms),
                    emptyText = stringResource(R.string.no_antonyms_added),
                    items = entry.antonyms,
                )
            }
        }
    }
}

@Composable
private fun RelationCountPill(
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
private fun CardActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .bouncyClickable(RoundedCornerShape(14.dp), pressedScale = 0.88f, onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun RelationDisplaySection(
    title: String,
    emptyText: String,
    items: List<WordDetails>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = stringResource(R.string.word_count_badge, items.size),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items.forEach { details ->
                WordDetailsDisplayBox(details = details)
            }
        }
    }
}

@Composable
private fun WordDetailsDisplayBox(details: WordDetails) {
    val dash = stringResource(R.string.value_not_provided)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DetailFieldRow(
            label = stringResource(R.string.field_word),
            value = details.word.ifBlank { dash },
            emphasize = true,
        )
        DetailFieldRow(
            label = stringResource(R.string.field_meaning_english),
            value = details.meaningEnglish.ifBlank { dash },
        )
        DetailFieldRow(
            label = stringResource(R.string.field_meaning_hinglish),
            value = details.meaningHinglish.ifBlank { dash },
        )
        DetailFieldRow(
            label = stringResource(R.string.field_example_sentence),
            value = details.exampleSentence.ifBlank { dash },
        )
    }
}

@Composable
private fun DetailFieldRow(
    label: String,
    value: String,
    emphasize: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (emphasize) {
                MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
