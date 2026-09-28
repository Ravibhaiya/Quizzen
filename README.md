# Quizzen

A native Android (Kotlin + Jetpack Compose) practice app for competitive-exam style mental math, built in a
Material 3 "Expressive" visual language: organic blob shapes, a warm orange palette, big rounded surfaces and
springy press animations. It is a native re-implementation of the original single-file web prototype
(kept for reference in [`docs/reference/quizzen-web-reference.html`](docs/reference/quizzen-web-reference.html)).

## Features

- **Home** with a Math / Language segmented control and swipeable tabs (animated pill indicator).
- **Multiply**: pick digit counts (2-5) for each operand, set a per-question timer (0-180 s), practice.
- **Tables Practice**: pick any of the tables 2-31 (or Select All), set a timer, practice `n x 1..10`.
- **Practice**: live countdown chip (pulses in the last 5 s), numeric answer field, animated feedback
  bottom sheet (Correct / Incorrect / Time's Up), shake on wrong answers, auto-advance to a fresh question.
- **Settings** bottom sheet with a persisted Haptic Feedback switch.
- Light / dark theme following the system, edge-to-edge, IME-aware layouts, 440 dp max content width on large screens.
- Other tiles (Powers & Roots, Fraction & Percentage, Alphabet Reasoning, Vocabulary, Fixed Preposition,
  Phrasal Verb) and Database Manager are placeholders, exactly as in the prototype (they show "Coming soon").

## Tech stack

Kotlin 2.0, Jetpack Compose (BOM 2024.12.01) + Material 3, Navigation Compose, Lifecycle ViewModel + StateFlow,
DataStore Preferences, Kotlin Coroutines. `minSdk 26`, `targetSdk/compileSdk 35`, JDK 17.
Plus Jakarta Sans (variable font, SIL OFL) is bundled in `res/font`.

## Build & run

Requirements: Android Studio Ladybug (2024.2) or newer, or JDK 17 + Android SDK 35 on the command line.

```bash
./gradlew assembleDebug        # build a debug APK  -> app/build/outputs/apk/debug
./gradlew installDebug         # install on a connected device / emulator
./gradlew testDebugUnitTest    # run unit tests
./gradlew lintDebug            # Android lint
```

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
docs/                      ARCHITECTURE, DESIGN_SYSTEM, BEHAVIOR_SPEC, reference web prototype
AGENTS.md                  rules for AI coding assistants (start here when changing code)
```

## Documentation

- [AGENTS.md](AGENTS.md) - conventions and checklists for AI assistants and contributors
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) - layers, data flow, state, navigation
- [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) - tokens (color, type, shape, motion) mapped from the web original
- [docs/BEHAVIOR_SPEC.md](docs/BEHAVIOR_SPEC.md) - screen-by-screen behavior, plus every deliberate deviation from the prototype
- [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md)

## License

No license file is included yet; add the one you want before making the repository public.
