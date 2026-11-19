# Quizzen

A native Android (Kotlin + Jetpack Compose) practice app for competitive-exam style mental math and vocabulary, built in a
Material 3 "Expressive" visual language: organic blob shapes, a warm orange palette, big rounded surfaces and
springy press animations.

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
  the next 10 questions and never right away; nothing is saved between quizzes (Math quizzes always start fresh).
- **Remembered settings**: each quiz's setup screen reopens with what you last started it with (digits, numbers, types, ranges,
  timer); stored on the device only.
- **Practice**: live countdown chip (pulses in the last 5 s), numeric answer field, animated feedback
  bottom sheet (Correct / Incorrect / Time's Up), shake on wrong answers, auto-advance to a fresh question.
- **Settings** bottom sheet with a persisted Haptic Feedback switch, the **Database Manager** entry and a **Gemini API Key** field (the key is saved on the device only and is used for the vocabulary checks below).
- Light theme only (by design, regardless of the system dark-mode setting), edge-to-edge, IME-aware layouts, 440 dp max content width on large screens.
- **Vocabulary (Language)**: choose **Synonyms**, **Antonyms**, or both simultaneously, select **1 to 25 words** to attempt, review all selected words (with their **Part of Speech** badge on the right side of the main word and distinct emerald-teal **Synonyms** and plum-violet **Antonyms** sections) on a pre-quiz **Study** page, and take a finite **4-option MCQ quiz** with inline feedback, post-answer meaning reveals, manual **Next** button progression, and local mistake tracking (saving wrong/timed-out words locally, prioritizing them in future quizzes, and automatically deleting a word once enough questions for that word are answered right in a session). If the app is closed (or the quiz left) mid-quiz, the Vocabulary setup screen offers **Resume** from the question you stopped at (or **Discard**); only Vocabulary can be resumed.
- **Database Manager -> Vocabulary**: add/edit vocabulary words (including optional `partOfSpeech`) via single/bulk JSON format or by importing a `.json` file from device storage, saved to Cloud Firestore (internet is required to add, merge or delete, and a word counts as added only after the server confirmed it; the words can still be read offline), collapsible cards with part-of-speech badges and color-coded synonym/antonym count pills, and header search.
- **Approval Requests** (bell icon in the Vocabulary manager): duplicate, variant (root form, spelling) and synonym/antonym meaning-conflict checks done by Gemini Decision 1 with your Gemini API key; every finding can be merged, deleted, edited or dismissed, and only new or edited words are checked again.
- Other tiles (Fixed Preposition, Phrasal Verb) are placeholders (they show "Coming soon").

## Tech stack

Kotlin 2.0, Jetpack Compose (BOM 2024.12.01) + Material 3, Navigation Compose, Lifecycle ViewModel + StateFlow,
DataStore Preferences, Cloud Firestore (Firebase BoM 33.7.0), Kotlin Coroutines. `minSdk 26`, `targetSdk/compileSdk 35`, JDK 17.
Plus Jakarta Sans (five static weights, SIL OFL) is bundled in `res/font`.

## Build & run

**Use a release build to judge speed** (see [docs/PERFORMANCE.md](docs/PERFORMANCE.md)); debug builds of Compose apps are much slower.
For Cloud Firestore access, place your private Firebase config at `app/google-services.json` (git-ignored).

Requirements: Android Studio Ladybug (2024.2) or newer, or JDK 17 + Android SDK 35 on the command line.

```bash
gradle assembleDebug           # build a debug APK  -> app/build/outputs/apk/debug
gradle assembleRelease         # build a release APK -> app/build/outputs/apk/release
gradle testDebugUnitTest       # run unit tests
gradle lintDebug               # Android lint
```

## Project layout

```
app/src/main/java/com/ravibhaiya/quizzen/
  MainActivity.kt          single-activity entry, edge-to-edge, theme, nav host
  data/                    DataStore settings, Cloud Firestore vocabulary repository, Gemini decision client, local caches
  domain/                  pure Kotlin (no Android): questions, rules, quiz engines, vocabulary and Gemini request/response rules
  ui/theme/                colors, typography, blob shapes, angled gradients
  ui/components/           reusable Compose building blocks (buttons, chips, segmented control, timer footer, ...)
  ui/home | multiply | tables | powers | alphabet | fractions | practice | settings | database | vocabulary | vocabularyquiz | splash
                           one package per feature (Screen + ViewModel)
  ui/navigation/           routes, argument codec, NavHost + transitions
design/                    quizzen-logo.svg (logo source of truth)
tools/                     generate_logo_drawables.py (SVG -> Android vector drawables), check_docs.py (docs vs code)
firestore.rules            Cloud Firestore security rules
docs/                      ARCHITECTURE, DESIGN_SYSTEM, BEHAVIOR_SPEC, PERFORMANCE, THIRD_PARTY_NOTICES
AGENTS.md                  rules for AI coding assistants (start here when changing code)
```

## Documentation

- [AGENTS.md](AGENTS.md) - conventions and checklists for AI assistants and contributors
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) - layers, data flow, state, navigation
- [docs/PERFORMANCE.md](docs/PERFORMANCE.md) - low-end phone / Android 11 measures, rules and measuring tips
- [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) - tokens (color, type, shape, shadow, motion) and component styles
- [docs/BEHAVIOR_SPEC.md](docs/BEHAVIOR_SPEC.md) - screen-by-screen behavior, plus the deliberate design decisions and why they were made
- [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md)

## License

MIT, see [LICENSE](LICENSE).
