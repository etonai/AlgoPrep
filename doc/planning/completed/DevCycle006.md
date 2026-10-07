# DevCycle 006: Studied Tracking

**Status:** Verified
**Start Date:** 2026-10-07
**Target Completion:** TBD
**Focus:** Add a button that records the date a problem was successfully studied, store it in a CSV file, and show a `(STUDIED yyyy-mm-dd)` tag on the Problems list.

Design source: `doc/planning/AlgoPrepPlan-Claude02.md` (the "Plan"), mainly sections 5, 6, 10 and 11, plus the conversation that led to this document. Section numbers refer to the Plan. This cycle builds on DevCycles 001-004 (Verified) and the shared `UploadController` pattern from DevCycle 005.

---

## Goal

Let the user keep a simple record of what has been studied. After solving a problem, the user presses one button and AlgoPrep stores today's date against that problem. Only the **last studied date** is kept, so a later press overwrites the earlier date. There is no history, no outcome rating and no review scheduling. The Problems list shows the date next to each studied problem, in the same way it shows the `(selected)` marker, so the user can see at a glance what they have done.

This is the first time AlgoPrep writes data about the user's problems, so where the file lives and how safely it is written are the main design points (Open Questions 1 and 5).

## Desired Outcome

- A **Studied** button marks the selected problem as studied today. It is available on the MAIN tab and on the Problems tab, next to Upload, and both buttons behave identically.
- The Problems list shows `(STUDIED 2026-10-07)` on studied rows, alongside the existing `(selected)` marker when both apply.
- The records are kept in a CSV file (`key,date`, one row per problem) that the user can open and edit by hand.
- A damaged or hand-edited file never loses the user's history silently: bad rows are skipped, an unreadable file is preserved as `.bad`, and writes cannot leave a half-written file.
- The new logic is unit tested, and the full build passes.

**Not in this cycle:** a history of sessions, outcomes or ratings, review scheduling, summary or statistics views, a Studied filter (Open Question 4), keyboard shortcuts (DC5 work in the carried-in list), and any change to ChatGPT, upload or the display pane.

---

## Tasks

### Phase 1: StudiedStore (pure logic)

**Status:** Verified

- [x] Create package `com.algoprep.studied` and a pure `StudiedStore` (no Swing) holding a map from base key to `LocalDate`.
- [x] `load(path)`: read the CSV tolerantly. Accept an optional header row. Skip blank lines, rows with the wrong number of fields and rows with an unparsable date, and count what was skipped so the UI can report it. Read as UTF-8 and drop a leading byte-order mark.
- [x] A missing file means "no records", not an error.
- [x] An unreadable file (I/O error) is reported and the previous in-memory records are kept.
- [x] `markStudied(key, date)`: set or overwrite the date, save, and notify listeners only on a real change (Plan section 10 style, `addListener(Runnable)`).
- [x] `clear(key)` if Open Question 2 is accepted.
- [x] `studiedOn(key)` returns `Optional<LocalDate>`.
- [x] Save writes the whole file to a temporary file in the same folder and then renames it over the real file (Open Question 5), sorted by key so the file diffs cleanly. Keys containing a comma or quote are written with standard CSV quoting.
- [x] Rows for problems that no longer exist are kept and written back unchanged.
- [x] Take the file path and a clock (`java.time.Clock`) in the constructor, so tests use a temp directory and a fixed date.
- [x] Unit tests (with `@TempDir`): round trip; missing file; header present and absent; blank lines; bad rows skipped and counted; duplicate keys (last row wins); BOM; quoted keys; overwrite with a later date; same-date press does not notify; clear; sorted output; unknown keys preserved; a save failure is reported and leaves the old file intact; a malformed file is preserved as `.bad` if Open Question 5 is accepted.

**Technical Notes:**
The key is the base key (`0001_two-sum`), the same identity the scanner, selection model and upload use. The date format is ISO `yyyy-MM-dd`. No time of day is stored. The store reports problems through a `Consumer<String>` status callback, as `SettingsStore` does.

### Phase 2: File Location

**Status:** Verified

- [x] Implement the location chosen in Open Question 1.
- [x] If the file lives in HOME: the fixed name is `AlgoPrep_studied.csv`, the path is derived from the HOME setting, and the store is reloaded when HOME changes. With HOME unset, the Studied buttons are disabled with the reason `Select a HOME directory in Settings.`, and the list shows no tags.
- [x] If the file lives in `%APPDATA%\AlgoPrep`: add the path to `AppConfig`.
- [x] Either way, load once at startup and report skipped rows or a preserved `.bad` file through `StatusReporter`.
- [x] Re-read the file when the Problems tab is shown or Refresh is pressed, so hand edits appear without a file watcher (same approach as the display tabs).
- [x] Unit tests for the path derivation and the HOME-change reload.

**Technical Notes:**
The Plan says My Notes are display-only and that AlgoPrep does not write into HOME (sections 2 and 11). Choosing HOME relaxes that for this single file only. The notes files themselves stay read-only.

### Phase 3: Studied Tag in the Problems List

**Status:** Verified

- [x] Show `(STUDIED 2026-10-07)` at the end of a studied row's text, in the same style as the bold `(selected)` marker. A selected and studied row shows both.
- [x] Update the list when the store changes, without rebuilding the whole list model and without changing the selection or the scroll position.
- [x] The filter behavior is unchanged: it still matches only the number and name, not the tag (Open Question 4).
- [x] Unit test the row text builder (pure), including both markers together.

**Technical Notes:**
The row-text rule should be a small pure function so it can be tested without Swing, like `ProblemFilter`.

### Phase 4: Studied Button

**Status:** Verified

- [x] Add a **Studied** button next to Upload on the MAIN tab (Problem Files panel) and at the bottom of the Problems tab, as decided in Open Question 3.
- [x] Pressing it marks the **selected** problem as studied today, like Upload, not the row that is merely highlighted.
- [x] Share one implementation between the buttons, in the style of `UploadController`: one controller owns the enabled state, the tooltip reason and the action, and both buttons are bound to it. The "why is it disabled" decision is a pure function with unit tests.
- [x] Disabled, with a tooltip giving the reason, when no problem is selected, the selected problem is unavailable, or the location in Open Question 1 is not usable (for example HOME unset).
- [x] Status message after a press: `Marked 1 - Two Sum as studied on 2026-10-07.` A save failure shows its own message and leaves the old date in place.
- [x] The tooltip shows the current stored date when there is one, for example `Studied 2026-10-03. Press to update to today.`
- [x] If Open Question 2 is accepted, add the **Clear** control as decided there.
- [x] Expose `triggerStudied()` for a future shortcut and leave it unwired.
- [x] Mention the button and the CSV file in `README.md`.

**Technical Notes:**
A separate, small controller is preferred over adding to `UploadController`, because the two have different enable rules and different actions. Reuse its binding approach, not its code.

### Phase 5: Refinements

**Status:** Verified

Two changes the user asked for after seeing the first version.

- [x] **Rename Clear to "Clear Studied Tag"** on both the MAIN tab and the Problems tab, so the button says what it does. The tooltips, the status message and the README use the same wording. The buttons still share one controller, so they stay identical.
- [x] Check the wider button fits: the MAIN tab's button row (Upload, Studied, Clear Studied Tag) must not be clipped at the right pane's normal width, and the Problems tab's row likewise. If it does not fit, move Studied and Clear Studied Tag to a second row (Open Question 9).
- [x] **Studied label in the display window:** when the selected problem has a studied date, show `STUDIED 2026-10-07` at the top of the display window. It is hidden (takes no space) when the problem is not studied or nothing is selected (Open Question 8).
- [x] The label updates at once when Studied or Clear Studied Tag is pressed, when the selection changes, when the file is reloaded or edited elsewhere, and when HOME changes. It follows the theme.
- [x] The label is read-only, and the `+` and `-` text-size buttons are unchanged. The label keeps a fixed size and is not scaled by them.
- [x] A selected problem whose statement is gone but still has a date shows the label too, since the date is stored by key.
- [x] Unit tests: the label text for studied, not studied and nothing selected (pure function, with a malformed or missing date giving no label); `DisplayPanel` shows and hides the label as the store changes, using a real panel without a window as `DisplayPanelTest` does; the renamed button text on both tabs.
- [x] Update `README.md` (the button name and the label).

**Technical Notes:**
`DisplayPanel` currently has a bar above the tabs holding the `-` and `+` buttons, right-aligned. The label goes in the same bar on the left, so it stays visible on all three tabs, and it needs the `SelectedProblemModel` and `StudiedStore` (already passed to other panels). The label text is a small pure function so it can be tested, like `ProblemRowText`.

### Phase 6: Manual Verification and Closeout

**Status:** Verified

- [x] `gradlew.bat build` is green. Record the test counts.
- [x] Select a problem, press **Studied**, and confirm the tag appears with today's date on both tabs' views of the list. Restart and confirm it is remembered.
- [x] Press it again on a later date (change the date in the CSV by hand) and confirm the date is overwritten.
- [x] Edit the CSV by hand (change a date, add a row, add a bad row) and confirm Refresh picks up the good rows and reports the bad one.
- [x] Corrupt the CSV completely and confirm it is preserved as `.bad` and the app still starts.
- [x] Make the file read-only or lock it and confirm the failure message is clear and the tag does not change.
- [x] With the location unusable (for example HOME unset), confirm the buttons are disabled with a reason and the Problems list still works.
- [x] Studied tag and `(selected)` together on one row; filter still works; many problems studied in a large list stays responsive.
- [x] Both themes readable; both buttons behave identically.
- [x] The renamed button fits on both tabs, and the `STUDIED yyyy-mm-dd` label appears and disappears in the display window as you press Studied and Clear Studied Tag, change problems, and edit the CSV by hand.
- [x] Complete the Completion Summary and report **Work Complete**. Only the user marks anything **Verified**.

---

## Open Questions

**Resolved:** all seven recommendations were accepted (see Decisions and Implementation Results below). The questions are kept for the record.

1. **Where does the CSV live?**
   Options: (a) in HOME next to your notes, as `AlgoPrep_studied.csv`; (b) in `%APPDATA%\AlgoPrep` next to `settings.json`.
   Recommendation: **(a) HOME.** The study history then follows your notes if HOME is backed up or synced, and it is easy to find and edit. The cost is that AlgoPrep writes into HOME for the first time, which relaxes the Plan's read-only stance for this one file. The notes files stay read-only. With (b) nothing changes about HOME, but the history is easy to lose and is tied to this machine's profile. If you pick (a), the Studied buttons need HOME to be set.

2. **Can a mistaken press be undone?**
   Recommendation: **Yes, with a small Clear button** beside Studied (or a tooltip-described second press, which I would avoid because it hides the action). Only the last date is stored, so a mis-press otherwise needs a hand edit of the CSV. Clear removes the row for the selected problem.

3. **Where do the buttons go?**
   Recommendation: **Both tabs, next to Upload:** the MAIN tab's Problem Files panel and the bottom of the Problems tab, driven by one shared controller so they cannot drift apart. Both act on the selected problem.

4. **Should the Problems filter know about Studied?**
   Recommendation: **Not in this cycle.** The tag is for scanning the list by eye. If you later want "show only unstudied", the clean way is a separate All / Studied / Not studied selector, not matching the tag text in the filter box. It can be logged as a finding after you use the tag.

5. **How careful should the file handling be?**
   Recommendation: **Careful but small:** tolerate bad rows and a missing file, preserve an unparsable file as `.bad` before starting fresh, and write through a temporary file and rename so a crash cannot truncate your history. This mirrors what `SettingsStore` already does and costs little.

6. **Should the `Selected:` line on the MAIN tab also show the studied date?**
   Recommendation: **No.** The list tag is what you asked for, and the tooltip on the button already shows the date. It is easy to add later.

7. **What counts as "today"?**
   Recommendation: **The local date from the system clock when the button is pressed**, stored as `yyyy-MM-dd` with no time of day. Pressing it twice on the same day changes nothing and does not notify.

8. **Where does the label go in the display window?** (added in Phase 5)
   Options: (a) at the top of the Problem tab's content only; (b) in the bar above the tabs, on the left, opposite the `-` and `+` buttons, visible on all three tabs.
   Recommendation: **(b).** The studied status belongs to the selected problem, not to one tab, and the bar already exists. The label is plain text, `STUDIED 2026-10-07`, in bold, and takes no space when the problem is not studied. Your message wrote `2026-10=07`, which I took to be a typo for `2026-10-07`, the same format as the list tag.

9. **What if "Clear Studied Tag" does not fit on the MAIN tab's button row?** (added in Phase 5)
   Recommendation: **Put Studied and Clear Studied Tag on their own row below Upload** on the MAIN tab if they do not fit, and do the same on the Problems tab. It will be checked at the normal window width during the phase.

---

## Decisions

All seven recommendations were adopted:

1. **Location:** `AlgoPrep_studied.csv` in HOME. The notes files stay read-only, and this is the only file AlgoPrep writes there.
2. **Undo:** a **Clear** button beside Studied.
3. **Buttons:** Studied and Clear on both the MAIN tab (Problem Files panel, next to Upload) and the Problems tab, driven by one `StudiedController`.
4. **Filter:** unchanged. It does not match the tag.
5. **File handling:** tolerant load, `.bad` preservation, write through a temporary file and rename.
6. **`Selected:` line:** unchanged.
7. **Today:** the local date from the system clock, `yyyy-MM-dd`.

---

## Implementation Results

Phases 1-4 are implemented. The full build passes with 328 tests and 0 failures (38 new: `StudiedStoreTest` 24, `StudiedAvailabilityTest` 6, `StudiedControllerTest` 8). Phase 6, the manual check, was closed by the user, who approved Verified after trying the buttons, the list tag and the banner in the real window, without a step-by-step record of each manual item.

- **`studied/StudiedStore`** (pure): holds key to date, case-insensitive. `setFile` points it at a file and loads it (the same path again does nothing), `reload` re-reads, `markStudied`, `clear`, `studiedOn`, and `addListener`. Listeners fire only on a real change.
  - **Loading:** blank lines and an optional header are accepted. Unreadable rows (bad date, wrong field count, empty key, unbalanced quote) are skipped and counted in one status message. Invalid UTF-8 and a byte-order mark are tolerated. A file whose rows are all unreadable is moved to `.bad` first.
  - **Saving:** every change re-reads the file first, so edits made outside AlgoPrep since the last load are kept, then writes sorted rows to `AlgoPrep_studied.csv.tmp` and renames it over the real file. If the file cannot be read, nothing is written. A failed write is reported and leaves the old file and the old date in place. Rows for problems that no longer exist are kept.
  - **Same-day press:** changes nothing, writes nothing and does not notify.
- **`studied/StudiedAvailability`** (pure): the reason each button is disabled (nothing selected, HOME unset, HOME not found, statement gone, nothing to clear). An unavailable problem cannot be marked, but its date can still be cleared.
- **`problem/ProblemRowText`** (pure): `1 - Two Sum   (selected)   (STUDIED 2026-10-07)`.
- **`ui/StudiedController`:** binds any number of Studied and Clear buttons to one implementation, like `UploadController`. Tooltips give the reason when disabled, or the stored date (`Studied 2026-10-03. Press to update to today.`). Status messages are `Marked 1 - Two Sum as studied on 2026-10-07.` and `Cleared the studied date for 1 - Two Sum.`. It has a `trigger()` for a future shortcut, and `MainPanel.triggerStudied()` exists but is not wired.
- **Wiring:** `Main` creates the store, points it at HOME's file at startup, and follows HOME changes. `AppFrame` creates the controller and passes it to both panels. The Problems list repaints on a store change, and reloads the file when Refresh is pressed or the tab is shown.
- **Layout:** the Problems tab's bottom area now has a row of Upload, Studied and Clear, with `Selected: ...` on its own line below, because the right pane is narrow. Not checked on screen.
- **Docs:** `README.md` describes the buttons and the file.
- **Not yet confirmed live:** none of this has been run in the real window.
- **Phase 5, refinements:** The Clear button is now **Clear Studied Tag** on both tabs, and its tooltip and status message (`Cleared the studied tag for 1 - Two Sum.`) use the same wording. The display window shows `STUDIED 2026-10-07` as a large (22 pt bold) dark-on-light-gray banner at the top of the **Problem** tab, shown only while the selected problem has a date (first built in the bar above the tabs, then moved into the Problem tab at the user's request, Open Question 8 option (a), and later made slightly smaller and light gray instead of green). The banner sets its own colors, because the theme applier recolors every label (`studied/StudiedLabel` is the pure text rule). It follows the selection, the Studied and Clear Studied Tag buttons, reloads of the file, and HOME changes, and still shows when the statement is gone. It does not scale with the text-size buttons, and the Notes and My Notes tabs do not have it. Open Questions 8 and 9 were resolved by adopting the recommendations.
  - **Fit:** a test lays out both tabs at 350 px wide and checks Upload, Studied and Clear Studied Tag all lie inside it, so no second row was needed. This used Swing's default look, so it is worth a glance in the real window.
  - **Tests:** the full build passes with 337 tests and 0 failures (9 new in `StudiedLabelAndButtonsTest`). `DisplayPanelTest` was adjusted because the buttons now sit one level deeper in the bar.
- **Known limitation:** rows skipped as unreadable are dropped the next time a date is saved. The message says so.

---

## Notes and Risks

- **Risk: first write into HOME.** AlgoPrep has so far only read HOME. Mitigation: it writes one fixed file name, never deletes anything there, and writes through a temp file and rename. The upload staging safety rules are unaffected.
- **Risk: losing the history through a bad save or a hand-edit mistake.** Mitigation: Open Question 5 (tolerant load, `.bad` preservation, atomic write) and tests for each.
- **Risk: stale tags.** There is no file watcher. Mitigation: reload on Refresh and when the Problems tab is shown.
- **Risk: orphaned rows** for renamed or deleted problems. Mitigation: they are kept and ignored, so they return if the problem file returns.
- DevCycle 005 is still In Progress. This document is for planning and does not start before the user says so. If the work is done inside DC5's window instead, add it to DC5 as a new phase and fold this document into it.
- Git is managed by the user. Agents do not run git commands in this project.
- Creating this document does not authorize implementation. Work starts only on the user's explicit instruction.

---

## Completion Summary

**Completion Date:** 2026-10-07
**Phases Completed:** All (1-6)
**Work Deferred:** None. A Studied filter, a history of sessions, review scheduling and a shortcut for Studied remain out of scope.

**Accomplishments:**
- `StudiedStore`: last studied date per problem in `AlgoPrep_studied.csv` in HOME, with tolerant loading, `.bad` preservation and write-then-rename saving
- Studied and Clear Studied Tag buttons on the MAIN and Problems tabs, sharing one `StudiedController`
- `(STUDIED 2026-10-07)` tag on Problems list rows
- A light gray `STUDIED 2026-10-07` banner at the top of the Problem tab in the display window

**Metrics:**
- Files: 6 new main classes (`StudiedStore`, `StudiedAvailability`, `StudiedLabel`, `ProblemRowText`, `StudiedController`, plus the banner inside `DisplayPanel`), 5 changed (`MainPanel`, `ProblemsPanel`, `DisplayPanel`, `AppFrame`, `Main`), 4 new test classes
- Tests passing: 337, 0 failures (46 new)

**Lessons / Notes:**
Verified by the user on 2026-10-07. This is the first data AlgoPrep writes about the user's problems, and the only file it writes in HOME. The decisions made along the way: the file lives in HOME, a Clear button undoes a mistaken press, and the banner sits in the Problem tab (first tried in the bar above the tabs). The theme applier recolors every label, so the banner overrides its own colors. The "Clear Studied Tag" name was chosen so the button says what it does.
