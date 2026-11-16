package com.ravibhaiya.quizzen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravibhaiya.quizzen.R

/** Back button + title row shared by Multiply, Tables and Practice. [trailing] lands at the far end. */
@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuizzenIconButton(
            icon = QuizzenIcons.ChevronLeft,
            contentDescription = stringResource(R.string.back),
            onClick = onBack,
        )
        FitText(
            text = AnnotatedString(title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            maxFontSize = 24.sp,
            minFontSize = 12.sp,
            step = 0.5.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}
