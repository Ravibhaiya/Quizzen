package com.ravibhaiya.quizzen.ui.vocabulary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.ui.components.EmphasizedEasing
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.PrimaryButton
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
    modifier: Modifier = Modifier,
    viewModel: VocabularyManagerViewModel = viewModel(factory = VocabularyManagerViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(hapticEnabled)

    val searchBtnBg by animateColorAsState(
        targetValue = if (state.isSearchActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(200),
        label = "searchBtnBg",
    )
    val searchBtnTint by animateColorAsState(
        targetValue = if (state.isSearchActive) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(200),
        label = "searchBtnTint",
    )

    QuizzenScreen(modifier = modifier) {
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
                                    .size(48.dp)
                                    .testTag("toggle_search_button")
                                    .semantics { selected = state.isSearchActive }
                                    .cssShadow(
                                        NeutralShadowColor.copy(alpha = 0.05f),
                                        offsetY = 2.dp,
                                        blur = 6.dp,
                                        spread = 0.dp,
                                        shape = CircleShape,
                                    )
                                    .bouncyClickable(
                                        shape = CircleShape,
                                        pressedScale = 0.88f,
                                        role = Role.Button,
                                        onClick = {
                                            haptics.click()
                                            viewModel.onToggleSearch()
                                        },
                                    )
                                    .background(searchBtnBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (state.isSearchActive) QuizzenIcons.CloseBold else QuizzenIcons.Search,
                                    contentDescription = stringResource(
                                        if (state.isSearchActive) R.string.cd_close_search else R.string.cd_search_words,
                                    ),
                                    tint = searchBtnTint,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("header_add_word_button")
                                    .cssShadow(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                        offsetY = 4.dp,
                                        blur = 12.dp,
                                        spread = (-2).dp,
                                        shape = CircleShape,
                                    )
                                    .bouncyClickable(
                                        shape = CircleShape,
                                        pressedScale = 0.88f,
                                        role = Role.Button,
                                        onClick = {
                                            haptics.click()
                                            viewModel.onStartAdd()
                                        },
                                    )
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = QuizzenIcons.Plus,
                                    contentDescription = stringResource(R.string.add_word),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    },
                )

                val errorMessage = state.errorMessage
                if (errorMessage != null) {
                    FirebaseErrorBanner(
                        message = errorMessage,
                        onDismiss = {
                            haptics.click()
                            viewModel.onDismissError()
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                }

                AnimatedVisibility(
                    visible = state.isSearchActive,
                    enter = fadeIn(tween(200)) + expandVertically(tween(250, easing = EmphasizedEasing)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
                ) {
                    Column {
                        VocabularySearchBar(
                            query = state.searchQuery,
                            onQueryChange = viewModel::onSearchQueryChange,
                            onClear = {
                                haptics.tick()
                                viewModel.onSearchQueryChange("")
                            },
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.vocabulary_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    ) {
                        val total = state.filteredEntries.size
                        Text(
                            text = stringResource(R.string.word_count_badge, total),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("vocabulary_list"),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (state.filteredEntries.isEmpty()) {
                    item(key = "empty_state") {
                        VocabularyEmptyState(
                            title = if (state.entries.isEmpty()) {
                                stringResource(R.string.vocabulary_empty_title)
                            } else {
                                stringResource(R.string.vocabulary_no_matches_title)
                            },
                            subtitle = if (state.entries.isEmpty()) {
                                stringResource(R.string.vocabulary_empty_subtitle)
                            } else {
                                stringResource(R.string.vocabulary_no_matches_subtitle)
                            },
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                } else {
                    items(
                        items = state.filteredEntries,
                        key = { it.id },
                    ) { entry ->
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

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .cssShadow(
                        NeutralShadowColor.copy(alpha = 0.25f),
                        offsetY = (-6).dp,
                        blur = 20.dp,
                        spread = (-16).dp,
                        shape = RectangleShape,
                    ),
                color = Color.Transparent,
            ) {
                Column(
                    modifier = Modifier
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
