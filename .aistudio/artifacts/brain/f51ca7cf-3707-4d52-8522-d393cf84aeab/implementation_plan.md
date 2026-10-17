# Database Manager & Vocabulary Management

Enable the **Database Manager** from the Settings sheet, displaying all three Language categories (**Vocabulary**, **Fixed Preposition**, and **Phrasal Verb**) in the warm, expressive Quizzen visual style, and provide a complete **Vocabulary Database Manager** for adding, editing, searching, and deleting vocabulary words along with their multiple synonyms and antonyms synced to Cloud Firestore with offline caching.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following product and UX choices were confirmed and guide this implementation:

- **Confirmed Decision 1 — Cloud Firestore + Offline Caching**: Vocabulary entries (along with their nested synonyms and antonyms) are stored in Cloud Firestore (`quizzen-463d8`) with Firestore's local offline persistence enabled so reads and writes work seamlessly both online and offline.
- **Confirmed Decision 2 — Multiple Synonyms & Antonyms per Word**: Each primary vocabulary word supports zero, one, or many synonyms and zero, one, or many antonyms. Every main word, synonym, and antonym carries the exact same four structured fields: **Word**, **Meaning in English**, **Meaning in Hinglish**, and **Example Sentence**.
- **Confirmed Decision 3 — Expandable Word Cards + Search & Add/Edit Sheet**: The Vocabulary screen provides a real-time search bar, expandable word cards that reveal meanings, example sentences, and grouped Synonym/Antonym sub-cards, and a rich modal sheet for creating or editing a word and its synonyms/antonyms.

---

### 1. Overview & Core Concept

- **What It Does**:
  1. **Database Manager Hub**: Accessible from **Settings → Database Manager**, displaying the three Language categories matching the Home Language tab: **Vocabulary** (hero card, enabled), **Fixed Preposition** (tile, shows "Coming soon" snackbar), and **Phrasal Verb** (tile, shows "Coming soon" snackbar).
  2. **Vocabulary Manager**: Opens when **Vocabulary** is tapped in the Database Manager. Lets the user browse, search, expand, add, edit, and delete vocabulary words and their associated synonyms and antonyms.
- **Target Audience / Persona**: Learners preparing for competitive exams and language mastery who want to curate rich bilingual (English + Hinglish) vocabulary entries with contextual example sentences and structured synonym/antonym families.
- **Key Value**: Keeps every word's English meaning, Hinglish explanation, example usage, and complete synonym/antonym network organized in one place while maintaining 100% visual, typographic, haptic, and motion consistency with Quizzen.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Opening the Database Manager**:
     - Tap the **Settings** gear icon on the Home header → tap **Database Manager**.
     - The settings sheet dismisses and navigates with Quizzen's signature 380 ms fade + 16 dp rise transition to the **Database Manager** screen.
     - Displays `ScreenHeader("Database Manager")` and the three Language options:
       - **Vocabulary** (`HeroCard` with `"V"` badge) → navigates to the **Vocabulary** manager screen.
       - **Fixed Preposition** (`FeatureTile` with `"F"` badge, `BlobA`) → triggers light tick haptic and `"Coming soon"` snackbar.
       - **Phrasal Verb** (`FeatureTile` with `"P"` badge, `BlobB`) → triggers light tick haptic and `"Coming soon"` snackbar.
  2. **Browsing & Searching Vocabulary**:
     - Header shows `Back` button, `"Vocabulary"` title, word count pill badge (e.g., `12`), and a `+` (`Add`) icon button.
     - A rounded **Search Bar** (`surfaceContainer`, 24 dp radius, primary focus ring) filters words in real time across the headword, meanings (English/Hinglish), or any synonym/antonym word.
     - **Expandable Word Cards**:
       - **Collapsed State**: Shows the bold headword, its English & Hinglish meanings in a concise two-line summary, count pills for Synonyms (`3 Syn`) and Antonyms (`2 Ant`), quick `Edit` and `Delete` action buttons, and a rotating chevron.
       - **Expanded State**: Smoothly reveals the headword's full **Meaning in English**, **Meaning in Hinglish**, and **Example Sentence**, followed by clearly organized **Synonyms** and **Antonyms** sections where each item displays its own **Word**, **Meaning in English**, **Meaning in Hinglish**, and **Example Sentence**.
  3. **Adding & Editing a Word (with Synonyms & Antonyms)**:
     - Tapping `+` (or the Sticky Bottom `"Add Word"` CTA when empty, or the `Edit` icon on any card) opens the **Add / Edit Word Sheet**.
     - **Primary Word Section**: Styled input fields for:
       - **Word** *(required)*
       - **Meaning in English** *(required)*
       - **Meaning in Hinglish**
       - **Example Sentence**
     - **Synonyms Section**: Header with `"Synonyms"` title and an `"+ Add Synonym"` pill button. Each added synonym renders in a collapsible/editable sub-card with the same four fields (**Word**, **Meaning in English**, **Meaning in Hinglish**, **Example Sentence**) and a remove button.
     - **Antonyms Section**: Header with `"Antonyms"` title and an `"+ Add Antonym"` pill button. Each added antonym renders in an editable sub-card with the same four fields (**Word**, **Meaning in English**, **Meaning in Hinglish**, **Example Sentence**) and a remove button.
     - **Save Action**: Validates that the primary word and any added synonyms/antonyms have a non-empty word field, triggers a confirmation haptic, persists to Firestore, and closes the sheet.
  4. **Deleting a Word**:
     - Tapping the `Delete` icon on a word card opens a clean confirmation sheet/dialog (`"Delete Word?"`) to prevent accidental removal, with `Cancel` and `Delete` actions.

- **Visual Identity & Theme (Strict Quizzen Consistency)**:
  - **Light-Only Palette**: Uses `MaterialTheme.colorScheme` and `MaterialTheme.quizzen` exclusively (`surface` `#FFFAF6`, `surfaceContainer` `#FBEEE3`, `surfaceContainerHigh` `#F5E1CE`, `primary` `#EA7A31`, `primaryContainer` `#FCE1CB`, `onPrimaryContainer` `#7A3B0E`, `onSurface` `#211710`, `onSurfaceVariant` `#5C4A3B`, `error` `#E14C4C`).
  - **Typography**: Plus Jakarta Sans static weights (`headlineLarge` for screen headers, `titleLarge` / `titleMedium` for headwords and section titles, `bodyMedium` for meanings and examples, `labelSmall` uppercase for field labels like `ENGLISH`, `HINGLISH`, `EXAMPLE`, `SYNONYMS`, `ANTONYMS`).
  - **Shadows & Shapes**: `QuizzenScreen` wrapper (max width `440.dp` + soft glow discs), `Modifier.cssShadow(...)` for cards and buttons, `bouncyClickable` / `pressScale` on all interactive cards and buttons.
  - **Haptics**: Semantic `Haptics` effects (`click()` on navigation and opening sheets, `tick()` on expanding cards/toggling sections/coming-soon taps, `heavy()` on saving a word, `error()` on deleting).

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Embedded Synonyms & Antonyms inside each Vocabulary Document**
  - *Chosen Approach*: Store each vocabulary entry as a single Firestore document in the `vocabulary` collection containing the primary word's four fields plus `synonyms: List<LexicalEntry>` and `antonyms: List<LexicalEntry>`.
  - *Why*: Fetching, displaying, editing, and deleting a word along with all its synonyms and antonyms happens atomically in a single document read/write, with instant offline caching and zero orphaned sub-documents.
  - *Alternatives Considered*: Separate collections for synonyms/antonyms with foreign keys—rejected because it requires multi-query joins and complicates offline atomic edits.
- **Decision 2: Unified `LexicalEntry` Structure for Headwords, Synonyms, and Antonyms**
  - *Chosen Approach*: Use a consistent 4-field domain model (`word`, `meaningEnglish`, `meaningHinglish`, `exampleSentence`) for the headword, every synonym, and every antonym.
  - *Why*: Directly fulfills the requirement that the exact same four details are captured and displayed for each word, its synonyms, and its antonyms, and allows reusable form and display components.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌──────────────────────────────────────────────────────────────────────────┐
│                              UI Layer (Compose)                          │
│                                                                          │
│  ┌─────────────────────────┐       ┌──────────────────────────────────┐  │
│  │  DatabaseManagerScreen  │──────▶│      VocabularyManagerScreen     │  │
│  │  • HeroCard: Vocabulary │       │  • ScreenHeader + Word Count     │  │
│  │  • Tile: Fixed Prep     │       │  • Search Field                  │  │
│  │  • Tile: Phrasal Verb   │       │  • Expandable Vocabulary Cards   │  │
│  └─────────────────────────┘       │    ├─ Word / English / Hinglish  │  │
│                                    │    ├─ Example Sentence           │  │
│                                    │    ├─ Synonyms List (4 fields)   │  │
│                                    │    └─ Antonyms List (4 fields)   │  │
│                                    │  • Add / Edit Word Modal Sheet   │  │
│                                    │  • Delete Confirmation Sheet     │  │
│                                    └─────────────────┬────────────────┘  │
└──────────────────────────────────────────────────────┼───────────────────┘
                                                       │ StateFlow<UiState>
                                                       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                          ViewModel & Domain Layer                        │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                     VocabularyManagerViewModel                     │  │
│  │  • Search query filtering & sorting                                │  │
│  │  • Expanded card IDs state                                         │  │
│  │  • Editor form state (Word + dynamic Synonyms & Antonyms lists)    │  │
│  └─────────────────────────────────┬──────────────────────────────────┘  │
│                                    │                                     │
│  ┌─────────────────────────────────▼──────────────────────────────────┐  │
│  │                   Domain Models (Pure Kotlin)                      │  │
│  │  • VocabularyEntry(id, details: WordDetails,                       │  │
│  │                    synonyms: List<WordDetails>,                    │  │
│  │                    antonyms: List<WordDetails>, updatedAt)         │  │
│  │  • WordDetails(word, meaningEnglish, meaningHinglish, example)     │  │
│  └─────────────────────────────────┬──────────────────────────────────┘  │
└────────────────────────────────────┼─────────────────────────────────────┘
                                     │
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                              Data Layer                                  │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                       VocabularyRepository                         │  │
│  │  • Cloud Firestore ("vocabulary" collection) + Offline Persistence │  │
│  │  • Real-time SnapshotListener Flow<List<VocabularyEntry>>          │  │
│  │  • CRUD: addWord / updateWord / deleteWord                         │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
```

- **Data Model & State**:
  - `WordDetails` (pure Kotlin in `domain`):
    - `word: String`
    - `meaningEnglish: String`
    - `meaningHinglish: String`
    - `exampleSentence: String`
  - `VocabularyEntry` (pure Kotlin in `domain`):
    - `id: String`
    - `wordDetails: WordDetails`
    - `synonyms: List<WordDetails>`
    - `antonyms: List<WordDetails>`
    - `updatedAt: Long`
- **Interactive Component & State Mapping**:
  - **Settings → Database Manager Navigation**: Selecting `Database Manager` in `SettingsSheet` closes the sheet and navigates to `Routes.DATABASE_MANAGER`.
  - **Database Manager Selection**: Tapping `Vocabulary` navigates to `Routes.VOCABULARY_MANAGER`; tapping `Fixed Preposition` or `Phrasal Verb` shows the `"Coming soon"` snackbar.
  - **Search & Filter**: Typing in the search box updates `searchQuery` in `VocabularyManagerViewModel`, filtering the displayed list by matching `word`, `meaningEnglish`, `meaningHinglish`, or any synonym/antonym `word`.
  - **Expand / Collapse Card**: Tapping a word card toggles its ID in `expandedIds: Set<String>`, animating the detail section and synonym/antonym sub-cards into view.
  - **Add / Edit Editor Sheet**: Maintains draft state for the primary `WordDetails` and mutable lists of `synonyms` and `antonyms`. Users can add new blank synonym/antonym cards, edit any of the four fields on each, remove individual synonyms/antonyms, and save the complete entry.
  - **Delete Flow**: Tapping delete sets `pendingDelete: VocabularyEntry?`, showing a confirmation sheet that removes the document from Firestore on confirm.
