# Behavior specification

Normative description of the app. "Web" = the prototype in `docs/reference/quizzen-web-reference.html`.

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
- **Language** page: hero "Vocabulary"; tiles "Fixed Preposition", "Phrasal Verb" (all placeholders).
- Placeholders show a "Coming soon" snackbar.

## Settings sheet (modal bottom sheet)
- **Haptic Feedback** switch, default on, persisted. Toggling on gives a confirmation tick.
- **Database Manager** row (placeholder: closes the sheet, shows "Coming soon").
- Dismiss by scrim tap, drag down, or back.

## Multiply configuration
- "Digits in 1st Number": 2/3/4/5 (default **3**). "Digits in 2nd Number": 2/3/4/5 (default **2**). Single choice each.
- Timer field default **20** s. Start -> Practice.

## Tables configuration
- Grid of 2..31 (4 columns), multi-select, nothing selected initially. "Select All" selects all; if all selected it clears.
  The button appears active whenever all numbers are selected (also when selected manually).
- Timer field default **10** s. Start is disabled until at least one number is selected.

## Remembered settings
- Each quiz's setup screen opens with the settings it was **last started with**: Multiply (digits of both numbers, timer), Tables
  (selected numbers, timer), Powers & Roots (selected types, both ranges, timer), Alphabet (challenge type, letter range, timer)
  and Fraction & Percentage (fraction range, what to answer in, timer).
  They are saved when **Start** is pressed.
- The first time (nothing saved) the screen shows its defaults. A saved value that is no longer valid is repaired or replaced by the
  default, and a damaged or unreadable save is treated as "nothing saved"; saving can never crash the app.
- The values are read a few milliseconds after the screen opens; the screen fades in once they are ready so the defaults never
  flash. Anything the user touches before that is not overwritten.
- Saved settings are only about the setup screen. A quiz itself always starts fresh, and nothing about answers is saved.
- Stored on the device only (a few bytes in the app's preferences file); clearing the app data resets everything.

## Alphabet configuration
- Title "Alphabet", subtitle "Configure your challenge".
- **Letter Range**: one card like the Powers & Roots range cards. At the top-left a pill with the letter picked by the left thumb,
  at the top-right a pill with the letter picked by the right thumb. Under them one two-thumb slider that moves both ends (whole
  letters, A to Z; the small letters under the slider show its ends). Starts at **A to Z**; one letter (From = To) is allowed.
  Releasing a thumb gives a light tick (haptics).
- **Challenge Type** (single select, default **Find Position**): three full-width cards with a radio mark and just the name:
  - **Find Position**: a letter is shown, type its place in the alphabet (A = 1 ... Z = 26).
  - **Find Letter**: a place is shown, type the letter.
  - **Reverse Letter**: a letter is shown, type the letter at the same place counted from the other end
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
  full-width cards (the Alphabet challenge cards, with a check box instead of a radio mark) with just the name:
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
  - Nothing is saved: closing the quiz, or starting a new one, begins from nothing.
  - With only one or two possible questions (e.g. Min = Max) there is nothing else to show, so immediate repeats cannot be avoided.
- Check: exact integer match = **Correct**; otherwise (including blank) = **Incorrect** (shake).
- Countdown reaching 0 = **Time's Up**.
- Feedback bottom sheet (green / red / orange) for 1.7 s, then slides away; 250 ms later a new question appears, the input is
  cleared, and the timer restarts from the configured value. Input and Check are ignored while the sheet is showing.
- Haptics (only when the Haptic Feedback setting is on; played through the device vibrator, so it does not depend on the
  system "touch vibration" toggle). Every kind of action has its own sensation:

  | Effect | When |
  |---|---|
  | Tick (light) | selecting a digit option, table number, challenge type or what to answer in, pressing the "/" key, range slider released, switching Math/Language, turning haptics off, "Coming soon" taps |
  | Click (medium) | opening Multiply/Tables/Powers & Roots/Alphabet/Fraction & Percentage/Settings, Back, Select All, turning haptics on |
  | Heavy click (firm thump) | Start |
  | Success (two rising taps) | correct answer |
  | Error (three hard buzzes) | wrong answer |
  | Timeout (one long softer buzz) | time is up |
- Timer pauses while the app is in the background and resumes without resetting.
- Back returns to the configuration screen.

## Deliberate deviations from the web prototype

| Web | Native | Why |
|---|---|---|
| Tables "Start" opened a Multiply-style practice using Multiply's chips | Real tables practice (`n x 1..10`) | The web wiring was unfinished. The `1..10` multiplier range is an assumption; change `PracticeConfig.Tables.MULTIPLIER_MAX`. |
| Tables Start always enabled | Disabled until a number is chosen | Cannot generate a question from an empty selection. |
| Back from Practice always went to Multiply | Back goes to the originating screen | Correct with two entry points. |
| Timer field snapped to `0` when cleared; `0` fell back to 20 s | Blank restored to the screen default on blur; 0/blank -> screen default (20 or 10) | Cleared-field editing works; behavior on Start is equivalent. |
| Whole home page scrolled including header | Header and tab control fixed; each tab page scrolls | Native pager pattern; identical on normal screen sizes. |
| Placeholder tiles did nothing | Snackbar "Coming soon" | Feedback for taps. |
| Feedback could be re-triggered while visible | Input locked while visible | Avoids stacked timeouts (web bug). |
| Timer kept running in a hidden tab | Pauses in background | Native lifecycle. |
| No auto-focus | Answer field auto-focused | Faster practice with the number pad. |
| Header showed a gradient blob logo with a "Q" | No logo in the header | Owner request. |
| No splash / launcher icon design | Launcher icon and full-screen splash use the owner's `quizzen-logo.svg` | Owner request. |
| Hero titles could wrap ("Vocabula/ry" on narrow phones) | Title shrinks to stay on one line | Owner request; same idea as the practice question auto-fit. |
| "Time's Up" sheet was orange | Sky blue (`#2E9FE0`) | Owner request; orange was too close to the app's primary colour. |
| Powers & Roots was a placeholder | Implemented (types, two range sliders, timer) in the Quizzen style, from a reference screenshot of another app | Owner request. The reference's "0 disables the timer" is **not** adopted: 0 or blank uses the default like every other screen. |
| Alphabet Reasoning was a placeholder | Implemented (letter range, three challenge types, timer) in the Quizzen style, from a reference screenshot of another app | Owner request. The reference's two letter pickers became two pills (the picked letters) above one range slider (like the Powers & Roots ranges), the challenge cards use the app's primary selection style and show only the name, and the reference's "0 disables the timer" is **not** adopted. |
| Fraction & Percentage was a placeholder | Implemented (answer in Fraction or Percentage, timer) in the Quizzen style, asking only the 24 unit fractions of the owner's chart | Owner request. Fraction answers get an on-screen "/" key because the number keyboard has none; percentage answers get an automatic `%` sign and may be typed with a decimal point. |
| Followed `prefers-color-scheme` (light + dark) | Light only | Product decision: the app is light-only. |
| Haptics toggle was not persisted or connected | Persisted (DataStore) and applied everywhere, with a distinct effect per action | Real feature. |
| Option chips had 22 px side padding | 18 dp side padding | On 360 dp phones three chips need 326 dp but only 324 dp are available, so "4 Digits" wrapped and left a gap on the right. With 18 dp, three chips fit per row (2, 3, 4 Digits, then 5 Digits). |
