# Design system

Ported 1:1 from the CSS custom properties of the web prototype. `rem` values assume 16 px = 16 dp/sp.

## Color

| Token (Material role) | Light | Dark |
|---|---|---|
| primary | `#EA7A31` | `#F4B178` |
| onPrimary | `#FFFFFF` | `#4A2409` |
| primaryContainer | `#FCE1CB` | `#8A4212` |
| onPrimaryContainer | `#7A3B0E` | `#FCE1CB` |
| surface / background | `#FFFAF6` | `#16110C` |
| surfaceContainer | `#FBEEE3` | `#241B14` |
| surfaceContainerHigh | `#F5E1CE` | `#2F251B` |
| onSurface | `#211710` | `#F1E6DB` |
| onSurfaceVariant | `#5C4A3B` | `#D3C2B3` |
| quizzen.tone30 (gradient end) | `#B85E1E` | `#F0964F` |
| quizzen.tone40 / 50 / 60 / 70 | `#EA7A31 #F0964F #F4B178 #F8CBA3` | `#EE8D42 #F2A464 #F6BE8F #FAD5B8` |
| quizzen.success / error / warning | `#1FAE6A / #E14C4C / #F2924B` (same in both themes) | |

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
| bodyMedium | 16.3 sp | 600 | 0 | Chips (16.32 sp / 700), settings rows (16.64 sp) |
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

Colored shadows use `Modifier.shadow(..., ambientColor, spotColor)` (colored on API 28+, neutral on 26-27):
hero (tone30, 14 dp), logo (tone30, 6 dp), selected chip/cell/segment indicator/primary button (primary, 5-10 dp),
tiles (2 dp neutral), footer bar (8 dp upward), feedback sheet (14 dp).

## Layout

Screen padding 18 dp horizontal, 24 dp top; bento gap 14 dp; page spacing 18 dp; content max width 440 dp; two decorative
glow circles (280 dp top-right, 220 dp bottom-left, primaryContainer @ 35%).

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
