# AGENTS.md - instructions for AI assistants and contributors

Quizzen is a single-module Android app (Kotlin, Jetpack Compose, Material 3). Read this file first, then the
doc that matches your task: `docs/ARCHITECTURE.md` (structure), `docs/DESIGN_SYSTEM.md` (visuals),
`docs/BEHAVIOR_SPEC.md` (what each screen must do). The original web prototype is the visual/behavioral source of
truth: `docs/reference/quizzen-web-reference.html`.

## Commands

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug   # run before finishing any change
python3 tools/check_docs.py                           # docs must not name code or files that no longer exist
```

Nothing may be committed that fails these. If you cannot run Gradle, say so explicitly and keep changes small and
conservative; do not claim verification you did not do.

## CI rules

CI is intentionally cheap: `ci.yml` runs on pull requests only (and skips docs-only changes), APKs are built only by the manual
`apk.yml`, `release-check.yml` runs only when build settings change. Do not add push-to-main triggers, per-run artifact uploads or
long retention; keep new jobs path-filtered. See the README table.

## Architecture rules

1. **Layers**: `domain` (pure Kotlin, no Android/Compose imports) <- `data` (DataStore) <- `ui` (Compose + ViewModels).
   `domain` must stay unit-testable on the JVM.
2. **One package per feature** under `ui/` containing `XScreen.kt` (stateless-ish composable) and
   `XViewModel.kt` (state holder). Screens take state + lambdas; ViewModels expose `StateFlow<UiState>` and plain
   functions for events. No business logic in composables.
3. **Reusable UI** lives in `ui/components/`. If a composable is used by more than one feature, move it there.
4. **No new dependency** without a strong reason. No DI framework: ViewModels are created with
   `viewModel()` / `viewModelFactory`. Keep the dependency list in `gradle/libs.versions.toml` (single source of versions).
5. **Navigation** is string routes in `ui/navigation/Routes.kt`. Practice configuration travels as route arguments and is
   decoded by `PracticeArgs` (so it survives process death). Add new routes there, never hard-code route strings elsewhere.
6. **State**: `MutableStateFlow` private, `StateFlow` public. UI collects with `collectAsStateWithLifecycle()`.
   Timers run in `viewModelScope` and must pause when the screen is not resumed (see `PracticeViewModel.onPause`).
7. **Settings** persist through `SettingsRepository` (DataStore). Add keys there; expose via `SettingsViewModel`.

## UI rules (do not regress the look and feel)

1. **Never hard-code colors, text sizes, or shapes** in screens. Use `MaterialTheme.colorScheme`, `MaterialTheme.quizzen`
   (extra tokens), `MaterialTheme.typography`, and `QuizzenShapes`. New tokens go in `ui/theme/` AND `docs/DESIGN_SYSTEM.md`.
2. **Light theme only.** Never add a dark palette, `values-night`, `isSystemInDarkTheme()` or a theme switch unless the
   owner explicitly changes this decision (then update all docs).
3. **Haptics** go through `Haptics` (`rememberHaptics(enabled)`) with a semantic `HapticEffect`; never call
   `View.performHapticFeedback` or `Vibrator` directly. Give each new kind of action its own effect and update the table in
   `docs/BEHAVIOR_SPEC.md`. Backgrounds/decoration must look the same on every API level (no `Modifier.blur`).
4. **Shadows** use `Modifier.cssShadow(...)` with the values in `docs/DESIGN_SYSTEM.md`; never `Modifier.shadow`.
   All Material color roles are set in `ui/theme/Color.kt`; never introduce a default/baseline role or a literal color.
5. **User-visible strings** go in `res/values/strings.xml`. Never inline literals in composables (the single-letter
   badges "M", "T", "P", "F", "A", "V" are decoration, not copy).
6. **Interactions**: tappable surfaces use `bouncyClickable` / `pressScale` (scale to 0.96, 0.88 for small icon buttons)
   plus the default ripple. Buttons must have a >= 48 dp touch target or a visually larger container.
7. **Motion** uses `EmphasizedEasing` (cubic-bezier .22, 1, .36, 1). Standard durations: 380 ms screen enter, 350 ms
   sheets, 200 ms color/selection changes, 150 ms outgoing screens.
8. **Accessibility**: every icon-only control needs a `contentDescription` from strings; toggles/selection expose
   `Role` + `selected` semantics; feedback sheet is an assertive live region. Layouts must survive 200% font scale
   (use `FlowRow`, scrolling containers, `FitText`; avoid fixed heights on text).
9. **Insets**: screens are edge-to-edge. Use `statusBarsPadding()` at the top, `navigationBars` insets at the bottom, and
   `imePadding()` on screens with text input. Content is capped at `ContentMaxWidth` (440 dp) via `QuizzenScreen`.
10. **Every screen root is wrapped in `QuizzenScreen`** (surface background + glow circles + max width).
11. **No experimental Material 3 Expressive APIs** are used; the expressive look is built from custom shapes/motion so the
   app compiles against stable Material 3. If you adopt official expressive components later, do it in one commit and
   update `docs/DESIGN_SYSTEM.md`.

## Performance

Full rules in `docs/PERFORMANCE.md`. The short version: nothing may animate or redraw while idle (no endless transitions), animated
values are read inside `graphicsLayer`/`offset`/`drawBehind` lambdas, text is never fitted by relayout loops (`FitText`), no
`Modifier.blur`, and speed is judged on the release APK (`quizzen-release-apk` from CI), never on a debug build.

## Fonts

- Plus Jakarta Sans ships as **static** files (`res/font/plus_jakarta_sans_<weight>.ttf`), never as a variable font: the variable
  font crashed the app on some devices ("Could not load font"). Every `Font(...)` uses `FontLoadingStrategy.OptionalLocal` so a
  bad font file falls back to the system font instead of crashing. Only use weights that exist (400/500/600/700/800).

## Branding assets

- The logo source of truth is `design/quizzen-logo.svg`. To change the logo, replace that file and run
  `python3 tools/generate_logo_drawables.py`; commit the regenerated `ic_launcher_*`, `ic_wordmark` drawables and
  `values/logo_colors.xml`.
  Never hand-edit those generated files.
- The Home header has **no logo** (only the "Quizzen" title); do not add one back without the owner asking.
- Launch: `Theme.Quizzen.Splash` (plain logo-orange system splash, no icon) + `installSplashScreen()` in `MainActivity`
  (keep it before `super.onCreate`), then `SplashArt` draws the logo **full screen** for ~1.1 s and fades out. The system
  splash cannot show a full-screen image (it clips the icon to a circle), which is why `SplashArt` exists.
- Hero-card titles use `FitText` so they never wrap; reuse it for any single-line heading that must not wrap.

## Behavior rules

- `docs/BEHAVIOR_SPEC.md` is normative. If you change behavior, update the spec in the same change.
- Deliberate deviations from the web prototype are listed at the bottom of the spec; keep that list current.
- Timer rules live in `domain/TimerInput.kt` (clamp to 180, blank -> default on focus loss, 0/blank -> default when starting).
- Question rules live in `domain/QuestionGenerator.kt`; answers are `Long` (5x5-digit products overflow `Int`).

## Testing

- Add/adjust JVM unit tests in `app/src/test` for any change in `domain/` or a ViewModel. Use `StandardTestDispatcher` +
  `Dispatchers.setMain` (see `PracticeViewModelTest`).
- Keep generators injectable (`QuestionGenerator`, `kotlin.random.Random`) so tests stay deterministic.

## Style

- Kotlin official style, 4-space indent, 120 cols, trailing commas (see `.editorconfig`).
- Composables: PascalCase, `modifier: Modifier = Modifier` as the first optional parameter, hoist state.
- Prefer small files with a single responsibility; mirror the existing package structure.
- Commit messages: imperative mood, one logical change per commit.

## Question types

`Question` is a sealed type (`ProductQuestion`, `PowerQuestion`, `RootQuestion`, `AlphabetQuestion`, `FractionQuestion`). A new kind of
question needs: a subtype in `domain/Question.kt` (override `answerKind` / `answerText` if the answer is not a plain number: checking goes
through `Question.isCorrect`, never a direct comparison; override `asked(random)` only if the same question can be worded in more than
one way, and keep the wording out of `equals`, as `FractionQuestion` does), a branch in `ui/practice/QuestionText.kt` (`toDisplayText`, `spokenQuestion`
**and** `answerHint`), and a generator branch. Fraction & Percentage asks only the unit fractions of the owner's chart
(`FractionRules.DENOMINATORS`); never add other fractions without the owner asking. Its fraction answers rely on the on-screen "/" key
(`PracticeViewModel.onSlash`) and its percentage answers on the automatic `%` sign (`PercentSuffix` in `PracticeScreen`). Alphabet letters are positions 1..26 (`AlphabetRules`); never repeat 26 elsewhere. Practice-number limits for Powers & Roots (30 for squares and square roots, 20 for cubes and cube roots) live only in
`PowersRootsRules` / `PowerRootType.limit`; never repeat those numbers elsewhere.

## Remembered quiz settings

Each quiz's setup screen restores what the user last started it with: ViewModel takes a `QuizSettingsRepository`, loads in `init`
(with the `edited` guard and the `loaded` flag), and `startQuiz()` saves while returning the config. A new setup screen or a new
setting must go through `SettingsCodec` (encode, decode **with repair**) and get a round-trip test in `SettingsCodecTest`. Never
store anything about running quizzes or answers, with one exception: the unfinished **Vocabulary** quiz (next section). Math quizzes
are never stored.

## Vocabulary session resume

Only the **Language > Vocabulary** quiz can be resumed after the app was closed (or the quiz left); Math quizzes always start from
nothing. The rules, so this stays consistent:

- The unfinished quiz lives in one file, `vocabulary_session_v1.txt` in `filesDir`, behind `data/VocabularySessionStore`
  (`FileVocabularySessionStore`: single writer thread, atomic write, `save`/`clear` never suspend and never crash). It is **not** in
  DataStore (a preferences file is rewritten as a whole) and **not** part of `QuizSettingsRepository`/`SettingsCodec` (those are setup
  settings only).
- The text is made only by `VocabularyQuizSavedState.encodeSession` and read only by `decodeSession` (never trusts the text: wrong
  header, no questions/study words, counts that do not fit the index, an answered question without its result -> `null` = nothing to
  resume). It is self-contained (setup + every question with meanings), so edited or deleted words cannot break it. A new field
  in `VocabularyQuizUiState` that must survive a resume needs `encodeSession`, `decodeSession`, `buildSnapshot` **and** the
  `SavedStateHandle` keys updated together, plus a case in `VocabularyQuizSavedStateTest`.
- Only a running **Quiz** is written: when the quiz starts, when a question is answered, when the next one opens and on pause (never
  per timer tick). Study and Finished write nothing; finishing the quiz clears the file; an unreadable file is cleared.
- Resuming = Vocabulary setup screen -> `VocabularyResumeCard` (`VocabularyConfigViewModel.resume`, refreshed on every screen resume,
  **Discard** clears) -> route `vocabulary_quiz?...&resume=1` (`Routes.vocabularyQuiz(config, resume = true)`, `Routes.ARG_RESUME`).
  `VocabularyQuizViewModel(resumeSavedQuiz = true)` shows nothing (`isRestoring`) until the file is read. A `SavedStateHandle`
  snapshot (process death) always wins over the file.
- A question still open restarts with a **full timer**; an answered one comes back answered with **Next** ready. Do not add resume to
  `PracticeViewModel`/`PracticeSession` without the owner asking (see "Mistake repeats").
- Any change here updates `docs/BEHAVIOR_SPEC.md` ("Resume an unfinished quiz") and `docs/ARCHITECTURE.md` in the same change.

## Vocabulary database (Cloud Firestore)

The owner is the only user (no login, no accounts). Cloud Firestore is the **only source of truth** for the words; the rules,
so this stays consistent (details in `docs/BEHAVIOR_SPEC.md` "Vocabulary Manager" and `docs/ARCHITECTURE.md` "Vocabulary database"):

- **Every write needs internet** and is confirmed by the server before anything on the phone changes. All writes go through
  `FirestoreVocabularyRepository.inCloud` and use `runTransaction(...).await()`; never use a plain `set`/`delete`/`batch.commit`
  (Firestore would queue them while offline and report success later), never write to the database "locally first", never add
  a silent retry or queue. Failure = `Result.failure` and nothing changed.
- **The server list is the truth**: the snapshot listener replaces the known list; never merge the local file back into it (that
  brings deleted words back on other devices). The local JSON file (`vocabulary_cache_v1.json`) is a read-only copy for offline
  reading and quizzes.
- **Merging** (`mergeIntoExisting`) reads the word from the server inside the transaction, so two devices cannot overwrite each
  other. New words are written create-only; edits replace. Merge rules live only in `VocabularyRules` (`domain/Vocabulary.kt`).
- **ViewModel**: the editor and the approval sheet stay open while saving (`isSaving`) and a failure is shown inline; typed JSON
  must never be lost. Notices ("Added N new word(s) to the database.") are shown only after the server confirmed.
- **Never seed the database**: no starter words, no test or demo data in production code. `VocabularyQuizRules.FALLBACK_ENTRIES`
  is a read-only fallback for the quiz only.
- `firestore.rules` and `VocabularyRules.MAX_RELATIONS` must agree (`tools/check_docs.py` checks it). The rules are open on
  purpose (single owner, no login); do not add `request.auth` checks unless the owner asks, and keep `google-services.json` and
  the APK private.
- Tests use a fake `VocabularyRepository` that can fail every write; add a case there for each new write path.

## Keeping docs consistent (do this in the same change)

AI assistants edit this repo over many sessions, so docs drift unless every change updates them. Before you finish:

1. Behavior changed -> `docs/BEHAVIOR_SPEC.md`. Structure, new file, class or data flow -> `docs/ARCHITECTURE.md`. Look/tokens ->
   `docs/DESIGN_SYSTEM.md`. A new rule for future work -> this file. A user-visible feature -> the feature list in `README.md`.
2. Use the real names from the code in backticks (the checker `python3 tools/check_docs.py` fails on names/files that do not
   exist, and CI runs it). Rename something -> search the docs for the old name.
3. Do not describe things that are not implemented (for example timeouts, migrations or files). If you remove a feature, remove its
   text everywhere (docs, README, tests, the `strings.xml` entries) instead of leaving "legacy" notes.
4. Constants live in one place and are only referred to by name in docs and comments, with the numbers written once in
   `docs/BEHAVIOR_SPEC.md` (see the Powers & Roots limits, `MAX_RELATIONS`, `WRONG_REPEATS`).
5. Do not commit tool scratch files (plans, metadata, screenshots, `.aistudio/`); the repo only holds what the app, the build or the docs need.
6. Say in the final message which docs you changed, and what you could not verify (for example "Gradle not run").

## Mistake repeats

The order of questions (shuffled rounds over `QuestionPool`) and how wrong/slow answers come back live only in
`domain/PracticeSession.kt`, `domain/QuestionPool.kt` and `domain/AnswerOutcome.kt` (these are the **Math** quizzes; constants:
`WRONG_REPEATS = 3`, `SLOW_REPEATS = 2`, `WINDOW = 10`, slow = under 40% of the timer left). Keep it pure Kotlin with an injected
`Random`, keep its state inside the object (no static/global state, no persistence: every quiz must start from nothing), and
change `docs/BEHAVIOR_SPEC.md` and `PracticeSessionTest` together with any rule. A new practice mode opts in with
`repeatsMistakes = true` only if its set of questions is small enough for repeats to make sense, and then add it to
`QuestionPool.of` so it is asked in shuffled rounds.

## Adding a new practice mode (checklist)

1. Add a `PracticeConfig` subtype and generator branch in `domain/`; add tests.
2. Add `Routes.MODE_*`, extend `Routes.practice()` and `PracticeArgs.decode()`; add a round-trip test.
3. Create `ui/<mode>/` config screen + ViewModel (reuse `TimerFooter` and `TimerFieldState`).
4. Wire the Home tile/hero `onClick` in `HomeScreen` and the destination in `QuizzenNavHost`.
5. Add strings; update `docs/BEHAVIOR_SPEC.md`.
