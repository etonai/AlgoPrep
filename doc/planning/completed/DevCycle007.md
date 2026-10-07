# DevCycle 007: Study Lists

**Status:** Verified
**Start Date:** 2026-10-07
**Target Completion:** TBD
**Focus:** Let the user point AlgoPrep at a study list file (a CSV such as `Grind75.csv`) and get a new tab that lists those problems, in the file's order, with their difficulty and time.

Design source: `doc/planning/ideas/studylists.md` (the "Idea"), the Plan (`doc/planning/AlgoPrepPlan-Claude02.md`) for the problem identity and Problems tab rules (sections 5 and 6.2), and DevCycles 2, 5 and 6 for the selection model, the shared Upload controller and the studied tracking this builds on. This cycle builds on DevCycle 006, which is Verified.

---

## Goal

The user has several ready-made study lists (a LeetCode top 20 from Claude Code, a top 20 from ChatGPT, and the Grind 75) and wants to work through one of them in order. A new optional setting, **Study List File**, names one such CSV file. When it is set, AlgoPrep reads the file and adds a tab, titled with the file's name, that shows the listed problems in the order the file gives, with the extra information the user put in the file (such as `Easy` or `20 minutes`). The tab behaves like the Problems tab: a problem can be selected from it, and rows carry the same `(selected)` and `(STUDIED date)` tags. When no study list is set, there is no extra tab.

The list file only says *which problems, in what order, with what notes*. The problems themselves still come from the PROBLEMS directory, so a listed problem is matched to its files there. A listed problem with no files shows as NOT FOUND instead of being hidden, so gaps in the user's collection are visible.

## Desired Outcome

- The Settings tab has an optional **Study List File** setting with `Browse...` and a way to clear it. It persists, and a saved path that no longer exists stays visible with a warning.
- With a file set, a new tab titled with the file's name (`Grind75.csv` gives `Grind75`) appears. With no file set, the tab does not exist, and it appears and disappears as the setting changes, without restarting.
- Each row follows the Idea's examples:
  - `1 - Two Sum, Easy, 20 minutes`
  - `15_not-found, Tough, 15 minutes - NOT FOUND`
  - `99 - No Difficulty, 2 minutes`
  - `105 - No Time, Medium`
  Missing difficulty or time is left out, with no stray commas.
- Rows keep the file's order and carry the same `(selected)` and `(STUDIED yyyy-mm-dd)` tags as the Problems tab.
- Double-click or Enter selects a found problem, which then shows in the display window and can be uploaded and marked studied, exactly as from the Problems tab. A NOT FOUND row cannot be selected.
- The file is read tolerantly: blank lines and bad rows are skipped and reported, and a damaged file never stops the app.
- The new logic is unit tested, and the full build passes.

**Not in this cycle:** editing the study list inside AlgoPrep, creating list files, more than one list at a time (Open Question 1), importing from the web, progress charts, reordering, and any change to the Problems tab's own behavior.

---

## Tasks

### Phase 1: Study List File Format and Parser

**Status:** Verified

- [x] Create package `com.algoprep.studylist` and a pure `StudyListParser` (no Swing) that turns the file's text into a list of entries, in file order.
- [x] An entry holds the key as written, the difficulty and the time (the last two may be blank): `StudyListEntry(String key, String difficulty, String time)`.
- [x] Format (Idea, "Format of the study list"): one problem per line, `key, difficulty, time`, with spaces around the commas ignored. A line with fewer fields leaves the rest blank, so `105_notime, Medium,` and `0001_two-sum` both work, and `99_nodifficulty, , 2 minutes` has a blank difficulty.
- [x] Blank lines are skipped. Lines starting with `#` are comments (Open Question 4). A header row on the first line (`problem,difficulty,time`, or `key,...`, in any case) is skipped and not counted. Fields in double quotes may contain commas. Extra fields beyond the third are ignored (Open Question 5).
- [x] A line with a blank key is skipped and counted. The parser returns the entries plus the number of skipped lines, so the UI can report them.
- [x] Read as UTF-8, replace malformed bytes rather than failing, and drop a leading byte-order mark (Excel adds one).
- [x] Share the CSV line splitting with `StudiedStore` instead of copying it. Move `parseLine` to a small package-visible or public `CsvLine` helper and keep the existing `StudiedStore` tests passing.
- [x] The tab title comes from the file name without its extension, in a pure function.
- [x] Unit tests: the Idea's four example lines; spaces around commas; blank difficulty; blank time; a key alone; trailing comma; a quoted field with a comma; comments and blank lines; blank key skipped and counted; extra fields ignored; BOM; invalid UTF-8; Windows and Unix line endings; an empty file gives an empty list; order is preserved; duplicate keys are kept; the title function (`Grind75.csv` gives `Grind75`, no extension, several dots, upper-case extension).

**Technical Notes:**
The parser does no file I/O so it is easy to test. A small `StudyListFile.load(Path)` wrapper reads the file and returns either the parsed list or an error message (missing file, a directory, unreadable), so a bad file is reported and not thrown.

### Phase 2: Matching Entries to Problems

**Status:** Verified

- [x] Write a pure `StudyListResolver` that takes the entries and the catalog's problem list, and returns one row per entry, in order: either a found `Problem` or a NOT FOUND marker, plus the difficulty and time.
- [x] Match by the problem's **number and slug**, not by the exact text of the key, so `99_nodifficulty` in the list finds `0099_nodifficulty_problem.md` in the PROBLEMS directory (Open Question 2). Use `ProblemNames.parseKey` for both sides. Compare the slug case-insensitively. Never match by the number alone or by a partial slug, so `0001_two-sum-ii` is never matched to `0001_two-sum` (Plan section 5.1).
- [x] A key that does not parse as `number_slug` (for example `two-sum`) cannot be matched, so it shows as NOT FOUND with the text as written.
- [x] If two PROBLEMS entries share a number and slug (not normally possible), take the first by the catalog's order.
- [x] A pure `StudyListRowText` builds the row text:
  - found: `<display name>[, <difficulty>][, <time>]`, then `   (selected)` and `   (STUDIED date)` as in `ProblemRowText`;
  - not found: `<key as written>[, <difficulty>][, <time>] - NOT FOUND`, with no selected or studied tags.
- [x] Unit tests: the Idea's four examples produce exactly the four sample rows, given a catalog with `0001_two-sum`, `0099_nodifficulty` and `0105_notime` and no `15`; zero-padding differences both ways (`1_two-sum` and `0001_two-sum`); case differences; similar-prefix names do not match; unparsable key; blank difficulty and time leave no stray commas; selected and studied tags together; NOT FOUND rows never show selected or studied tags; list order is kept (not sorted by number).

**Technical Notes:**
Keep the study list file as the only source of order and extras. The display name comes from the matched `Problem`, so it is formatted the same as in the Problems tab. The resolver runs again whenever the catalog or the list changes, so the tab follows a PROBLEMS refresh.

### Phase 3: Study List File Setting

**Status:** Verified

- [x] Add an optional `studyListFile` field to `SettingsStore` (with getter and setter, saving and notifying only on a real change). It is optional in the file, so existing `settings.json` files still load.
- [x] Add a **Study List File** section to `SettingsPanel`: a path label, `Browse...` (a file chooser, defaulting to the saved file's folder), and **Clear** (removes the setting, so the tab goes away).
- [x] Show a warning and keep the path visible when the saved file does not exist or cannot be read (Plan section 6.3), in the style of the other settings.
- [x] Describe the expected format in the tooltip or a short hint under the setting.
- [x] Unit tests: the new field round trips; missing field gives null; setting and clearing notifies once; an old settings file without the field still loads.

**Technical Notes:**
The directory sections share a `directoryControls` helper driven by a spec record. A file setting is a little different (a file chooser, and a Clear button), so extend the helper if it fits, or write a short sibling, as long as the same label, warning and Browse behavior are not copied.

### Phase 4: The Study List Tab

**Status:** Verified

- [x] Write a `StudyListModel` (no Swing) that owns the loaded list: it follows the `studyListFile` setting, loads the file, resolves it against the catalog, keeps the last error, and has `addListener(Runnable)`. It reloads when the setting changes, when the catalog changes, and on an explicit `reload()`.
- [x] Write `ui/StudyListPanel`, modeled on `ProblemsPanel`: a list of the rows from Phase 2, a message line, and a **Refresh** button (Open Question 6 for a filter box). It is built from the model and the same selection and studied models as the Problems tab.
- [x] Double-click or Enter on a found row selects that problem. A single click only highlights it. A NOT FOUND row does nothing on double-click, and the message line says why (`15_not-found has no files in the PROBLEMS directory.`).
- [x] Rows use the row text from Phase 2. The selected problem's row is bold, as in the Problems tab. NOT FOUND rows are shown in a muted style so they stand out from the rest.
- [x] Add the tab **after Problems**, titled with the file's name, when the setting is set, and remove it when the setting is cleared, without a restart. Keep the user's current tab selected where possible (Open Question 3).
- [x] If the setting is set but the file cannot be read, still show the tab, with the error in the message line and an empty list, so the user can see why (Open Question 3). Report the error through `StatusReporter` too.
- [x] Refresh and tab-shown reload the list file and the studied file, and rescan problems, so hand edits to any of them appear, as on the Problems tab.
- [x] The message line summarizes the list, for example `75 problems, 12 studied, 3 not found` (Open Question 7).
- [x] Keep it independent of the browser: the tab must work while ChatGPT is loading, signed out or failed (Plan section 4).
- [x] Unit tests: `StudyListModel` follows the setting (set, change, clear); reloads on a catalog change; keeps the previous list if a reload fails (or shows the error, per the decision in Open Question 3); counts for the summary. `StudyListPanel` built without a window (as `MainPanelResetTest` does): row texts, selection on activation, NOT FOUND row not selectable, the tab appears and disappears with the setting.

**Technical Notes:**
`AppFrame` creates the right-hand `JTabbedPane` with MAIN, Problems and Settings. The study list tab is inserted at index 2 (before Settings), so inserting or removing it must not change which tab is selected unless it is the removed one. The `ProblemsPanel` and the new panel share the row renderer idea, so extract a small shared renderer rather than duplicating it.

### Phase 5: Buttons and Selection on the Study List Tab

**Status:** Verified

- [x] Add the same bottom controls as the Problems tab: **Upload**, **Studied** and **Clear Studied Tag**, bound to the existing shared `UploadController` and `StudiedController`, and the `Selected: ...` line (Open Question 8). No new upload or studied logic.
- [x] Selecting a problem from the study list tab selects it for everything: the display window, the MAIN tab's `Selected:` line, Upload and Studied. Selecting from the Problems tab updates the highlighted marker on the study list tab, and vice versa.
- [x] Pressing Studied or Clear Studied Tag updates the `(STUDIED ...)` tag on the study list rows at once, and the summary count.
- [x] Unit tests: the buttons on this tab are identical in state and tooltip to the other tabs' (as `UploadControllerTest` and `StudiedControllerTest` check); marking studied updates the row text and the summary; the same problem selected from either tab is the same selection.
- [x] Update `README.md`: the setting, the file format with an example, and the new tab.
- [x] Check at the normal window width (about 350 px for the right pane) that the three buttons and the long row text fit or are not clipped (rows may need a tooltip with the full text).

**Technical Notes:**
The study list tab does not own any selection or studied state. It only observes `SelectedProblemModel` and `StudiedStore`, and calls `select` on the first. Long rows (`15_not-found, Tough, 15 minutes - NOT FOUND   (selected)   (STUDIED 2026-10-07)`) can be wider than the pane, so the list scrolls sideways, and each row has a tooltip with its full text.

### Phase 6: Bug Fix, Study List Tab Ignores the Dark/Light Setting

**Status:** Verified

Found by the user while using the tab: the study list tab does not obey the dark/light mode setting.

- [x] Reproduce it and record exactly what is wrong in both directions: the tab's background, list, filter box, message line and buttons, in dark mode and in light mode, and whether it is wrong at startup, after the tab is added later by setting the Study List File, after a theme switch while the tab is open, or after a theme switch while the tab did not exist.
- [x] Check whether the Problems tab's list shows the same flaw (Open Question 12). Both are `JList`s inside panels built the same way.
- [x] Fix the cause so the tab matches the other tabs in both modes, in every case above.
- [x] A unit test that fails before the fix and passes after it: build the window pieces without a window, apply the theme, add the study list tab afterwards (as the setting does), and check its panel, filter box, list and message colors equal the other tabs' for the current theme. Repeat for a theme switch while the tab is present, and while it is absent and added later.
- [x] Keep the NOT FOUND rows readable in both themes: the muted color must contrast with the list background in dark and in light mode, and the `(selected)` and `(STUDIED ...)` rows must stay readable when highlighted.
- [x] Check that the fix does not change how any other tab looks, and that the display window's banner and Markdown colors are unaffected.

**Technical Notes:**
Suspected causes, not yet confirmed:
1. **The tab is added after the theme was applied.** `NativeThemeApplier.apply(window, theme)` recolors the components that are in the window at that moment. The study list panel is created at startup but only inserted into the right-hand tabs when a Study List File is set, which can be later, so it never receives the colors that the other tabs received when the theme was last applied. A theme switch while the tab is absent has the same effect. The likely fix is to apply the current theme to the window (or to the panel) right after the tab is inserted, in `AppFrame.syncStudyListTab`, and to do it through `UiThread` so it runs after the insertion.
2. **The applier has no rule for `JList`.** `NativeThemeApplier.applyTo` colors text areas, fields, editor panes, buttons, labels, tabbed panes, panels, scroll panes, viewports and split panes, but not `JList`, so a list keeps the look-and-feel's default (light) colors in dark mode. If so, this affects the Problems tab's list as well. The likely fix is a rule for `JList` (background, foreground, and selection colors) in the applier, and the cell renderers must not hard-code colors that fight it.
The first step of this phase is to find out which of the two (or both) is the real cause before changing anything.

### Phase 7: Manual Verification and Closeout

**Status:** Verified

- [x] `gradlew.bat build` is green. Record the test counts.
- [x] Make the Idea's `Grind75.csv` example in a folder outside the repository, with a PROBLEMS directory holding `0001_two-sum`, `0099_nodifficulty` and `0105_notime` (and not `15`). Set it in Settings and confirm the tab `Grind75` appears with exactly the four rows from the Idea.
- [x] Clear the setting and confirm the tab disappears. Set a different list and confirm the title and rows change.
- [x] Double-click and Enter select a found problem, and the display window, MAIN tab and Problems tab agree. A NOT FOUND row cannot be selected and says why.
- [x] Press Studied from this tab and confirm the tag and summary update here and on the Problems tab, and the banner shows in the display window.
- [x] Edit the list file in another editor (add, remove and reorder lines) and confirm Refresh, or switching to the tab, picks it up.
- [x] Add the missing `15` problem files and confirm Refresh turns it from NOT FOUND into a normal row.
- [x] Point the setting at a missing file, a directory, and an empty file, and confirm the messages are clear and nothing crashes.
- [x] Try the real lists: the two top-20 lists and the Grind 75. Confirm the order is the file's order and a long list is easy to use.
- [x] Both themes readable, the tab switching keeps the user's place, and the tab works while ChatGPT is loading or signed out.
- [x] Dark and light mode both look right on the study list tab, including when the setting is added after startup and when the theme is switched with the tab open and closed (Phase 6).
- [x] Complete the Completion Summary and report **Work Complete**. Only the user marks anything **Verified**.

---

## Decisions

All eleven recommendations were adopted:

1. **One list at a time**, set by the Study List File setting.
2. **Match by number and slug**, so `99_nodifficulty` finds `0099_nodifficulty`. Never by number alone or a partial slug.
3. **An unreadable file still shows the tab**, empty, with the error in its message line, and also in the status line. Adding or removing the tab keeps the user's current tab, except when the removed tab was selected, which goes to Problems. The tab sits after Problems.
4. **`#` comment lines and blank lines are ignored.** The file may start with a header row such as `problem,difficulty,time`, which is skipped and not counted (added after the first version, because the user's CSV files may include column names). Only the first line with content can be a header, and only when its first column is `problem` or `key` and its second, if any, is `difficulty` or blank, so a real problem is never mistaken for one.
5. **Fields beyond the third are ignored.**
6. **A filter box** matching number, name, difficulty and time.
7. **A summary line**, for example `75 problems, 12 studied, 3 not found`.
8. **The same three buttons** (Upload, Studied, Clear Studied Tag) on the tab, bound to the shared controllers.
9. **Read only.** AlgoPrep never writes the list file.
10. **The file can be anywhere**, chosen with Browse. No folder scanning.
11. **No "next up" marker.**

---

## Implementation Results

Phases 1-6 are implemented. The full build passes with 409 tests and 0 failures (72 more than before this cycle). Phase 7, the manual check, was closed by the user, who approved Verified after trying the study list in the real window, without a step-by-step record of each manual item.

- **Phase 1:** `studylist/StudyListParser` (pure), `StudyListEntry` (with the extras text), `StudyListFile.load` (never throws, returns an error message) and `StudyListTitle`. The CSV line splitting moved from `StudiedStore` into a shared `csv/CsvLine`, and the `StudiedStore` tests still pass. Tests: `StudyListParserTest` 22, `CsvLineTest` 4.
- **Phase 2:** `StudyListResolver` (number and lower-case slug, first problem wins), `StudyListRow`, `StudyListRowText` (reuses `ProblemRowText` for the tags), `StudyListFilter` and `StudyListSummary`. The Idea's four example lines produce exactly its four sample rows (the example keys use slugs such as `no-difficulty` so the names come out as `No Difficulty`). `StudyListResolverTest` 13.
- **Phase 3:** `SettingsStore` has an optional `studyListFile` (3 tests). The Settings tab has a **Study List File (optional)** section with a path label, a warning (not found, a folder, unreadable), Browse (opens in the file's folder, with a CSV filter) and Clear, plus a one-line format hint. It shares the `directoryControls` helper, extended with a file mode and a Clear button.
- **Phase 4:** `StudyListModel` follows the setting, re-resolves when the catalog is rescanned, reloads on request, and notifies only on a real change. `ui/StudyListPanel` has the filter, Refresh, the list (the selected row in bold, NOT FOUND rows muted, a tooltip with each row's full text) and the summary line. A NOT FOUND row cannot be selected and the message line says why. The tab is added, retitled and removed by `AppFrame.syncStudyListTab`. Long names are shortened on the tab, with the full name as its tooltip. Tab-shown re-reads quietly (no status message each time).
- **Phase 5:** The bottom area (message, Upload, Studied, Clear Studied Tag, `Selected: ...`) was pulled out of `ProblemsPanel` into `SelectionActions`, and both tabs use it, so they cannot drift apart. Studied and Clear Studied Tag update the rows and the summary at once, and a problem selected on either tab is the same selection. A test lays the tab out at 350 px and checks the three buttons are not clipped.
- **Tests:** `StudyListModelTest` 11, `StudyListPanelTest` 16 (rows, selection, NOT FOUND, filter, Refresh, error, the tab appearing and disappearing).
- **Phase 6, theme bug:** the cause was the first suspect, not the second. `NativeThemeApplier` recolors only the components that are in the window when it runs, and the study list panel is inserted into the right-hand tabs only when a Study List File is set, usually after the last apply, so it kept the default colors. The user reported that the Problems tab does not have the flaw, which agrees: it is in the window from the start, and the applier's lack of a rule for lists affects nothing visible there, so no list rule was added (Open Question 12 therefore changed only the study list tab).
  - **Fix:** `AppFrame.syncStudyListTab` takes a callback that runs after the tab is newly inserted, and `AppFrame` passes one that applies the current theme again. A theme switch while the tab is absent is covered the same way, because the theme applied on insertion is the current one.
  - **Test:** `StudyListThemeTest` (4) puts the Problems and study list panels in a real frame, applies each theme, adds the tab afterwards, and compares the panel, filter box, buttons, list viewport and label colors with the Problems tab's. It covers adding after a theme was applied, a switch while the tab is absent, a switch while it is present, and NOT FOUND text contrast of at least 3:1 against the list in both modes. With the re-apply removed, two of the four tests fail, so the test does catch the bug.
- **Not yet confirmed live:** none of this has been run in the real window.
- **Bug avoided:** the tab-shown reload uses a quiet catalog refresh, so switching tabs does not overwrite the status line with `N problem(s) found` each time.

---

## Notes and Risks

- **Risk: keys in the list that do not match the files.** The Idea itself mixes `0001_two-sum` with `15_not-found` and `99_nodifficulty`, so zero-padding will vary. Mitigation: match by number and slug (Open Question 2), with tests for padding and for similar names, and show NOT FOUND instead of guessing.
- **Risk: the study list becomes a second source of truth about problems.** Mitigation: it only orders and annotates. Problem identity, files and studied dates stay keyed by the base key from the PROBLEMS directory.
- **Risk: a hand-edited list file with odd content.** Mitigation: tolerant parsing, skipped lines counted and reported, and nothing is ever written to the list file (AlgoPrep only reads it).
- **Risk: tab insertion and removal confuses the user's place.** Mitigation: the Open Question 3 rules and a test for the selected-tab behavior.
- **Risk: duplicated list code between the Problems tab and the new tab.** Mitigation: a shared row renderer and the existing shared controllers, with no copied upload or studied logic.
- The list file is read, never written. AlgoPrep creates no list files.
- `chatStoryRef/` is still read-only reference.
- Git is managed by the user. Agents do not run git commands in this project.
- Creating this document does not authorize implementation. Work starts only on the user's explicit instruction.

---

## Completion Summary

**Completion Date:** 2026-10-07
**Phases Completed:** All (1-7)
**Work Deferred:** None. More than one list at a time, a "next unstudied" marker, folder scanning for lists, and editing lists inside AlgoPrep remain out of scope.

**Accomplishments:**
- Optional **Study List File** setting with Browse and Clear, and a warning for a missing or unreadable file
- A tab titled with the file's name, added and removed without a restart, listing the problems in the file's order with difficulty and time
- Matching of list keys to problems by number and name, with NOT FOUND rows that cannot be selected
- Filter box, progress summary line, and the shared Upload, Studied and Clear Studied Tag buttons on the tab
- Tolerant CSV reading: comments, blank lines, an optional header row, quoted fields, extra columns
- Fix for the tab ignoring the dark/light setting when it is added after the theme was applied

**Metrics:**
- Files: 12 new main classes (the `studylist` package, `csv/CsvLine`, `StudyListPanel`, `SelectionActions`), 5 changed (`SettingsStore`, `SettingsPanel`, `ProblemsPanel`, `AppFrame`, `StudiedStore`), 8 new test classes
- Tests passing: 409, 0 failures (72 new)

**Lessons / Notes:**
Verified by the user on 2026-10-07. The theme bug came from adding a tab after the theme was applied: the applier only recolors components already in the window, so anything inserted later must re-apply it. A header row was added to the parser after the first version because the user's CSV files may include column names. The bottom area shared by the Problems and study list tabs was extracted into `SelectionActions` so the two tabs cannot drift apart.

---

## Open Questions

**Resolved:** all eleven recommendations were adopted (see Decisions and Implementation Results above). The questions are kept for the record.

1. **One list or several?**
   The Idea describes one setting and one tab, but you have three lists. With one setting, switching lists means changing the setting each time.
   Recommendation: **One list at a time in this cycle**, as the Idea says. It is the simplest thing that works, and Clear plus Browse switches lists in a few clicks. If you find yourself switching often, the natural next step is a setting that holds several files, with one tab each, or a dropdown on the tab to choose among them. The parser, resolver and panel in this design would be reused unchanged, so nothing is lost by starting small.

2. **How should a key in the list match a problem on disk?**
   Your examples use `0001_two-sum`, but also `15_not-found` and `99_nodifficulty`, and you said 99 has the appropriate files, probably `0099_nodifficulty_problem.md`.
   Recommendation: **Match by number and slug**, so `99_nodifficulty` finds `0099_nodifficulty`, and the slug comparison is case-insensitive. Never by number alone and never by partial slug, so `two-sum-ii` cannot match `two-sum`. This means you do not have to pad numbers in the list. A key that does not look like `number_slug` shows as NOT FOUND.

3. **What happens to the tab when the file is set but cannot be read?**
   Options: (a) no tab, with only a Settings warning; (b) the tab still appears, empty, with the error in its message line.
   Recommendation: **(b).** A tab that silently vanishes is confusing, and the error is shown where you are looking. The Settings warning and a status message are added too. Also, adding or removing the tab keeps your current tab selected, except when the selected tab is the one removed, in which case it goes to Problems. The new tab sits right after Problems.

4. **Should the file allow comments and a header row?**
   Your format has neither.
   Recommendation: **Allow `#` comment lines and blank lines, and do not support a header row.** (Changed after the first version: a header row is now skipped, see Decisions.) A header such as `problem, difficulty, time` would be read as a problem called `problem` and would show as NOT FOUND, which is harmless but visible. Comments let you annotate or temporarily disable a line. If you prefer, a first line starting with `key,` could be skipped as a header, but it is easy to avoid by not writing one.

5. **What if a line has more than three fields?**
   Recommendation: **Ignore the extra fields silently** in this cycle, so you can keep other notes in the file. If you later want a fourth column shown (for example a category), it is a small change.

6. **Should the tab have a filter box like the Problems tab?**
   Recommendation: **Yes, if it is cheap.** A Grind 75 list is long enough to want one. It would match the number, the name, the difficulty and the time (so you could type `Easy` to see only the easy ones). The top-20 lists do not need it, and it can be left out if you want the smallest version.

7. **Should the message line show a progress count?**
   Recommendation: **Yes**, one line such as `75 problems, 12 studied, 3 not found`. It uses the studied dates you already record, and it costs a few lines of code. It is not a chart or a history.

8. **Should the study list tab have the Upload, Studied and Clear Studied Tag buttons?**
   Recommendation: **Yes, the same three, bound to the same shared controllers.** The point of a list is to work through it, so you should not have to go back to the Problems tab to press Studied. It adds no new logic. The cost is a crowded bottom area, which Phase 5 checks at the normal width.

9. **Should the order and extras ever be written back, or the list file be edited from AlgoPrep?**
   Recommendation: **No.** The file stays read-only to AlgoPrep, like your notes, and you edit it in your own editor. Refresh or switching to the tab picks up the changes.

10. **Where should the list files live?**
    Recommendation: **Wherever you like.** The setting is a full path chosen with Browse, so they can sit in HOME, in the PROBLEMS folder's parent, or in a separate folder of lists. AlgoPrep does not scan a folder for lists in this cycle. If you do keep several, a `lists` folder next to your notes is a sensible convention, and a future cycle could offer to pick from it.

11. **Should a study list mark which problems are "next"?**
    The Idea says rows show the same selected and studied tags, nothing about "next up".
    Recommendation: **No.** The studied tags already show where you are in the list at a glance. A "next unstudied" button could be a later idea if you want it.

12. **If the Problems tab's list has the same theme flaw, should the fix cover it too?** (added in Phase 6)
    Recommendation: **Yes.** If the cause is the applier having no rule for lists, fixing it once fixes every list, and fixing only the study list tab would leave the two tabs looking different. If the Problems tab turns out to be fine, only the study list tab changes.
