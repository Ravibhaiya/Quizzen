package com.ravibhaiya.quizzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A range picked from a short list of labelled places (the letters A-Z, the fractions 1/2-1/50), laid out like the Powers & Roots
 * range cards: what the left thumb picked in a pill at the left, what the right thumb picked in a pill at the right, and one
 * two-thumb slider under them with the label of the first and of the last place at the two ends of the track.
 *
 * [range], [lowest] and [highest] are slider positions; [firstLabel] / [lastLabel] are the pills, [lowestLabel] / [highestLabel]
 * the end labels, and [summary] is what a screen reader says for the two pills together.
 */
@Composable
fun PillRangeCard(
    range: IntRange,
    lowest: Int,
    highest: Int,
    firstLabel: String,
    lastLabel: String,
    lowestLabel: String,
    highestLabel: String,
    summary: String,
    startThumbDescription: String,
    endThumbDescription: String,
    onRangeChange: (IntRange) -> Unit,
    onRangeChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .cssShadow(
                NeutralShadowColor.copy(alpha = 0.05f),
                offsetY = 2.dp, blur = 6.dp, spread = 0.dp, shape = shape,
            )
            .background(colors.surfaceContainer, shape)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = summary },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RangePill(firstLabel)
            RangePill(lastLabel)
        }
        QuizzenRangeSlider(
            range = range,
            lowest = lowest,
            highest = highest,
            onRangeChange = onRangeChange,
            onRangeChangeFinished = onRangeChangeFinished,
            startThumbDescription = startThumbDescription,
            endThumbDescription = endThumbDescription,
            modifier = Modifier.padding(top = 12.dp),
        )
        // The two end labels line up with the ends of the track (half a handle in from the card edge).
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val endStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
            Text(lowestLabel, style = endStyle, color = colors.onSurfaceVariant)
            Text(highestLabel, style = endStyle, color = colors.onSurfaceVariant)
        }
    }
}

/** A picked place in a pill: `primaryContainer`, fully rounded, wide enough that short labels all get the same pill. */
@Composable
private fun RangePill(label: String) {
    Box(
        modifier = Modifier
            .widthIn(min = 56.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
