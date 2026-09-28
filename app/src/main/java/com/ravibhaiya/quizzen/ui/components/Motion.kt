package com.ravibhaiya.quizzen.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/** The web original's signature ease: cubic-bezier(.22, 1, .36, 1). */
val EmphasizedEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * Scales the element down while [interactionSource] is pressed (web: `:active { transform: scale(.96) }`).
 * Apply BEFORE shadow/clip/background so the whole visual scales together.
 */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.96f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Clip + ripple + click with the press-scale behavior. Backgrounds/shadows must be chained
 * between [pressScale] and this call by the caller, or use [bouncyClickable] for the simple case.
 */
@Composable
fun Modifier.bouncyClickable(
    shape: Shape,
    pressedScale: Float = 0.96f,
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    return this
        .pressScale(source, pressedScale)
        .clip(shape)
        .clickable(
            interactionSource = source,
            indication = LocalIndication.current,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}
