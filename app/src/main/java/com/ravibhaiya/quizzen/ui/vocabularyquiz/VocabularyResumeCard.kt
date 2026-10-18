package com.ravibhaiya.quizzen.ui.vocabularyquiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.ui.components.PrimaryButton
import com.ravibhaiya.quizzen.ui.components.bouncyClickable

/**
 * Shown at the top of the Vocabulary setup screen while an unfinished quiz is kept on the device: where the user stopped,
 * a **Resume** button (continues from that question) and **Discard** (forgets it and goes back to a fresh setup).
 */
@Composable
fun VocabularyResumeCard(
    info: VocabularyResumeInfo,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.vocabulary_resume_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.vocabulary_resume_progress, info.questionNumber, info.totalQuestions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryButton(text = stringResource(R.string.vocabulary_resume_action), onClick = onResume)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .bouncyClickable(RoundedCornerShape(18.dp), onClick = onDiscard),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.vocabulary_resume_discard),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
