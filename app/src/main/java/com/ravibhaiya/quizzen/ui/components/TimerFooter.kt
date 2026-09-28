package com.ravibhaiya.quizzen.ui.components

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.TimerInput
import com.ravibhaiya.quizzen.ui.theme.QuizzenShapes
import com.ravibhaiya.quizzen.ui.theme.quizzen

/**
 * Bottom bar shared by Multiply and Tables configuration: Timer card, inline max-value error, Start button.
 * Sits on its own surface with an upward shadow and keeps clear of the nav bar / keyboard.
 */
@Composable
fun TimerFooter(
    timer: TimerInput,
    onTimerChange: (String) -> Unit,
    onTimerFocusLost: () -> Unit,
    startEnabled: Boolean,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 18.dp),
        ) {
            TimerCard(
                timer = timer,
                onTimerChange = onTimerChange,
                onTimerFocusLost = onTimerFocusLost,
                modifier = Modifier.padding(bottom = 18.dp),
            )
            AnimatedVisibility(
                visible = timer.exceededMax,
                enter = fadeIn(tween(150)) + expandVertically(tween(150)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
            ) {
                Text(
                    text = stringResource(R.string.timer_max_error, TimerInput.MAX_SECONDS),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.6.sp),
                    color = MaterialTheme.quizzen.error,
                    modifier = Modifier.padding(start = 2.dp, bottom = 16.dp),
                )
            }
            PrimaryButton(text = stringResource(R.string.start), onClick = onStart, enabled = startEnabled)
        }
    }
}

@Composable
private fun TimerCard(
    timer: TimerInput,
    onTimerChange: (String) -> Unit,
    onTimerFocusLost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var hadFocus by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (timer.exceededMax) MaterialTheme.quizzen.error else Color.Transparent,
        animationSpec = tween(150),
        label = "timerBorder",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BlobIcon(icon = QuizzenIcons.Timer, shape = QuizzenShapes.BlobA, size = 52.dp, iconSize = 24.dp)
        Text(
            text = stringResource(R.string.timer),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.6.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Column(
            modifier = Modifier
                .widthIn(min = 64.dp)
                .border(2.dp, borderColor, RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicTextField(
                value = timer.text,
                onValueChange = onTimerChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .width(48.dp)
                    .onFocusChanged { state ->
                        if (hadFocus && !state.isFocused) onTimerFocusLost()
                        hadFocus = state.isFocused
                    },
            )
            Text(
                text = stringResource(R.string.timer_unit).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
