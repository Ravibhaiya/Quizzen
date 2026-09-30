package com.ravibhaiya.quizzen.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Choice pill (e.g. "3 Digits"). Radius 20, selected = primary fill with soft colored shadow.
 * Pass `role = Role.Checkbox` for multi-select groups so screen readers announce it correctly.
 */
@Composable
fun OptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.RadioButton,
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(20.dp)
    val container by animateColorAsState(
        targetValue = if (selected) primary else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(200),
        label = "chipContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "chipContent",
    )
    val chipShadow by animateColorAsState(
        targetValue = primary.copy(alpha = if (selected) 0.55f else 0f),
        animationSpec = tween(150),
        label = "chipShadow",
    )
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(source)
            .cssShadow(chipShadow, offsetY = 8.dp, blur = 16.dp, spread = (-6).dp, shape = shape)
            .clip(shape)
            .background(container)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = role,
                onClick = onClick,
            )
            .semantics { this.selected = selected }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.32.sp, fontWeight = FontWeight.Bold),
            color = content,
        )
    }
}

/** Multi-select number cell in the Tables grid (radius 22, aspect 1 : 0.95). */
@Composable
fun NumberCell(
    number: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(22.dp)
    val container by animateColorAsState(
        targetValue = if (selected) primary else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(150),
        label = "cellContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(150),
        label = "cellContent",
    )
    val cellShadow by animateColorAsState(
        targetValue = primary.copy(alpha = if (selected) 0.55f else 0f),
        animationSpec = tween(150),
        label = "cellShadow",
    )
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / 0.95f)
            .pressScale(source)
            .cssShadow(cellShadow, offsetY = 8.dp, blur = 16.dp, spread = (-8).dp, shape = shape)
            .clip(shape)
            .background(container)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.Checkbox,
                onClick = onClick,
            )
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.6.sp, letterSpacing = 0.sp),
            color = content,
            textAlign = TextAlign.Center,
        )
    }
}
