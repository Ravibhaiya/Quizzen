# Performance (low-end phones and Android 11)

Target: smooth on 2 GB / entry-level GPUs and on Android 8-11 (`minSdk 26`). This page records what the app does for that, how to
measure, and what was deliberately *not* done.

## Measure the right build

A **debug** build of a Compose app is several times slower than release (no R8, no ahead-of-time compilation, extra checks).
Judge speed only on the **release** build: every CI run uploads `quizzen-release-apk` (R8-minified, baseline profile, signed
with the debug key so it installs directly; built with `-PsignReleaseWithDebugKey`). A release build for the Play Store must use
the real upload key instead (the flag is off by default, so a plain `assembleRelease` stays unsigned).

## What the app already does

| Area | Measure |
|---|---|
| Startup / first frames | `baseline-prof.txt` (all app code) plus `androidx.profileinstaller`, so ART compiles hot code at install and merges the Compose/Material profiles; without it the first launch runs interpreted. |
| Idle cost | Nothing animates while idle: the timer chip pulses only in the last 5 seconds of an unanswered question (an always-running infinite transition used to redraw every frame). |
| Relayout | `FitText` picks its font size in one pass with a text measurer and a binary search (`fitFontSize`, about 5 measurements) instead of re-laying-out the screen up to ~20 times. |
| Recomposition | Animated values (press scale, segmented indicator, pulse, glow) are read inside `graphicsLayer` / `offset {}` / `drawBehind` lambdas, so they animate without recomposing. |
| Transitions | One animated layer at a time: forward = the new screen fades/rises in over the old one; back = the leaving screen fades out. No full-screen cross-fade. |
| Shadows | `cssShadow` draws one cached Gaussian-blurred outline; Skia renders blurred rounded rects/rects with a cheap analytic shader on the GPU. Alpha-0 shadows are skipped. API 26-27 fall back to a plain elevation shadow. |
| Glow | A radial-gradient disc (no `Modifier.blur`, which only exists on API 31+ and is expensive). |
| Haptics | Vibrator waveforms from precomputed patterns; no allocation per tap beyond the effect. |
| Size / memory | R8 + resource shrinking, English-only resources (`localeFilters`), five static font files loaded lazily, vector art instead of bitmaps. |
| Fonts | Static files with `OptionalLocal`, so a font failure falls back to the system font instead of crashing. |

## Rules for new code

1. No `rememberInfiniteTransition` / endless animation unless it is visible and needed; stop it when it is not.
2. Read animated `State` inside draw/layout/graphicsLayer lambdas (`graphicsLayer { scaleX = value }`), not in the composable body.
3. Do not add `Modifier.blur`, `RenderEffect` or `BlurMaskFilter` per frame outside `cssShadow`; do not add full-screen offscreen layers.
4. Never repeat a relayout loop to fit text; use `FitText` / `fitFontSize`.
5. Keep first-frame work small: no disk or network on the main thread, no heavy work in `onCreate`.
6. Test performance on the **release** APK on a real low-end phone.

## Considered and not done (no evidence they are needed yet)

- **Pre-rendering shadows into bitmaps**: the GPU blur path is already cheap for rounded rects; bitmaps would cost memory.
- **Removing the window background after the first frame** (saves one full-screen fill): small gain, risk of a black flash on some devices.
- **Merging per-cell animations on the Tables grid** (30 cells x 3 colour animations): only matters on the first composition; revisit if profiling shows it.
- **ABI filters**: x86 images would stop working; Play App Bundles already deliver per-ABI splits.

## How to measure further

- Android Studio Profiler / `adb shell dumpsys gfxinfo com.ravibhaiya.quizzen framestats` (janky-frame percentage) while scrolling Home and
  opening Tables.
- Developer options -> "Profile GPU rendering" and "Debug GPU overdraw".
- A Macrobenchmark module (`StartupTimingMetric`, `FrameTimingMetric`) would give repeatable numbers and can regenerate the
  baseline profile; add it if performance becomes a recurring topic.
