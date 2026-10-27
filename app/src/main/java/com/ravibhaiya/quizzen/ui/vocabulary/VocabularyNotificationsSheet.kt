package com.ravibhaiya.quizzen.ui.vocabulary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

            if (requests.isEmpty()) {
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
                                )
                            }
                            is DatabaseApprovalRequest.EntryIssue -> {
                                IssueApprovalRequestCard(
                                    request = request,
                                    onEdit = { onEditEntry(request.entry) },
                                    onDelete = { onDeleteEntry(request.entry) },
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

@Composable
private fun MergeApprovalRequestCard(
    request: DatabaseApprovalRequest.MergeDuplicateWord,
    onReview: () -> Unit,
    onDeleteDuplicate: () -> Unit,
) {
    val item = request.mergeItem
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(
                        if (item.hasChanges) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.quizzen.error.copy(alpha = 0.14f)
                        },
                        CircleShape,
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(
                        if (item.hasChanges) {
                            R.string.notification_badge_merge
                        } else {
                            R.string.notification_badge_duplicate
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = if (item.hasChanges) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.quizzen.error
                    },
                )
            }
            Text(
                text = request.word,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Text(
            text = stringResource(R.string.notification_merge_card_subtitle, request.duplicateCount, request.word),
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

@Composable
private fun IssueApprovalRequestCard(
    request: DatabaseApprovalRequest.EntryIssue,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.quizzen.error.copy(alpha = 0.14f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.notification_badge_issue),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.quizzen.error,
                )
            }
            Text(
                text = request.word,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
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
