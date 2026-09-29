package com.ravibhaiya.quizzen.ui.components

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asFrameworkPaint
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shadow tint used by the design for neutral (non-colored) shadows: CSS `rgba(20, 18, 32, a)`. */
val NeutralShadowColor = Color(0xFF141220)

/**
 * Draws the equivalent of the CSS `box-shadow: 0 <offsetY> <blur> <spread> <color>`.
 *
 * Android's built-in [shadow] cannot offset, spread or softly blur a shadow and ignores custom colors below API 28, so
 * the web design's soft colored glows come out too dark and hard-edged with it. This blurs the element outline with a
 * Gaussian mask (CSS blur radius B == Gaussian sigma B / 2) and needs API 28+ (hardware-accelerated blur mask).
 * Below API 28 it degrades to a plain elevation shadow for downward shadows and nothing for upward ones.
 *
 * Place it before `clip`/`background` so the shadow sits behind the element and scales with `pressScale`.
 */
fun Modifier.cssShadow(
    color: Color,
    offsetY: Dp,
    blur: Dp,
    spread: Dp,
    shape: Shape,
): Modifier {
    if (color.alpha == 0f) return this
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        return if (offsetY < 0.dp) this else shadow((blur / 4).coerceAtLeast(1.dp), shape, clip = false)
    }
    return drawWithCache {
        val spreadPx = spread.toPx()
        val width = size.width + spreadPx * 2f
        val height = size.height + spreadPx * 2f
        val sigma = blur.toPx() / 2f
        val radius = ((sigma - 0.5f) / SIGMA_PER_RADIUS).coerceAtLeast(0.1f)
        val paint = Paint().apply {
            this.color = color
            asFrameworkPaint().maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        }
        val outline = if (width > 0f && height > 0f) {
            shape.createOutline(Size(width, height), layoutDirection, this)
        } else {
            null
        }
        val dx = -spreadPx
        val dy = offsetY.toPx() - spreadPx
        onDrawBehind {
            if (outline != null) {
                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.translate(dx, dy)
                    canvas.drawOutline(outline, paint)
                    canvas.restore()
                }
            }
        }
    }
}

/** Skia relates a blur mask radius to Gaussian sigma as `sigma = radius * 0.57735 + 0.5`. */
private const val SIGMA_PER_RADIUS = 0.57735f
