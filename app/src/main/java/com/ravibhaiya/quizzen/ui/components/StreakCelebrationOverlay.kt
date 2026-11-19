package com.ravibhaiya.quizzen.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.domain.StreakRules
import com.ravibhaiya.quizzen.ui.theme.QuizzenShapes
import com.ravibhaiya.quizzen.ui.theme.quizzen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

const val STREAK_CELEBRATION_DURATION_MS = 3_000L

private enum class ConfettiKind { Ribbon, Diamond, Circle, Star }

private data class BurstPiece(
    val angleRad: Double,
    val speedFactor: Float,
    val gravityFactor: Float,
    val sizeDp: Float,
    val spinDegrees: Float,
    val paletteIndex: Int,
    val kind: ConfettiKind,
)

private data class ShowerPiece(
    val startXFraction: Float,
    val startYOffsetFraction: Float,
    val fallSpeed: Float,
    val swayAmplitudeDp: Float,
    val swayCycles: Float,
    val phaseRad: Float,
    val sizeDp: Float,
    val spinDegrees: Float,
    val paletteIndex: Int,
    val kind: ConfettiKind,
)

private val BURST_PIECES: List<BurstPiece> = List(34) { i ->
    val angle = -PI / 2.0 + (2.0 * PI * i / 34.0) + (((i * 23) % 9) - 4) * 0.035
    BurstPiece(
        angleRad = angle,
        speedFactor = 0.45f + ((i * 17) % 10) * 0.068f,
        gravityFactor = 0.35f + ((i * 11) % 7) * 0.08f,
        sizeDp = 7f + ((i * 5) % 5) * 1.4f,
        spinDegrees = (if (i % 2 == 0) 1f else -1f) * (220f + ((i * 31) % 6) * 65f),
        paletteIndex = i % 6,
        kind = ConfettiKind.entries[i % ConfettiKind.entries.size],
    )
}

private val SHOWER_PIECES: List<ShowerPiece> = List(34) { i ->
    ShowerPiece(
        startXFraction = ((i * 29 + 7) % 100) / 100f,
        startYOffsetFraction = -0.06f - ((i * 19) % 45) / 100f,
        fallSpeed = 0.88f + ((i * 13) % 8) * 0.075f,
        swayAmplitudeDp = 12f + ((i * 7) % 5) * 5f,
        swayCycles = 1.4f + ((i * 11) % 4) * 0.45f,
        phaseRad = (i * 0.9f),
        sizeDp = 6.5f + ((i * 3) % 5) * 1.3f,
        spinDegrees = (if (i % 2 == 0) 1f else -1f) * (280f + ((i * 37) % 5) * 75f),
        paletteIndex = (i + 2) % 6,
        kind = ConfettiKind.entries[(i + 1) % ConfettiKind.entries.size],
    )
}

private const val SUNBURST_RAY_COUNT = 12

private data class StreakPhrase(
    val badgeRes: Int,
    val sublineRes: Int,
)

private data class StreakPalette(
    val gradientStart: Color,
    val gradientEnd: Color,
    val badgeBg: Color,
    val badgeFg: Color,
)

private val EXTENDED_PHRASES: List<StreakPhrase> = listOf(
    StreakPhrase(R.string.streak_ext_badge_1, R.string.streak_ext_sub_1),
    StreakPhrase(R.string.streak_ext_badge_2, R.string.streak_ext_sub_2),
    StreakPhrase(R.string.streak_ext_badge_3, R.string.streak_ext_sub_3),
    StreakPhrase(R.string.streak_ext_badge_4, R.string.streak_ext_sub_4),
    StreakPhrase(R.string.streak_ext_badge_5, R.string.streak_ext_sub_5),
    StreakPhrase(R.string.streak_ext_badge_6, R.string.streak_ext_sub_6),
    StreakPhrase(R.string.streak_ext_badge_7, R.string.streak_ext_sub_7),
    StreakPhrase(R.string.streak_ext_badge_8, R.string.streak_ext_sub_8),
)

/**
 * Full-screen, 3-second celebratory animation shown when a user reaches a consecutive correct streak
 * milestone (`5`, `10`, `15`, ...) during Math Practice or Language Vocabulary quizzes.
 *
 * Visual layers (all driven by finite one-shot [Animatable]s read inside [Canvas] and [graphicsLayer]
 * lambdas, unmounting cleanly at 3,000 ms with zero idle frames):
 * 1. Warm atmospheric radial backdrop wash.
 * 2. Rotating sunburst light rays + triple staggered expanding shockwave rings.
 * 3. Two-wave 68-piece confetti, star, and sparkle cascade (explosive radial fireworks + full-screen shower).
 * 4. Spring-popped sculpted streak medallion, tier pill badge, bold headline, and encouraging subline.
 */
@Composable
fun StreakCelebrationOverlay(
    milestoneStreak: Int,
    eventId: Int,
    modifier: Modifier = Modifier,
) {
    if (eventId <= 0 || milestoneStreak <= 0) return

    var isVisible by remember { mutableStateOf(false) }
    var extendedPhraseIndex by remember { mutableIntStateOf(-1) }
    var extendedPaletteIndex by remember { mutableIntStateOf(-1) }
    val overlayAlpha = remember { Animatable(0f) }
    val timeline = remember { Animatable(0f) }
    val burstProgress = remember { Animatable(0f) }
    val medallionScale = remember { Animatable(0.35f) }
    val medallionRotation = remember { Animatable(-10f) }
    val copyAlpha = remember { Animatable(0f) }
    val copyOffsetY = remember { Animatable(28f) }
    val exitScale = remember { Animatable(1f) }

    LaunchedEffect(eventId) {
        if (StreakRules.isExtendedStreak(milestoneStreak)) {
            extendedPhraseIndex = StreakRules.pickRandomIndex(
                count = EXTENDED_PHRASES.size,
                previousIndex = extendedPhraseIndex,
            )
            extendedPaletteIndex = StreakRules.pickRandomIndex(
                count = StreakRules.EXTENDED_PALETTE_COUNT,
                previousIndex = extendedPaletteIndex,
            )
        }
        isVisible = true
        overlayAlpha.snapTo(0f)
        timeline.snapTo(0f)
        burstProgress.snapTo(0f)
        medallionScale.snapTo(0.35f)
        medallionRotation.snapTo(-10f)
        copyAlpha.snapTo(0f)
        copyOffsetY.snapTo(28f)
        exitScale.snapTo(1f)

        val backdropIn = launch {
            overlayAlpha.animateTo(1f, tween(durationMillis = 260, easing = EmphasizedEasing))
        }
        val timelineJob = launch {
            timeline.animateTo(1f, tween(durationMillis = STREAK_CELEBRATION_DURATION_MS.toInt(), easing = LinearEasing))
        }
        val burstJob = launch {
            burstProgress.animateTo(1f, tween(durationMillis = 2_400, easing = EmphasizedEasing))
        }
        val medallionJob = launch {
            launch {
                medallionScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.46f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
            launch {
                medallionRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.52f,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }
        }
        val copyJob = launch {
            delay(130)
            launch {
                copyAlpha.animateTo(1f, tween(durationMillis = 320, easing = EmphasizedEasing))
            }
            launch {
                copyOffsetY.animateTo(0f, tween(durationMillis = 380, easing = EmphasizedEasing))
            }
        }

        // Hold full celebration until 2,600 ms, then smoothly fade/scale out to finish right at 3,000 ms.
        delay(2_600)
        val exitJob = launch {
            launch {
                overlayAlpha.animateTo(0f, tween(durationMillis = 400, easing = EmphasizedEasing))
            }
            launch {
                exitScale.animateTo(1.07f, tween(durationMillis = 400, easing = EmphasizedEasing))
            }
        }

        backdropIn.join()
        medallionJob.join()
        copyJob.join()
        burstJob.join()
        timelineJob.join()
        exitJob.join()
        isVisible = false
    }

    if (!isVisible) return

    val colors = MaterialTheme.colorScheme
    val extra = MaterialTheme.quizzen
    val isExtended = StreakRules.isExtendedStreak(milestoneStreak)
    val tier = StreakRules.tierFor(milestoneStreak)

    val phrase = if (isExtended) {
        EXTENDED_PHRASES[extendedPhraseIndex.coerceIn(EXTENDED_PHRASES.indices)]
    } else {
        when (tier) {
            StreakRules.Tier.Momentum -> StreakPhrase(R.string.streak_tier_momentum, R.string.streak_sub_momentum)
            StreakRules.Tier.OnFire -> StreakPhrase(R.string.streak_tier_on_fire, R.string.streak_sub_on_fire)
            StreakRules.Tier.Unstoppable -> StreakPhrase(R.string.streak_tier_unstoppable, R.string.streak_sub_unstoppable)
        }
    }
    val tierLabelRes = phrase.badgeRes
    val sublineRes = phrase.sublineRes

    val palette = if (isExtended) {
        val extendedPalettes = listOf(
            StreakPalette(extra.streakIndigo, extra.streakViolet, extra.streakIndigoContainer, extra.onStreakIndigoContainer),
            StreakPalette(extra.streakCyan, extra.streakTeal, extra.streakCyanContainer, extra.onStreakCyanContainer),
            StreakPalette(extra.streakCrimson, extra.streakRose, extra.streakCrimsonContainer, extra.onStreakCrimsonContainer),
            StreakPalette(extra.streakEmerald, extra.streakForest, extra.streakEmeraldContainer, extra.onStreakEmeraldContainer),
            StreakPalette(extra.streakBlue, extra.antonym, extra.streakBlueContainer, extra.onStreakBlueContainer),
            StreakPalette(extra.antonym, extra.streakMagenta, extra.antonymContainer, extra.onAntonymContainer),
        )
        extendedPalettes[extendedPaletteIndex.coerceIn(extendedPalettes.indices)]
    } else {
        when (tier) {
            StreakRules.Tier.Momentum -> StreakPalette(
                extra.success,
                extra.synonym,
                extra.synonymContainer,
                extra.onSynonymContainer,
            )
            StreakRules.Tier.OnFire -> StreakPalette(
                extra.timeout,
                extra.streakBlue,
                extra.streakBlueContainer,
                extra.onStreakBlueContainer,
            )
            StreakRules.Tier.Unstoppable -> StreakPalette(
                extra.antonym,
                extra.streakMagenta,
                extra.antonymContainer,
                extra.onAntonymContainer,
            )
        }
    }
    val gradientStart = palette.gradientStart
    val gradientEnd = palette.gradientEnd
    val badgeBg = palette.badgeBg
    val badgeFg = palette.badgeFg
    val confettiPalette = listOf(
        gradientStart,
        gradientEnd,
        extra.streakCyan,
        extra.streakIndigo,
        extra.streakCrimson,
        extra.streakEmerald,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
            .testTag("streak_milestone_overlay"),
        contentAlignment = Alignment.Center,
    ) {
        val scratchPath = remember { Path() }
        // Full-screen atmospheric backdrop, sunburst rays, triple shockwaves, and dual-wave confetti.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = timeline.value.coerceIn(0f, 1f)
            val b = burstProgress.value.coerceIn(0f, 1f)
            val focusCenter = Offset(size.width / 2f, size.height * 0.44f)
            val maxRadius = hypot(size.width, size.height) * 0.65f
            val transparentSurface = colors.surface.copy(alpha = 0f)

            // 1. Warm radial full-screen wash.
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to gradientStart.copy(alpha = 0.24f),
                        0.45f to colors.surface.copy(alpha = 0.92f),
                        1f to colors.surface.copy(alpha = 0.96f),
                    ),
                    center = focusCenter,
                    radius = maxRadius,
                ),
            )

            // 2. Soft rotating sunburst rays radiating from behind the medallion.
            val rayAlpha = (sin(t * PI).toFloat().coerceIn(0f, 1f)) * 0.14f
            if (rayAlpha > 0.005f) {
                val rayLength = maxRadius * 1.1f
                val halfSpreadRad = (PI / SUNBURST_RAY_COUNT) * 0.42
                val rayBrush = Brush.radialGradient(
                    colors = listOf(
                        gradientStart.copy(alpha = rayAlpha),
                        gradientEnd.copy(alpha = rayAlpha * 0.35f),
                        transparentSurface,
                    ),
                    center = focusCenter,
                    radius = rayLength,
                )
                rotate(degrees = 36f * t, pivot = focusCenter) {
                    for (i in 0 until SUNBURST_RAY_COUNT) {
                        val centerAngle = 2.0 * PI * i / SUNBURST_RAY_COUNT
                        val a1 = centerAngle - halfSpreadRad
                        val a2 = centerAngle + halfSpreadRad
                        scratchPath.reset()
                        scratchPath.moveTo(focusCenter.x, focusCenter.y)
                        scratchPath.lineTo(
                            focusCenter.x + (cos(a1) * rayLength).toFloat(),
                            focusCenter.y + (sin(a1) * rayLength).toFloat(),
                        )
                        scratchPath.lineTo(
                            focusCenter.x + (cos(a2) * rayLength).toFloat(),
                            focusCenter.y + (sin(a2) * rayLength).toFloat(),
                        )
                        scratchPath.close()
                        drawPath(path = scratchPath, brush = rayBrush)
                    }
                }
            }

            // 3. Triple staggered shockwave halo rings expanding across the full screen.
            val ringDelays = floatArrayOf(0f, 0.09f, 0.19f)
            for (delayFrac in ringDelays) {
                val local = ((t - delayFrac) / 0.45f).coerceIn(0f, 1f)
                if (local > 0.01f && local < 0.99f) {
                    val eased = 1f - (1f - local) * (1f - local)
                    val radius = 44.dp.toPx() + (size.minDimension * 0.56f) * eased
                    val alpha = (1f - local) * 0.36f
                    drawCircle(
                        color = gradientStart.copy(alpha = alpha),
                        radius = radius,
                        center = focusCenter,
                        style = Stroke(width = (6.dp.toPx() * (1f - local * 0.65f)).coerceAtLeast(1.dp.toPx())),
                    )
                }
            }

            // 4. Wave 1: Explosive radial fireworks confetti shooting out from center.
            if (b > 0.01f && b < 0.99f) {
                val fadeIn = (b * 8f).coerceAtMost(1f)
                val fadeOut = ((1f - b) * 1.9f).coerceIn(0f, 1f)
                val alpha = (fadeIn * fadeOut).coerceIn(0f, 1f)
                val maxSpread = minOf(size.width * 0.54f, 230.dp.toPx())
                val gravity = 150.dp.toPx()

                for (p in BURST_PIECES) {
                    val dist = maxSpread * p.speedFactor * b
                    val x = focusCenter.x + (cos(p.angleRad) * dist).toFloat()
                    val y = focusCenter.y + (sin(p.angleRad) * dist).toFloat() + gravity * p.gravityFactor * b * b
                    val px = p.sizeDp.dp.toPx() * (1f - b * 0.25f)
                    val color = confettiPalette[p.paletteIndex].copy(alpha = alpha)
                    drawConfettiShape(p.kind, x, y, px, p.spinDegrees * b, color, scratchPath)
                }
            }

            // 5. Wave 2: Full-screen fluttering confetti shower raining across the entire viewport.
            val showerT = ((t - 0.05f) / 0.95f).coerceIn(0f, 1f)
            if (showerT > 0.01f && showerT < 0.99f) {
                val fadeIn = (showerT * 5f).coerceAtMost(1f)
                val fadeOut = ((1f - showerT) * 3.2f).coerceIn(0f, 1f)
                val alpha = (fadeIn * fadeOut).coerceIn(0f, 1f)

                for (p in SHOWER_PIECES) {
                    val travelY = size.height * 1.28f * p.fallSpeed * showerT
                    val y = size.height * p.startYOffsetFraction + travelY
                    if (y < -30f || y > size.height + 30f) continue
                    val swayPx = sin(showerT * 2f * PI.toFloat() * p.swayCycles + p.phaseRad) * p.swayAmplitudeDp.dp.toPx()
                    val x = size.width * p.startXFraction + swayPx
                    val px = p.sizeDp.dp.toPx()
                    val color = confettiPalette[p.paletteIndex].copy(alpha = alpha * 0.92f)
                    drawConfettiShape(p.kind, x, y, px, p.spinDegrees * showerT, color, scratchPath)
                }
            }
        }

        // Centerpiece medallion + bold milestone headline (stays clear of keyboard & system bars).
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 28.dp)
                .graphicsLayer {
                    val es = exitScale.value
                    scaleX = es
                    scaleY = es
                }
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag("streak_milestone_banner"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Sculpted Hero Streak Medallion.
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val s = medallionScale.value
                        scaleX = s
                        scaleY = s
                        rotationZ = medallionRotation.value
                        val gentleFloat = sin(timeline.value * 2f * PI.toFloat()) * 4.dp.toPx()
                        translationY = gentleFloat
                    },
                contentAlignment = Alignment.Center,
            ) {
                // Soft outer glowing halo disc.
                Box(
                    modifier = Modifier
                        .size(164.dp)
                        .background(gradientStart.copy(alpha = 0.16f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .softShadow(
                            color = gradientStart.copy(alpha = 0.34f),
                            offsetY = 12.dp,
                            blur = 26.dp,
                            spread = (-6).dp,
                            shape = QuizzenShapes.BlobA,
                        )
                        .background(
                            brush = Brush.linearGradient(listOf(gradientStart, gradientEnd)),
                            shape = QuizzenShapes.BlobA,
                        )
                        .border(3.dp, colors.surface.copy(alpha = 0.65f), QuizzenShapes.BlobA),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((-4).dp),
                    ) {
                        Icon(
                            imageVector = QuizzenIcons.Sparkle,
                            contentDescription = null,
                            tint = colors.onPrimary.copy(alpha = 0.92f),
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = milestoneStreak.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            color = colors.onPrimary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Tier pill + bold headline + encouraging subline.
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = copyAlpha.value
                    translationY = copyOffsetY.value.dp.toPx()
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .background(badgeBg, CircleShape)
                        .border(1.dp, gradientStart.copy(alpha = 0.35f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = QuizzenIcons.Sparkle,
                        contentDescription = null,
                        tint = badgeFg,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(tierLabelRes).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = badgeFg,
                    )
                }

                Text(
                    text = stringResource(R.string.streak_milestone_title, milestoneStreak),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = stringResource(sublineRes),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConfettiShape(
    kind: ConfettiKind,
    x: Float,
    y: Float,
    sizePx: Float,
    angleDeg: Float,
    color: Color,
    scratchPath: Path,
) {
    when (kind) {
        ConfettiKind.Circle -> {
            drawCircle(
                color = color,
                radius = sizePx * 0.42f,
                center = Offset(x, y),
            )
        }
        ConfettiKind.Ribbon -> {
            // Subtle tumble-width modulation gives a lively fluttering confetti feel.
            val flutter = (0.45f + 0.55f * abs(cos(angleDeg * PI / 180.0))).toFloat()
            rotate(degrees = angleDeg, pivot = Offset(x, y)) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x - sizePx * 0.78f, y - sizePx * 0.32f * flutter),
                    size = Size(sizePx * 1.56f, (sizePx * 0.64f * flutter).coerceAtLeast(2f)),
                    cornerRadius = CornerRadius(sizePx * 0.25f, sizePx * 0.25f),
                )
            }
        }
        ConfettiKind.Diamond -> {
            rotate(degrees = angleDeg * 0.65f, pivot = Offset(x, y)) {
                val r = sizePx * 0.64f
                scratchPath.reset()
                scratchPath.moveTo(x, y - r)
                scratchPath.lineTo(x + r * 0.68f, y)
                scratchPath.lineTo(x, y + r)
                scratchPath.lineTo(x - r * 0.68f, y)
                scratchPath.close()
                drawPath(path = scratchPath, color = color)
            }
        }
        ConfettiKind.Star -> {
            rotate(degrees = angleDeg * 0.45f, pivot = Offset(x, y)) {
                val outer = sizePx * 0.72f
                val inner = outer * 0.34f
                scratchPath.reset()
                for (i in 0 until 8) {
                    val r = if (i % 2 == 0) outer else inner
                    val rad = (PI / 4.0) * i - PI / 2.0
                    val px = x + (cos(rad) * r).toFloat()
                    val py = y + (sin(rad) * r).toFloat()
                    if (i == 0) scratchPath.moveTo(px, py) else scratchPath.lineTo(px, py)
                }
                scratchPath.close()
                drawPath(path = scratchPath, color = color)
            }
        }
    }
}
