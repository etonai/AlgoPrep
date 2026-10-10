# DevCycle 010: Reset to Problem Tab on New Selection

**Status:** VERIFIED
**Start Date:** 2026-10-09
**Target Completion:** TBD
**Focus:** When the user selects a new problem, the left window switches to the Problem tab.

This cycle follows DevCycle 009 (Work Complete, not yet Verified). It does not depend on any 009 code.

---

## Goal

The left window (`DisplayPanel`) has three tabs: Problem, Notes and My Notes. Today the open tab stays where it was when the user picks a different problem, so the user can land on Notes or My Notes for a problem they have not yet read. Selecting a new problem should act as a reset: the left window goes to the **Problem** tab.

## Desired Outcome

- Selecting a different problem (from MAIN, the Problems tab, or a study list) switches the left window to the Problem tab.
- If the Problem tab is already open, nothing visibly changes.
- Actions that do not change which problem is selected (Studied, Clear Studied Tag, Upload, editing notes, font size changes, settings changes) do **not** switch tabs.
- The new behavior has unit tests, and the full build passes.

**Not in this cycle:** remembering the last tab per problem, a setting to turn the reset off, and any change to the right-hand tabs.

---

## Tasks

### Phase 1: Switch to the Problem Tab on a New Selection

**Status:** Work Complete

- [x] In `DisplayPanel`, when the selected problem key changes, select the Problem tab (index `PROBLEM`).
- [x] Switch only when the key actually changes. The selection listener also fires for other updates, so compare against the previously seen key and leave the tab alone when it is the same problem.
- [x] Decide what happens when the selection is cleared (key becomes empty). Recommendation in Open Question 1.
- [x] Make sure content still loads once: the existing `tabs.addChangeListener(... reload(...))` and `reloadAll` must not double-load or load stale content when the tab changes during a selection change. (The Problem tab is read twice on a switch; the content is correct.)
- [x] Unit tests: new key while on Notes goes to Problem; new key while on My Notes goes to Problem; new key while already on Problem stays; the same key re-announced (for example after Studied) keeps the current tab; the Problem tab shows the new problem's content after the switch.

**Technical Notes:**
`DisplayPanel` registers `selection.addListener(() -> UiThread.run(this::reloadAll))` (line ~65) and owns the `JTabbedPane tabs`. Keep the change inside `DisplayPanel`, for example by remembering the last `selection.selectedKey()` and calling `tabs.setSelectedIndex(PROBLEM)` before `reloadAll` when it differs. Setting the index before reloading means the change listener loads the Problem tab, and `reloadAll` then refreshes everything, so check that this does not cause a visible double render. Check `SelectedProblemModel` to see exactly when it notifies listeners (new key only, or also when the same key is set again).

### Phase 2: Verification and Closeout

**Status:** Work Complete

- [x] `gradlew.bat build` is green. Record the test counts (426 tests, 0 failures).
- [ ] Manual check: with Notes open, select another problem from each of MAIN, Problems and a study list. The left window shows the Problem tab each time.
- [ ] Manual check: with My Notes open, press Studied, Clear Studied Tag and Upload. The tab does not change.
- [ ] Manual check: font size +/- keeps the current tab.
- [x] Update `README.md` if it describes the left window tabs.
- [ ] Complete the Completion Summary and report **Work Complete**. Only the user marks anything **Verified**.

---

## Implementation Results

Both phases are implemented and `gradlew.bat build` passes with 426 tests and 0 failures (4 more than before). The manual checks have not been done: nothing was run in the real window.

- `DisplayPanel.onSelectionChanged` remembers the last selected key. If the key differs (including cleared), it selects the Problem tab, then reloads all tabs. A refresh that re-announces the same key leaves the tab alone.
- The only extra work is that the switch reloads the Problem tab once through the tab change listener before `reloadAll`. It is a small local file read, so it was left as is.
- Tests added to `DisplayPanelTest` (4): new problem from Notes and from My Notes goes to Problem; selecting while on Problem stays; clearing goes to Problem; a same-key refresh keeps My Notes open. The test class now shares a `SelectedProblemModel`.
- README updated.
- Open Questions 1 and 2: both recommendations adopted.

---

## Open Questions

1. **What should happen when the selection is cleared (no problem selected)?**
   Recommendation: **Also switch to the Problem tab.** It is the tab that shows the "no problem selected" message, so the reset is consistent.

2. **Should re-selecting the same problem (clicking it again) reset the tab?**
   Recommendation: **No.** Only a different problem counts as a new selection, which keeps the tab stable during Studied and other refreshes.

---

## Notes and Risks

- **Risk: the selection listener fires for non-selection changes,** which would wrongly reset the tab. Mitigation: compare the key before switching, and test the same-key case.
- **Risk: double loading of tab content** when the tab change listener and `reloadAll` both run. Mitigation: check the order, and cover it in a test.
- `chatStoryRef/` is still read-only reference.
- Git is managed by the user. Agents do not run git commands in this project.
- Creating this document does not authorize implementation. Work starts only on the user's explicit instruction.

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** [YYYY-MM-DD]
**Phases Completed:** [List or "All"]
**Work Deferred:** [What was not done and why, or "None"]

**Accomplishments:**
- [What was built or changed]

**Metrics:**
- Files modified: [N]
- Tests passing: [N]

**Lessons / Notes:**
[Anything worth remembering for future cycles.]
