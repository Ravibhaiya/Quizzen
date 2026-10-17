package com.ravibhaiya.quizzen.ui.vocabulary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.WordDetails
import com.ravibhaiya.quizzen.ui.components.BlobLetter
import com.ravibhaiya.quizzen.ui.components.EmphasizedEasing
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.PrimaryButton
import com.ravibhaiya.quizzen.ui.components.QuizzenIconButton
import com.ravibhaiya.quizzen.ui.components.QuizzenIcons
import com.ravibhaiya.quizzen.ui.components.QuizzenScreen
import com.ravibhaiya.quizzen.ui.components.ScreenHeader
import com.ravibhaiya.quizzen.ui.components.bouncyClickable
import com.ravibhaiya.quizzen.ui.components.cssShadow
import com.ravibhaiya.quizzen.ui.components.rememberHaptics
import com.ravibhaiya.quizzen.ui.theme.QuizzenShapes
import com.ravibhaiya.quizzen.ui.theme.quizzen

@Composable
fun VocabularyManagerScreen(
    hapticEnabled: Boolean,
    onBack: () -> Unit,
    viewModel: VocabularyManagerViewModel = viewModel(factory = VocabularyManagerViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(hapticEnabled)

    QuizzenScreen {
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
                    title = stringResource(R.string.feature_vocabulary),
                    onBack = {
                        haptics.click()
                        onBack()
                    },
                    trailing = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.word_count_badge, state.entries.size),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            QuizzenIconButton(
                                icon = if (state.isSearchActive) QuizzenIcons.CloseBold else QuizzenIcons.Search,
                                contentDescription = stringResource(
                                    if (state.isSearchActive) R.string.cd_close_search else R.string.cd_search_words,
                                ),
                                onClick = {
                                    haptics.tick()
                                    viewModel.onToggleSearch()
                                },
                                modifier = Modifier.testTag("header_search_button"),
                            )
                        }
                    },
                )

                Text(
                    text = stringResource(R.string.vocabulary_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                AnimatedVisibility(
                    visible = state.isSearchActive,
                    enter = fadeIn(tween(200)) + expandVertically(tween(240, easing = EmphasizedEasing)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
                ) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        VocabularySearchBar(
                            query = state.searchQuery,
                            onQueryChange = viewModel::onSearchQueryChange,
                            onClear = {
                                haptics.tick()
                                viewModel.onSearchQueryChange("")
                            },
                        )
                    }
                }

                val errorMsg = state.errorMessage
                AnimatedVisibility(
                    visible = errorMsg != null,
                    enter = fadeIn(tween(200)) + expandVertically(tween(240, easing = EmphasizedEasing)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
                ) {
                    if (errorMsg != null) {
                        Column {
                            Spacer(Modifier.height(14.dp))
                            FirebaseErrorBanner(
                                message = errorMsg,
                                onDismiss = {
                                    haptics.tick()
                                    viewModel.onDismissError()
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val filtered = state.filteredEntries
                when {
                    state.entries.isEmpty() -> {
                        VocabularyEmptyState(
                            title = stringResource(R.string.vocabulary_empty_title),
                            subtitle = stringResource(R.string.vocabulary_empty_subtitle),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        )
                    }
                    filtered.isEmpty() -> {
                        VocabularyEmptyState(
                            title = stringResource(R.string.vocabulary_no_matches_title),
                            subtitle = stringResource(R.string.vocabulary_no_matches_subtitle),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("vocabulary_list"),
                            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(filtered, key = { it.id }) { entry ->
                                VocabularyWordCard(
                                    entry = entry,
                                    expanded = entry.id in state.expandedIds,
                                    onToggleExpand = {
                                        haptics.tick()
                                        viewModel.onToggleExpand(entry.id)
                                    },
                                    onEdit = {
                                        haptics.click()
                                        viewModel.onStartEdit(entry)
                                    },
                                    onDelete = {
                                        haptics.click()
                                        viewModel.onRequestDelete(entry)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .cssShadow(
                        color = NeutralShadowColor.copy(alpha = 0.18f),
                        offsetY = (-6).dp,
                        blur = 18.dp,
                        spread = (-14).dp,
                        shape = RectangleShape,
                    )
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 26.dp),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.add_word),
                    onClick = {
                        haptics.click()
                        viewModel.onStartAdd()
                    },
                    modifier = Modifier.testTag("footer_add_word_button"),
                )
            }
        }
    }

    val editorState = state.editorState
    if (editorState != null) {
        VocabularyEditorSheet(
            state = editorState,
            onUpdateJsonText = viewModel::onUpdateJsonText,
            onInsertTemplate = {
                haptics.tick()
                viewModel.onInsertSampleTemplate()
            },
            onSave = {
                val saved = viewModel.onSaveEntry()
                if (saved) haptics.heavyClick() else haptics.error()
            },
            onDismiss = viewModel::onDismissEditor,
        )
    }

    val pendingDelete = state.pendingDelete
    if (pendingDelete != null) {
        DeleteWordConfirmSheet(
            entry = pendingDelete,
            onConfirm = {
                haptics.error()
                viewModel.onConfirmDelete()
            },
            onDismiss = viewModel::onCancelDelete,
        )
    }
}

@Composable
private fun FirebaseErrorBanner(
    message: String,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("firebase_error_banner")
            .semantics { liveRegion = LiveRegionMode.Assertive }
            .clip(shape)
            .background(MaterialTheme.quizzen.error.copy(alpha = 0.12f), shape)
            .border(1.5.dp, MaterialTheme.quizzen.error.copy(alpha = 0.4f), shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.quizzen.error,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .testTag("dismiss_error_button")
                .bouncyClickable(CircleShape, pressedScale = 0.88f, onClick = onDismiss)
                .background(MaterialTheme.quizzen.error.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = QuizzenIcons.CloseBold,
                contentDescription = stringResource(R.string.cd_dismiss_error),
                tint = MaterialTheme.quizzen.error,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun VocabularySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val shape = RoundedCornerShape(24.dp)
    val bgColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(200),
        label = "searchBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent,
        animationSpec = tween(200),
        label = "searchBorder",
    )

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cssShadow(NeutralShadowColor.copy(alpha = 0.05f), offsetY = 2.dp, blur = 8.dp, spread = 0.dp, shape = shape)
            .clip(shape)
            .background(bgColor, shape)
            .border(3.dp, borderColor, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = QuizzenIcons.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .testTag("vocabulary_search_input")
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.vocabulary_search_placeholder),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                fontSize = 15.6.sp,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .testTag("clear_search_button")
                    .bouncyClickable(CircleShape, pressedScale = 0.88f, onClick = onClear)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = QuizzenIcons.CloseBold,
                    contentDescription = stringResource(R.string.cd_clear_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun VocabularyEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(30.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .cssShadow(NeutralShadowColor.copy(alpha = 0.05f), offsetY = 2.dp, blur = 6.dp, spread = 0.dp, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, QuizzenShapes.BlobA),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = QuizzenIcons.BookOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VocabularyWordCard(
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
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = entry.wordDetails.word,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
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
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CardActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
