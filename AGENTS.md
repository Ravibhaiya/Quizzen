# AGENTS.md - instructions for AI assistants and contributors

Quizzen is a single-module Android app (Kotlin, Jetpack Compose, Material 3). Read this file first, then the
doc that matches your task: `docs/ARCHITECTURE.md` (structure), `docs/DESIGN_SYSTEM.md` (visuals),
`docs/BEHAVIOR_SPEC.md` (what each screen must do). The original web prototype is the visual/behavioral source of
truth: `docs/reference/quizzen-web-reference.html`.

## Commands

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug   # run before finishing any change
```

Nothing may be committed that fails these. If you cannot run Gradle, say so explicitly and keep changes small and
conservative; do not claim verification you did not do.

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

## Adding a new practice mode (checklist)

1. Add a `PracticeConfig` subtype and generator branch in `domain/`; add tests.
2. Add `Routes.MODE_*`, extend `Routes.practice()` and `PracticeArgs.decode()`; add a round-trip test.
3. Create `ui/<mode>/` config screen + ViewModel (reuse `TimerFooter` and `TimerFieldState`).
4. Wire the Home tile/hero `onClick` in `HomeScreen` and the destination in `QuizzenNavHost`.
5. Add strings; update `docs/BEHAVIOR_SPEC.md`.
