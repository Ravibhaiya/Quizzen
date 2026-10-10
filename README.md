# Quizzen

A native Android (Kotlin + Jetpack Compose) practice app for competitive-exam style mental math, built in a
Material 3 "Expressive" visual language: organic blob shapes, a warm orange palette, big rounded surfaces and
springy press animations. It is a native re-implementation of the original single-file web prototype
(kept for reference in [`docs/reference/quizzen-web-reference.html`](docs/reference/quizzen-web-reference.html)).

## Features

- **Home** with a Math / Language segmented control and swipeable tabs (animated pill indicator).
- **Multiply**: pick digit counts (2-5) for each operand, set a per-question timer (0-180 s), practice.
- **Tables Practice**: pick any of the tables 2-31 (or Select All), set a timer, practice `n x 1..10`.
- **Powers & Roots**: choose squares, cubes, square roots and/or cube roots, a two number-range sliders (squares and square
  roots up to 30, cubes and cube roots up to 20) and a timer.
- **Alphabet Reasoning**: pick a letter range (first and last letter, one slider), one of three challenges (find a letter's
  position, find the letter at a position, or the opposite letter from the other end of the alphabet) and a timer.
- **Fraction & Percentage**: the 24 unit fractions of the chart (1/2 ... 1/50, choose the range with a two-thumb slider). Answer in **Fraction** (a percentage such as `33⅓%` or
  `33.33%` is shown, type `1/3` with the on-screen "/" key), in **Percentage** (a fraction is shown, type `33.33`; the `%` sign is
  added for you) or in both mixed together, and a timer.
- **Shuffled rounds** (Tables, Powers & Roots, Alphabet, Fraction & Percentage): every question is asked once in a random order before any repeats, then a new
  shuffle, forever.
- **Mistake repeats** (Tables, Powers & Roots, Alphabet, Fraction & Percentage): a wrong answer brings the same question back 3 times, a slow one 2 times, within
  the next 10 questions and never right away; nothing is saved between quizzes.
- **Remembered settings**: each quiz's setup screen reopens with what you last started it with (digits, numbers, types, ranges,
  timer); stored on the device only.
- **Practice**: live countdown chip (pulses in the last 5 s), numeric answer field, animated feedback
  bottom sheet (Correct / Incorrect / Time's Up), shake on wrong answers, auto-advance to a fresh question.
- **Settings** bottom sheet with a persisted Haptic Feedback switch.
- Light theme only (by design, regardless of the system dark-mode setting), edge-to-edge, IME-aware layouts, 440 dp max content width on large screens.
- Other tiles (Fraction & Percentage, Vocabulary, Fixed Preposition,
  Phrasal Verb) and Database Manager are placeholders, exactly as in the prototype (they show "Coming soon").

## Tech stack

Kotlin 2.0, Jetpack Compose (BOM 2024.12.01) + Material 3, Navigation Compose, Lifecycle ViewModel + StateFlow,
DataStore Preferences, Kotlin Coroutines. `minSdk 26`, `targetSdk/compileSdk 35`, JDK 17.
Plus Jakarta Sans (five static weights, SIL OFL) is bundled in `res/font`.

## Build & run

CI uploads two APKs per run (Actions tab): `quizzen-debug-apk` and `quizzen-release-apk`. **Use the release one to judge speed**
(see [docs/PERFORMANCE.md](docs/PERFORMANCE.md)); debug builds of Compose apps are much slower.

Requirements: Android Studio Ladybug (2024.2) or newer, or JDK 17 + Android SDK 35 on the command line.

```bash
./gradlew assembleDebug        # build a debug APK  -> app/build/outputs/apk/debug
./gradlew installDebug         # install on a connected device / emulator
./gradlew testDebugUnitTest    # run unit tests
./gradlew lintDebug            # Android lint
```

## Continuous integration (GitHub Actions)

Kept deliberately light so the free plan is never a concern (a public repository has no minutes limit; a private one on the free
plan gets 2,000 minutes and 500 MB of storage a month, and this setup uses roughly 4 minutes per pull request).

| Workflow | When it runs | What it does |
|---|---|---|
| `ci.yml` | pull requests (not for docs-only changes) | unit tests, lint, debug build, logo-sync check; uploads nothing |
| `release-check.yml` | pull requests that change build settings, dependencies or shrinker rules | optimized (R8) release build |
| `apk.yml` | only when you press **Run workflow** in the Actions tab, or push a `v*` tag | optimized APK, kept 14 days (debug APK optional) |

To get an APK to install: Actions tab, **Build APK**, **Run workflow**, then download `quizzen-release-apk` from the finished run.

## Project layout

```
app/src/main/java/com/ravibhaiya/quizzen/
  MainActivity.kt          single-activity entry, edge-to-edge, theme, nav host
  data/                    SettingsRepository (DataStore)
  domain/                  pure Kotlin: Question, PracticeConfig, QuestionGenerator, TimerInput
  ui/theme/                colors, typography, blob shapes, CSS-style gradients
  ui/components/           reusable Compose building blocks (buttons, chips, segmented control, timer footer, ...)
  ui/home | multiply | tables | practice | settings     one package per feature (Screen + ViewModel)
  ui/navigation/           routes, argument codec, NavHost + transitions
design/                    quizzen-logo.svg (logo source of truth)
tools/                     generate_logo_drawables.py (SVG -> Android vector drawables)
docs/                      ARCHITECTURE, DESIGN_SYSTEM, BEHAVIOR_SPEC, reference web prototype
AGENTS.md                  rules for AI coding assistants (start here when changing code)
```

## Documentation

- [AGENTS.md](AGENTS.md) - conventions and checklists for AI assistants and contributors
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) - layers, data flow, state, navigation
- [docs/PERFORMANCE.md](docs/PERFORMANCE.md) - low-end phone / Android 11 measures, rules and measuring tips
- [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) - tokens (color, type, shape, motion) mapped from the web original
- [docs/BEHAVIOR_SPEC.md](docs/BEHAVIOR_SPEC.md) - screen-by-screen behavior, plus every deliberate deviation from the prototype
- [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md)

## License

No license file is included yet; add the one you want before making the repository public.
