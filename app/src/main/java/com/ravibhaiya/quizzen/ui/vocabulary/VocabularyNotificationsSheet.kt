package com.ravibhaiya.quizzen.ui.vocabulary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.DecisionDuplicateKind
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.ui.components.ContentMaxWidth
import com.ravibhaiya.quizzen.ui.components.PrimaryButton
import com.ravibhaiya.quizzen.ui.components.QuizzenIconButton
import com.ravibhaiya.quizzen.ui.components.QuizzenIcons
import com.ravibhaiya.quizzen.ui.components.bouncyClickable
import com.ravibhaiya.quizzen.ui.theme.QuizzenShapes
import com.ravibhaiya.quizzen.ui.theme.quizzen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatabaseNotificationsSheet(
    requests: List<DatabaseApprovalRequest>,
    hasEntries: Boolean,
    isDecisionRunning: Boolean,
    isDecisionPendingRetry: Boolean = false,
    decisionRetryRemainingSeconds: Int = 0,
    decisionError: String?,
    onRunDecisionCheck: () -> Unit,
    onDismissDecisionError: () -> Unit,
    onDismissDecisionFinding: (String) -> Unit,
    onReviewMerge: (wordKey: String) -> Unit,
    onReviewAllMerges: () -> Unit,
    onDeleteDuplicates: (List<VocabularyEntry>) -> Unit,
    onEditEntry: (VocabularyEntry) -> Unit,
    onDeleteEntry: (VocabularyEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val mergeRequestsCount = requests.count {
        it is DatabaseApprovalRequest.MergeDuplicateWord && it.mergeItem.hasChanges
    }
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
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.notifications_sheet_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.notifications_sheet_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                QuizzenIconButton(
                    icon = QuizzenIcons.CloseBold,
                    contentDescription = stringResource(R.string.cd_close_sheet),
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_notifications_button"),
                )
            }

            if (hasEntries) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isDecisionRunning) 0.65f else 1f)
                        .testTag("verify_gemini_decision_button")
                        .let { mod ->
                            if (!isDecisionRunning) {
                                mod.bouncyClickable(CircleShape, role = Role.Button, onClick = onRunDecisionCheck)
                            } else {
                                mod
                            }
                        }
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when {
                            isDecisionRunning -> stringResource(R.string.notification_verifying_gemini_decision)
                            isDecisionPendingRetry -> stringResource(
                                R.string.notification_pending_retry_button,
                                decisionRetryRemainingSeconds.coerceAtLeast(1),
                            )
                            else -> stringResource(R.string.notification_verify_gemini_decision)
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (decisionError != null) {
                val errShape = RoundedCornerShape(18.dp)
                val toneColor = if (isDecisionPendingRetry) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.quizzen.error
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("decision_error_banner")
                        .clip(errShape)
                        .background(toneColor.copy(alpha = 0.12f), errShape)
                        .border(1.dp, toneColor.copy(alpha = 0.35f), errShape)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isDecisionPendingRetry) {
                        Box(
                            modifier = Modifier
                                .testTag("pending_verification_badge")
                                .background(toneColor.copy(alpha = 0.18f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.notification_pending_retry_badge,
                                    decisionRetryRemainingSeconds.coerceAtLeast(1),
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = toneColor,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = decisionError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDecisionPendingRetry) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.quizzen.error
                            },
                            modifier = Modifier.weight(1f),
                        )
                        if (!isDecisionRunning) {
                            Box(
                                modifier = Modifier
                                    .testTag("retry_decision_check_button")
                                    .bouncyClickable(CircleShape, role = Role.Button, onClick = onRunDecisionCheck)
                                    .background(toneColor.copy(alpha = 0.18f), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(
                                        if (isDecisionPendingRetry) {
                                            R.string.verify_now_action
                                        } else {
                                            R.string.retry_action
                                        },
                                    ),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = toneColor,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("dismiss_decision_error_button")
                                .bouncyClickable(CircleShape, pressedScale = 0.88f, onClick = onDismissDecisionError)
                                .background(toneColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = QuizzenIcons.CloseBold,
                                contentDescription = stringResource(R.string.cd_dismiss_error),
                                tint = toneColor,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }

            if ((isDecisionRunning || isDecisionPendingRetry) && requests.isEmpty()) {
                NotificationsSkeletonState(
                    isPendingRetry = isDecisionPendingRetry && !isDecisionRunning,
                    retryRemainingSeconds = decisionRetryRemainingSeconds,
                )
            } else if (requests.isEmpty()) {
                NotificationsEmptyState()
            } else {
                if (mergeRequestsCount > 1) {
                    PrimaryButton(
                        text = stringResource(R.string.notification_review_all_merges, mergeRequestsCount),
                        onClick = onReviewAllMerges,
                        modifier = Modifier.testTag("review_all_merges_button"),
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    for (request in requests) {
                        when (request) {
                            is DatabaseApprovalRequest.MergeDuplicateWord -> {
                                MergeApprovalRequestCard(
                                    request = request,
                                    onReview = { onReviewMerge(request.wordKey) },
                                    onDeleteDuplicate = { onDeleteDuplicates(request.redundantEntries) },
                                    onKeepBoth = request.dismissKey?.let { key ->
                                        { onDismissDecisionFinding(key) }
                                    },
                                )
                            }
                            is DatabaseApprovalRequest.EntryIssue -> {
                                IssueApprovalRequestCard(
                                    request = request,
                                    onEdit = { onEditEntry(request.entry) },
                                    onDelete = { onDeleteEntry(request.entry) },
                                    onDismissIssue = request.dismissKey?.let { key ->
                                        { onDismissDecisionFinding(key) }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsSkeletonState(
    isPendingRetry: Boolean = false,
    retryRemainingSeconds: Int = 0,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notifications_skeleton_state"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (isPendingRetry) {
                stringResource(
                    R.string.notification_pending_skeleton_subtitle,
                    retryRemainingSeconds.coerceAtLeast(1),
                )
            } else {
                stringResource(R.string.notification_skeleton_subtitle)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        repeat(2) { index ->
            val shape = RoundedCornerShape(22.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(width = 112.dp, height = 22.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f), CircleShape),
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 86.dp, height = 22.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (index == 0) 0.56f else 0.44f)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .height(14.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f), CircleShape),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationsEmptyState() {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notifications_empty_state")
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, QuizzenShapes.BlobA),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = QuizzenIcons.Bell,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(
            text = stringResource(R.string.notifications_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.notifications_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MergeApprovalRequestCard(
    request: DatabaseApprovalRequest.MergeDuplicateWord,
    onReview: () -> Unit,
    onDeleteDuplicate: () -> Unit,
    onKeepBoth: (() -> Unit)? = null,
) {
    val item = request.mergeItem
    val shape = RoundedCornerShape(22.dp)
    val badgeTextRes = when (request.decisionKind) {
        DecisionDuplicateKind.ROOT_OR_INFLECTED -> R.string.notification_badge_root_form
        DecisionDuplicateKind.SPELLING_OR_TYPO -> R.string.notification_badge_spelling_variant
        DecisionDuplicateKind.EXACT_DUPLICATE, null -> {
            if (item.hasChanges) R.string.notification_badge_merge else R.string.notification_badge_duplicate
        }
    }
    val usePrimaryBadge = item.hasChanges || request.decisionKind != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("approval_request_${request.id}")
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(
                        if (usePrimaryBadge) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.quizzen.error.copy(alpha = 0.14f)
                        },
                        CircleShape,
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(badgeTextRes),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = if (usePrimaryBadge) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.quizzen.error
                    },
                )
            }
            if (request.confidencePercent != null) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.notification_confidence_badge, request.confidencePercent),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            text = if (request.matchedWord != null) {
                "${request.word} ↔ ${request.matchedWord}"
            } else {
                request.word
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = if (request.matchedWord != null) {
                stringResource(R.string.notification_variant_card_subtitle, request.matchedWord, request.word)
            } else {
                stringResource(R.string.notification_merge_card_subtitle, request.duplicateCount, request.word)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (item.addedSynonyms.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.merge_item_new_synonyms,
                    item.addedSynonyms.size,
                    item.addedSynonyms.joinToString(", "),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
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
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (item.mainDetailsEnriched) {
            Text(
                text = stringResource(R.string.merge_item_main_details_filled),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (item.enrichedRelations > 0) {
            Text(
                text = stringResource(R.string.merge_item_relations_enriched, item.enrichedRelations),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (!item.hasChanges) {
            Text(
                text = stringResource(R.string.merge_item_no_changes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (onKeepBoth != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("keep_both_${request.wordKey}")
                        .bouncyClickable(CircleShape, role = Role.Button, onClick = onKeepBoth)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.keep_both_words),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .testTag("delete_duplicate_${request.wordKey}")
                    .bouncyClickable(CircleShape, role = Role.Button, onClick = onDeleteDuplicate)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.delete_duplicate),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.quizzen.error,
                    textAlign = TextAlign.Center,
                )
            }
            if (item.hasChanges) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("review_merge_${request.wordKey}")
                        .bouncyClickable(CircleShape, role = Role.Button, onClick = onReview)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.review_merge),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IssueApprovalRequestCard(
    request: DatabaseApprovalRequest.EntryIssue,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismissIssue: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("approval_request_${request.id}")
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.quizzen.error.copy(alpha = 0.14f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(
                        if (request.isAiConflict) {
                            R.string.notification_badge_meaning_conflict
                        } else {
                            R.string.notification_badge_issue
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.quizzen.error,
                )
            }
            if (request.confidencePercent != null) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.notification_confidence_badge, request.confidencePercent),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            text = request.word,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (!request.conflictingWord.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.quizzen.error.copy(alpha = 0.10f),
                        RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = stringResource(R.string.notification_conflict_word_label, request.conflictingWord),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.quizzen.error,
                )
            }
        }

        Text(
            text = request.reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (onDismissIssue != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dismiss_issue_${request.entry.id}")
                        .bouncyClickable(CircleShape, role = Role.Button, onClick = onDismissIssue)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.dismiss_issue),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .testTag("delete_issue_${request.entry.id}")
                    .bouncyClickable(CircleShape, role = Role.Button, onClick = onDelete)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.delete),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.quizzen.error,
                    textAlign = TextAlign.Center,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .testTag("edit_issue_${request.entry.id}")
                    .bouncyClickable(CircleShape, role = Role.Button, onClick = onEdit)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.edit_json),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
