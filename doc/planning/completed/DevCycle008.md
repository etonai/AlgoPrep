# DevCycle 008: Selected Problem Visibility and Clear Studied Confirmation

**Status:** Verified
**Start Date:** 2026-10-07
**Target Completion:** TBD
**Focus:** Make the selected problem easy to find when the Problems and study list tabs are opened, and ask "Are you sure?" before Clear Studied Tag removes a studied date.

This cycle builds on DevCycle 007, which is Verified. It is two small refinements. The first was changed after Phase 1: the larger-row emphasis was tried, rejected by the user and undone in Phase 3, and replaced by highlighting the selected row on tab open in Phase 4.

---

## Goal

Two things came up while using the app:

1. In a long list it is hard to see which problem is the selected one. Today it is only bold and tagged `(selected)`. The first attempt (a larger row, Phase 1) was undone. Now, opening the Problems tab or the study list tab highlights the selected problem, as if the user had clicked it.
2. **Clear Studied Tag** removes a studied date with one click and no way back. A mis-click loses a date, so the button should ask first.

## Desired Outcome

- Whenever the Problems tab or the study list tab is opened, the selected problem's row, if there is one, is highlighted (as if clicked) and scrolled into view. The selected row looks as it did before this cycle (bold, with the `(selected)` tag).
- Pressing **Clear Studied Tag**, on any tab where it appears, first shows an "Are you sure?" dialog. **Yes** clears the tag as today. **No** (or closing the dialog) changes nothing.
- The new logic is unit tested, and the full build passes.

**Not in this cycle:** a confirmation for **Studied** or any other button, a "don't ask again" option, changing the font size of the display window, and any change to which rows are tagged or how they are matched.

---

## Tasks

### Phase 1: Selected Row Emphasis (superseded, undone in Phase 3)

**Status:** Verified (later reverted)

*The user did not like this result. It was removed in Phase 3. The tasks are kept for the record.*

- [x] In the Problems tab, draw the selected problem's row (the one `ProblemRowText` tags `(selected)`) at 150% of the list's normal font size, bold as now.
- [x] Leave a half line of space above and below that row. A "line" is the normal row height, so the extra space is half of it on each side. Other rows keep their current height.
- [x] Do the same in the study list tab, with the same size and spacing. NOT FOUND rows can never be the selected row, so they are unaffected.
- [x] Share the rule between the two tabs instead of copying it. `ProblemsPanel.RowRenderer` and `StudyListPanel.RowRenderer` are near duplicates, so put the size factor, the spacing and the font/border logic in one small shared helper (or a shared renderer base) that both use.
- [x] The selected row must stay readable and well formed in both themes, when it is also the highlighted row, and when the list is narrow (long text scrolls sideways or is shown in the tooltip, as now).
- [x] The extra row height must not break scrolling or the highlight, and the selected row must still be fully visible when it is scrolled into view.
- [x] Unit tests: the selected row's font is 1.5 times the normal size and bold; other rows are unchanged; the spacing above and below is half the normal row height; the same on both tabs; a row that stops being selected returns to normal; the study list's NOT FOUND and found-but-unselected rows are unchanged.

**Technical Notes:**
Both tabs use a `DefaultListCellRenderer` subclass whose `getListCellRendererComponent` already sets the font bold for the selected row (`ProblemsPanel.java` around line 160, `StudyListPanel.java` around line 208). The new size is `font.deriveFont(Font.BOLD, font.getSize2D() * 1.5f)`. The base font comes from the list, so it follows the look-and-feel and any theme change, and the factor is applied to that, never to a stored size.
Half a line of space can be an empty border on the renderer, with top and bottom insets of half the plain row height (`list.getFontMetrics(plainFont).getHeight() / 2`), so it scales with the font. Because the renderer is a `JLabel`, the border must be set (and reset to the default for other rows) on every call, since the renderer component is reused.
Check whether either list sets `setFixedCellHeight` or `setPrototypeCellValue`. A fixed height would clip the taller row and would need to be removed so heights come from the renderer. Re-check `NativeThemeApplier` too: it must not override the renderer's font or border.

### Phase 2: Confirm Before Clearing the Studied Tag

**Status:** Verified

- [x] When **Clear Studied Tag** is pressed, ask "Are you sure?" before doing anything. The dialog names the problem (for example, `Clear the studied tag for 1 - Two Sum?`) and offers **Yes** and **No**, with **No** as the default.
- [x] Only **Yes** calls `StudiedStore.clear`. **No**, closing the dialog and pressing Escape leave the date and the status line unchanged.
- [x] The question is asked in one place, in `StudiedController`, so it applies to every Clear button (MAIN, Problems and study list tabs) with no per-tab code.
- [x] A disabled Clear button still cannot be pressed, and no dialog shows when there is nothing to clear.
- [x] The dialog is parented to the window the button is in, so it appears over the app and blocks it until answered.
- [x] Unit tests: **Yes** clears and reports; **No** does nothing (date kept, no status message); the question names the selected problem; no question is asked when Clear is not allowed; all bound Clear buttons share the behavior. Existing `StudiedControllerTest` and `StudiedLabelAndButtonsTest` cases that press Clear must be updated, because they would otherwise wait on a real dialog.

**Technical Notes:**
`StudiedController.bindClear` attaches `e -> clear()`, and `clear()` calls `store.clear(key)` directly. Add the confirmation inside `clear()`, before the store call.
The dialog must be replaceable in tests, as a modal `JOptionPane` would block a headless test. Give `StudiedController` a small `Confirmer` (for example `boolean confirm(Component parent, String message)`) that defaults to `JOptionPane.showConfirmDialog` with `YES_NO_OPTION`, `WARNING_MESSAGE` and No preselected, and let tests pass a stub through a second constructor. Take the parent from the action event's source button (`SwingUtilities.getWindowAncestor`). Keep the existing constructor so `AppFrame` and `MainPanel` need no change.

### Phase 3: Undo the Selected Row Emphasis

**Status:** Verified

- [x] Remove `ui/SelectedRowStyle` and `SelectedRowStyleTest`.
- [x] Restore both `RowRenderer`s to their earlier behavior: the selected problem's row is bold at the normal size, with no extra border.
- [x] Take the description of the larger row out of `README.md`.

**Technical Notes:**
Only Phase 1's code is removed. Phase 2 (the confirmation) is unchanged.

### Phase 4: Highlight the Selected Problem When a Tab Is Opened

**Status:** Verified

- [x] When the Problems tab is opened, highlight the row of the selected problem (as if the user clicked it), and scroll it into view.
- [x] Do the same for the study list tab.
- [x] If no problem is selected, or the selected problem is not in the list (filtered out, or not on the study list), leave the highlight as it is.
- [x] Highlighting only highlights. It does not change the selection or any studied or upload state.
- [x] Unit tests for both tabs: opening the tab with a selected problem highlights its row; with nothing selected, nothing is highlighted and nothing is selected.

**Technical Notes:**
Both panels already have a hierarchy listener that reacts to the tab becoming visible (`SHOWING_CHANGED`), where they reload the studied file. After that, they call a new `highlightSelected()`, queued with `UiThread.run` so it runs after the list has been rebuilt by those reloads. It finds the row whose problem is the selected one (`SelectedProblemModel.selectedKey`), then calls `setSelectedIndex` and `ensureIndexIsVisible` on the list. The study list matches the row through its resolved problem, so NOT FOUND rows are never highlighted this way. Tests show the panel in a real `JFrame` tab (`StudyListPanelTest`, new `ProblemsPanelHighlightTest`).

### Phase 5: Verification and Closeout

**Status:** Verified

- [x] `gradlew.bat build` is green. Record the test counts (415 tests, 0 failures).
- [x] Manual check, Problems tab: select a problem (double-click), go to another tab and back, and confirm the row is highlighted and in view, including for a problem far down a long list. With nothing selected, nothing is highlighted.
- [x] Manual check, study list tab: the same, plus selecting from the Problems tab and then opening the study list tab, and a selected problem that is not on the list or is filtered out.
- [x] Both themes: the highlight is readable.
- [x] Manual check, Clear Studied Tag from the MAIN, Problems and study list tabs: **No** keeps the date and tag, **Yes** removes them, and the dialog names the right problem.
- [x] Update `README.md` if it describes the selected-row look or the Clear button.
- [x] Complete the Completion Summary and report **Work Complete**. Only the user marks anything **Verified**.

---

## Implementation Results

All phases are implemented and `gradlew.bat build` passes with 415 tests and 0 failures. Phase 5, the manual check, was closed by the user, who approved Verified after trying the cycle, without a step-by-step record of each manual item.

- **Phase 1 (undone):** a shared `SelectedRowStyle` made the selected row bold at 150% with half a line of space above and below. The user did not like it.
- **Phase 2:** `StudiedController.clear` asks `Clear the studied tag for <name>?` through a `Confirmer` before calling the store. The default shows a Yes/No dialog titled "Are you sure?" with No preselected, parented to the pressed button's window. A second constructor takes a stub confirmer. Existing tests that press Clear now pass an always-yes confirmer. New tests in `StudiedControllerTest`: the question text, No keeps the date and reports nothing, no question when nothing can be cleared.
- **Phase 3:** `SelectedRowStyle` and its test are deleted, and both renderers are back to plain bold. The README sentence is replaced.
- **Phase 4:** `ProblemsPanel.highlightSelected()` and `StudyListPanel.highlightSelected()`, called from the tab-shown listener. Tests: 2 in `StudyListPanelTest`, 2 in the new `ProblemsPanelHighlightTest`.
- README updated.

---

## Open Questions

1. **(Obsolete after Phase 3.) What does "half a line" measure?**
   Recommendation: **Half of a normal row's height** (the plain font's line height), above and below the selected row. It scales with the font and the look-and-feel, and a 150% row ends up about 2.5 lines tall in total on a normal list. If you meant half of the *enlarged* line, it is a one-number change.

2. **(Obsolete after Phase 3.) Should the 150% apply to the whole row text, including the `(selected)` and `(STUDIED ...)` tags?**
   Recommendation: **Yes, the whole row**, as it is one label. The text is longer when enlarged, so it scrolls sideways sooner and the tooltip still shows the full text.

3. **Wording and default of the confirmation.**
   Recommendation: `Clear the studied tag for <name>?` with **Yes** and **No** buttons and **No** as the default, so pressing Enter by habit does not delete a date. If you want a different message or a plain "Are you sure?" title, that is text only.

4. **Should the confirmation also cover Studied when it overwrites an existing date?**
   Recommendation: **No.** It is outside this request, and updating a date to today is much less costly than removing one. It can be a later cycle if you want it.

---

## Notes and Risks

- **Risk: the highlight runs before the list is rebuilt on tab open**, so it finds nothing. Mitigation: it is queued after the reloads, and tests open the tab in a real frame.
- **Risk: highlighting could be mistaken for selecting.** Mitigation: it only sets the list highlight, and a test checks the selection is unchanged.
- **Risk: a modal dialog blocks tests.** Mitigation: the injectable confirmer in Phase 2, and the existing Clear tests updated to use it.
- **Risk: the dialog is attached to the wrong window.** Mitigation: take the parent from the button that was pressed.
- `chatStoryRef/` is still read-only reference.
- Git is managed by the user. Agents do not run git commands in this project.
- Creating this document does not authorize implementation. Work starts only on the user's explicit instruction.

---

## Completion Summary

**Completion Date:** 2026-10-07
**Phases Completed:** All (1-5). Phase 1 was undone by Phase 3.
**Work Deferred:** None. A confirmation for Studied remains out of scope.

**Accomplishments:**
- Clear Studied Tag asks "Clear the studied tag for <name>?" (Yes/No, No by default) on every tab that has it
- Opening the Problems tab or the study list tab highlights and scrolls to the selected problem
- The larger-row emphasis was tried, rejected and fully removed

**Metrics:**
- Files: 2 new test classes' worth of tests (`ProblemsPanelHighlightTest`, additions to `StudiedControllerTest` and `StudyListPanelTest`), 3 main classes changed (`StudiedController`, `ProblemsPanel`, `StudyListPanel`)
- Tests passing: 415, 0 failures (6 new over DC 7's 409)

**Lessons / Notes:**
Verified by the user on 2026-10-07. The row-emphasis idea was built and then replaced after the user saw it; highlighting on tab open reused the existing tab-shown listener. The confirmation uses an injectable `Confirmer` so tests never open a modal dialog.
