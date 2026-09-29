package com.ravibhaiya.quizzen.ui.theme

import androidx.compose.material3.ColorScheme
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

// Palette = CSS `:root` of the web reference. The app is light-only by design (no dark palette).
private val Primary = Color(0xFFEA7A31)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFFFCE1CB)
private val OnPrimaryContainer = Color(0xFF7A3B0E)
private val Surface = Color(0xFFFFFAF6)
private val SurfaceContainer = Color(0xFFFBEEE3)
private val SurfaceContainerHigh = Color(0xFFF5E1CE)
private val OnSurface = Color(0xFF211710)
private val OnSurfaceVariant = Color(0xFF5C4A3B)
private val Tone30 = Color(0xFFB85E1E)

// Feedback colors.
private val Success = Color(0xFF1FAE6A)
private val Error = Color(0xFFE14C4C)
private val Warning = Color(0xFFF2924B)

// Only Material's `inversePrimary` slot needs this; the web design has no equivalent.
private val InversePrimary = Color(0xFFF4B178)

internal val QuizzenExtraColors = QuizzenColors(Tone30, Success, Error, Warning)

/**
 * Every Material role is set explicitly so no component (Snackbar, dividers, sheets...) can fall back to the
 * baseline purple palette. Roles the web design never defines reuse the design's own tokens.
 */
internal val QuizzenColorScheme: ColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = InversePrimary,
    secondary = Primary,
    onSecondary = OnPrimary,
    secondaryContainer = PrimaryContainer,
    onSecondaryContainer = OnPrimaryContainer,
    tertiary = Tone30,
    onTertiary = OnPrimary,
    tertiaryContainer = PrimaryContainer,
    onTertiaryContainer = OnPrimaryContainer,
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = Primary,
    inverseSurface = OnSurface,
    inverseOnSurface = Surface,
    error = Error,
    onError = Color.White,
    errorContainer = Error,
    onErrorContainer = Color.White,
    outline = OnSurfaceVariant,
    outlineVariant = SurfaceContainerHigh,
    scrim = Color.Black,
    surfaceBright = Surface,
    surfaceDim = SurfaceContainerHigh,
    surfaceContainerLowest = Surface,
    surfaceContainerLow = Surface,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHigh,
)
