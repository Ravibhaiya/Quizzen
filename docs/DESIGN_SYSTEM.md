# Design system

Design tokens of the app, with the exact values used in `ui/theme/` and the shared components.

## Color

The app is **light-only by design**: it ignores the system dark-mode setting, forces dark status/navigation bar icons and
opts out of OEM "force dark". There is no dark palette, no `values-night` resources and no theme switch.

| Token (Material role) | Value |
|---|---|
| primary | `#EA7A31` |
| onPrimary | `#FFFFFF` |
| primaryContainer | `#FCE1CB` |
| onPrimaryContainer | `#7A3B0E` |
| surface / background | `#FFFAF6` |
| surfaceContainer | `#FBEEE3` |
| surfaceContainerHigh | `#F5E1CE` |
| onSurface | `#211710` |
| onSurfaceVariant | `#5C4A3B` |
| quizzen.tone30 (gradient end) | `#B85E1E` |
| quizzen.success / error / timeout | `#1FAE6A / #E14C4C / #2E9FE0` (timeout = the sky-blue "Time's Up" sheet) |
| quizzen.synonym / synonymContainer / onSynonymContainer | `#15806A / #D6F3EC / #0B4A3E` (emerald-teal badge, border, and surface tint for Synonyms) |
| quizzen.antonym / antonymContainer / onAntonymContainer | `#7B3FA0 / #EDE0FA / #421861` (plum-violet badge, border, and surface tint for Antonyms) |
| quizzen.badgeBronze (`StreakBadgePalette`) | `#C68B59 / #9A5B32 / #68381A`, container `#F2E4D8 / #4E2912`, trim `#E8C5A5` (Badge 1 — Bronze, streak 5) |
| quizzen.badgeSilver (`StreakBadgePalette`) | `#CBD5E1 / #7C8EA6 / #475569`, container `#E7EDF5 / #243244`, trim `#F1F5F9` (Badge 2 — Silver, streak 10) |
| quizzen.badgeGold (`StreakBadgePalette`) | `#F3C948 / #C88E0B / #8A5A04`, container `#FEF3C7 / #5C3B02`, trim `#FDE68A` (Badge 3 — Gold, streak 15) |
| quizzen.badgePlatinum (`StreakBadgePalette`) | `#9CD4EC / #4A8DB0 / #285A78`, container `#E0F2FA / #163A50`, trim `#D8F1FC` (Badge 4 — Platinum, streak 20) |
| quizzen.badgeEmerald (`StreakBadgePalette`) | `#34D399 / #059669 / #046C4E`, container `#D1FAE5 / #064E3B`, trim `#A7F3D0` (Badge 5 — Emerald, streak 25) |
| quizzen.badgeSapphire (`StreakBadgePalette`) | `#60A5FA / #2563EB / #1E40AF`, container `#DBEAFE / #1E3A8A`, trim `#BFDBFE` (Badge 6 — Sapphire, streak 30) |
| quizzen.badgeRuby (`StreakBadgePalette`) | `#FB7185 / #E11D48 / #9F1239`, container `#FFE4E6 / #881337`, trim `#FECDD3` (Badge 7 — Ruby, streak 35) |
| quizzen.badgeAmethyst (`StreakBadgePalette`) | `#A78BFA / #7C3AED / #5B21B6`, container `#EDE9FE / #3B0764`, trim `#DDD6FE` (Badge 8 — Amethyst, streak 40) |
| quizzen.badgeDiamond (`StreakBadgePalette`) | `#67E8F9 / #0891B2 / #155E75`, container `#CFFAFE / #164E63`, trim `#E0F9FF` (Badge 9 — Diamond, streak 45) |
| quizzen.badgeLegend (`StreakBadgePalette`) | `#C026D3 / #4F46E5 / #312E81`, container `#E0E7FF / #2E1065`, trim `#F3C948` (Badge 10 — Legend, streak 50+) |

Material roles without a dedicated token (secondary, tertiary, inverse*, outline*, extra surface containers) reuse these
same tokens (see `ui/theme/Color.kt`) so no component can fall back to the baseline purple palette.
`QuizzenScreen` sets `LocalContentColor` to `onSurface`.

Signature gradient: 140 degrees from `primary` to `tone30` = `primaryGradient()`; the hero card uses 135 degrees = `heroGradient()`.
`angledGradient()` builds a linear gradient from an angle (0 = to top, 90 = to right) whose line spans the whole box.

## Typography (Plus Jakarta Sans, static weights 400/500/600/700/800)

| Role | Size | Weight | Tracking | Used for |
|---|---|---|---|---|
| displayLarge | 49.6 sp | 800 | -0.02em | Practice question (auto-shrinks to 24 sp in 1.6 sp steps) |
| headlineLarge | 24.8 sp | 800 | -0.02em | App name (-0.03em), hero title, screen titles (24 sp) |
| headlineMedium | 20.8 sp | 800 | -0.02em | Settings title |
| titleLarge | 19.2 sp | 800 | -0.01em | Section titles, tile letters (700), table cells (21.6 sp) |
| titleMedium | 17.6 sp | 800 | 0 | Primary button label, "Timer" (700) |
| titleSmall | 16.96 sp | 700 | -0.01em | Tile titles (line height 20 sp) |
| bodyLarge | 16.8 sp | 400 | 0 | Section label; segmented labels use 16 sp |
| bodyMedium | 16.3 sp | 600 | 0 | Chips (16.32 sp / 700, 18 dp side padding), settings rows (16.64 sp) |
| labelLarge | 15.2 sp | 800 | 0 | Timer chip |
| labelSmall | 10.56 sp | 700 | +0.05em | "SEC" unit |

## Shape

| Element | Radius |
|---|---|
| Hero card | 38 dp |
| Feature tile | 30 dp |
| Timer card | 28 dp; answer field 28 dp |
| Number cell | 22 dp |
| Chip | 20 dp; select-all 18 dp; timer value box 18 dp |
| Icon button | 15 dp (44 dp square) |
| Sheets | 32 dp top corners |
| Segmented control, primary button, timer chip | pill |

Organic blobs (`QuizzenShapes`, four elliptical corners; the numbers are the horizontal radii, then `/`, then the vertical radii,
each listed from the top-left corner clockwise, as percentages of the width and height):
Hero `32% 68% 62% 38% / 46% 38% 62% 54%`,
BlobA `38% 62% 55% 45% / 48% 40% 60% 52%`, BlobB `62% 38% 45% 55% / 40% 55% 45% 60%`.

## Elevation / shadow

Shadows use `Modifier.softShadow(color, offsetY, blur, spread, shape)`, which blurs the element outline with a Gaussian
mask (needs API 28+; below that it degrades to a plain elevation shadow, or nothing for upward shadows). Never use
`Modifier.shadow`, it cannot offset, spread, soften or tint a shadow the way the design needs. "Neutral" below is
`NeutralShadowColor` (`#141220`) at the given alpha.

| Element | Shadow (offsetY blur spread color) |
|---|---|
| Hero card | `18 32 -14` tone30 @ 65% |
| Segmented indicator | `8 16 -5` primary @ 60% |
| Primary button | `14 26 -12` primary @ 60% |
| Selected chip | `8 16 -6` primary @ 55% (animated 150 ms) |
| Selected number cell | `8 16 -8` primary @ 55% (animated 150 ms) |
| Feature tile | `2 6 0` neutral @ 5% |
| Answer field | `2 8 0` neutral @ 5%; focused adds `10 20 -10` primary @ 40% + 4 dp ring primary @ 20% |
| Footer bar | `-6 18 -14` neutral @ 18% |
| Feedback sheet | `-14 32 -12` black @ 28% |
| Switch thumb | `2 5 0` black @ 20% |

## Layout

Screen padding 18 dp horizontal, 24 dp top; bento gap 14 dp; page spacing 18 dp; content max width 440 dp; two decorative
glow circles (280 dp top-right, 220 dp bottom-left, primaryContainer @ 35%, edge softened with a Gaussian profile (sigma 10 dp) drawn as a radial gradient, identical on every API level).

## Motion

- Easing `cubic-bezier(.22, 1, .36, 1)` (`EmphasizedEasing`).
- Screen enter: fade + 16 dp rise, 380 ms. Sheets: 350 ms slide. Segmented indicator: follows pager position; tab tap animates the pager 400 ms.
- Press: scale 0.96 (0.88 icon buttons), spring. Selection color changes 150-200 ms.
- Timer chip pulses 1.0 -> 1.1 (1 s cycle) when <= 5 s remain.
- Wrong answer: 400 ms horizontal shake, keyframes -3, 5, -9, 9, -9, 9, -9, 5, -3, 0 dp.
- Answer field focus: fills with `surface`, 4 dp primary@20% ring.

## Range slider (Powers & Roots)

`QuizzenRangeSlider` (a styled Material 3 `RangeSlider`, whole-number steps, no tick marks) sits in a card per kind (24 dp
radius, surfaceContainer, `2 6 0` neutral shadow at 5%). Card header: kind name (16 sp Bold) on the left, a pill with the range
on the right (primaryContainer, onPrimaryContainer, 15 sp ExtraBold, `2–30`). The slider follows the Material 3 Expressive
shape. Track: 16 dp tall and cut into three pieces: soft peach (`surfaceContainerHigh`) before the first handle, primary between the
handles, soft peach after the second. Outer ends are fully round, the ends that face a handle have a 3 dp corner. Handles: slim
vertical pills (6 dp wide, 40 dp tall, primary) with a 6 dp gap to the track on both sides; the pill narrows to 4 dp (spring) while
held. A 4 dp primary dot sits near each end of the track while the range does not reach that end. Under the track, the two end
numbers (1 and the limit, 12 sp onSurfaceVariant) line up with the track ends (3 dp in from the card padding). A card whose kinds
are not selected fades to 45% and is disabled.

## Alphabet setup (letter range, challenge cards)

No new colours or shapes: everything is built from the tokens above.

- **Letter range card**: same card as the Powers & Roots range cards (24 dp radius, `surfaceContainer`, `2 6 0` neutral shadow at 5%).
  Top row: the picked letters as two pills, the left thumb's letter at the left and the right thumb's letter at the right (`primaryContainer`
  with `onPrimaryContainer`, fully rounded, min 56 dp wide, 16 / 8 dp padding, letter in `titleMedium` at 20 sp ExtraBold). Under it
  `QuizzenRangeSlider` (1 to 26) and the two end letters (12 sp `onSurfaceVariant`).
- **Challenge card** (single select, radio): full width, 24 dp radius, 18 dp padding, a 24 dp radio mark (2 dp ring, 12 dp dot) and the
  name only (16.32 sp Bold). Selection is the chip language: selected = `primary` fill, `onPrimary` text and the soft `primary@55%`
  shadow (`0 8 16 -6`); not selected = `surfaceContainer`. Press scale 0.96, 200 ms colour changes.
  It lives in `ui/components/ChoiceCard.kt` (with `SectionTitle`) and is shared with the Fraction & Percentage setup screen.

## Fraction & Percentage (setup and practice)

No new colours or shapes either.

- **Setup**: section title "Answer in" and two Challenge cards (Fraction, Percentage) in multi-select form (a rounded-square check
  box, 24 dp, 7 dp corners, 2 dp border in the content colour; chosen = filled with a bold check in the card colour), then the usual
  timer footer.
- **Range**: the `PillRangeCard` (`ui/components/PillRangeCard.kt`, shared with the Alphabet letter range): a pill per thumb (`1/2` ...
  `1/50`), the two-thumb slider and the end labels under it.
- **Question text** (`StackedFractionText`): a fraction is drawn stacked, `1` over a bar over the denominator, at the size of the
  other question texts (bar in the text colour, 10% of the digit size thick, reaching a little past the digits). A percentage ends
  with a `primary` `%`. A mixed number (`33 1/3%`) is the whole part at full size, a gap of 10% of its size, the same stacked fraction
  at 50% size centred on the whole part, and the `%`. The decimal writing (`33.33%`) is plain text.
- **"/" key** (fraction answers only): a 48 dp circle at the end of the answer field, 12 dp from the field edge, `primaryContainer`
  with the slash in `onPrimaryContainer` (24 sp). Press scale 0.88 like the small icon buttons, light tick haptic.
- **Automatic `%` sign** (percentage answers only): shown after the typed digits in `onSurfaceVariant`, same size as the answer.

## Logo, launcher icon and splash

Single source: `design/quizzen-logo.svg` (rounded-square orange-to-red gradient, white "Quizzen" wordmark, yellow dot and Q
tail, bottom swoosh). `python3 tools/generate_logo_drawables.py` turns it into generated resources (never edited by hand):
`ic_launcher_background` (gradient + swoosh, full bleed: adaptive icon background **and** the full-screen splash backdrop),
`ic_launcher_foreground` (wordmark 64 dp wide inside the 66 dp adaptive-icon safe zone), `ic_launcher_monochrome`
(Android 13+ themed icons), `ic_wordmark` (tightly cropped wordmark for the splash) and `values/logo_colors.xml`
(`splash_background`, the midpoint of the logo gradient).

Splash = two steps: the system splash (`Theme.Quizzen.Splash`, solid `splash_background`, transparent icon) then `SplashArt`,
which draws `ic_launcher_background` with `ContentScale.Crop` over the whole screen and `ic_wordmark` centred at 72% of the
screen width. This is needed because the system splash can only show an icon clipped to a circle, never a full-screen image.
The Home header carries no logo.

The generated art matches the SVG exactly except for the soft blurred drop shadow under the letters: `VectorDrawable` has no
blur filter, so it is omitted.

## Icons

Feather-style 24 dp stroke icons in `QuizzenIcons`. Stroke 2 dp
(2.4 for chevrons/check-all, 3 for feedback icons), round caps/joins, tinted through `Icon(tint=...)`.
