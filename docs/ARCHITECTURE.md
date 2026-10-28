# Architecture

Single Gradle module `:app`, single Activity, Compose UI, MVVM with unidirectional data flow.

```
 UI (Compose)  --events-->  ViewModel  --uses-->  domain (pure Kotlin)
      ^                         |                     ^
      |  StateFlow<UiState>     v                     |
      +-------------------- data (DataStore / Firestore, local file = read-only copy)
```

## Layers

| Layer | Package | Rules |
|-------|---------|-------|
| domain | `domain/` | Pure Kotlin. `Question` (sealed: `ProductQuestion`, `PowerQuestion`, `RootQuestion`, `AlphabetQuestion`, `FractionQuestion`; `answerKind` / `answerText` / `isCorrect` say what is typed and how it is checked, `asked(random)` lets a question pick its wording), `Feedback`, `PracticeConfig` (Multiply / Tables / PowersRoots / Alphabet / Fractions), `QuestionGenerator`, `TimerInput`, `PowersRootsRules` (limits 30 / 20, validation), `AlphabetChallenge` + `AlphabetRules` (letters as positions 1..26, opposite letter, range validation), `FractionChallenge` + `FractionRules` (the 24 denominators, percentage as a mixed number or decimal worked out from n, fraction and percentage answer checking), `Vocabulary` (`VocabularyEntry`, `WordDetails`, `VocabularyJsonFormat`), `VocabularyQuiz` (`VocabularyQuizMode`, `VocabularyQuizConfig`, `VocabularyQuizItem`, `VocabularyQuizRules`), `VocabularyFallbackData` (25 starter entries), and `VocabularyQuizEngine` (`VocabularyQuizEngine`, `VocabularyMistakeTracker`). No Android imports. |
| data | `data/` | `VocabularyRepository` (`FirestoreVocabularyRepository`: the server list is the truth, every write needs internet and is a confirmed Firestore transaction, `mergeIntoExisting` merges into the server copy; see "Vocabulary database" below), `VocabularyFirestoreMapper` (schema v2 Firestore mapping), and `VocabularyJsonCodec` (versioned JSON, read-only offline copy). Also persists local mistake words (`vocabulary_mistakes_v1.txt`) and `VocabularySessionStore` (`FileVocabularySessionStore`, `vocabulary_session_v1.txt`): the one unfinished Vocabulary quiz, so it can be resumed after the app is closed (Math quizzes are never stored). One DataStore file (`QuizzenDataStore`). `SettingsRepository` (`haptic_enabled`, default `true`) and `QuizSettingsRepository`: the last-used setup of each quiz, one short text per quiz written by `SettingsCodec` (pure Kotlin, repairs bad values on read). |
| ui | `ui/*` | Feature packages with `Screen` + `ViewModel`. `components/` shared widgets. `theme/` design tokens. `navigation/` routes + NavHost. |

## Screens and state

| Screen | ViewModel | State | Notes |
|--------|-----------|-------|-------|
| Home | none (local UI state) | pager position, settings sheet visibility | Pager page = tab. Haptic value comes from `SettingsViewModel`. |
| Multiply config | `MultiplyConfigViewModel` | `firstDigits`, `secondDigits`, `TimerFieldState` | `buildConfig()` -> `PracticeConfig.Multiply`. |
| Tables config | `TablesConfigViewModel` | `selected: Set<Int>`, `TimerFieldState` | Start disabled until >= 1 number selected. |
| Powers & Roots config | `PowersRootsConfigViewModel` | selected `PowerRootType`s, one `IntRange` per kind (squares & roots, cubes & roots), `TimerFieldState` | Limits live in `domain/PowersRoots.kt`; the sliders can only make valid ranges, so Start needs just one selected type. |
| Fractions config | `FractionsConfigViewModel` | `FractionChallenge` (a set of one or both), `FractionRules` (range places), `TimerFieldState` | Range slider (`PillRangeCard`) and two multi-select choices (`ChoiceCard`, at least one stays chosen), so Start is always enabled. |
| Alphabet config | `AlphabetConfigViewModel` | `AlphabetChallenge`, one `IntRange` of letter positions (A = 1 ... Z = 26), `TimerFieldState` | The slider can only make valid ranges (`AlphabetRules.coerce` is the safety net), so Start is always enabled. |
| Vocabulary config | `VocabularyConfigViewModel` | `VocabularyQuizMode`s (`Synonyms` / `Antonyms`), `wordCount` (`5..25`), `TimerFieldState`, `resume` (`VocabularyResumeInfo?`) | Persisted via `SettingsCodec`. Opens `VocabularyQuizScreen`. Shows `VocabularyResumeCard` (Resume / Discard) while `VocabularySessionStore` holds an unfinished quiz; re-reads it on every screen resume. |
| Vocabulary quiz | `VocabularyQuizViewModel` | `VocabularyQuizUiState` (`Study` / `Quiz` / `Finished`, `studyEntries`, `questions`, inline feedback, manual Next) | UI split across `VocabularyQuizScreen`, `VocabularyStudyContent`, `VocabularyActiveQuizContent`, and `VocabularyFinishedContent`. Prioritizes locally saved mistake words via `VocabularyQuizRules.selectStudyWords`, tracks per-word mastery via `VocabularyMistakeTracker`, and preserves session progress across background process death via `VocabularyQuizSavedState` (`SavedStateHandle`). The same class also writes/reads the quiz as one text (`encodeSession` / `decodeSession`) for `VocabularySessionStore`, so a running Quiz survives the app being closed; with the `resume=1` route argument the ViewModel starts with `isRestoring = true`, loads it and continues at the saved question (open question: full timer; answered question: stays answered). |
| Database / Vocabulary manager | `VocabularyManagerViewModel` | `VocabularyUiState` (entries, search query, expanded cards, selection, JSON editor sheet with `isSaving`, `MergePrompt`, delete sheet, notice and error messages) | UI split across `VocabularyManagerScreen`, `VocabularyWordCard`, `VocabularyEditorSheet` (add/edit JSON sheet, `MergeWordConfirmSheet`, `DeleteWordConfirmSheet`) and `VocabularyNotificationsSheet` (approval requests for duplicate or broken documents). |
| Practice | `PracticeViewModel` | `PracticeUiState` (question, remainingSeconds, answer, feedback, isLocked, shakeCount) | Created with `PracticeViewModel.Factory` reading nav args and restoring active question/timer state across process death via `PracticeSavedState` (`SavedStateHandle`). |
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
`onPause` cancels ticking without resetting the remaining time and persists the current question and remaining seconds to
`SavedStateHandle`; `onResume` continues.

## Navigation

`ui/navigation/Routes.kt` defines `home`, `multiply`, `tables`, `powers`, `fractions`, `alphabet`, `vocabulary_config`,
`database_manager`, `vocabulary_manager`, `vocabulary_quiz`, and `practice/{mode}`. `Routes.practice(config)` and
`Routes.vocabularyQuiz(config, resume)` (adds `&resume=1` only when resuming a saved quiz) encode; `PracticeArgs` and `VocabularyQuizArgs` decode (defensively: missing/invalid values fall
back to defaults). Back from a quiz returns to whichever config screen started it.
Transitions: incoming = fade + 16 px rise (380 ms, emphasized easing); outgoing = 150 ms fade.

## Theming

`QuizzenTheme` wraps `MaterialTheme` with a custom `ColorScheme`, `Typography` (Plus Jakarta Sans, five static font files) and an
extra `QuizzenColors` set (tone30, success, error, timeout) via `MaterialTheme.quizzen`. The app is light-only: no dark
palette, no `values-night`, system bars forced to light style in `MainActivity`.

## Vocabulary database

Cloud Firestore (`vocabulary` collection, one document per main word, id from `VocabularyRules.documentIdForWord`) is the only source of truth; the app is for one owner and has no login.

- Reads: `FirestoreVocabularyRepository.entries` follows a snapshot listener. Each server list replaces what the app knew (deletes on another device propagate) and is saved to `vocabulary_cache_v1.json` for offline reading. An empty answer from Firestore's own cache is ignored.
- Writes (save, merge, delete) go through `inCloud`: fail at once without internet (`NoInternetException`), time out after 20 s, and use `runTransaction(...).await()` so that Firestore never queues a write for later. State (list, local file, mistake words) changes only after the server confirmed.
- Merge: one transaction per word reads the server copy, applies `VocabularyRules.mergeRelations` and writes it back. Saving a new word is create-only (refused when the document already exists), editing replaces.
- Nothing is seeded: no starter words and no test data go into the database. `VocabularyQuizRules.FALLBACK_ENTRIES` is a read-only fallback for the quiz.
- ViewModel: `VocabularyManagerViewModel` keeps the editor / approval sheet open while saving and shows failures inline, so typed JSON is never lost.
- Tests: the repository cannot run on the JVM (Firebase), so rules live in `domain/Vocabulary.kt` (`VocabularyRulesTest`) and the ViewModel is tested against a fake repository that fails every write on demand (`VocabularyManagerViewModelTest`).

## Testing strategy

JVM unit tests only (fast, no emulator): generator ranges, timer clamping rules, practice and vocabulary quiz state machines with
a virtual clock, saved-state process-death restoration, Vocabulary resume from the device (`VocabularyQuizSavedStateTest`, `VocabularyQuizViewModelTest`, `ConfigViewModelsTest` with `FakeVocabularySessionStore`), settings and JSON/Firestore codec round-trips.

## Known extension points

- Implement the remaining Language placeholder tiles (`Fixed Preposition`, `Phrasal Verb`) using the same database and MCQ quiz
  architecture as `Vocabulary`.
