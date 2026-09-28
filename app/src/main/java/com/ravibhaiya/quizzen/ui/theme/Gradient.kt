package com.ravibhaiya.quizzen.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Linear gradient using CSS angle semantics (0deg = to top, 90deg = to right, 140deg = toward bottom-right),
 * so the gradient line spans the box exactly like `linear-gradient(140deg, ...)` in the web original.
 */
fun cssLinearGradient(angleDegrees: Float, colors: List<Color>): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val dx = sin(radians).toFloat()
        val dy = -cos(radians).toFloat()
        val length = abs(size.width * dx) + abs(size.height * dy)
        val center = Offset(size.width / 2f, size.height / 2f)
        val half = Offset(dx * length / 2f, dy * length / 2f)
        return LinearGradientShader(
            from = center - half,
            to = center + half,
            colors = colors,
            tileMode = TileMode.Clamp,
        )
    }
}

/** The signature primary -> tone30 gradient (140deg) used on the logo, hero, tile and timer icons. */
fun primaryGradient(primary: Color, tone30: Color): Brush = cssLinearGradient(140f, listOf(primary, tone30))

/** Hero card uses 135deg. */
fun heroGradient(primary: Color, tone30: Color): Brush = cssLinearGradient(135f, listOf(primary, tone30))
