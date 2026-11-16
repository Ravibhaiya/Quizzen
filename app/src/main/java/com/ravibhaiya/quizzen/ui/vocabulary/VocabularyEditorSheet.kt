package com.ravibhaiya.quizzen.ui.vocabulary

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.ui.components.ContentMaxWidth
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.PrimaryButton
import com.ravibhaiya.quizzen.ui.components.QuizzenIconButton
import com.ravibhaiya.quizzen.ui.components.QuizzenIcons
import com.ravibhaiya.quizzen.ui.components.bouncyClickable
import com.ravibhaiya.quizzen.ui.components.softShadow
import com.ravibhaiya.quizzen.ui.theme.quizzen

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VocabularyEditorSheet(
    state: VocabularyEditorState,
    onUpdateJsonText: (String) -> Unit,
    onUpdateConfirmText: (String) -> Unit = {},
    onSelectFocusedAction: (FocusedRelationAction) -> Unit = {},
    onImportJsonFile: () -> Unit = {},
    onInsertTemplate: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focused = state.focusedRelation
    val isFocusedWordMode = focused != null && focused.action != FocusedRelationAction.FULL_ENTRY_JSON

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = ContentMaxWidth,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color(0x730A0818),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 14.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isFocusedWordMode ->
                                stringResource(
                                    R.string.edit_focused_word_title,
                                    focused.target.word,
                                    state.editingWordLabel.orEmpty(),
                                )
                            state.isEditing ->
                                stringResource(R.string.edit_word_json_title, state.editingWordLabel.orEmpty())
                            else ->
                                stringResource(R.string.add_words_json_title)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            isFocusedWordMode ->
                                stringResource(R.string.edit_focused_word_subtitle, state.editingWordLabel.orEmpty())
                            state.isEditing ->
                                stringResource(R.string.edit_word_json_subtitle)
                            else ->
                                stringResource(R.string.add_words_json_subtitle)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                QuizzenIconButton(
                    icon = QuizzenIcons.CloseBold,
                    contentDescription = stringResource(R.string.cd_close_sheet),
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_editor_button"),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (focused != null) {
                    FocusedConflictHeader(
                        focused = focused,
                        parentWordLabel = state.editingWordLabel.orEmpty(),
                        onSelectAction = onSelectFocusedAction,
                    )
                }

                if (focused?.action == FocusedRelationAction.REMOVE_WORD) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("focused_remove_word_banner")
                            .background(MaterialTheme.quizzen.error.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                            .border(1.5.dp, MaterialTheme.quizzen.error.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.focused_remove_word_summary,
                                focused.target.conflictingWordLabel,
                                state.editingWordLabel.orEmpty(),
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.quizzen.error,
                        )
                    }
                } else {
                    if (focused?.action == FocusedRelationAction.MOVE_TO_OPPOSITE_LIST) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("focused_move_word_banner")
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    if (focused.target.isAntonym) {
                                        R.string.focused_move_to_synonyms_summary
                                    } else {
                                        R.string.focused_move_to_antonyms_summary
                                    },
                                    focused.target.word,
                                    state.editingWordLabel.orEmpty(),
                                ),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (isFocusedWordMode) {
                                focused.target.conflictingWordLabel.uppercase()
                            } else {
                                stringResource(R.string.json_payload_label).uppercase()
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = 4.dp),
                        )
                        if (!state.isEditing) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .testTag("import_json_file_button")
                                        .bouncyClickable(CircleShape, role = Role.Button, onClick = onImportJsonFile)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = QuizzenIcons.Upload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.import_json_file),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .testTag("insert_json_template_button")
                                        .bouncyClickable(CircleShape, role = Role.Button, onClick = onInsertTemplate)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(R.string.insert_json_template),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    JsonCodeField(
                        value = state.jsonText,
                        placeholder = stringResource(
                            if (state.isEditing) R.string.hint_single_word_json else R.string.hint_bulk_words_json,
                        ),
                        onValueChange = onUpdateJsonText,
                        hasError = state.validationError != null,
                    )
                }
            }

            if (state.isEditing) {
                Spacer(Modifier.height(12.dp))
                ConfirmKeywordField(
                    label = stringResource(R.string.type_confirm_to_edit),
                    value = state.confirmText,
                    placeholder = stringResource(R.string.hint_type_confirm),
                    isMatched = state.canSave,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onValueChange = onUpdateConfirmText,
                    testTag = "edit_confirm_input",
                )
            }

            if (state.validationError != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(MaterialTheme.quizzen.error.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = QuizzenIcons.CloseBold,
                        contentDescription = null,
                        tint = MaterialTheme.quizzen.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = state.validationError,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.8.sp),
                        color = MaterialTheme.quizzen.error,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            PrimaryButton(
                text = stringResource(if (state.isEditing) R.string.save_word else R.string.save_words),
                onClick = onSave,
                enabled = state.canSave && !state.isSaving,
                modifier = Modifier.testTag("save_word_button"),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusedConflictHeader(
    focused: FocusedRelationEditorContext,
    parentWordLabel: String,
    onSelectAction: (FocusedRelationAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("focused_conflict_header")
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.quizzen.error.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.notification_conflict_word_label,
                    focused.target.conflictingWordLabel,
                ),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.8.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.quizzen.error,
            )
        }
        Text(
            text = focused.target.reason,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FocusedActionPill(
                label = stringResource(R.string.focused_action_edit_word, focused.target.word),
                selected = focused.action == FocusedRelationAction.EDIT_WORD,
                onClick = { onSelectAction(FocusedRelationAction.EDIT_WORD) },
                testTag = "focused_action_edit_word",
            )
            FocusedActionPill(
                label = stringResource(
                    if (focused.target.isAntonym) {
                        R.string.focused_action_move_to_synonyms
                    } else {
                        R.string.focused_action_move_to_antonyms
                    },
                ),
                selected = focused.action == FocusedRelationAction.MOVE_TO_OPPOSITE_LIST,
                onClick = { onSelectAction(FocusedRelationAction.MOVE_TO_OPPOSITE_LIST) },
                testTag = "focused_action_move_word",
            )
            FocusedActionPill(
                label = stringResource(R.string.focused_action_remove_word, focused.target.word),
                selected = focused.action == FocusedRelationAction.REMOVE_WORD,
                onClick = { onSelectAction(FocusedRelationAction.REMOVE_WORD) },
                testTag = "focused_action_remove_word",
            )
            FocusedActionPill(
                label = stringResource(R.string.focused_action_full_json, parentWordLabel),
                selected = focused.action == FocusedRelationAction.FULL_ENTRY_JSON,
                onClick = { onSelectAction(FocusedRelationAction.FULL_ENTRY_JSON) },
                testTag = "focused_action_full_json",
            )
        }
    }
}

@Composable
private fun FocusedActionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(200),
        label = "focusedActionPillBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(200),
        label = "focusedActionPillFg",
    )
    Box(
        modifier = Modifier
            .testTag(testTag)
            .semantics { this.selected = selected }
            .bouncyClickable(CircleShape, role = Role.RadioButton, onClick = onClick)
            .background(bg, CircleShape)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.8.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = contentColor,
        )
    }
}

@Composable
private fun ConfirmKeywordField(
    label: String,
    value: String,
    placeholder: String,
    isMatched: Boolean,
    accentColor: Color,
    onValueChange: (String) -> Unit,
    testTag: String,
    inputModifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    val bgColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(200),
        label = "confirmFieldBg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isMatched -> accentColor
            focused -> accentColor.copy(alpha = 0.45f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(200),
        label = "confirmFieldBorder",
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            ),
            cursorBrush = SolidColor(accentColor),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(inputModifier)
                .testTag(testTag)
                .onFocusChanged { focused = it.isFocused }
                .clip(shape)
                .background(bgColor, shape)
                .border(2.dp, borderColor, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun JsonCodeField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    hasError: Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp)
    val bgColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(200),
        label = "jsonFieldBg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            hasError -> MaterialTheme.quizzen.error
            focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(200),
        label = "jsonFieldBorder",
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = false,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.8.sp,
            lineHeight = 20.sp,
            fontFamily = FontFamily.Monospace,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Default,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .testTag("vocabulary_json_input")
            .onFocusChanged { focused = it.isFocused }
            .softShadow(NeutralShadowColor.copy(alpha = 0.05f), offsetY = 2.dp, blur = 6.dp, spread = 0.dp, shape = shape)
            .clip(shape)
            .background(bgColor, shape)
            .border(2.dp, borderColor, shape)
            .padding(16.dp),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.TopStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            fontSize = 13.2.sp,
                            lineHeight = 19.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }
                innerTextField()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteWordConfirmSheet(
    entries: List<VocabularyEntry>,
    confirmText: String,
    onUpdateConfirmText: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val canDelete = confirmText == VocabularyManagerViewModel.REQUIRED_DELETE_CONFIRM
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = ContentMaxWidth,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color(0x730A0818),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 14.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (entries.size == 1) {
                    stringResource(R.string.delete_word_title, entries.first().wordDetails.word)
                } else {
                    stringResource(R.string.delete_words_title, entries.size)
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (entries.size == 1) {
                    stringResource(R.string.delete_word_message)
                } else {
                    stringResource(R.string.delete_words_message, entries.size)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ConfirmKeywordField(
                label = stringResource(R.string.type_delete_to_confirm),
                value = confirmText,
                placeholder = stringResource(R.string.hint_type_delete),
                isMatched = canDelete,
                accentColor = MaterialTheme.quizzen.error,
                onValueChange = onUpdateConfirmText,
                testTag = "delete_confirm_input",
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cancel_delete_button")
                        .bouncyClickable(CircleShape, role = Role.Button, onClick = onDismiss)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (canDelete) 1f else 0.45f)
                        .testTag("confirm_delete_button")
                        .let { mod ->
                            if (canDelete) {
                                mod.bouncyClickable(CircleShape, role = Role.Button, onClick = onConfirm)
                            } else {
                                mod
                            }
                        }
                        .background(MaterialTheme.quizzen.error, CircleShape)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeWordConfirmSheet(
    prompt: MergePrompt,
    onUpdateConfirmText: (String) -> Unit = {},
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val canConfirm = prompt.canConfirm
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = ContentMaxWidth,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color(0x730A0818),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 14.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (prompt.items.size == 1) {
                    stringResource(R.string.merge_word_title, prompt.existingWord)
                } else {
                    stringResource(R.string.merge_words_title, prompt.items.size)
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.merge_word_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (prompt.newEntriesToSave.isNotEmpty()) {
                    val newWordNames = prompt.newEntriesToSave.joinToString(", ") { it.wordDetails.word }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.merge_new_words_summary,
                                prompt.newEntriesToSave.size,
                                newWordNames,
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                for (item in prompt.items) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = item.existingWord,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (item.addedSynonyms.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.merge_item_new_synonyms,
                                    item.addedSynonyms.size,
                                    item.addedSynonyms.joinToString(", "),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (item.addedAntonyms.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.merge_item_new_antonyms,
                                    item.addedAntonyms.size,
                                    item.addedAntonyms.joinToString(", "),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (item.mainDetailsEnriched) {
                            Text(
                                text = stringResource(R.string.merge_item_main_details_filled),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (item.enrichedRelations > 0) {
                            Text(
                                text = stringResource(R.string.merge_item_relations_enriched, item.enrichedRelations),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!item.hasChanges) {
                            Text(
                                text = stringResource(R.string.merge_item_no_changes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            ConfirmKeywordField(
                label = stringResource(R.string.type_confirm_to_merge),
                value = prompt.confirmText,
                placeholder = stringResource(R.string.hint_type_confirm),
                isMatched = canConfirm,
                accentColor = MaterialTheme.colorScheme.primary,
                onValueChange = onUpdateConfirmText,
                testTag = "merge_confirm_input",
            )

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cancel_merge_button")
                        .bouncyClickable(CircleShape, role = Role.Button, onClick = onDismiss)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (canConfirm) 1f else 0.45f)
                        .testTag("confirm_merge_button")
                        .let { mod ->
                            if (canConfirm) {
                                mod.bouncyClickable(CircleShape, role = Role.Button, onClick = onConfirm)
                            } else {
                                mod
                            }
                        }
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.merge),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
