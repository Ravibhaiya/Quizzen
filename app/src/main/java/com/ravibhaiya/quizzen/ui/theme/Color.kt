package com.ravibhaiya.quizzen.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Tokens that Material 3's [ColorScheme] has no slot for. Read via `MaterialTheme.quizzen`. */
@Immutable
data class QuizzenColors(
    val tone30: Color,
    val tone40: Color,
    val tone50: Color,
    val tone60: Color,
    val tone70: Color,
    val success: Color,
    val error: Color,
    val warning: Color,
)

// Feedback colors are theme-independent in the original design.
private val Success = Color(0xFF1FAE6A)
private val Error = Color(0xFFE14C4C)
private val Warning = Color(0xFFF2924B)

internal val LightQuizzenColors = QuizzenColors(
    tone30 = Color(0xFFB85E1E),
    tone40 = Color(0xFFEA7A31),
    tone50 = Color(0xFFF0964F),
    tone60 = Color(0xFFF4B178),
    tone70 = Color(0xFFF8CBA3),
    success = Success,
    error = Error,
    warning = Warning,
)

internal val DarkQuizzenColors = QuizzenColors(
    tone30 = Color(0xFFF0964F),
    tone40 = Color(0xFFEE8D42),
    tone50 = Color(0xFFF2A464),
    tone60 = Color(0xFFF6BE8F),
    tone70 = Color(0xFFFAD5B8),
    success = Success,
    error = Error,
    warning = Warning,
)

internal val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFEA7A31),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFCE1CB),
    onPrimaryContainer = Color(0xFF7A3B0E),
    background = Color(0xFFFFFAF6),
    onBackground = Color(0xFF211710),
    surface = Color(0xFFFFFAF6),
    onSurface = Color(0xFF211710),
    onSurfaceVariant = Color(0xFF5C4A3B),
    surfaceContainer = Color(0xFFFBEEE3),
    surfaceContainerHigh = Color(0xFFF5E1CE),
    error = Error,
)

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFF4B178),
    onPrimary = Color(0xFF4A2409),
    primaryContainer = Color(0xFF8A4212),
    onPrimaryContainer = Color(0xFFFCE1CB),
    background = Color(0xFF16110C),
    onBackground = Color(0xFFF1E6DB),
    surface = Color(0xFF16110C),
    onSurface = Color(0xFFF1E6DB),
    onSurfaceVariant = Color(0xFFD3C2B3),
    surfaceContainer = Color(0xFF241B14),
    surfaceContainerHigh = Color(0xFF2F251B),
    error = Error,
)
