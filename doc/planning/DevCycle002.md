# DevCycle 002: Problems

**Status:** Verified
**Start Date:** TBD
**Target Completion:** TBD
**Focus:** Let the user point AlgoPrep at a PROBLEMS directory, browse and filter the problems in it, and select one.

Design source: `doc/planning/AlgoPrepPlan-Claude02.md` (the "Plan"), mainly sections 5, 6.2, 6.3, 10, 11 and 13. Section numbers below refer to it. This cycle builds on DevCycle 001 (project skeleton, `SettingsStore`, three-pane frame, MAIN tab), which is Verified.

---

## Goal

Give AlgoPrep its list of problems. A pure, well-tested `ProblemScanner` reads the flat PROBLEMS directory using the filename convention, the Problems tab shows the result with a filter, and a selection model tells the rest of the app which problem is selected. DC3 (display) and DC4 (upload) both depend on that selection, so its rules, including what happens when files disappear, are settled here. The work is local and does not involve the browser, so it should be fully usable while ChatGPT is loading or signed out (Plan section 4).

## Desired Outcome

- The Settings tab has a **PROBLEMS directory** setting with `Browse...`. It persists, and a saved path that no longer exists stays visible with a warning.
- The **Problems** tab shows a filter box, a list of `number - name` rows (for example `1 - Two Sum`), and **Refresh**. Double-click or Enter selects a problem. A single click only highlights it.
- The MAIN tab's Problem panel shows `Selected: 1 - Two Sum`, or the problem marked unavailable if its statement has since disappeared.
- The selection is remembered across restarts (by base key) and restored only if a scan still finds it.
- `ProblemScanner` and `ProblemNames` have the unit tests listed in Plan section 13. The full build passes.

**Not in this cycle:** displaying problem or notes content, the HOME setting, Markdown (DC3); upload, staging, and the "Last attached" indicator (DC4); the Ctrl+Shift+P shortcut and other shortcuts (DC5).

---

## Tasks

### Phase 1: Problem Model and Display Names

**Status:** Verified

- [x] Create package `com.algoprep.problem` and the record `Problem(String key, int number, String title, Path statement, Optional<Path> notes)` (Plan section 5.1).
- [x] Write `ProblemNames`: split a base key into its number and slug, and produce the display name (`0001_two-sum` becomes `1 - Two Sum`: strip leading zeros, replace hyphens and underscores with spaces, title-case) (Plan section 5.2).
- [x] Unit tests for `ProblemNames`: leading zeros, all-zero number (`0000_x`), multiple hyphens, underscores inside the slug, mixed-case slug, a single-word slug, and a key that does not start with digits.

**Technical Notes:**
The base key is the identity and the display name is only for show, so the key is never rebuilt from the display name. `Problem.title` is the formatted name without the number (`Two Sum`). The number is an `int`, so a digit run too long to parse is treated as a non-matching name (see Open Question 1).

### Phase 2: ProblemScanner

**Status:** Verified

- [x] Write `ProblemScanner`, a pure class with no Swing: given a directory, return the problems sorted by number, then key.
- [x] Scan regular files directly inside PROBLEMS only (no recursion). A problem exists when there is a file ending in `_problem.md`. Removing that suffix gives the base key.
- [x] Resolve the supplied notes by the **full base key**: `<key>_notes.md`. Never match by the numeric ID or a partial prefix.
- [x] Match names case-insensitively, and keep the real file names found on disk in the paths.
- [x] Duplicate numbers with different slugs become separate rows.
- [x] Ignore solutions, test cases and unrelated files. Log (do not show) names that look like statements but do not match the convention.
- [x] A missing, unreadable or non-directory path throws a clear exception, or returns a failure the caller can show. It must not return an empty list that looks like success (Plan section 5.3).
- [x] Unit tests (with `@TempDir`): grouping by full key, missing notes, notes present, non-matching names, similar-prefix names (`0001_two-sum` and `0001_two-sum-ii` stay separate), duplicate numbers, case differences (`_Problem.MD`, `_NOTES.md`), sort order (numeric, not text), subdirectories ignored, solution and test-case files ignored, empty directory, missing directory, and a path that is a file.

**Technical Notes:**
The scanner runs on the calling thread. A flat directory of a few hundred files takes milliseconds (Plan section 5.3). Use a lowercase-name lookup for notes, but never let it relax the full-key rule. The `_problem.md` and `_notes.md` suffixes are the only files the scanner reads names of. Solutions and test cases are never opened or listed, so there is nothing to leak later.

### Phase 3: Selection Model and Catalog

**Status:** Verified

- [x] Write `ProblemCatalog`: holds the current problem list and the last scan error, scans a directory on demand, and has `addListener(Runnable)`. If a scan fails, it keeps the previous list and records the error for the status line (Plan section 5.3).
- [x] Write `SelectedProblemModel` with `addListener(Runnable)`, in the style of the other models. It holds the selected base key and, when found, the matching `Problem`, plus an "unavailable" flag.
- [x] Selection rules (Plan section 5.3): after a refresh, find the selected problem again by base key. If its statement is gone, mark it **unavailable** rather than selecting something else. Changing the PROBLEMS directory **clears** the selection.
- [x] Persist the selected base key to `SettingsStore.setLastSelectedKey`. At startup, restore it only if the first scan finds it. Otherwise start with no selection.
- [x] Unit tests: select, clear, refresh keeps the selection by key, refresh with the statement removed gives unavailable (not a different problem), a scan failure keeps the old list, directory change clears the selection, listeners fire on real changes only, and restore-at-startup finds or skips the saved key.

**Technical Notes:**
The Problems tab sets the selection, and the MAIN tab observes it. DC3's display pane and DC4's Upload button will observe the same model, so expose `Optional<Problem> current()`, `Optional<String> selectedKey()` and `boolean isUnavailable()`. `ProblemCatalog` is not in the Plan's package list. It is added so that the refresh and error logic is testable without Swing, and it lives in `com.algoprep.problem` (see Open Question 2).

### Phase 4: PROBLEMS Setting

**Status:** Verified

- [x] Add a **PROBLEMS directory** section to `SettingsPanel`: a path label and `Browse...` (directory chooser), modeled on ChatStory's `ConfigurationPanel` (Plan section 6.3).
- [x] The setting writes to `SettingsStore.setProblemsDir`. A change triggers a rescan and clears the selection.
- [x] A saved path that no longer exists stays visible with a warning, and is not silently replaced (Plan section 6.3).
- [x] The scan error, if any, goes to the status line through `StatusReporter`.
- [x] Scan at startup, when the setting changes, and on Refresh.

**Technical Notes:**
`SettingsStore` already has `problemsDir` and `lastSelectedKey` fields, so the file format does not change. Keep `AppFrame` thin: construct the catalog and selection model in `Main` (like the theme model), and pass them in.

### Phase 5: Problems Tab and MAIN Problem Panel

**Status:** Verified

- [x] Write `ui/ProblemsPanel`: a filter box, a `JList` of `number - name` rows, and a **Refresh** button. Replace the placeholder tab in `AppFrame`.
- [x] The filter is case-insensitive and matches the number or any part of the name, and updates as the user types. The selected row stays selected when it is still visible.
- [x] Double-click or Enter selects the problem. A single click only highlights it. Show only the number and name, never individual files (Plan section 6.2).
- [x] The list marks the currently selected problem, so the user can see which one is active after a click elsewhere.
- [x] Show a short empty-state message: no PROBLEMS directory set, directory empty, or no filter matches.
- [x] Update the MAIN tab's Problem panel to show `Selected: 1 - Two Sum`, `Selected: 1 - Two Sum (unavailable)`, or `Selected: (none)`. The `Last attached` part is added in DC4.
- [x] Keep the panel usable with a few hundred problems (list model updates must not rebuild the whole UI on every keystroke).

**Technical Notes:**
The list holds `Problem` objects with a `toString` or renderer that uses `ProblemNames`. Selection is by base key, so a refresh that reorders or removes rows must not move the selection to a different problem. UI updates happen through `UiThread`.

### Phase 6: Manual Verification and Closeout

**Status:** Verified

- [x] `gradlew.bat build` is green. Record the test counts.
- [x] Make a PROBLEMS directory with several problems, including: one with notes and one without, two with similar names (`two-sum` and `two-sum-ii`), a number gap, a duplicate number with a different slug, mixed-case file names, plus solution and test-case files and an unrelated file. Choose it in Settings and confirm only the right problems appear, in numeric order.
- [x] Confirm solutions, test cases and unrelated files never appear anywhere in the UI.
- [x] Filter by number and by name. Double-click and Enter both select. A single click only highlights.
- [x] Select a problem, restart, and confirm it is restored. Delete its statement file while the app runs, press Refresh, and confirm it shows as unavailable and nothing else is selected.
- [x] Change the PROBLEMS directory and confirm the selection clears. Point the setting at a folder, then delete or rename the folder and confirm the path stays visible with a warning and the error is reported.
- [x] Add a new problem file while the app runs and confirm Refresh picks it up.
- [x] With ChatGPT still loading or signed out, confirm the Problems tab and Settings work normally.
- [x] Test with a large directory (a few hundred problems) and confirm the tab stays responsive.
- [x] Complete the Completion Summary and report **Work Complete**. Only the user marks anything **Verified**.

**Technical Notes:**
A script or a few copy commands can generate the large test directory. It should be created outside the repository.

---

## Decisions

The four open questions from planning were resolved by adopting the recommendations:

1. **Non-numeric statement names** (for example `two-sum_problem.md`) do not match the convention. They are not listed and are logged.
2. **`ProblemCatalog`** is a separate class in `com.algoprep.problem`, so the refresh and error rules are testable without Swing.
3. **The filter** matches the displayed number and name, plus zero-padded numbers (`1` and `0001` both find problem 1). It does not match the base key or file names.
4. **Refresh** stays an explicit button. There is no focus refresh or file watcher.

---

## Implementation Results

Phases 1-5 are implemented and the full build passes with 135 tests and 0 failures (56 new). Phase 6 (the manual check) was closed by the user, who approved Verified without a step-by-step record of each manual item.

- **Phase 1:** `Problem` (with `displayName()`), `ProblemNames` (`parseKey`, `titleOf`, `display`, `displayForKey`). A number too large for an `int` does not parse. `ProblemNamesTest` has 9 tests.
- **Phase 2:** `ProblemScanner.scan(dir)` and `scan(dir, log)`. It lists regular files directly in the directory, finds companions by the full base key only, matches names case-insensitively while keeping the real file names, sorts by number then key, and logs statement-like names that do not match. A missing or non-directory path throws (`NoSuchFileException`, `NotDirectoryException`) rather than returning an empty list. `ProblemScannerTest` has 14 tests.
- **Phase 3:**
  - **`ProblemCatalog`:** `setDirectory` starts from an empty list, so a bad new directory never shows the old directory's problems. `refresh` keeps the previous list if the scan fails.
  - **`SelectedProblemModel`:** Holds the key, with `reconcile` marking it unavailable when the statement is gone. `current()` is empty while unavailable, and `describe()` gives the MAIN tab text.
  - **`ProblemWorkspace`:** A facade, added beyond the plan's list, that ties the catalog, selection and `SettingsStore` together.
    - **Directory handling:** Choosing a different directory clears the selection, and choosing the same one only rescans.
    - **Saved selection:** It is saved on every change and restored at startup only if the scan finds it. A stale saved key is left in the settings until the user selects something else.
  - **Tests:** `ProblemCatalogTest` (9), `SelectedProblemModelTest` (9), `ProblemWorkspaceTest` (9).
- **Phase 4:** The Settings tab has a PROBLEMS directory section with a path label, a warning when the saved path is not a directory, and Browse. Scan errors go to the status line.
- **Phase 5:** `ui/ProblemsPanel` (filter, list, Refresh, empty-state and error messages, bold "(selected)" marker on the selected row, double-click or Enter to select) replaces the placeholder tab. The MAIN Problem panel shows `Selected: 1 - Two Sum`, `(unavailable)` or `(none)`. `ProblemFilter` holds the filter rule (6 tests). `Main` creates the `ProblemWorkspace`, and `AppFrame` passes it to the panels.
- **Not yet confirmed on screen:** the Problems tab, the Settings section and the MAIN panel have been compiled but not run.
- **Test data:** For Phase 6, a small directory with the awkward cases (similar names, duplicate number, mixed case, a solution, test cases, an unnumbered statement, a nested folder) and a 400-problem directory were generated outside the repository, in the session scratchpad: `...\scratchpad\problems-sample` and `...\scratchpad\problems-large`.

---

## Notes and Risks

- The scanner is the foundation for DC3 and DC4. Keeping it pure and thoroughly tested here avoids rework later.
- **Risk: wrong companion matching.** A partial-prefix match would attach notes from `two-sum-ii` to `two-sum`. Mitigation: full-key matching only, with a dedicated test.
- **Risk: stale selection.** Files can change outside AlgoPrep. Mitigation: selection is by base key and a missing statement shows as unavailable instead of silently selecting something else.
- The scan runs on the calling thread. If a very large or network directory ever makes that slow, a background scan can be added later. It is not needed now (Plan section 5.3).
- DC1 left `chatStoryRef/` in place, and it is still read-only reference. Make changes only in the real source tree.
- Git is managed by the user. Agents do not run git commands in this project.
- Creating this document does not authorize implementation. Work starts only on the user's explicit instruction.

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** 2026-10-06
**Phases Completed:** All (1-6)
**Work Deferred:** None. Displaying problem content in the left pane is DC3, as planned.

**Accomplishments:**
- `Problem`, `ProblemNames`, `ProblemScanner`, `ProblemFilter` (pure, unit tested)
- `ProblemCatalog`, `SelectedProblemModel` and `ProblemWorkspace` (scan, selection and saved-setting rules)
- PROBLEMS directory setting with a missing-path warning
- Problems tab (filter, list, Refresh, double-click or Enter to select) and the MAIN `Selected:` line

**Metrics:**
- Files: 8 new main classes (7 in `problem/`, plus `ProblemsPanel`), 4 changed (`SettingsPanel`, `MainPanel`, `AppFrame`, `Main`), 6 new test classes
- Tests passing: 135, 0 failures (56 new)

**Lessons / Notes:**
Verified by the user on 2026-10-06. The left display pane is still a placeholder, so the selection is only visible on the MAIN tab and in the Problems list until DC3. Added `ProblemWorkspace` beyond the plan so the directory-change and restore rules are testable. A stale saved selection key is kept in the settings until the user selects something else.
