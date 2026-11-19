package com.ravibhaiya.quizzen.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.data.DataStoreSettingsRepository
import com.ravibhaiya.quizzen.ui.components.ContentMaxWidth
import com.ravibhaiya.quizzen.ui.components.QuizzenIcons
import com.ravibhaiya.quizzen.ui.components.QuizzenSwitch
import com.ravibhaiya.quizzen.ui.settings.ApiKeyTestState
import com.ravibhaiya.quizzen.ui.theme.quizzen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    hapticEnabled: Boolean,
    onHapticChange: (Boolean) -> Unit,
    onDatabaseManager: () -> Unit,
    onDismiss: () -> Unit,
    geminiApiKey: String = "",
    onGeminiApiKeyChange: (String) -> Unit = {},
    apiKeyTestState: ApiKeyTestState = ApiKeyTestState.Idle,
    onTestGeminiApiKey: (String) -> Unit = {},
    onClearApiKeyTest: () -> Unit = {},
) {
    var showApiKeyEditor by rememberSaveable { mutableStateOf(false) }
    var isApiKeyVisible by rememberSaveable { mutableStateOf(false) }
    val effectiveKey = DataStoreSettingsRepository.effectiveGeminiApiKey(geminiApiKey)
    var draftApiKey by rememberSaveable(geminiApiKey) { mutableStateOf(effectiveKey) }
    val clipboardManager = LocalClipboardManager.current
    val isKeyConfigured = effectiveKey.isNotEmpty()
    val sanitizedDraft = DataStoreSettingsRepository.sanitizeApiKey(draftApiKey)

    val commitDraftKeyIfChanged = {
        val cleaned = DataStoreSettingsRepository.sanitizeApiKey(draftApiKey)
        if (cleaned.isNotEmpty() && cleaned != geminiApiKey) {
            onGeminiApiKeyChange(cleaned)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            commitDraftKeyIfChanged()
            onClearApiKeyTest()
            onDismiss()
        },
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
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 26.dp),
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.8.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            SettingsRow(
                icon = QuizzenIcons.Vibrate,
                label = stringResource(R.string.haptic_feedback),
                modifier = Modifier.toggleable(
                    value = hapticEnabled,
                    role = Role.Switch,
                    onValueChange = onHapticChange,
                ),
            ) {
                QuizzenSwitch(checked = hapticEnabled)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            SettingsRow(
                icon = QuizzenIcons.Database,
                label = stringResource(R.string.database_manager),
                modifier = Modifier.clickable(
                    role = Role.Button,
                    onClick = {
                        commitDraftKeyIfChanged()
                        onClearApiKeyTest()
                        onDatabaseManager()
                    },
                ),
            ) {
                Icon(
                    QuizzenIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            SettingsRow(
                icon = QuizzenIcons.Key,
                label = stringResource(R.string.gemini_api_key),
                modifier = Modifier
                    .testTag("settings_gemini_api_key_row")
                    .clickable(
                        role = Role.Button,
                        onClick = { showApiKeyEditor = !showApiKeyEditor },
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isKeyConfigured) {
                                    MaterialTheme.quizzen.success.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                                shape = RoundedCornerShape(999.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(
                                if (isKeyConfigured) {
                                    R.string.gemini_api_key_status_saved
                                } else {
                                    R.string.gemini_api_key_status_missing
                                },
                            ),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (isKeyConfigured) {
                                MaterialTheme.quizzen.success
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    Icon(
                        imageVector = if (showApiKeyEditor) QuizzenIcons.ChevronDown else QuizzenIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            AnimatedVisibility(visible = showApiKeyEditor) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(20.dp),
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.gemini_api_key_subtitle),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    OutlinedTextField(
                        value = draftApiKey,
                        onValueChange = {
                            draftApiKey = it
                            onClearApiKeyTest()
                        },
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.gemini_api_key_placeholder),
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { isApiKeyVisible = !isApiKeyVisible },
                                    modifier = Modifier.testTag("toggle_api_key_visibility_button"),
                                ) {
                                    Icon(
                                        imageVector = if (isApiKeyVisible) QuizzenIcons.EyeOff else QuizzenIcons.Eye,
                                        contentDescription = stringResource(
                                            if (isApiKeyVisible) R.string.cd_hide_api_key else R.string.cd_show_api_key,
                                        ),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                if (draftApiKey.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            draftApiKey = ""
                                            onClearApiKeyTest()
                                        },
                                        modifier = Modifier.testTag("clear_draft_api_key_button"),
                                    ) {
                                        Icon(
                                            imageVector = QuizzenIcons.CloseBold,
                                            contentDescription = stringResource(R.string.cd_clear_api_key_input),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_api_key_input"),
                    )

                    AnimatedVisibility(visible = apiKeyTestState !is ApiKeyTestState.Idle) {
                        ApiKeyTestStatusBanner(state = apiKeyTestState)
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clip = DataStoreSettingsRepository.sanitizeApiKey(
                                    clipboardManager.getText()?.text.orEmpty(),
                                )
                                if (clip.isNotEmpty()) {
                                    draftApiKey = clip
                                    onClearApiKeyTest()
                                    onGeminiApiKeyChange(clip)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("paste_gemini_api_key_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.gemini_api_key_paste),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        OutlinedButton(
                            onClick = { onTestGeminiApiKey(draftApiKey) },
                            enabled = sanitizedDraft.isNotEmpty() && apiKeyTestState !is ApiKeyTestState.Testing,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("test_gemini_api_key_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.gemini_api_key_test),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        if (geminiApiKey.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    draftApiKey = ""
                                    onClearApiKeyTest()
                                    onGeminiApiKeyChange("")
                                    showApiKeyEditor = false
                                },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("remove_gemini_api_key_button"),
                            ) {
                                Text(
                                    text = stringResource(R.string.gemini_api_key_clear),
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                                    color = MaterialTheme.quizzen.error,
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        Button(
                            onClick = {
                                onGeminiApiKeyChange(sanitizedDraft)
                                showApiKeyEditor = false
                            },
                            enabled = sanitizedDraft.isNotEmpty(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("save_gemini_api_key_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.gemini_api_key_save),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiKeyTestStatusBanner(
    state: ApiKeyTestState,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val extra = MaterialTheme.quizzen
    val bgColor = when (state) {
        ApiKeyTestState.Valid -> extra.success.copy(alpha = 0.14f)
        is ApiKeyTestState.Invalid -> extra.error.copy(alpha = 0.14f)
        else -> colors.surface
    }
    val fgColor = when (state) {
        ApiKeyTestState.Valid -> extra.success
        is ApiKeyTestState.Invalid -> extra.error
        else -> colors.onSurfaceVariant
    }
    val text = when (state) {
        ApiKeyTestState.Testing -> stringResource(R.string.gemini_api_key_testing)
        ApiKeyTestState.Valid -> stringResource(R.string.gemini_api_key_test_valid)
        is ApiKeyTestState.Invalid -> state.message
        ApiKeyTestState.Idle -> ""
    }
    if (text.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("api_key_test_status"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state) {
            ApiKeyTestState.Testing -> CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = colors.primary,
            )
            ApiKeyTestState.Valid -> Icon(
                imageVector = QuizzenIcons.CheckBold,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(14.dp),
            )
            is ApiKeyTestState.Invalid -> Icon(
                imageVector = QuizzenIcons.CloseBold,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(14.dp),
            )
            ApiKeyTestState.Idle -> Unit
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = fgColor,
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.64.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}
