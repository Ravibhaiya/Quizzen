package com.ravibhaiya.quizzen.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Heading of one section of a setup screen. */
@Composable
fun SectionTitle(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 14.dp),
    )
}

/**
 * One full-width choice, just its name: the Alphabet challenge types (pick one) and the Fraction & Percentage directions (pick
 * one or more, [multiSelect]). Same selection language as the option chips and the table cells: primary fill with a soft
 * coloured shadow when chosen, plain `surfaceContainer` otherwise; a radio mark on the left says "pick one", a check box says
 * "pick any".
 */
@Composable
fun ChoiceCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    multiSelect: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val container by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.surfaceContainer,
        animationSpec = tween(200),
        label = "choiceContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.onSurface,
        animationSpec = tween(200),
        label = "choiceContent",
    )
    val glow by animateColorAsState(
        targetValue = colors.primary.copy(alpha = if (selected) 0.55f else 0f),
        animationSpec = tween(150),
        label = "choiceShadow",
    )
    val source = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(source)
            .cssShadow(glow, offsetY = 8.dp, blur = 16.dp, spread = (-6).dp, shape = shape)
            .clip(shape)
            .background(container)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = if (multiSelect) Role.Checkbox else Role.RadioButton,
                onClick = onClick,
            )
            .semantics { this.selected = selected }
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (multiSelect) {
            CheckMark(selected = selected, color = content, checkTint = container)
        } else {
            RadioMark(selected = selected, color = content)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.32.sp, fontWeight = FontWeight.Bold),
            color = content,
            modifier = Modifier.weight(1f),
        )
    }
}

/** The check box of a multi-select card: a rounded square, filled with a check when chosen. */
@Composable
private fun CheckMark(selected: Boolean, color: Color, checkTint: Color) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(2.dp, color, shape)
            .background(if (selected) color else Color.Transparent, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(QuizzenIcons.CheckBold, contentDescription = null, tint = checkTint, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun RadioMark(selected: Boolean, color: Color) {
    Box(
        modifier = Modifier.size(24.dp).border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(12.dp).background(color, CircleShape))
    }
}
