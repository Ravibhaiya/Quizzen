# Design system

Ported 1:1 from the CSS custom properties of the web prototype. `rem` values assume 16 px = 16 dp/sp.

## Color

The app is **light-only by design**: it ignores the system dark-mode setting, forces dark status/navigation bar icons and
opts out of OEM "force dark". There is no dark palette, no `values-night` resources and no theme switch. (The web reference
has a dark palette; it was intentionally not ported.)

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
| quizzen.success / error / warning | `#1FAE6A / #E14C4C / #F2924B` |

The web CSS also defines tone40-70 and dark-on-light, but no rule uses them, so they are intentionally not ported.
All tokens above were verified against the CSS of `docs/reference/quizzen-web-reference.html`, and the rendered light-theme
web screenshots match them pixel for pixel.

Material roles the web design never defines (secondary, tertiary, inverse*, outline*, extra surface containers) reuse these
same tokens (see `ui/theme/Color.kt`) so no component can fall back to the baseline purple palette.
`QuizzenScreen` sets `LocalContentColor` to `onSurface`.

Signature gradient: `linear-gradient(140deg, primary, tone30)` = `primaryGradient()`; hero card uses 135deg = `heroGradient()`.
`cssLinearGradient()` reproduces CSS angle semantics exactly.

## Typography (Plus Jakarta Sans, variable weight axis)

| Role | Size | Weight | Tracking | Used for |
|---|---|---|---|---|
| displayLarge | 49.6 sp (3.1rem) | 800 | -0.02em | Practice question (auto-shrinks to 24 sp in 1.6 sp steps) |
| headlineLarge | 24.8 sp | 800 | -0.02em | App name (-0.03em), hero title, screen titles (24 sp) |
| headlineMedium | 20.8 sp | 800 | -0.02em | Settings title, logo letter |
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

Organic blobs (`QuizzenShapes`, four elliptical corners = CSS `border-radius` with `/`):
Logo `34% 66% 60% 40% / 44% 36% 64% 56%`, Hero `32% 68% 62% 38% / 46% 38% 62% 54%`,
BlobA `38% 62% 55% 45% / 48% 40% 60% 52%`, BlobB `62% 38% 45% 55% / 40% 55% 45% 60%`.

## Elevation / shadow

Shadows are CSS-accurate: `Modifier.cssShadow(color, offsetY, blur, spread, shape)` blurs the element outline with a Gaussian
mask (needs API 28+; below that it degrades to a plain elevation shadow, or nothing for upward shadows). Never use
`Modifier.shadow`, it cannot offset, spread, soften or tint a shadow the way the web design does.

| Element | CSS `box-shadow` (offsetY blur spread color) |
|---|---|
| Hero card | `18 32 -14` tone30 @ 65% |
| Logo | `8 16 -5` tone30 @ 55% |
| Segmented indicator | `8 16 -5` primary @ 60% |
| Primary button | `14 26 -12` primary @ 60% |
| Selected chip | `8 16 -6` primary @ 55% (animated 150 ms) |
| Selected number cell | `8 16 -8` primary @ 55% (animated 150 ms) |
| Feature tile | `2 6 0` `rgba(20,18,32,.05)` |
| Answer field | `2 8 0` `rgba(20,18,32,.05)`; focused adds `10 20 -10` primary @ 40% + 4 dp ring primary @ 20% |
| Footer bar | `-6 18 -14` `rgba(20,18,32,.18)` |
| Feedback sheet | `-14 32 -12` black @ 28% |
| Switch thumb | `2 5 0` black @ 20% |

## Layout

Screen padding 18 dp horizontal, 24 dp top; bento gap 14 dp; page spacing 18 dp; content max width 440 dp; two decorative
glow circles (280 dp top-right, 220 dp bottom-left, primaryContainer @ 35%, edge softened like CSS `blur(10px)` with a radial gradient, identical on every API level).

## Motion

- Easing `cubic-bezier(.22, 1, .36, 1)` (`EmphasizedEasing`).
- Screen enter: fade + 16 dp rise, 380 ms. Sheets: 350 ms slide. Segmented indicator: follows pager position; tab tap animates the pager 400 ms.
- Press: scale 0.96 (0.88 icon buttons), spring. Selection color changes 150-200 ms.
- Timer chip pulses 1.0 -> 1.1 (1 s cycle) when <= 5 s remain.
- Wrong answer: 400 ms horizontal shake, keyframes -3, 5, -9, 9, -9, 9, -9, 5, -3, 0 dp.
- Answer field focus: fills with `surface`, 4 dp primary@20% ring.

## Icons

Feather-style 24 dp stroke icons ported to `QuizzenIcons` (path data from the prototype's inline SVGs). Stroke 2 dp
(2.4 for chevrons/check-all, 3 for feedback icons), round caps/joins, tinted through `Icon(tint=...)`.
