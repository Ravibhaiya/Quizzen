package com.ravibhaiya.quizzen.ui.powers

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.PowerRootType
import com.ravibhaiya.quizzen.domain.PowersRootsRules
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.cssShadow
import com.ravibhaiya.quizzen.ui.theme.quizzen

/** Both bars use the same scale (1 to the largest limit), so a bar for 20 is visibly shorter than a bar for 30. */
private const val SCALE_MAX = PowersRootsRules.MAX_ALLOWED

/**
 * The two limit cards under the Min/Max fields. They replace the plain grey text lines: each card names a kind, shows the
 * numbers that will really be used (or the allowed 1 to limit before anything is chosen), a bar of that range on a scale
 * up to 30 with the unavailable part dotted, and the limit. Kinds that are not selected fade out.
 */
@Composable
fun LimitCards(models: List<LimitCardModel>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        models.forEach { model ->
            LimitCard(model, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun LimitCard(model: LimitCardModel, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    val title = stringResource(
        if (model.family == PowerRootType.Family.Square) R.string.limit_title_squares else R.string.limit_title_cubes,
    )
    val value = when (model.status) {
        LimitCardModel.Status.Using -> stringResource(R.string.limit_range, model.used!!.first, model.used.last)
        LimitCardModel.Status.NoNumbers -> stringResource(R.string.limit_none)
        LimitCardModel.Status.Idle -> stringResource(R.string.limit_range, PowersRootsRules.MIN_ALLOWED, model.limit)
    }
    val caption = if (model.cut) {
        stringResource(R.string.limit_cut, model.limit)
    } else {
        stringResource(R.string.limit_caption, model.limit)
    }
    val description = when (model.status) {
        LimitCardModel.Status.Using ->
            stringResource(R.string.limit_a11y_using, title, model.used!!.first, model.used.last, model.limit)
        LimitCardModel.Status.NoNumbers -> stringResource(R.string.limit_a11y_none, title, model.limit)
        LimitCardModel.Status.Idle -> stringResource(R.string.limit_a11y_idle, title, model.limit)
    }

    val fade by animateFloatAsState(if (model.dimmed) 0.45f else 1f, tween(200), label = "limitCardFade")
    val valueColor = when (model.status) {
        LimitCardModel.Status.NoNumbers -> MaterialTheme.quizzen.error
        LimitCardModel.Status.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
        LimitCardModel.Status.Using -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .alpha(fade)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .cssShadow(
                NeutralShadowColor.copy(alpha = 0.05f),
                offsetY = 2.dp, blur = 6.dp, spread = 0.dp, shape = shape,
            )
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, letterSpacing = 0.sp),
            color = valueColor,
        )
        LimitBar(model)
        Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                fontWeight = if (model.cut) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = if (model.cut) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Bar on a 1..30 scale: track up to the limit, a dotted line for the numbers beyond it, and the used range filled. */
@Composable
private fun LimitBar(model: LimitCardModel) {
    val start by animateFloatAsState(
        targetValue = model.used?.let { (it.first - 1f) / SCALE_MAX } ?: 0f,
        animationSpec = tween(250),
        label = "limitBarStart",
    )
    val end by animateFloatAsState(
        targetValue = model.used?.let { it.last.toFloat() / SCALE_MAX } ?: 0f,
        animationSpec = tween(250),
        label = "limitBarEnd",
    )
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val fill = MaterialTheme.colorScheme.primary
    val unavailable = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val limitFraction = model.limit.toFloat() / SCALE_MAX

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(8.dp),
    ) {
        val radius = CornerRadius(size.height / 2f)
        val limitX = size.width * limitFraction
        // Allowed numbers.
        drawRoundRect(color = track, size = Size(limitX, size.height), cornerRadius = radius)
        // Numbers above this kind's limit: dotted, so they read as "not available" rather than "empty".
        if (limitFraction < 1f) {
            drawLine(
                color = unavailable,
                start = Offset(limitX + 6.dp.toPx(), size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 6.dp.toPx())),
            )
        }
        // The range that will be used.
        if (end > start) {
            drawRoundRect(
                color = fill,
                topLeft = Offset(size.width * start, 0f),
                size = Size(size.width * (end - start), size.height),
                cornerRadius = radius,
            )
        }
    }
}
