package com.ravibhaiya.quizzen.ui.theme

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Organic "blob" made of four elliptical corners, equivalent to CSS
 * `border-radius: tlH trH brH blH / tlV trV brV blV` with percentages (0..1).
 * Horizontal radii on one edge and vertical radii on one side must each sum to <= 1.
 */
class BlobShape(
    private val tlH: Float, private val trH: Float, private val brH: Float, private val blH: Float,
    private val tlV: Float, private val trV: Float, private val brV: Float, private val blV: Float,
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(tlH * w, 0f)
            lineTo(w - trH * w, 0f)
            arcTo(Rect(w - 2 * trH * w, 0f, w, 2 * trV * h), -90f, 90f, false)
            lineTo(w, h - brV * h)
            arcTo(Rect(w - 2 * brH * w, h - 2 * brV * h, w, h), 0f, 90f, false)
            lineTo(blH * w, h)
            arcTo(Rect(0f, h - 2 * blV * h, 2 * blH * w, h), 90f, 90f, false)
            lineTo(0f, tlV * h)
            arcTo(Rect(0f, 0f, 2 * tlH * w, 2 * tlV * h), 180f, 90f, false)
            close()
        }
        return Outline.Generic(path)
    }
}

object QuizzenShapes {
    /** Hero icon: 32% 68% 62% 38% / 46% 38% 62% 54% */
    val Hero = BlobShape(.32f, .68f, .62f, .38f, .46f, .38f, .62f, .54f)

    /** Tile / timer icon variant A: 38% 62% 55% 45% / 48% 40% 60% 52% */
    val BlobA = BlobShape(.38f, .62f, .55f, .45f, .48f, .40f, .60f, .52f)

    /** Tile icon variant B: 62% 38% 45% 55% / 40% 55% 45% 60% */
    val BlobB = BlobShape(.62f, .38f, .45f, .55f, .40f, .55f, .45f, .60f)
}
