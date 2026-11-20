# Behavior specification

Normative description of the app: what every screen shows and does.

## Screen transitions
- Forward: the new screen fades in and rises 16 dp (380 ms). Back: the leaving screen fades out (200 ms) over the screen underneath.
  Only one screen animates at a time (see docs/PERFORMANCE.md).

## Launch
- Cold start: a plain logo-orange system splash, immediately followed by the logo shown **full screen** (gradient and swoosh fill
  the whole screen, wordmark centred at 72% of the width, fades/scales in). After 1.1 s it fades out (350 ms) into Home; touches
  are ignored meanwhile. Status/navigation bar icons are white during the logo and dark afterwards. Rotation does not replay it.
- Launcher icon is an adaptive icon (wordmark on the logo gradient, plus a monochrome layer for themed icons).

## Theme
- Light only. The system dark-mode setting is ignored (status/navigation bar icons stay dark; OEM force-dark is opted out).

## Home
- Header: "Quizzen" title (no logo) and the Settings icon button (opens the settings sheet).
- Hero-card titles ("Multiply", "Vocabulary") are always a single line: the text shrinks (24.8 sp down to 14 sp) instead of wrapping.
- Segmented control: **Math** | **Language**. Tap animates the pager; swiping the pager moves the indicator with the finger.
- **Math** page: hero "Multiply" (-> Multiply config); tiles "Tables Practice" (-> Tables config), "Powers & Roots"
  (-> Powers & Roots config), "Alphabet Reasoning" (-> Alphabet config), "Fraction & Percentage" (-> Fraction & Percentage config).
- **Language** page: hero "Vocabulary" (-> Vocabulary config); tiles "Fixed Preposition", "Phrasal Verb" (placeholders).
- Placeholders show a "Coming soon" snackbar.

## Settings sheet (modal bottom sheet)
- **Haptic Feedback** switch, default on, persisted. Toggling on gives a confirmation tick.
- **Database Manager** row -> closes the sheet and navigates to the **Database Manager** screen.
- **Gemini API Key** row -> expands an inline editor (`gemini_api_key_input`, masked by default with a show/hide eye toggle `toggle_api_key_visibility_button`) to paste, test (`Test Key`, running `GeminiDecisionClient.verifyApiKey` with an inline status banner), save (`Save Key`), or remove (`Remove`) a local Gemini API key stored on-device via `SettingsRepository` (`DataStoreSettingsRepository`, never published to GitHub; falls back to `BuildConfig.GEMINI_API_KEY`). Shows a `Configured` status pill when a key is active or `Add key` when unset.
- Dismiss by scrim tap, drag down, or back.

## Database Manager
- Header: back, "Database Manager", subtitle "Select a category to manage data".
- Shows all options from the Language section:
  - Hero card **Vocabulary** ("V") -> opens **Vocabulary** manager screen.
  - Tiles **Fixed Preposition** ("F") and **Phrasal Verb** ("P") -> show the "Coming soon" snackbar.

## Vocabulary Manager
- Header: back, "Vocabulary", word count pill, a Search icon button that toggles the search bar, and a Notification bell icon button (with a numeric badge when pending database approval requests exist) that opens the **Approval Requests** sheet; sticky bottom "Add Word" button (or "Cancel" / "Delete (N)" in selection mode).
- Search bar (when toggled open from the top header) filters words in real time across the headword, part of speech (`WordDetails.partOfSpeech`), meanings (English/Hinglish), example sentence, and any synonym or antonym.
- Word cards remain collapsed by default (even right after adding or editing) and show the word with its initial badge, two color-coded count pills under the word (`N synonyms` in `quizzen.synonymContainer` / `quizzen.onSynonymContainer` and `N antonyms` in `quizzen.antonymContainer` / `quizzen.onAntonymContainer`, with no prefix icons), edit button, delete button, and expand/collapse arrow button. Tapping the arrow button opens or closes the card to reveal full **Word** (with **Part of Speech** on the right when present), **Meaning in English**, **Meaning in Hinglish**, and **Example sentence** for the main word and for each synonym and antonym (with dedicated emerald-teal Synonym and plum-violet Antonym section badges and bordered item cards).
- Hold-to-select (multi-word selection mode):
  - Long-pressing (holding) any word card enters selection mode with that word selected (and plays a heavy click haptic): each word card displays a selection checkbox, a top row provides **Select All** (scoped to the currently visible/filtered words) and a `%1$d selected` pill badge, and the sticky footer shows **Cancel** and **Delete (N)**.
  - While in selection mode, tapping any word card (or its checkbox) toggles its selection; deselecting the last selected word automatically exits selection mode. Pressing Back or **Cancel** also exits selection mode and clears the selection.
- Add / Edit modal bottom sheet uses JSON format:
  - **Add Words (JSON)**: accepts a JSON array (`[...]`) or single JSON object (`{...}`) to save multiple words (with `partOfSpeech` on the main word and every synonym and antonym) at once, with an **Insert Template** (`insert_json_template_button`, `VocabularyJsonFormat.formatBulkTemplate`) shortcut and an **Import JSON** (`import_json_file_button`, `VocabularyManagerViewModel.onImportJsonFile`) button that opens the system file picker (`ActivityResultContracts.OpenDocument`) to load a `.json` file from device storage (stripping any UTF-8 BOM and validating JSON syntax immediately).
  - **Edit Word (JSON)**: shows and edits only the JSON object for that specific word (with `partOfSpeech` included on the main word and every synonym/antonym, or on the individual relation word when focused via `VocabularyJsonFormat.formatWordDetails`), keeps the `CONFIRM` input pinned outside the scrollable JSON area right above the **Save Word** button so it stays visible even on words with many synonyms/antonyms, and keeps **Save Word** inactive until `CONFIRM` is typed in capital letters.
- Delete confirmation (single word via card trash icon or multiple words via **Delete (N)** in selection mode): opens a confirmation sheet that requires typing `DELETE` in capital letters before the word(s) are permanently removed from Cloud Firestore. Deleting needs internet: the words leave the list only after the server confirmed the delete (then a notice says so); without internet they stay and an error banner says nothing was deleted. Deleting all remaining words from the database also clears any saved unfinished quiz session (`VocabularySessionStore`).
- Stored in Cloud Firestore (`vocabulary` collection), one document per word using a deterministic document ID derived from `VocabularyRules.wordKey` (`VocabularyRules.documentIdForWord`), with a versioned schema (`schemaVersion` = 2; `category`; search/index fields `wordLowercase`, `synonymWords`, `antonymWords`; the word fields only inside nested `wordDetails`, `synonyms`, `antonyms`; `createdAt`/`updatedAt`). Version 1 documents (with duplicated top-level word fields) and external AI-inserted documents (even without `updatedAt`) are read cleanly and upgraded when saved.
- One entry per `(main word, part of speech)` & zero automatic merging: words and parts of speech are compared ignoring capitalization, extra or non-breaking spaces, hyphens versus spaces, surrounding punctuation, and curly versus straight apostrophes (`VocabularyRules.wordKey`, `VocabularyRules.wordIdentityKey`). Two entries (or synonyms/antonyms) with the same word but different parts of speech (`VocabularyRules.hasDifferentPartOfSpeech`, e.g. `"Present"` as `"Noun"` and `"Present"` as `"Verb"`) are not duplicates and are saved as separate entries with disambiguated document IDs (`_2`, `_3`, ... via `VocabularyRules.prepareEntriesForSave`) and excluded from AI duplicate-pair checks (`VocabularyDecisionRules.selectCandidatePairs`). Synonyms and antonyms are never compared with other entries' main words, so a word may be a main word and also a synonym of another word. Nothing is ever merged automatically without the user's explicit awareness and approval:
  - Repeated main words with the same part of speech inside the same JSON input (`VocabularyRules.findDuplicateMainWords`), duplicate/conflicting synonyms/antonyms with the same part of speech inside an entry (`VocabularyRules.findEntryRelationConflict`), or fields exceeding the `firestore.rules` length limits (`VocabularyRules.validateFieldLengths`: **120** characters for a word via `MAX_WORD_LENGTH`, **60** for a part of speech via `MAX_PART_OF_SPEECH_LENGTH`, **500** for English/Hinglish meanings via `MAX_MEANING_LENGTH`, and **600** for an example sentence via `MAX_EXAMPLE_LENGTH`) are rejected immediately with a clear inline validation error in the JSON editor.
  - Adding one or more words (single or bulk JSON) where any word already exists with the same part of speech never writes to the database automatically: it opens the **Approve & Merge** confirmation sheet (`MergeWordConfirmSheet`) detailing every change (each existing word to update, the exact new synonym words, the exact new antonym words, any blank part-of-speech/Hinglish/example fields to fill on the main word or existing synonyms/antonyms, plus any brand-new words in the batch) and requires typing `CONFIRM` in capital letters before saving. If nothing new would be added, the editor shows a validation error instead.
  - Background cloud sync (the snapshot listener of `FirestoreVocabularyRepository`) never merges entries automatically by word name. In **Approval Requests** (`DatabaseNotificationsSheet`), all duplicate, variant, conflict, and part-of-speech checks are performed exclusively by `GeminiDecisionClient` (`VocabularyDecisionRules` using `gemini-decision-1` `choice` and per-relation `noul` questions with zero output tokens on `Dispatchers.IO`). Verification uses **Option B incremental batching** (`VocabularyDecisionRules.buildRequestBatches`, up to **100** questions per batch via `BATCH_MAX_QUESTIONS` with **30** dedicated pair slots per batch via `MIN_PAIR_SLOTS_PER_BATCH`): only new or edited words (tracked by `VocabularyDecisionRules.entryContentDigest` and persisted on-device in `vocabulary_decision_state_v1.json`) are checked for main-word meaning, synonym/antonym conflicts, and the `partOfSpeech` of the main word and every synonym/antonym according to how each word is used in its listed meaning and example sentence (most recently updated words first), while each unverified word is compared against **all** database words (`unverified × all`, skipping pairs with different parts of speech via `VocabularyRules.hasDifferentPartOfSpeech`) for duplicate/variant pair checks so old verified words are never re-checked. When `gemini-decision-1` flags exact duplicates (`DecisionDuplicateKind.EXACT_DUPLICATE`), root/inflected forms (`DecisionDuplicateKind.ROOT_OR_INFLECTED`), spelling/typo variants (`DecisionDuplicateKind.SPELLING_OR_TYPO`), or synonym/antonym meaning or part-of-speech conflicts (`VocabularyDecisionFinding.MeaningConflict`), they appear under the header Notification bell icon (`DatabaseNotificationsSheet`) with a count badge (showing skeleton cards via `NotificationsSkeletonState` while an AI check is running). If an API rate limit (`HTTP 429` / `50K TPM`) or transient timeout occurs during verification (`DecisionPendingRetryException`), `VocabularyManagerViewModel` preserves any findings and verified words from completed batches, places verification into **Pending Status** (`isDecisionPendingRetry = true`, `decisionRetryRemainingSeconds = 60`), displays a live 60-second countdown in `DatabaseNotificationsSheet`, and automatically resumes only the remaining unverified batches after 1 minute. From the **Approval Requests** sheet, exact duplicates with nothing new to merge show only **Delete Duplicate** (opening `DeleteWordConfirmSheet` requiring `DELETE` so one word remains in the database), duplicates or variants with new items to merge show **Review & Merge** (opening `MergeWordConfirmSheet` requiring `CONFIRM` in capital letters) and **Delete Duplicate** (plus **Keep Both** for fuzzy AI matches), and meaning or part-of-speech conflicts display a highlight pill naming the exact conflicting word (`VocabularyDecisionFinding.MeaningConflict.conflictingTarget`, `VocabularyRules.findEntryRelationConflictTarget`) alongside the detailed explanation and **Edit JSON** (requiring `CONFIRM`), **Delete** (requiring `DELETE`), and **Dismiss**. Tapping **Edit JSON** on a synonym or antonym conflict card (`VocabularyManagerViewModel.onStartEditIssue`) opens `VocabularyEditorSheet` focused directly on that exact conflicting synonym/antonym word (`FocusedRelationEditorContext`, showing only its `WordDetails` JSON via `VocabularyJsonFormat.formatWordDetails`) with quick-action pills (`FocusedRelationAction`) to edit only that word, move it to the opposite list (synonyms <-> antonyms), remove it from the parent entry, or switch to the full parent entry JSON, before typing `CONFIRM` to save.
  - Editing a word keeps working even if older duplicates exist; renaming it onto another saved word with the same part of speech is refused. Only entries with no ID before saving (`VocabularyRules.prepareEntriesForSave`) are marked create-only, and if a renamed word or another part of speech of the same word already holds the base deterministic ID (`VocabularyRules.documentIdForWord`), saving assigns a disambiguated ID (`_2`, `_3`, ...) so the existing entry is never overwritten. `FirestoreVocabularyRepository.saveEntries` refuses (`DuplicateWordException`) any save that would still create a duplicate, including a word another device created a moment ago.
- **Internet is required to change the database.** Add, edit, merge and delete are sent to Cloud Firestore first (as transactions, in groups of at most 400 documents; Firestore's limit is 500) and only after the server confirmed them do the list and the local copy change. Without internet, or when the server does not answer within 20 seconds, nothing is saved and nothing is queued for later (`NoInternetException`). While a save or merge runs, the editor and the approval sheet stay open, the Save button is disabled and closing them is ignored; if it fails, the editor stays open with the typed JSON and shows why (inline error), and the approval sheet closes with the same error shown in the editor (or as a banner when no editor is open). If a multi-step **Approve & Merge** operation saves brand-new words first and then fails while merging existing words or deleting redundant duplicates, `VocabularyManagerViewModel.onConfirmMerge` reports which step(s) succeeded before the failure and removes the already-saved new words from the open editor JSON so retrying only runs the remaining merge. On full success the editor closes and a notice says what happened ("Added N new word(s) to the database.", "Word updated in the database.", the merge summary, "Deleted N word(s) from the database."). A merge reads the word as it is on the server inside its transaction, so two devices merging into the same word cannot erase each other's synonyms. Stored synonym/antonym items that cannot be read are skipped and logged.
- Reading and sync: the snapshot listener makes the list equal to the server list, so a word deleted on another device disappears here too. An empty answer that comes from Firestore's own cache (first start without internet) is ignored. The list is also written to a local JSON file (`vocabulary_cache_v1.json`, read-only copy for showing and quizzing the words offline); it is never a source of truth and never changes the database. Nothing is seeded: no starter or test words are ever written to the database (`VocabularyQuizRules.FALLBACK_ENTRIES` is only used as a last-resort distractor pool when a user has fewer than 4 words in the database).

## Vocabulary configuration, study & finite quiz
- **Configuration screen**:
  - Title "Vocabulary", subtitle "Configure your challenge", followed by compact status pills showing the current database word count (`vocabulary_db_count_badge`) and, when weak words exist, the number of prioritized weak words (`vocabulary_weak_words_badge`).
  - **Words to Attempt**: single-thumb slider from **1** to **25** (default **10**), showing the selected count in a pill badge.
  - **Number of Questions**: single-thumb slider from **1** to **100** (default **25**), showing the selected count in a pill badge.
  - **Quiz Mode** (multi-select, default both **Synonyms** and **Antonyms**; at least one always remains selected):
    - **Synonyms**: tests synonym relationships within the studied word clusters.
    - **Antonyms**: tests antonym relationships within the studied word clusters.
  - Timer field default **15** s. Start -> opens the **Study Words** screen.
- **Study Words screen**:
  - Header (`ScreenHeader`) shows back, "Study Words" (auto-resized via `FitText` so it always fits on a single line), the word count pill (`3 words`), and a **Shuffle** icon button (when words exist) that picks a new random set of words from the database (excluding the currently displayed words whenever more eligible words exist and scrolling the list back to the top); there is no extra introductory subtitle text below the header.
  - Selects the configured number of eligible vocabulary entries (`1..25`) from the user's database (`VocabularyQuizRules.selectStudyWords`), prioritizing locally saved mistake words (`VocabularyRepository.mistakeWords`) before filling remaining slots randomly.
  - When the database has no eligible words for the selected modes, `selectStudyWords` returns an empty list, `VocabularyStudyView` shows an empty-state card (`StudyEmptyStateCard`, `vocabulary_study_empty_state`) instructing the user to add or import words in **Database Manager > Vocabulary**, hides the **Shuffle** button, and disables **Start Quiz**.
  - Each studied word renders its **Main Word** header (`StudyMainWordStickyHeader`, `study_sticky_main_word_{index}`) as a `stickyHeader` in `LazyColumn` showing the word number badge, the **Main Word** on the left (auto-resized via `FitText` to fit on one line), its optional **Part of Speech** pill badge on the right (`study_word_pos_{index}`), and the Synonym/Antonym count pills (with no prefix icons). As the user scrolls down through the main word's **Meaning in English**, **Meaning in Hinglish**, **Example sentence**, and its visually differentiated **Synonyms** (emerald-teal `quizzen.synonymContainer` / `quizzen.synonym`) and **Antonyms** (plum-violet `quizzen.antonymContainer` / `quizzen.antonym`) cards, the **Main Word** sticky header remains pinned at the top of the list until the end of that word's synonyms and antonyms.
  - Sticky bottom **Start Quiz** button begins the finite quiz.
- **Finite Vocabulary Quiz & Algorithm (`VocabularyQuizEngine` & `VocabularyMistakeTracker`)**:
  - Separate from Math's infinite `PracticeSession`.
  - Top bar displays **Total** and **Attempted** question counts alongside the countdown timer chip.
  - Each studied entry forms a semantic cluster with a same-meaning set `S+ = { mainWord } U synonyms` and an opposite-meaning set `S- = antonyms`.
  - Every active word in the studied entries (main word, each synonym, and each antonym) is asked once as a question prompt, and there is no restriction that the main word must be the question or the correct option:
    - In **Synonyms** mode, words in `S+` are paired via a shuffled single-cycle derangement (`w_i -> w_{(i+1) mod k}`), so two synonyms directly test each other (`Synonym A -> Synonym B`) whenever `|S+| >= 3`.
    - In **Antonyms** mode, antonyms in `S-` pair across all members of `S+` (both synonyms and the main word), and in Antonyms-only mode words in `S+` also ask for antonyms in `S-`.
  - Studied main words (`clusters`) are ordered randomly, and all synonym and antonym questions belonging to a studied main word (`clusterId`) stay grouped together contiguously while **mixed in random order** (rather than asking all synonyms first and then all antonyms) before moving to the next studied main word. The quiz asks up to the configured **Number of Questions** (`1..100`, distributed evenly across the studied words so every word participates while staying within the available unique questions). `QuestionPromptCard` (`VocabularyActiveQuizContent`) does **not** display a "Main Word" header during active questions (so the answer is never leaked when the main word is the correct option); it shows the mode badge, the prompt word (auto-resized via `FitText`), and the prompt word's optional **Part of Speech** pill badge (`vocabulary_prompt_word_pos`).
  - Synonym questions and Antonym questions are visually differentiated on the prompt card: **Synonyms** use an emerald-teal pill badge (`SYNONYM`, `quizzen.synonymContainer` / `quizzen.onSynonymContainer` / `quizzen.synonym`) and card border tint, whereas **Antonyms** use a plum-violet pill badge (`ANTONYM`, `quizzen.antonymContainer` / `quizzen.onAntonymContainer` / `quizzen.antonym`) and card border tint.
  - Each question presents 4 multiple-choice options: 1 correct answer and 3 conflict-free distractors (excluding any other valid synonym/antonym for the prompt, prioritizing opposite-relation traps from the active cluster and words from the other studied entries).
  - **Local Mistake-Tracking & Mastery Deletion (`VocabularyMistakeTracker`)**:
    - Initial study selection waits until `VocabularyRepository.mistakeWordsLoaded` is true so saved weak words are always prioritized on first open.
    - Weak words are keyed by canonical entry ID (or `word::partOfSpeech` identity when ID is blank) via `VocabularyQuizRules.entryMistakeKeys` so homographs with different parts of speech never collide.
    - Whenever the user selects a wrong option (`Incorrect`) or runs out of time (`Timeout`) on any question belonging to a word cluster, that word is immediately saved locally in device storage (`VocabularyRepository.mistakeWords`).
    - A saved word is **automatically deleted** from local mistake storage when the user answers **every question generated for that word correctly** within a single quiz session (zero wrong selections and zero timeouts) **and** enough of its cluster was tested (all questions for clusters up to `VocabularyMistakeTracker.MIN_CAPPED_MASTERY_QUESTIONS`, or at least half of a larger cluster when capped by `questionCount`).
  - **MCQ Feedback, Meanings & Next Button**:
    - Bottom sheet feedback (`FeedbackSheet`) is **not** used on MCQ-based questions. Instead, feedback (`Correct!`, `Incorrect`, or `Time's Up`) is shown inline on the quiz card and option cards (with a green border/check badge on the correct option and a red border/cross badge on a wrong selected option).
    - While the countdown timer is running (before an option is selected or a timeout occurs), only the prompt word and the 4 option words are shown.
    - As soon as an answer is selected (correct or incorrect) or a timeout occurs, the meaning of the prompt word and the meanings (plus each option's `partOfSpeech` pill on the right side of the option word when present) of all 4 option words are revealed directly on the quiz screen.
    - The quiz does **not** advance automatically; a bottom **Next** button becomes enabled once the question is resolved (correct, incorrect, or timeout) and any active streak celebration finishes, and clicking **Next** advances to the next question and scrolls the active quiz back to the top.
    - **Streak Milestone Celebration (`StreakRules`, `StreakBadgeEmblem` & `StreakCelebrationOverlay`)**: answering `StreakRules.MILESTONE_STEP` (**5**) questions correctly in a row (`5`, `10`, `15`, ...) plays a **full-screen, 3-second** (`STREAK_CELEBRATION_DURATION_MS`) one-shot celebration with a radial backdrop wash, rotating sunburst rays, triple shockwave rings, a 2-wave confetti/star cascade, and a spring-animated custom vector rank badge (**10** badges in order via `StreakRules.Badge`: **Bronze** at 5, **Silver** at 10, **Gold** at 15, **Platinum** at 20, **Emerald** at 25, **Sapphire** at 30, **Ruby** at 35, **Amethyst** at 40, **Diamond** at 45, and **Legend** at 50) showing the badge rank (`Badge X of 10`), badge title, and exact streak count (*"5 in a row!"*, *"50 in a row!"*) while holding **Next** disabled for the 3 seconds so the next question's timer cannot start hidden behind the overlay. Beyond 50 (`55`, `60`, `65`, ...), the final **Legend** badge continues to appear while the streak number keeps updating. Any wrong answer or timeout resets the streak to `0`.
  - Once all questions are attempted and **Next** is clicked after the final question (`Attempted == Total`), the quiz finishes and shows the animated **Quiz Complete** summary screen (`VocabularyFinishedView`): a one-shot animated circular score progress ring (`ScoreProgressRing`) with animated count-ups for rounded score percentage, Total, Attempted, Correct, and Incorrect counts, a spring-scaled score headline badge, and a one-shot celebratory particle burst on scores of 80% or higher (all animations stop completely once finished). When any questions were missed or timed out (`VocabularyQuizUiState.missedQuestions`), a collapsible **Missed Questions** review card lists each missed item with its mode badge, prompt word, correct answer, and meaning, and a **Practice Missed Words** button (`VocabularyQuizViewModel.retryMissedWords`) starts a fresh quiz containing only those missed words alongside **Retry Quiz** and **Review Words**.

## Resume an unfinished quiz (Vocabulary only)
- Applies only to the **Language > Vocabulary** quiz. Math quizzes (Multiply, Tables, Powers & Roots, Alphabet, Fraction &
  Percentage) are never saved and always start from nothing.
- While a **Quiz** is running, its position is kept on the device (a small file in app storage): it is written when the quiz
  starts, when a question is answered, when the next question opens and when the screen goes to the background or is left. It
  holds the quiz setup (modes, words, timer), the studied words, every generated question with its options and meanings, the
  question the user is on, the score and which questions were answered. The countdown is **not** kept.
- The **Study Words** screen and the **Quiz Complete** screen keep nothing; finishing the quiz (Next after the last question)
  or deleting all words from the Vocabulary database deletes the saved quiz. Starting a new quiz replaces it when that quiz's first question appears.
- Leaving the app (swipe away, system kill, restart of the phone) or leaving the quiz with Back both leave the quiz resumable.
- **Where it shows**: at the top of the Vocabulary configuration screen, above **Words to Attempt**, a card "Resume your quiz"
  with "Question N of M" (N = the question the user will continue from), a primary **Resume** button and a **Discard** text
  button. The card appears only while a saved quiz exists and updates when the user comes back to the screen.
  - **Resume** opens the quiz straight at that question, with the same setup, the same questions in the same order, and the same
    score; nothing flashes before it (the screen stays empty for a few milliseconds while the file is read). The saved setup is
    used, not what the setup screen currently shows.
  - **Discard** deletes the saved quiz and hides the card; the user then starts a fresh quiz as usual.
  - **Start** with the card showing starts a new quiz from the current setup; the saved one stays until that new quiz begins.
- **Where the user continues**: a question that was still open restarts with a **full timer** (the time already used is not
  charged); a question that was already answered (result and meanings showing, Next not pressed yet) comes back answered with
  **Next** enabled. Per-word mistake tracking continues exactly where it stopped, so words saved or mastered at the end of the
  quiz are the same as if the app had never been closed.
- If the saved quiz cannot be read (damaged, from a different version), it is deleted and the user simply gets the normal setup /
  Study screen. A failed save never crashes the app.
- Clearing the app data removes the saved quiz.

## Multiply configuration
- "Digits in 1st Number": 2/3/4/5 (default **3**). "Digits in 2nd Number": 2/3/4/5 (default **2**). Single choice each.
- Timer field default **20** s. Start -> Practice.

## Tables configuration
- Grid of 2..31 (4 columns), multi-select, nothing selected initially. "Select All" selects all; if all selected it clears.
  The button appears active whenever all numbers are selected (also when selected manually).
- Timer field default **10** s. Start is disabled until at least one number is selected.

## Remembered settings
- Each quiz's setup screen opens with the settings it was **last started with**: Multiply (digits of both numbers, timer), Tables
  (selected numbers, timer), Powers & Roots (selected types, both ranges, timer), Alphabet (challenge type, letter range, timer),
  Fraction & Percentage (fraction range, what to answer in, timer), and Vocabulary (selected modes, word count, question count, timer).
  They are saved when **Start** is pressed.
- The first time (nothing saved) the screen shows its defaults. A saved value that is no longer valid is repaired or replaced by the
  default, and a damaged or unreadable save is treated as "nothing saved"; saving can never crash the app.
- The values are read a few milliseconds after the screen opens; the screen fades in once they are ready so the defaults never
  flash. Anything the user touches before that is not overwritten.
- Saved settings are only about the setup screen. A quiz itself always starts fresh, and nothing about answers is saved. The only
  exception is an unfinished **Vocabulary** quiz, which can be resumed (see "Resume an unfinished quiz" below); Math quizzes never.
- Stored on the device only (a few bytes in the app's preferences file); clearing the app data resets everything.

## Alphabet configuration
- Title "Alphabet", subtitle "Configure your challenge".
- **Letter Range**: one card like the Powers & Roots range cards. At the top-left a pill with the letter picked by the left thumb,
  at the top-right a pill with the letter picked by the right thumb. Under them one two-thumb slider that moves both ends (whole
  letters, A to Z; the small letters under the slider show its ends). Starts at **A to Z**; one letter (From = To) is allowed.
  Releasing a thumb gives a light tick (haptics).
- **Challenge Type** (multi select, default **Find Position**; at least one always stays chosen, the last one cannot be switched off):
  three full-width cards with a check box and just the name:
  - **Find Position**: a letter is shown, type its place in the alphabet (A = 1 ... Z = 26).
  - **Find Letter**: a place is shown, type the letter.
  - **Reverse Letter**: a letter is shown, type the letter at the same place counted from the other end
  - With several chosen, every letter of the range is asked once in each chosen way, all mixed together in the same shuffled
    rounds (so 3 letters with two ways give 6 questions). Each letter-and-way is its own question.
    (A <-> Z, B <-> Y, ... M <-> N).
- Timer field default **10** s (a blank or 0 uses the default, like every other screen). Start is always enabled.
- Start -> Practice. Every letter of the range is one question; the quiz uses shuffled rounds and mistake repeats like Tables and
  Powers & Roots (see Practice).

## Fraction & Percentage configuration
- Title "Fraction & Percentage", subtitle "Configure your challenge".
- **Fraction Range**: a two-thumb slider (same card as the Alphabet letter range: a pill for each thumb, `1/2` and `1/50` under the
  ends of the track). The slider works on the places of the 24 fractions of the chart (1 = 1/2 ... 24 = 1/50, in the order below), so
  every position is a fraction that is asked; default is the whole chart. One place (both thumbs together) is allowed: then that one
  fraction is asked again and again.
- **Answer in** (multi select, default **Fraction**; at least one always stays chosen, the last one cannot be switched off): two
  full-width cards (the same check-box cards as the Alphabet challenge types) with just the name:
  - **Fraction**: a percentage is shown, type the fraction (`33⅓%` or `33.33%` -> `1/3`).
  - **Percentage**: a fraction is shown, type the percentage (`1/3` -> `33.33`; the `%` sign is added by the app).
- With both chosen, every fraction of the range is asked once in each direction, all mixed together in the same shuffled rounds
  (so 24 fractions give 48 questions). The two directions of one fraction are different questions.
- Timer field default **10** s. Start is always enabled. Start -> Practice.
- The questions are exactly the 24 unit fractions of the reference chart, nothing else: 1/2 to 1/20, then 1/24, 1/25, 1/30, 1/40
  and 1/50. Every one is one question; the quiz uses shuffled rounds and mistake repeats like Tables (see Practice). The
  percentage of each is `100 / n` (50, 33⅓, 25, 20, 16⅔, 14²⁄₇, 12½, 11⅑, 10, 9¹⁄₁₁, 8⅓, 7⁹⁄₁₃, 7¹⁄₇, 6⅔, 6¼, 5¹⁵⁄₁₇, 5⁵⁄₉,
  5⁵⁄₁₉, 5, 4⅙, 4, 3⅓, 2½, 2); it is worked out from n, never typed in a table.

## Powers & Roots configuration
- **Practice Types** (multi-select, none selected initially): Squares (x²), Cubes (x³), Square Roots (√x), Cube Roots (³√x).
- **Number Range**: two sliders, each with two thumbs (smallest and largest number), snapping to whole numbers:
  - **Squares & roots**: 1 to **30** (starts at 2–30), used for squares and square roots.
  - **Cubes & roots**: 1 to **20** (starts at 2–20), used for cubes and cube roots.
  The end of each slider is its limit, so the limits need no extra text; the two small numbers under a slider show its ends and a
  pill on the card shows the range in use (e.g. `2–30`). A range is over the base number (for a root question it is the answer):
  2–30 gives squares up to 30² = 900 and square roots of up to 900; 2–20 gives cubes up to 20³ = 8000.
- A slider whose kinds are not selected (while other kinds are) is faded and cannot be dragged; when nothing is selected both can
  be set. Releasing a thumb gives a light tick (haptics).
- Because the sliders can only produce valid ranges, the only rule for **Start** is that at least one type is selected.
- Timer field default **10** s (same rules as below).

## Timer field (all config screens)
- Digits only; value > 180 is replaced by 180 and the field shows a red outline plus "Max allowed timer is 180 seconds"
  for 2.5 s. Blank is allowed while typing and becomes the screen default when focus is lost.
- On Start, a value of 0 or blank runs with the screen default.

## Practice
- Question forms: `a × b` (Multiply, Tables), `n` with a raised 2 or 3 (Squares, Cubes), `√N` (Square Roots) and a raised 3 in front
  of the root sign (Cube Roots, `³√N`). The operator part is tinted with the primary colour; the answer is always a whole number.
  Screen readers say "17 squared", "cube root of 1728", etc.
- Header: back, "Practice", countdown chip "{n}s" (pulses at <= 5).
- Question "a x b" with the operator in the primary color; text shrinks to fit one line.
  - Multiply: operands random with the chosen digit counts (1 digit -> 1..9; n digits -> 10^(n-1)..10^n-1).
  - Tables: `table number x random(1..10)`, table number drawn uniformly from the selection.
- Numeric answer field (digits only, max 12) and **Check** button; the IME "Done" action also checks. The field is focused
  automatically for each question.
- Alphabet quizzes: the question is the letter (`C`) or the number (`3`) in the text colour. When the answer is a letter the field
  opens the normal keyboard in capitals, takes **one letter** (typing another replaces it) and is checked whatever the case; when it is a
  number it is the usual number pad. The placeholder says what to type ("Position", "Letter", "Opposite letter"). The feedback sheet
  shows the correct answer as the number or the capital letter. Screen readers say "letter C", "position 3", "opposite of letter C".
- Fraction & Percentage quizzes:
  - **Answer in Fraction**: the question is the percentage in the primary-tinted `%` style. A percentage with a fractional part is
    written either as a mixed number (`33 1/3%` drawn with a stacked fraction: the whole part, then a half-size numerator over a
    bar over a half-size denominator centred on it, then `%`) or as a decimal
    rounded to two places (`33.33%`); one of the two is picked at random each time a question is asked (whole percentages such
    as `25%` have only one writing). A question that comes back after a mistake is written exactly as it was missed. The
    answer is typed as digits around one slash (`1/3`, at most 7 characters, never starting with the slash). The number
    keyboard has no slash, so a round **"/" key** sits at the end of the answer field: it adds one slash after the digits typed (it
    does nothing on an empty field or when there already is a slash) and gives a light tick. A fraction with the same value
    counts (`2/6` for 1/3).
  - **Answer in Percentage**: the question is the fraction, drawn stacked as well: `1` over a bar over `25`, as big as the other
    questions' text. The decimal keyboard is used and the answer is
    typed as digits with at most one decimal point (a decimal comma counts as the point; at most 7 characters). The **`%` sign is
    shown automatically** after what is typed (`33.33` reads `33.33%`) and is never typed. A percentage that ends (25, 12.5,
    6.25, 2.5, 2) must be exact; one with endless decimals (33⅓, 16⅔, 5¹⁵⁄₁₇ ...) is right when within 0.05 of the exact
    value, so it may be rounded or cut (`33.3`, `33.33` and `33.34` are all right for 33⅓; `33` and `33.5` are not).
  - The keyboard, the placeholder, the "/" key and the automatic `%` sign follow each question, so with both directions chosen they
    change from question to question.
  - The answer field always keeps its cursor at the end of what is typed, also after the "/" key (typing `1`, `/`, `3` gives `1/3`).
  - The placeholder says "Fraction" or "Percentage". The feedback sheet shows the correct answer as `1/3`, or for a percentage in
    both writings (`33 1/3% ≈ 33.33%`, `6 1/4% = 6.25%`, `25%`). Screen readers say "1 over 25", "33 and 1 over 3 percent",
    "33.33 percent".
- **Order of questions** (Tables, Powers & Roots, Alphabet and Fraction & Percentage, the quizzes with a limited set of questions): when the quiz starts, all its
  questions are shuffled into a random order and asked one after the other, so **every question comes up once before any
  question comes up again**. When the last one has been asked they are shuffled again (a new random order) and the quiz goes on,
  forever. Every quiz gets its own shuffle; nothing is saved. Multiply has far too many possible questions to list, so each
  of its questions is drawn at random.
- **Mistakes and slow answers come back** (same four quizzes; Multiply is not affected). They are added into that order. Inside one quiz:
  - A wrong answer or a time-up brings the same question back **3 times** among the next 10 questions.
  - A correct but **slow** answer brings it back **2 times** among the next 10. Slow = answered with **less than 40% of the
    timer left** on the countdown (timer 10 s: 4 s left is still fast, 3 s left is slow; a wrong answer is never "slow", it is wrong).
  - Comebacks never land on the very next question, never two in a row, and the next question of the round is never the same as
    the one just before it (a question that would be is moved a little later in the round). Where exactly they land in the 10 is random each time.
  - A comeback answered wrong/slow again starts a fresh set (3 or 2) counted from that moment; the old unused ones are dropped.
    A comeback answered fast keeps the rest of its set.
  - If many mistakes pile up so the 10 slots are full, the leftover comebacks come right after the window instead of being lost.
  - Nothing is saved: closing the quiz, or starting a new one, begins from nothing. (This is about the Math quizzes; the
    Vocabulary quiz can be resumed, see its own section.)
  - With only one or two possible questions (e.g. Min = Max) there is nothing else to show, so immediate repeats cannot be avoided.
- Check: exact integer match = **Correct**; otherwise (including blank) = **Incorrect** (shake).
- Countdown reaching 0 = **Time's Up**.
- Feedback bottom sheet (green / red / orange) for 1.7 s, then slides away; 250 ms later a new question appears, the input is
  cleared, and the timer restarts from the configured value. Input and Check are ignored while the sheet is showing.
- **Streak Milestone Celebration (`StreakRules`, `StreakBadgeEmblem` & `StreakCelebrationOverlay`)**: answering `StreakRules.MILESTONE_STEP` (**5**) questions correctly in a row (`5`, `10`, `15`, ...) plays the same **full-screen, 3-second** (`STREAK_CELEBRATION_MS`) 10-badge celebration (Bronze through Legend, repeating Legend with the updated streak count beyond 50) over the screen while holding the next question's timer until the celebration finishes (without firing on every single correct answer). Any wrong answer or timeout resets the streak to `0`.
- Haptics (only when the Haptic Feedback setting is on; played through the device vibrator, so it does not depend on the
  system "touch vibration" toggle). Every kind of action has its own sensation:

  | Effect | When |
  |---|---|
  | Tick (light) | selecting a digit option, table number, challenge type or what to answer in, pressing the "/" key, range slider released, switching Math/Language, turning haptics off, "Coming soon" taps |
  | Click (medium) | opening Multiply/Tables/Powers & Roots/Alphabet/Fraction & Percentage/Settings, Back, Select All, turning haptics on, Discard (saved Vocabulary quiz) |
  | Heavy click (firm thump) | Start, Resume (saved Vocabulary quiz) |
  | Success (two rising taps) | correct answer |
  | Error (three hard buzzes) | wrong answer |
  | Timeout (one long softer buzz) | time is up |
- Timer pauses while the app is in the background and resumes without resetting.
- Back returns to the configuration screen.

## Design decisions

Choices that are deliberate, most of them at the owner's request. Keep this list current when one of them changes.

| Decision | Why |
|---|---|
| Tables Practice asks `n x 1..10` for the chosen tables | The `1..10` multiplier range is an assumption; change `PracticeConfig.Tables.MULTIPLIER_MAX`. |
| Tables Start is disabled until a number is chosen | Cannot generate a question from an empty selection. |
| A blank timer field is restored to the screen default on blur; 0 or blank starts with the screen default (20 or 10) | Cleared-field editing works. |
| Header and tab control stay fixed; each tab page scrolls | Native pager pattern. |
| Placeholder tiles show a snackbar "Coming soon" | Feedback for taps. |
| Input is locked while the feedback sheet is visible | Avoids stacked timeouts. |
| The timer pauses while the app is in the background | Native lifecycle. |
| The answer field is auto-focused | Faster practice with the number pad. |
| No logo in the Home header | Owner request. |
| The launcher icon and the full-screen splash use the owner's `quizzen-logo.svg` | Owner request. |
| Hero titles shrink to stay on one line ("Vocabulary" never wraps on narrow phones) | Owner request; same idea as the practice question auto-fit. |
| The "Time's Up" sheet is sky blue (`#2E9FE0`) | Owner request; orange was too close to the app's primary colour. |
| Powers & Roots: types, two range sliders and a timer, in the Quizzen style (modelled on a screenshot of another app) | Owner request. That app's "0 disables the timer" is **not** adopted: 0 or blank uses the default like every other screen. |
| Alphabet Reasoning: letter range, three challenge types and a timer, in the Quizzen style (modelled on a screenshot of another app) | Owner request. That app's two letter pickers became two pills (the picked letters) above one range slider (like the Powers & Roots ranges), the challenge cards use the app's primary selection style and show only the name, and "0 disables the timer" is **not** adopted. |
| Fraction & Percentage: answer in Fraction or Percentage and a timer, asking only the 24 unit fractions of the owner's chart | Owner request. Fraction answers get an on-screen "/" key because the number keyboard has none; percentage answers get an automatic `%` sign and may be typed with a decimal point. |
| Light theme only | Product decision: the app is light-only. |
| The haptics toggle is persisted (DataStore) and applied everywhere, with a distinct effect per action | Real feature. |
| Option chips have 18 dp side padding | With 22 dp, three chips need 326 dp but only 324 dp are available on 360 dp phones, so "4 Digits" wrapped and left a gap on the right. With 18 dp, three chips fit per row (2, 3, 4 Digits, then 5 Digits). |
| Streak celebration (`StreakCelebrationOverlay` + `StreakBadgeEmblem`) uses 10 progressive metallic/gemstone badges (Bronze, Silver, Gold, Platinum, Emerald, Sapphire, Ruby, Amethyst, Diamond, Legend) instead of a sparkle icon and phrases; after 50, Legend repeats with the updated streak count | Owner request; rewards longer streaks with clear collectible rank progression distinct from the app's primary orange palette. |
