package com.ravibhaiya.quizzen.ui.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.Feedback
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.ui.components.EmphasizedEasing
import com.ravibhaiya.quizzen.ui.components.QuizzenIcons
import com.ravibhaiya.quizzen.ui.theme.quizzen

/** Colored bottom sheet announcing Correct / Incorrect / Time's Up. Slides in over the practice screen. */
@Composable
fun FeedbackSheet(
    feedback: Feedback?,
    modifier: Modifier = Modifier,
) {
    // Keep the last content around so the exit animation doesn't render an empty sheet.
    val last = remember { arrayOfNulls<Feedback>(1) }
    if (feedback != null) last[0] = feedback

    AnimatedVisibility(
        visible = feedback != null,
        modifier = modifier,
        enter = slideInVertically(tween(350, easing = EmphasizedEasing)) { it },
        exit = slideOutVertically(tween(350, easing = EmphasizedEasing)) { it },
    ) {
        last[0]?.let { FeedbackContent(it) }
    }
}

@Composable
private fun FeedbackContent(feedback: Feedback) {
    val colors = MaterialTheme.quizzen
    val background = when (feedback.type) {
        FeedbackType.Correct -> colors.success
        FeedbackType.Incorrect -> colors.error
        FeedbackType.Timeout -> colors.warning
    }
    val icon = when (feedback.type) {
        FeedbackType.Correct -> QuizzenIcons.CheckBold
        FeedbackType.Incorrect -> QuizzenIcons.CloseBold
        FeedbackType.Timeout -> QuizzenIcons.TimerBold
    }
    val title = when (feedback.type) {
        FeedbackType.Correct -> stringResource(R.string.feedback_correct_title)
        FeedbackType.Incorrect -> stringResource(R.string.feedback_incorrect_title)
        FeedbackType.Timeout -> stringResource(R.string.feedback_timeout_title)
    }
    val subtitle = when (feedback.type) {
        FeedbackType.Correct -> stringResource(R.string.feedback_correct_sub)
        FeedbackType.Incorrect -> stringResource(R.string.feedback_incorrect_sub, feedback.correctAnswer)
        FeedbackType.Timeout -> stringResource(R.string.feedback_timeout_sub, feedback.correctAnswer)
    }
    val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, shape)
            .background(background, shape)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(22.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color.White.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.4.sp, fontWeight = FontWeight.ExtraBold),
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.08.sp, fontWeight = FontWeight.Normal),
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
