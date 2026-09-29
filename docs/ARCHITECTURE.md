# Architecture

Single Gradle module `:app`, single Activity, Compose UI, MVVM with unidirectional data flow.

```
 UI (Compose)  --events-->  ViewModel  --uses-->  domain (pure Kotlin)
      ^                         |                     ^
      |  StateFlow<UiState>     v                     |
      +-------------------- data (DataStore) ---------+ (settings only)
```

## Layers

| Layer | Package | Rules |
|-------|---------|-------|
| domain | `domain/` | Pure Kotlin. `Question`, `Feedback`, `PracticeConfig` (Multiply / Tables), `QuestionGenerator`, `TimerInput`. No Android imports. |
| data | `data/` | `SettingsRepository` interface + `DataStoreSettingsRepository` (`haptic_enabled`, default `true`). |
| ui | `ui/*` | Feature packages with `Screen` + `ViewModel`. `components/` shared widgets. `theme/` design tokens. `navigation/` routes + NavHost. |

## Screens and state

| Screen | ViewModel | State | Notes |
|--------|-----------|-------|-------|
| Home | none (local UI state) | pager position, settings sheet visibility | Pager page = tab. Haptic value comes from `SettingsViewModel`. |
| Multiply config | `MultiplyConfigViewModel` | `firstDigits`, `secondDigits`, `TimerFieldState` | `buildConfig()` -> `PracticeConfig.Multiply`. |
| Tables config | `TablesConfigViewModel` | `selected: Set<Int>`, `TimerFieldState` | Start disabled until >= 1 number selected. |
| Practice | `PracticeViewModel` | `PracticeUiState` (question, remainingSeconds, answer, feedback, isLocked, shakeCount) | Created with `PracticeViewModel.Factory` reading nav args via `SavedStateHandle`. |
| App-wide | `SettingsViewModel` (Activity scoped) | `hapticEnabled` | Passed down as a plain `Boolean` + callback. |

### Practice loop

```
onResume -> tick every 1 s -> remaining == 0 -> Timeout
onCheck  -> Correct | Incorrect
showFeedback: lock input, cancel ticking, feedback visible for 1700 ms,
              hide sheet, wait 250 ms, next question, reset timer, resume ticking (if still in foreground)
```
`onPause` cancels ticking without resetting the remaining time, `onResume` continues.

## Navigation

`ui/navigation/Routes.kt` defines `home`, `multiply`, `tables` and
`practice/{mode}?d1=&d2=&numbers=&seconds=`. `Routes.practice(config)` encodes; `PracticeArgs` decodes (defensively:
missing/invalid values fall back to defaults). Back from Practice returns to whichever config screen started it.
Transitions: incoming = fade + 16 px rise (380 ms, emphasized easing); outgoing = 150 ms fade.

## Theming

`QuizzenTheme` wraps `MaterialTheme` with a custom `ColorScheme`, `Typography` (Plus Jakarta Sans variable font) and an
extra `QuizzenColors` set (tone30, success, error, warning) via `MaterialTheme.quizzen`. The app is light-only: no dark
palette, no `values-night`, system bars forced to light style in `MainActivity`.

## Testing strategy

JVM unit tests only (fast, no emulator): generator ranges, timer clamping rules, practice state machine with a virtual
clock, navigation argument round-trips. Compose UI tests are a good next addition (instrumented, `androidTest`).

## Known extension points

- Persist last-used configuration (digit counts, table selection, timers) in `SettingsRepository`.
- Implement the placeholder tiles as new `PracticeConfig` subtypes (see the checklist in `AGENTS.md`).
- "Database Manager" row in Settings is a placeholder (results/history storage would go in a Room-backed `data` layer).
