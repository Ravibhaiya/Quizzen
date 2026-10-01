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
  (-> Powers & Roots config), "Fraction & Percentage", "Alphabet Reasoning" (placeholders).
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

## Powers & Roots configuration
- **Practice Types** (multi-select, none selected initially): Squares (x²), Cubes (x³), Square Roots (√x), Cube Roots (³√x).
- **Number Range**: Min Number (default 2) and Max Number (default 30), digits only, 1 to 30. A value above 30 is cut to 30 and
  "Numbers can't be above 30" shows for 2.5 s. An empty or zero field returns to its default when it loses focus.
- **Limits**: squares and square roots use base numbers up to **30**; cubes and cube roots up to **20**. The range is over the
  base number (for a root question it is the answer): 2-30 gives squares up to 30² = 900, square roots of up to 900, cubes up to
  20³ = 8000 and cube roots of up to 8000. A range that goes above 20 is cut at 20 for the cube kinds only.
- A line under the fields shows the limits until something is selected, then the range each selected kind will really use, e.g.
  "Squares & roots: 2–30 · Cubes & roots: 2–20".
- **Start** is enabled when at least one type is selected and the range is valid. Errors: Min greater than Max, a blank/zero field
  (blocks Start, no message while typing), and "only cubes/cube roots selected but Min is above 20" (no numbers to ask).
  If squares are also selected and Min is above 20, only the square kinds are asked.
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
- Check: exact integer match = **Correct**; otherwise (including blank) = **Incorrect** (shake).
- Countdown reaching 0 = **Time's Up**.
- Feedback bottom sheet (green / red / orange) for 1.7 s, then slides away; 250 ms later a new question appears, the input is
  cleared, and the timer restarts from the configured value. Input and Check are ignored while the sheet is showing.
- Haptics (only when the Haptic Feedback setting is on; played through the device vibrator, so it does not depend on the
  system "touch vibration" toggle). Every kind of action has its own sensation:

  | Effect | When |
  |---|---|
  | Tick (light) | selecting a digit option or table number, switching Math/Language, turning haptics off, "Coming soon" taps |
  | Click (medium) | opening Multiply/Tables/Settings, Back, Select All, turning haptics on |
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
| Powers & Roots was a placeholder | Implemented (types, range, timer) in the Quizzen style, from a reference screenshot of another app | Owner request. The reference's "0 disables the timer" is **not** adopted: 0 or blank uses the default like every other screen. |
| Followed `prefers-color-scheme` (light + dark) | Light only | Product decision: the app is light-only. |
| Haptics toggle was not persisted or connected | Persisted (DataStore) and applied everywhere, with a distinct effect per action | Real feature. |
| Option chips had 22 px side padding | 18 dp side padding | On 360 dp phones three chips need 326 dp but only 324 dp are available, so "4 Digits" wrapped and left a gap on the right. With 18 dp, three chips fit per row (2, 3, 4 Digits, then 5 Digits). |
