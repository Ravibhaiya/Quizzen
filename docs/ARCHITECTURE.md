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
| domain | `domain/` | Pure Kotlin. `Question` (sealed: `ProductQuestion`, `PowerQuestion`, `RootQuestion`, `AlphabetQuestion`, `FractionQuestion`; `answerKind` / `answerText` / `isCorrect` say what is typed and how it is checked, `asked(random)` lets a question pick its wording), `Feedback`, `PracticeConfig` (Multiply / Tables / PowersRoots / Alphabet / Fractions), `QuestionGenerator`, `TimerInput`, `PowersRootsRules` (limits 30 / 20, validation), `AlphabetChallenge` + `AlphabetRules` (letters as positions 1..26, opposite letter, range validation), `FractionChallenge` + `FractionRules` (the 24 denominators, percentage as a mixed number or decimal worked out from n, fraction and percentage answer checking). No Android imports. |
| data | `data/` | One DataStore file (`QuizzenDataStore`). `SettingsRepository` (`haptic_enabled`, default `true`) and `QuizSettingsRepository`: the last-used setup of each quiz, one short text per quiz written by `SettingsCodec` (pure Kotlin, repairs bad values on read). |
| ui | `ui/*` | Feature packages with `Screen` + `ViewModel`. `components/` shared widgets. `theme/` design tokens. `navigation/` routes + NavHost. |

## Screens and state

| Screen | ViewModel | State | Notes |
|--------|-----------|-------|-------|
| Home | none (local UI state) | pager position, settings sheet visibility | Pager page = tab. Haptic value comes from `SettingsViewModel`. |
| Multiply config | `MultiplyConfigViewModel` | `firstDigits`, `secondDigits`, `TimerFieldState` | `buildConfig()` -> `PracticeConfig.Multiply`. |
| Tables config | `TablesConfigViewModel` | `selected: Set<Int>`, `TimerFieldState` | Start disabled until >= 1 number selected. |
| Powers & Roots config | `PowersRootsConfigViewModel` | selected `PowerRootType`s, one `IntRange` per kind (squares & roots, cubes & roots), `TimerFieldState` | Limits live in `domain/PowersRoots.kt`; the sliders can only make valid ranges, so Start needs just one selected type. |
| Fractions config | `FractionsConfigViewModel` | `FractionChallenge`, `FractionRules` (range places), `TimerFieldState` | Range slider (`PillRangeCard`) and two choices (`ChoiceCard`), so Start is always enabled. |
| Alphabet config | `AlphabetConfigViewModel` | `AlphabetChallenge`, one `IntRange` of letter positions (A = 1 ... Z = 26), `TimerFieldState` | The slider can only make valid ranges (`AlphabetRules.coerce` is the safety net), so Start is always enabled. |
| Practice | `PracticeViewModel` | `PracticeUiState` (question, remainingSeconds, answer, feedback, isLocked, shakeCount) | Created with `PracticeViewModel.Factory` reading nav args via `SavedStateHandle`. |
| App-wide | `SettingsViewModel` (Activity scoped) | `hapticEnabled` | Passed down as a plain `Boolean` + callback. |

### Choosing the next question

`PracticeSession` (domain, pure Kotlin) sits between `PracticeViewModel` and the `QuestionGenerator`. The ViewModel reports each
answer (`AnswerOutcome.classify(correct, remainingSeconds, totalSeconds)`: Fast / Slow / Wrong) and asks for the next question.
For limited-question quizzes (`PracticeConfig.repeatsMistakes`) the session asks the whole list from `QuestionPool` in shuffled
rounds (every question once per round, then a new shuffle, forever) and adds comebacks (3 for Wrong, 2 for Slow, inside the
next 10, never next, never adjacent); Multiply draws each question at random from the generator. The session is owned
by the ViewModel, which is created per quiz start, so it is empty at the start of every quiz and discarded when the quiz is left.

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

`QuizzenTheme` wraps `MaterialTheme` with a custom `ColorScheme`, `Typography` (Plus Jakarta Sans, five static font files) and an
extra `QuizzenColors` set (tone30, success, error, timeout) via `MaterialTheme.quizzen`. The app is light-only: no dark
palette, no `values-night`, system bars forced to light style in `MainActivity`.

## Testing strategy

JVM unit tests only (fast, no emulator): generator ranges, timer clamping rules, practice state machine with a virtual
clock, navigation argument round-trips. Compose UI tests are a good next addition (instrumented, `androidTest`).

## Known extension points

- Persist last-used configuration (digit counts, table selection, timers) in `SettingsRepository`.
- Implement the placeholder tiles as new `PracticeConfig` subtypes (see the checklist in `AGENTS.md`).
- "Database Manager" row in Settings is a placeholder (results/history storage would go in a Room-backed `data` layer).
