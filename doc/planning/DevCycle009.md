# DevCycle 009: Studied Prompt to Clipboard

**Status:** Work Complete
**Start Date:** 2026-10-07
**Target Completion:** TBD
**Focus:** After the user presses Studied, offer to copy a ready-made `Studied leetcode problem #X` prompt to the clipboard.

This cycle builds on DevCycle 008, which is Verified, and on the shared `StudiedController` from DevCycles 6 to 8.

---

## Goal

Marking a problem as studied is usually followed by telling ChatGPT about it. The user wants that sentence ready to paste. When **Studied** is pressed, a dialog asks `Copy Studied prompt to clipboard?`. If the answer is Yes, the text `Studied leetcode problem #X` is placed on the system clipboard, where `X` is the problem's number.

## Desired Outcome

- Pressing **Studied** on any tab (MAIN, Problems, study list) records today's date as before, then shows a Yes/No dialog: `Copy Studied prompt to clipboard?`.
- **Yes** puts `Studied leetcode problem #X` on the clipboard, for example `Studied leetcode problem #1` for `0001_two-sum` (the number without zero padding), and says so in the status line.
- **No**, or closing the dialog, copies nothing. The studied date is recorded either way.
- The dialog is shared by every Studied button, like the Clear confirmation in DC 8.
- The new logic is unit tested, and the full build passes.

**Not in this cycle:** sending the prompt to ChatGPT automatically, a "don't ask again" option or a setting to turn the dialog off (Open Question 2), changing the wording by setting, and any change to Clear Studied Tag or Upload.

---

## Tasks

### Phase 1: The Prompt Text

**Status:** Work Complete

- [x] Add a pure function that builds the prompt from a problem key: `Studied leetcode problem #<number>`, using the number from `ProblemNames.parseKey` with no zero padding (`0001_two-sum` gives `#1`, `0105_notime` gives `#105`).
- [x] If the key cannot be parsed (not `number_slug`), there is no number to put in the prompt, so no dialog is shown and nothing is copied (Open Question 3).
- [x] Unit tests: padded and unpadded keys; a large number; a key that does not parse; the exact text.

**Technical Notes:**
The number already exists on `Problem` and in `ProblemNames.parseKey(String).number()`. The controller works from the selected key (`SelectedProblemModel.selectedKey()`), so parse the key, as `ProblemNames.displayForKey` does, rather than reaching for a `Problem`. Put the function next to the other name helpers, or in a small `StudiedPrompt` class in the `studied` package.

### Phase 2: Ask and Copy After Studied

**Status:** Work Complete

- [x] After `StudiedStore.markStudied` succeeds in `StudiedController.markStudied`, ask `Copy Studied prompt to clipboard?` with **Yes** and **No** (Open Question 1 for the default).
- [x] On **Yes**, copy the Phase 1 text to the system clipboard, and report `Copied "Studied leetcode problem #X" to the clipboard.` through `StatusReporter`.
- [x] On **No** or a closed dialog, copy nothing and add nothing to the status line beyond the existing `Marked ... as studied` message.
- [x] If marking fails (the store already reports the error), do not ask.
- [x] If the clipboard cannot be used (headless, or the system refuses), report a short message in the status line and do not throw.
- [x] Ask in one place, in `StudiedController`, so all Studied buttons behave the same. The dialog is parented to the pressed button's window, as the Clear dialog is.
- [x] Reuse the existing `Confirmer` from DC 8 for the question, and add a small replaceable clipboard writer, so tests never touch the real clipboard or open a real dialog.
- [x] Unit tests: Yes copies the exact text and reports; No copies nothing; the date is recorded in both cases; no question when marking is not allowed or fails; the question text; a clipboard failure is reported and does not stop the studied date; the same behavior from a second bound button. Update the existing tests that press Studied (`StudiedControllerTest`, `StudiedLabelAndButtonsTest`, `StudyListPanelTest`, and any others found) so they pass stubs, because they would otherwise wait on a real dialog.

**Technical Notes:**
`StudiedController` already has a `Confirmer` (`boolean confirm(Component parent, String message)`, default a Yes/No `JOptionPane`) and a constructor that takes one. The Clear dialog uses No as the default button; the Studied question needs its own default (Open Question 1), so either the `Confirmer` gains a way to say which button is the default, or a second small interface is used for this question. Prefer the first, with one implementation of the dialog.
The clipboard is `Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null)`. Wrap it behind a small interface (for example `ClipboardWriter`, with a default and a test stub), and catch `IllegalStateException` and `HeadlessException`.
`trigger()`, the "one trigger for every Studied button" hook, calls `markStudied` and so gets the dialog too. Check that nothing else calls it in a way that would now block.

### Phase 3: Verification and Closeout

**Status:** Work Complete

- [x] `gradlew.bat build` is green. Record the test counts (422 tests, 0 failures).
- [ ] Manual check: press Studied from the MAIN, Problems and study list tabs. **Yes** puts the right text on the clipboard (paste it somewhere to see it), **No** leaves the clipboard unchanged, and the studied date and tag appear in both cases.
- [ ] Manual check: a problem with a zero-padded key shows an unpadded number, and a problem that is already studied can be studied again and asked again.
- [ ] Manual check: the dialog opens over the app window, and pressing Escape acts as No.
- [x] Update `README.md` for the new dialog.
- [ ] Complete the Completion Summary and report **Work Complete**. Only the user marks anything **Verified**.

---

## Implementation Results

All phases are implemented and `gradlew.bat build` passes with 422 tests and 0 failures (7 more than before). The manual checks in Phase 3 have not been done: nothing has been run in the real window.

- **Phase 1:** `studied/StudiedPrompt.forKey(key)` returns `Studied leetcode problem #<number>` (unpadded), or empty when the key has no number. `StudiedPromptTest` (2).
- **Phase 2:** `StudiedController.markStudied` records the date, then asks `Copy Studied prompt to clipboard?` through the `Confirmer`. `Confirmer` gained a default method with a "default answer is Yes" flag, which stubs ignore. The real dialog is now `DialogConfirmer` (Clear keeps No as its default, the copy question defaults to Yes, Escape counts as No). Yes copies through the new `ClipboardWriter` (the system clipboard, replaceable in tests) and reports `Copied "..." to the clipboard.`. A failure is reported as `Could not copy to the clipboard: ...` and the date is kept. No question is asked when the save fails or the key has no number. The `trigger()` hook asks too. Tests: 5 new in `StudiedControllerTest`. Existing tests that press Studied now pass a stub confirmer and a no-op clipboard (`StudiedControllerTest`, `StudiedLabelAndButtonsTest`, `StudyListPanelTest`).
- README updated.

---

## Open Questions

**Resolved:** all five recommendations were adopted (Yes is the default; no opt-out; skip when the key has no number; unpadded number; the same dialog for an already-studied problem). They are kept for the record.

1. **Which answer should be the default?**
   Recommendation: **Yes**. Copying is harmless and non-destructive, and the user will mostly want it, so pressing Enter should accept. (The Clear dialog defaults to No because it deletes something.)

2. **Should there be a way to stop the question appearing?**
   Recommendation: **Not in this cycle.** The user asked for the dialog every time. If it becomes annoying, a setting or a "don't ask again" checkbox is a small later change.

3. **What if the selected problem's key has no number?**
   Recommendation: **Skip the dialog and copy nothing.** Problems come from `number_slug` names, so this should not happen, and an empty or wrong number in the prompt would be worse than none.

4. **Should the text use the number as written (`0001`) or without padding?**
   Recommendation: **Without padding**, `#1`, matching how the app displays problems (`1 - Two Sum`) and how LeetCode numbers them.

5. **Should the dialog appear when Studied is pressed on an already-studied problem (updating its date)?**
   Recommendation: **Yes**, the same as the first time, since the user may want the prompt again. It keeps the behavior simple and predictable.

---

## Notes and Risks

- **Risk: a modal dialog blocks tests.** Mitigation: the existing injectable `Confirmer` and a replaceable clipboard writer, and updating every existing test that presses Studied.
- **Risk: the clipboard is unavailable** (headless or locked by another program). Mitigation: catch the failure, report it in the status line, and keep the studied date.
- **Risk: an extra click on every Studied press becomes annoying.** Mitigation: Yes as the default (Open Question 1), and a possible later setting (Open Question 2).
- **Risk: the dialog hides the Studied banner and tag update.** Mitigation: record the date first, so the tag and banner are already updated behind the dialog.
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
