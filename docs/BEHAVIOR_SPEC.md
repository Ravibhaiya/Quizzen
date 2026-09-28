# Behavior specification

Normative description of the app. "Web" = the prototype in `docs/reference/quizzen-web-reference.html`.

## Home
- Header: logo blob "Q" + "Quizzen" title, Settings icon button (opens settings sheet).
- Segmented control: **Math** | **Language**. Tap animates the pager; swiping the pager moves the indicator with the finger.
- **Math** page: hero "Multiply" (-> Multiply config); tiles "Tables Practice" (-> Tables config), "Powers & Roots",
  "Fraction & Percentage", "Alphabet Reasoning" (placeholders).
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

## Timer field (both config screens)
- Digits only; value > 180 is replaced by 180 and the field shows a red outline plus "Max allowed timer is 180 seconds"
  for 2.5 s. Blank is allowed while typing and becomes the screen default when focus is lost.
- On Start, a value of 0 or blank runs with the screen default.

## Practice
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
- Haptics (if enabled): confirm on correct, reject on incorrect/timeout, tick on selection changes and tab taps.
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
| Haptics toggle was not persisted or connected | Persisted (DataStore) and applied everywhere | Real feature. |
