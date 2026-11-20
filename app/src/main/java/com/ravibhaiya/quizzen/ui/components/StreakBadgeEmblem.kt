package com.ravibhaiya.quizzen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ravibhaiya.quizzen.domain.StreakRules
import com.ravibhaiya.quizzen.ui.theme.StreakBadgePalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom vector badge renderer for the 10 progressive streak ranks ([StreakRules.Badge]).
 *
 * Each badge has its own handcrafted metallic/gemstone silhouette, rim, and inner emblem drawn
 * directly on a [Canvas] with reused scratch [Path] instances (zero per-frame allocations) plus
 * a high-contrast streak-number crest plate anchored at the base.
 */
@Composable
internal fun StreakBadgeEmblem(
    badge: StreakRules.Badge,
    milestoneStreak: Int,
    palette: StreakBadgePalette,
    shimmerProgress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val bodyPath = remember { Path() }
    val detailPath = remember { Path() }

    Box(
        modifier = modifier
            .size(188.dp)
            .testTag("streak_badge_emblem"),
        contentAlignment = Alignment.Center,
    ) {
        // Soft radial halo disc sitting behind the sculpted badge.
        Box(
            modifier = Modifier
                .size(172.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.highlight.copy(alpha = 0.28f),
                            palette.primary.copy(alpha = 0.08f),
                            colors.surface.copy(alpha = 0f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        // Sculpted vector badge emblem.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 14.dp),
        ) {
            val shimmer = shimmerProgress().coerceIn(0f, 1f)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension * 0.42f
            val sheen = colors.onPrimary

            when (badge) {
                StreakRules.Badge.Bronze -> drawBronzeMedal(cx, cy, r, palette, sheen, shimmer, bodyPath, detailPath)
                StreakRules.Badge.Silver -> drawSilverShield(cx, cy, r, palette, sheen, shimmer, bodyPath, detailPath)
                StreakRules.Badge.Gold -> drawGoldLaurel(cx, cy, r, palette, sheen, shimmer, bodyPath, detailPath)
                StreakRules.Badge.Platinum -> drawPlatinumHex(cx, cy, r, palette, sheen, shimmer, bodyPath, detailPath)
                StreakRules.Badge.Emerald -> drawEmeraldOctagon(cx, cy, r, palette, sheen, shimmer, bodyPath, detailPath)
                StreakRules.Badge.Sapphire -> drawSapphireKite(cx, cy, r, palette, sheen, shimmer, bodyPath, detailPath)
                StreakRules.Badge.Ruby -> drawRubyCrest(cx, cy, r, palette, sheen, shimmer, bodyPath, detailPath)
                StreakRules.Badge.Amethyst -> drawAmethystSeal(cx, cy, r, palette, sheen, shimmer, bodyPath, detailPath)
                StreakRules.Badge.Diamond -> drawDiamondGem(cx, cy, r, palette, sheen, shimmer, bodyPath, detailPath)
                StreakRules.Badge.Legend -> drawLegendCrownCrest(cx, cy, r, palette, sheen, shimmer, bodyPath, detailPath)
            }
        }

        // Bold streak-number crest plate overlapping the bottom rim of the badge.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .softShadow(
                    color = palette.deep.copy(alpha = 0.32f),
                    offsetY = 6.dp,
                    blur = 14.dp,
                    spread = (-3).dp,
                    shape = CircleShape,
                )
                .background(
                    brush = Brush.linearGradient(listOf(palette.primary, palette.deep)),
                    shape = CircleShape,
                )
                .border(2.5.dp, palette.trim, CircleShape)
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .testTag("streak_badge_count"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = milestoneStreak.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun badgeBrush(cx: Float, cy: Float, r: Float, palette: StreakBadgePalette): Brush =
    Brush.linearGradient(
        colors = listOf(palette.highlight, palette.primary, palette.deep),
        start = Offset(cx - r, cy - r),
        end = Offset(cx + r, cy + r),
    )

private fun drawSpecularPulse(
    scope: DrawScope,
    cx: Float,
    cy: Float,
    r: Float,
    sheen: Color,
    shimmer: Float,
) {
    val pulse = sin(shimmer * PI).toFloat().coerceIn(0f, 1f)
    val glowAlpha = 0.14f + 0.16f * pulse
    scope.drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(sheen.copy(alpha = glowAlpha), sheen.copy(alpha = 0f)),
            center = Offset(cx - r * 0.34f, cy - r * 0.36f),
            radius = r * 0.72f,
        ),
        radius = r * 0.72f,
        center = Offset(cx - r * 0.34f, cy - r * 0.36f),
    )
}

/** 1. Bronze: Ribboned circular medal with single upward chevron. */
private fun DrawScope.drawBronzeMedal(
    cx: Float,
    cy: Float,
    r: Float,
    palette: StreakBadgePalette,
    sheen: Color,
    shimmer: Float,
    bodyPath: Path,
    detailPath: Path,
) {
    // Twin forked medal ribbons behind the lower edge.
    for (dir in intArrayOf(-1, 1)) {
        bodyPath.reset()
        bodyPath.moveTo(cx + dir * r * 0.24f, cy + r * 0.42f)
        bodyPath.lineTo(cx + dir * r * 0.76f, cy + r * 0.48f)
        bodyPath.lineTo(cx + dir * r * 0.94f, cy + r * 0.98f)
        bodyPath.lineTo(cx + dir * r * 0.54f, cy + r * 0.86f)
        bodyPath.lineTo(cx + dir * r * 0.32f, cy + r * 1.04f)
        bodyPath.close()
        drawPath(bodyPath, color = palette.deep)
        drawPath(bodyPath, color = palette.trim.copy(alpha = 0.55f), style = Stroke(width = 2.dp.toPx()))
    }

    val medalR = r * 0.88f
    drawCircle(brush = badgeBrush(cx, cy, medalR, palette), radius = medalR, center = Offset(cx, cy))
    drawCircle(color = palette.trim, radius = medalR, center = Offset(cx, cy), style = Stroke(width = 4.dp.toPx()))
    drawCircle(
        color = palette.deep.copy(alpha = 0.45f),
        radius = medalR * 0.78f,
        center = Offset(cx, cy),
        style = Stroke(width = 2.dp.toPx()),
    )
    drawSpecularPulse(this, cx, cy, medalR, sheen, shimmer)

    // Single rank-1 upward chevron.
    drawChevrons(cx, cy, r * 0.36f, count = 1, color = sheen, strokeWidth = 5.5.dp.toPx(), path = detailPath)
}

/** 2. Silver: Winged sterling heater shield with twin chevrons. */
private fun DrawScope.drawSilverShield(
    cx: Float,
    cy: Float,
    r: Float,
    palette: StreakBadgePalette,
    sheen: Color,
    shimmer: Float,
    bodyPath: Path,
    detailPath: Path,
) {
    drawSideWings(cx, cy - r * 0.14f, r, palette, detailPath)
    buildHeaterShieldPath(cx, cy, r * 0.88f, bodyPath)
    drawPath(bodyPath, brush = badgeBrush(cx, cy, r, palette))
    drawPath(bodyPath, color = palette.trim, style = Stroke(width = 4.dp.toPx(), join = StrokeJoin.Round))

    buildHeaterShieldPath(cx, cy, r * 0.70f, detailPath)
    drawPath(detailPath, color = sheen.copy(alpha = 0.32f), style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
    drawSpecularPulse(this, cx, cy, r * 0.85f, sheen, shimmer)

    drawChevrons(cx, cy + r * 0.04f, r * 0.34f, count = 2, color = sheen, strokeWidth = 4.8.dp.toPx(), path = detailPath)
}

/** 3. Gold: Laurel-wreathed bullion medallion with 5-point royal star. */
private fun DrawScope.drawGoldLaurel(
    cx: Float,
    cy: Float,
    r: Float,
    palette: StreakBadgePalette,
    sheen: Color,
    shimmer: Float,
    bodyPath: Path,
    detailPath: Path,
) {
    drawLaurelWreath(cx, cy, r * 1.02f, palette.highlight, palette.deep, detailPath)

    val medalR = r * 0.80f
    drawCircle(brush = badgeBrush(cx, cy, medalR, palette), radius = medalR, center = Offset(cx, cy))
    drawCircle(color = palette.trim, radius = medalR, center = Offset(cx, cy), style = Stroke(width = 4.dp.toPx()))
    drawCircle(
        color = palette.deep.copy(alpha = 0.42f),
        radius = medalR * 0.80f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.8.dp.toPx()),
    )
    drawSpecularPulse(this, cx, cy, medalR, sheen, shimmer)

    // Bold 5-point star centerpiece.
    buildStarPath(cx, cy, outerR = medalR * 0.56f, innerR = medalR * 0.25f, points = 5, path = bodyPath)
    drawPath(bodyPath, color = sheen.copy(alpha = 0.95f))
    drawPath(bodyPath, color = palette.deep.copy(alpha = 0.3f), style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round))
}

/** 4. Platinum: Winged architectural hexagon crest with triple chevrons. */
private fun DrawScope.drawPlatinumHex(
    cx: Float,
    cy: Float,
    r: Float,
    palette: StreakBadgePalette,
    sheen: Color,
    shimmer: Float,
    bodyPath: Path,
    detailPath: Path,
) {
    // Sleek angled wing flares flanking the hexagon.
    for (dir in intArrayOf(-1, 1)) {
        detailPath.reset()
        detailPath.moveTo(cx + dir * r * 0.64f, cy - r * 0.34f)
        detailPath.lineTo(cx + dir * r * 1.08f, cy - r * 0.20f)
        detailPath.lineTo(cx + dir * r * 0.92f, cy + r * 0.14f)
        detailPath.lineTo(cx + dir * r * 0.64f, cy + r * 0.28f)
        detailPath.close()
        drawPath(detailPath, color = palette.highlight)
        drawPath(detailPath, color = palette.deep.copy(alpha = 0.45f), style = Stroke(width = 1.6.dp.toPx()))
    }

    buildRegularPolygonPath(cx, cy, r * 0.88f, sides = 6, startAngleDeg = -90.0, path = bodyPath)
    drawPath(bodyPath, brush = badgeBrush(cx, cy, r, palette))
    drawPath(bodyPath, color = palette.trim, style = Stroke(width = 4.dp.toPx(), join = StrokeJoin.Miter))

    buildRegularPolygonPath(cx, cy, r * 0.70f, sides = 6, startAngleDeg = -90.0, path = detailPath)
    drawPath(detailPath, color = sheen.copy(alpha = 0.35f), style = Stroke(width = 1.8.dp.toPx()))
    drawSpecularPulse(this, cx, cy, r * 0.86f, sheen, shimmer)

    drawChevrons(cx, cy + r * 0.06f, r * 0.32f, count = 3, color = sheen, strokeWidth = 4.2.dp.toPx(), path = detailPath)
}

/** 5. Emerald: Step-cut octagonal emerald gem in a pronged bezel. */
private fun DrawScope.drawEmeraldOctagon(
    cx: Float,
    cy: Float,
    r: Float,
    palette: StreakBadgePalette,
    sheen: Color,
    shimmer: Float,
    bodyPath: Path,
    detailPath: Path,
) {
    val outerR = r * 0.90f
    buildRegularPolygonPath(cx, cy, outerR, sides = 8, startAngleDeg = -67.5, path = bodyPath)
    drawPath(bodyPath, brush = badgeBrush(cx, cy, outerR, palette))
    drawPath(bodyPath, color = palette.trim, style = Stroke(width = 4.dp.toPx(), join = StrokeJoin.Round))

    // Inner step-cut table facet + diagonal facet ridges.
    val innerR = outerR * 0.62f
    buildRegularPolygonPath(cx, cy, innerR, sides = 8, startAngleDeg = -67.5, path = detailPath)
    drawPath(detailPath, color = palette.highlight.copy(alpha = 0.35f))
    drawPath(detailPath, color = sheen.copy(alpha = 0.55f), style = Stroke(width = 2.dp.toPx()))

    for (i in 0 until 8) {
        val rad = Math.toRadians(-67.5 + i * 45.0)
        drawLine(
            color = sheen.copy(alpha = 0.40f),
            start = Offset(cx + (cos(rad) * innerR).toFloat(), cy + (sin(rad) * innerR).toFloat()),
            end = Offset(cx + (cos(rad) * outerR).toFloat(), cy + (sin(rad) * outerR).toFloat()),
            strokeWidth = 1.6.dp.toPx(),
        )
    }
    drawSpecularPulse(this, cx, cy, outerR, sheen, shimmer)

    // Crisp 4-point gem spark in the table center.
    buildStarPath(cx, cy, outerR = r * 0.28f, innerR = r * 0.09f, points = 4, path = detailPath)
    drawPath(detailPath, color = sheen)
}

/** 6. Sapphire: Winged royal kite shield with faceted lozenge jewel. */
private fun DrawScope.drawSapphireKite(
    cx: Float,
    cy: Float,
    r: Float,
    palette: StreakBadgePalette,
    sheen: Color,
    shimmer: Float,
    bodyPath: Path,
    detailPath: Path,
) {
    drawSideWings(cx, cy - r * 0.22f, r * 1.05f, palette, detailPath)

    // Tall faceted kite shield.
    bodyPath.reset()
    bodyPath.moveTo(cx, cy - r * 0.94f)
    bodyPath.lineTo(cx + r * 0.78f, cy - r * 0.42f)
    bodyPath.lineTo(cx + r * 0.58f, cy + r * 0.44f)
    bodyPath.lineTo(cx, cy + r * 0.96f)
    bodyPath.lineTo(cx - r * 0.58f, cy + r * 0.44f)
    bodyPath.lineTo(cx - r * 0.78f, cy - r * 0.42f)
    bodyPath.close()
    drawPath(bodyPath, brush = badgeBrush(cx, cy, r, palette))
    drawPath(bodyPath, color = palette.trim, style = Stroke(width = 4.dp.toPx(), join = StrokeJoin.Round))
    drawSpecularPulse(this, cx, cy, r * 0.88f, sheen, shimmer)

    // Faceted lozenge jewel core.
    val jw = r * 0.34f
    val jh = r * 0.48f
    detailPath.reset()
    detailPath.moveTo(cx, cy - jh)
    detailPath.lineTo(cx + jw, cy)
    detailPath.lineTo(cx, cy + jh)
    detailPath.lineTo(cx - jw, cy)
    detailPath.close()
    drawPath(detailPath, color = sheen.copy(alpha = 0.22f))
    drawPath(detailPath, color = sheen.copy(alpha = 0.92f), style = Stroke(width = 2.6.dp.toPx(), join = StrokeJoin.Round))
    drawLine(sheen.copy(alpha = 0.55f), Offset(cx, cy - jh), Offset(cx, cy + jh), strokeWidth = 1.6.dp.toPx())
    drawLine(sheen.copy(alpha = 0.55f), Offset(cx - jw, cy), Offset(cx + jw, cy), strokeWidth = 1.6.dp.toPx())
}

/** 7. Ruby: Sovereign pointed crest with marquise ruby crystal core. */
private fun DrawScope.drawRubyCrest(
    cx: Float,
    cy: Float,
    r: Float,
    palette: StreakBadgePalette,
    sheen: Color,
    shimmer: Float,
    bodyPath: Path,
    detailPath: Path,
) {
    // Ornate pointed crest silhouette.
    bodyPath.reset()
    bodyPath.moveTo(cx, cy - r * 0.98f)
    bodyPath.cubicTo(cx + r * 0.36f, cy - r * 0.70f, cx + r * 0.86f, cy - r * 0.62f, cx + r * 0.86f, cy - r * 0.16f)
    bodyPath.cubicTo(cx + r * 0.86f, cy + r * 0.42f, cx + r * 0.46f, cy + r * 0.78f, cx, cy + r * 0.96f)
    bodyPath.cubicTo(cx - r * 0.46f, cy + r * 0.78f, cx - r * 0.86f, cy + r * 0.42f, cx - r * 0.86f, cy - r * 0.16f)
    bodyPath.cubicTo(cx - r * 0.86f, cy - r * 0.62f, cx - r * 0.36f, cy - r * 0.70f, cx, cy - r * 0.98f)
    bodyPath.close()
    drawPath(bodyPath, brush = badgeBrush(cx, cy, r, palette))
    drawPath(bodyPath, color = palette.trim, style = Stroke(width = 4.dp.toPx(), join = StrokeJoin.Round))
    drawSpecularPulse(this, cx, cy, r * 0.88f, sheen, shimmer)

    // Marquise-cut ruby flame/crystal centerpiece.
    detailPath.reset()
    detailPath.moveTo(cx, cy - r * 0.48f)
    detailPath.quadraticTo(cx + r * 0.42f, cy, cx, cy + r * 0.48f)
    detailPath.quadraticTo(cx - r * 0.42f, cy, cx, cy - r * 0.48f)
    detailPath.close()
    drawPath(detailPath, color = sheen.copy(alpha = 0.92f))

    // Inner ruby core cutout.
    detailPath.reset()
    detailPath.moveTo(cx, cy - r * 0.28f)
    detailPath.quadraticTo(cx + r * 0.22f, cy, cx, cy + r * 0.28f)
    detailPath.quadraticTo(cx - r * 0.22f, cy, cx, cy - r * 0.28f)
    detailPath.close()
    drawPath(detailPath, color = palette.primary)

    // Flanking accent pips.
    drawCircle(color = palette.trim, radius = r * 0.065f, center = Offset(cx - r * 0.42f, cy - r * 0.18f))
    drawCircle(color = palette.trim, radius = r * 0.065f, center = Offset(cx + r * 0.42f, cy - r * 0.18f))
}

/** 8. Amethyst: Regal 8-point starburst seal with imperial 3-peak crystal crown. */
private fun DrawScope.drawAmethystSeal(
    cx: Float,
    cy: Float,
    r: Float,
    palette: StreakBadgePalette,
    sheen: Color,
    shimmer: Float,
    bodyPath: Path,
    detailPath: Path,
) {
    // 8-point royal star seal base.
    buildStarPath(cx, cy, outerR = r * 1.02f, innerR = r * 0.72f, points = 8, path = bodyPath)
    drawPath(bodyPath, brush = badgeBrush(cx, cy, r, palette))
    drawPath(bodyPath, color = palette.trim, style = Stroke(width = 3.2.dp.toPx(), join = StrokeJoin.Round))

    // Inner beveled circular amethyst gemstone.
    val coreR = r * 0.66f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.highlight, palette.primary, palette.deep),
            center = Offset(cx - coreR * 0.25f, cy - coreR * 0.25f),
            radius = coreR * 1.3f,
        ),
        radius = coreR,
        center = Offset(cx, cy),
    )
    drawCircle(color = palette.trim, radius = coreR, center = Offset(cx, cy), style = Stroke(width = 2.6.dp.toPx()))
    drawSpecularPulse(this, cx, cy, coreR, sheen, shimmer)

    // Imperial 3-peak crystal tiara motif.
    detailPath.reset()
    detailPath.moveTo(cx - r * 0.34f, cy + r * 0.22f)
    detailPath.lineTo(cx - r * 0.38f, cy - r * 0.12f)
    detailPath.lineTo(cx - r * 0.16f, cy + r * 0.02f)
    detailPath.lineTo(cx, cy - r * 0.30f)
    detailPath.lineTo(cx + r * 0.16f, cy + r * 0.02f)
    detailPath.lineTo(cx + r * 0.38f, cy - r * 0.12f)
    detailPath.lineTo(cx + r * 0.34f, cy + r * 0.22f)
    detailPath.close()
    drawPath(detailPath, color = sheen.copy(alpha = 0.94f))
}

/** 9. Diamond: Brilliant-cut multi-faceted diamond with prism spark glints. */
private fun DrawScope.drawDiamondGem(
    cx: Float,
    cy: Float,
    r: Float,
    palette: StreakBadgePalette,
    sheen: Color,
    shimmer: Float,
    bodyPath: Path,
    detailPath: Path,
) {
    // Subtle crystalline outer ring + 4 compass sparkles.
    drawCircle(
        color = palette.highlight.copy(alpha = 0.28f),
        radius = r * 0.96f,
        center = Offset(cx, cy),
        style = Stroke(width = 2.2.dp.toPx()),
    )
    val glintOffsets = arrayOf(
        Offset(cx - r * 0.86f, cy - r * 0.62f),
        Offset(cx + r * 0.86f, cy - r * 0.56f),
        Offset(cx - r * 0.74f, cy + r * 0.66f),
        Offset(cx + r * 0.78f, cy + r * 0.62f),
    )
    for (pt in glintOffsets) {
        buildStarPath(pt.x, pt.y, outerR = r * 0.12f, innerR = r * 0.04f, points = 4, path = detailPath)
        drawPath(detailPath, color = palette.highlight)
    }

    // Classic brilliant-cut diamond silhouette.
    val topY = cy - r * 0.66f
    val girdleY = cy - r * 0.14f
    val tipY = cy + r * 0.88f
    val crownHalfW = r * 0.56f
    val girdleHalfW = r * 0.90f

    bodyPath.reset()
    bodyPath.moveTo(cx - crownHalfW, topY)
    bodyPath.lineTo(cx + crownHalfW, topY)
    bodyPath.lineTo(cx + girdleHalfW, girdleY)
    bodyPath.lineTo(cx, tipY)
    bodyPath.lineTo(cx - girdleHalfW, girdleY)
    bodyPath.close()
    drawPath(bodyPath, brush = badgeBrush(cx, cy, r, palette))

    // Luminous top table facet.
    detailPath.reset()
    detailPath.moveTo(cx - crownHalfW, topY)
    detailPath.lineTo(cx + crownHalfW, topY)
    detailPath.lineTo(cx + r * 0.36f, girdleY)
    detailPath.lineTo(cx - r * 0.36f, girdleY)
    detailPath.close()
    drawPath(detailPath, color = sheen.copy(alpha = 0.34f))

    // Facet ribs (girdle + crown splits + pavilion ribs).
    val facetStroke = 2.dp.toPx()
    val facetColor = sheen.copy(alpha = 0.78f)
    drawLine(facetColor, Offset(cx - girdleHalfW, girdleY), Offset(cx + girdleHalfW, girdleY), facetStroke)
    drawLine(facetColor, Offset(cx - crownHalfW, topY), Offset(cx - r * 0.36f, girdleY), facetStroke)
    drawLine(facetColor, Offset(cx + crownHalfW, topY), Offset(cx + r * 0.36f, girdleY), facetStroke)
    drawLine(facetColor, Offset(cx, topY), Offset(cx - r * 0.36f, girdleY), facetStroke)
    drawLine(facetColor, Offset(cx, topY), Offset(cx + r * 0.36f, girdleY), facetStroke)
    drawLine(facetColor, Offset(cx - r * 0.36f, girdleY), Offset(cx, tipY), facetStroke)
    drawLine(facetColor, Offset(cx + r * 0.36f, girdleY), Offset(cx, tipY), facetStroke)

    drawPath(bodyPath, color = palette.trim, style = Stroke(width = 3.6.dp.toPx(), join = StrokeJoin.Round))
    drawSpecularPulse(this, cx, cy - r * 0.1f, r * 0.8f, sheen, shimmer)
}

/** 10. Legend: Grand sovereign winged shield crowned in royal gold with radiant 8-point star. */
private fun DrawScope.drawLegendCrownCrest(
    cx: Float,
    cy: Float,
    r: Float,
    palette: StreakBadgePalette,
    sheen: Color,
    shimmer: Float,
    bodyPath: Path,
    detailPath: Path,
) {
    val shieldCy = cy + r * 0.10f
    // Golden laurel aura + sweeping wings behind the sovereign shield.
    drawLaurelWreath(cx, shieldCy, r * 1.04f, palette.trim, palette.deep, detailPath)
    drawSideWings(cx, shieldCy - r * 0.18f, r * 1.02f, palette, detailPath)

    // Royal 5-peak gold crown perched atop the crest.
    val crownBaseY = shieldCy - r * 0.64f
    detailPath.reset()
    detailPath.moveTo(cx - r * 0.44f, crownBaseY)
    detailPath.lineTo(cx - r * 0.56f, crownBaseY - r * 0.36f)
    detailPath.lineTo(cx - r * 0.24f, crownBaseY - r * 0.18f)
    detailPath.lineTo(cx, crownBaseY - r * 0.46f)
    detailPath.lineTo(cx + r * 0.24f, crownBaseY - r * 0.18f)
    detailPath.lineTo(cx + r * 0.56f, crownBaseY - r * 0.36f)
    detailPath.lineTo(cx + r * 0.44f, crownBaseY)
    detailPath.close()
    drawPath(detailPath, color = palette.trim)
    drawPath(detailPath, color = palette.deep.copy(alpha = 0.45f), style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round))
    // Crown tip jewel pips.
    drawCircle(sheen, radius = r * 0.055f, center = Offset(cx, crownBaseY - r * 0.48f))
    drawCircle(sheen, radius = r * 0.045f, center = Offset(cx - r * 0.56f, crownBaseY - r * 0.38f))
    drawCircle(sheen, radius = r * 0.045f, center = Offset(cx + r * 0.56f, crownBaseY - r * 0.38f))

    // Sovereign indigo-to-orchid shield trimmed in royal gold.
    buildHeaterShieldPath(cx, shieldCy, r * 0.80f, bodyPath)
    drawPath(bodyPath, brush = badgeBrush(cx, shieldCy, r * 0.85f, palette))
    drawPath(bodyPath, color = palette.trim, style = Stroke(width = 4.2.dp.toPx(), join = StrokeJoin.Round))

    buildHeaterShieldPath(cx, shieldCy, r * 0.64f, detailPath)
    drawPath(detailPath, color = palette.trim.copy(alpha = 0.45f), style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round))
    drawSpecularPulse(this, cx, shieldCy, r * 0.80f, sheen, shimmer)

    // Radiant 8-point sovereign gold-and-white star centerpiece.
    buildStarPath(cx, shieldCy - r * 0.04f, outerR = r * 0.38f, innerR = r * 0.15f, points = 8, path = detailPath)
    drawPath(detailPath, color = palette.trim)
    buildStarPath(cx, shieldCy - r * 0.04f, outerR = r * 0.21f, innerR = r * 0.08f, points = 4, path = detailPath)
    drawPath(detailPath, color = sheen)
}

private fun DrawScope.drawSideWings(
    cx: Float,
    cy: Float,
    r: Float,
    palette: StreakBadgePalette,
    path: Path,
) {
    for (dir in intArrayOf(-1, 1)) {
        path.reset()
        path.moveTo(cx + dir * r * 0.52f, cy - r * 0.16f)
        path.lineTo(cx + dir * r * 1.10f, cy - r * 0.34f)
        path.lineTo(cx + dir * r * 0.96f, cy - r * 0.04f)
        path.lineTo(cx + dir * r * 1.04f, cy + r * 0.06f)
        path.lineTo(cx + dir * r * 0.84f, cy + r * 0.26f)
        path.lineTo(cx + dir * r * 0.50f, cy + r * 0.24f)
        path.close()
        drawPath(path, color = palette.highlight)
        drawPath(path, color = palette.deep.copy(alpha = 0.45f), style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round))
    }
}

private fun DrawScope.drawLaurelWreath(
    cx: Float,
    cy: Float,
    r: Float,
    leafColor: Color,
    outlineColor: Color,
    path: Path,
) {
    val leafAnglesDeg = doubleArrayOf(138.0, 162.0, 186.0, 210.0)
    for (dir in intArrayOf(-1, 1)) {
        for (deg in leafAnglesDeg) {
            val angle = if (dir == -1) Math.toRadians(deg) else Math.toRadians(180.0 - deg)
            val bx = cx + (cos(angle) * r * 0.86f).toFloat()
            val by = cy + (sin(angle) * r * 0.86f).toFloat()
            val tipAngle = angle + dir * 0.42
            val tx = bx + (cos(tipAngle) * r * 0.26f).toFloat()
            val ty = by + (sin(tipAngle) * r * 0.26f).toFloat()
            val midX = (bx + tx) / 2f
            val midY = (by + ty) / 2f
            val perpX = -(ty - by) * 0.34f
            val perpY = (tx - bx) * 0.34f

            path.reset()
            path.moveTo(bx, by)
            path.quadraticTo(midX + perpX, midY + perpY, tx, ty)
            path.quadraticTo(midX - perpX, midY - perpY, bx, by)
            path.close()
            drawPath(path, color = leafColor)
            drawPath(path, color = outlineColor.copy(alpha = 0.38f), style = Stroke(width = 1.2.dp.toPx()))
        }
    }
}

private fun DrawScope.drawChevrons(
    cx: Float,
    cy: Float,
    width: Float,
    count: Int,
    color: Color,
    strokeWidth: Float,
    path: Path,
) {
    val stepY = width * 0.56f
    val totalHeight = (count - 1) * stepY
    val startY = cy - totalHeight / 2f
    for (i in 0 until count) {
        val y = startY + i * stepY
        path.reset()
        path.moveTo(cx - width, y + width * 0.34f)
        path.lineTo(cx, y - width * 0.34f)
        path.lineTo(cx + width, y + width * 0.34f)
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

private fun buildHeaterShieldPath(cx: Float, cy: Float, r: Float, path: Path) {
    path.reset()
    path.moveTo(cx, cy - r * 0.96f)
    path.quadraticTo(cx + r * 0.48f, cy - r * 0.88f, cx + r * 0.86f, cy - r * 0.66f)
    path.lineTo(cx + r * 0.82f, cy + r * 0.04f)
    path.cubicTo(cx + r * 0.78f, cy + r * 0.54f, cx + r * 0.38f, cy + r * 0.82f, cx, cy + r * 0.98f)
    path.cubicTo(cx - r * 0.38f, cy + r * 0.82f, cx - r * 0.78f, cy + r * 0.54f, cx - r * 0.82f, cy + r * 0.04f)
    path.lineTo(cx - r * 0.86f, cy - r * 0.66f)
    path.quadraticTo(cx - r * 0.48f, cy - r * 0.88f, cx, cy - r * 0.96f)
    path.close()
}

private fun buildRegularPolygonPath(
    cx: Float,
    cy: Float,
    r: Float,
    sides: Int,
    startAngleDeg: Double,
    path: Path,
) {
    path.reset()
    for (i in 0 until sides) {
        val rad = Math.toRadians(startAngleDeg + (360.0 * i / sides))
        val px = cx + (cos(rad) * r).toFloat()
        val py = cy + (sin(rad) * r).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
}

private fun buildStarPath(
    cx: Float,
    cy: Float,
    outerR: Float,
    innerR: Float,
    points: Int,
    path: Path,
) {
    path.reset()
    val total = points * 2
    for (i in 0 until total) {
        val rad = -PI / 2.0 + (PI * i / points)
        val radius = if (i % 2 == 0) outerR else innerR
        val px = cx + (cos(rad) * radius).toFloat()
        val py = cy + (sin(rad) * radius).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
}
