package com.ravibhaiya.quizzen.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Tokens that Material 3's [ColorScheme] has no slot for. Read via `MaterialTheme.quizzen`. */
@Immutable
data class QuizzenColors(
    /** End stop of the signature primary -> tone30 gradient (and the tint of hero/logo shadows). */
    val tone30: Color,
    val success: Color,
    val error: Color,
    val warning: Color,
)

// Feedback colors are theme-independent in the original design.
private val Success = Color(0xFF1FAE6A)
private val Error = Color(0xFFE14C4C)
private val Warning = Color(0xFFF2924B)

// Light palette (CSS :root)
private val LightPrimary = Color(0xFFEA7A31)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFFCE1CB)
private val LightOnPrimaryContainer = Color(0xFF7A3B0E)
private val LightSurface = Color(0xFFFFFAF6)
private val LightSurfaceContainer = Color(0xFFFBEEE3)
private val LightSurfaceContainerHigh = Color(0xFFF5E1CE)
private val LightOnSurface = Color(0xFF211710)
private val LightOnSurfaceVariant = Color(0xFF5C4A3B)
private val LightTone30 = Color(0xFFB85E1E)

// Dark palette (CSS prefers-color-scheme: dark)
private val DarkPrimary = Color(0xFFF4B178)
private val DarkOnPrimary = Color(0xFF4A2409)
private val DarkPrimaryContainer = Color(0xFF8A4212)
private val DarkOnPrimaryContainer = Color(0xFFFCE1CB)
private val DarkSurface = Color(0xFF16110C)
private val DarkSurfaceContainer = Color(0xFF241B14)
private val DarkSurfaceContainerHigh = Color(0xFF2F251B)
private val DarkOnSurface = Color(0xFFF1E6DB)
private val DarkOnSurfaceVariant = Color(0xFFD3C2B3)
private val DarkTone30 = Color(0xFFF0964F)

internal val LightQuizzenColors = QuizzenColors(LightTone30, Success, Error, Warning)
internal val DarkQuizzenColors = QuizzenColors(DarkTone30, Success, Error, Warning)

/**
 * Every Material role is set explicitly so no component (Snackbar, dividers, sheets...) can fall back to the
 * baseline purple palette. Roles the web design never defines reuse the design's own tokens.
 */
internal val LightColorScheme: ColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    inversePrimary = DarkPrimary,
    secondary = LightPrimary,
    onSecondary = LightOnPrimary,
    secondaryContainer = LightPrimaryContainer,
    onSecondaryContainer = LightOnPrimaryContainer,
    tertiary = LightTone30,
    onTertiary = LightOnPrimary,
    tertiaryContainer = LightPrimaryContainer,
    onTertiaryContainer = LightOnPrimaryContainer,
    background = LightSurface,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainerHigh,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = LightPrimary,
    inverseSurface = LightOnSurface,
    inverseOnSurface = LightSurface,
    error = Error,
    onError = Color.White,
    errorContainer = Error,
    onErrorContainer = Color.White,
    outline = LightOnSurfaceVariant,
    outlineVariant = LightSurfaceContainerHigh,
    scrim = Color.Black,
    surfaceBright = LightSurface,
    surfaceDim = LightSurfaceContainerHigh,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHigh,
)

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    inversePrimary = LightPrimary,
    secondary = DarkPrimary,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkPrimaryContainer,
    onSecondaryContainer = DarkOnPrimaryContainer,
    tertiary = DarkTone30,
    onTertiary = DarkOnPrimary,
    tertiaryContainer = DarkPrimaryContainer,
    onTertiaryContainer = DarkOnPrimaryContainer,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainerHigh,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = DarkPrimary,
    inverseSurface = DarkOnSurface,
    inverseOnSurface = DarkSurface,
    error = Error,
    onError = Color.White,
    errorContainer = Error,
    onErrorContainer = Color.White,
    outline = DarkOnSurfaceVariant,
    outlineVariant = DarkSurfaceContainerHigh,
    scrim = Color.Black,
    surfaceBright = DarkSurfaceContainerHigh,
    surfaceDim = DarkSurface,
    surfaceContainerLowest = DarkSurface,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHigh,
)
