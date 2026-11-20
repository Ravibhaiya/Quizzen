package com.ravibhaiya.quizzen.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Metallic/gemstone color palette for a single progressive streak badge. */
@Immutable
data class StreakBadgePalette(
    val highlight: Color,
    val primary: Color,
    val deep: Color,
    val container: Color,
    val onContainer: Color,
    val trim: Color,
)

/** Tokens that Material 3's [ColorScheme] has no slot for. Read via `MaterialTheme.quizzen`. */
@Immutable
data class QuizzenColors(
    /** End stop of the signature primary -> tone30 gradient (and the tint of hero/logo shadows). */
    val tone30: Color,
    val success: Color,
    val error: Color,
    /** Background of the "Time's Up" sheet. */
    val timeout: Color,
    /** Accent and badge colors that visually distinguish Synonyms (same meaning) from Antonyms (opposite meaning). */
    val synonym: Color,
    val synonymContainer: Color,
    val onSynonymContainer: Color,
    val antonym: Color,
    val antonymContainer: Color,
    val onAntonymContainer: Color,
    /** Palettes for the 10 progressive streak milestone badges (Bronze through Legend). */
    val badgeBronze: StreakBadgePalette,
    val badgeSilver: StreakBadgePalette,
    val badgeGold: StreakBadgePalette,
    val badgePlatinum: StreakBadgePalette,
    val badgeEmerald: StreakBadgePalette,
    val badgeSapphire: StreakBadgePalette,
    val badgeRuby: StreakBadgePalette,
    val badgeAmethyst: StreakBadgePalette,
    val badgeDiamond: StreakBadgePalette,
    val badgeLegend: StreakBadgePalette,
)

// Palette of the app. It is light-only by design (no dark palette).
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
// Sky blue: sits with the green/red sheets at a similar brightness (white text contrast ~2.9:1, like the green one).
private val Timeout = Color(0xFF2E9FE0)

// Vocabulary relation tokens: Emerald-Teal for Synonyms vs Plum-Violet for Antonyms.
private val SynonymAccent = Color(0xFF15806A)
private val SynonymContainer = Color(0xFFD6F3EC)
private val OnSynonymContainer = Color(0xFF0B4A3E)
private val AntonymAccent = Color(0xFF7B3FA0)
private val AntonymContainer = Color(0xFFEDE0FA)
private val OnAntonymContainer = Color(0xFF421861)

// Progressive 10-badge streak palettes (metallic and gemstone tiers, distinct from the app's primary orange).
private val BadgeBronze = StreakBadgePalette(
    highlight = Color(0xFFC68B59),
    primary = Color(0xFF9A5B32),
    deep = Color(0xFF68381A),
    container = Color(0xFFF2E4D8),
    onContainer = Color(0xFF4E2912),
    trim = Color(0xFFE8C5A5),
)
private val BadgeSilver = StreakBadgePalette(
    highlight = Color(0xFFCBD5E1),
    primary = Color(0xFF7C8EA6),
    deep = Color(0xFF475569),
    container = Color(0xFFE7EDF5),
    onContainer = Color(0xFF243244),
    trim = Color(0xFFF1F5F9),
)
private val BadgeGold = StreakBadgePalette(
    highlight = Color(0xFFF3C948),
    primary = Color(0xFFC88E0B),
    deep = Color(0xFF8A5A04),
    container = Color(0xFFFEF3C7),
    onContainer = Color(0xFF5C3B02),
    trim = Color(0xFFFDE68A),
)
private val BadgePlatinum = StreakBadgePalette(
    highlight = Color(0xFF9CD4EC),
    primary = Color(0xFF4A8DB0),
    deep = Color(0xFF285A78),
    container = Color(0xFFE0F2FA),
    onContainer = Color(0xFF163A50),
    trim = Color(0xFFD8F1FC),
)
private val BadgeEmerald = StreakBadgePalette(
    highlight = Color(0xFF34D399),
    primary = Color(0xFF059669),
    deep = Color(0xFF046C4E),
    container = Color(0xFFD1FAE5),
    onContainer = Color(0xFF064E3B),
    trim = Color(0xFFA7F3D0),
)
private val BadgeSapphire = StreakBadgePalette(
    highlight = Color(0xFF60A5FA),
    primary = Color(0xFF2563EB),
    deep = Color(0xFF1E40AF),
    container = Color(0xFFDBEAFE),
    onContainer = Color(0xFF1E3A8A),
    trim = Color(0xFFBFDBFE),
)
private val BadgeRuby = StreakBadgePalette(
    highlight = Color(0xFFFB7185),
    primary = Color(0xFFE11D48),
    deep = Color(0xFF9F1239),
    container = Color(0xFFFFE4E6),
    onContainer = Color(0xFF881337),
    trim = Color(0xFFFECDD3),
)
private val BadgeAmethyst = StreakBadgePalette(
    highlight = Color(0xFFA78BFA),
    primary = Color(0xFF7C3AED),
    deep = Color(0xFF5B21B6),
    container = Color(0xFFEDE9FE),
    onContainer = Color(0xFF3B0764),
    trim = Color(0xFFDDD6FE),
)
private val BadgeDiamond = StreakBadgePalette(
    highlight = Color(0xFF67E8F9),
    primary = Color(0xFF0891B2),
    deep = Color(0xFF155E75),
    container = Color(0xFFCFFAFE),
    onContainer = Color(0xFF164E63),
    trim = Color(0xFFE0F9FF),
)
private val BadgeLegend = StreakBadgePalette(
    highlight = Color(0xFFC026D3),
    primary = Color(0xFF4F46E5),
    deep = Color(0xFF312E81),
    container = Color(0xFFE0E7FF),
    onContainer = Color(0xFF2E1065),
    trim = Color(0xFFF3C948),
)

// Only Material's `inversePrimary` slot needs this extra color.
private val InversePrimary = Color(0xFFF4B178)

internal val QuizzenExtraColors = QuizzenColors(
    tone30 = Tone30,
    success = Success,
    error = Error,
    timeout = Timeout,
    synonym = SynonymAccent,
    synonymContainer = SynonymContainer,
    onSynonymContainer = OnSynonymContainer,
    antonym = AntonymAccent,
    antonymContainer = AntonymContainer,
    onAntonymContainer = OnAntonymContainer,
    badgeBronze = BadgeBronze,
    badgeSilver = BadgeSilver,
    badgeGold = BadgeGold,
    badgePlatinum = BadgePlatinum,
    badgeEmerald = BadgeEmerald,
    badgeSapphire = BadgeSapphire,
    badgeRuby = BadgeRuby,
    badgeAmethyst = BadgeAmethyst,
    badgeDiamond = BadgeDiamond,
    badgeLegend = BadgeLegend,
)

/**
 * Every Material role is set explicitly so no component (Snackbar, dividers, sheets...) can fall back to the
 * baseline purple palette. Roles without a dedicated token reuse the design's own tokens.
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
