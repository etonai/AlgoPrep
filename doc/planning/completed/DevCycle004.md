# DevCycle 004: Upload

**Status:** Verified
**Start Date:** TBD
**Target Completion:** TBD
**Focus:** Stage the selected problem's files and open ChatGPT's file dialog, so the user can attach them in one step.

Design source: `doc/planning/AlgoPrepPlan-Claude02.md` (the "Plan"), mainly sections 6.1, 6.3, 8, 10, 13 and Appendix A.8. Section numbers below refer to it. This cycle builds on DevCycle 003 (display and HOME), which is Verified.

---

## Goal

Add the **Upload** button to the MAIN tab. For the selected problem, AlgoPrep chooses exactly which files to attach (the statement, the supplied notes if present, and the user's saved notes from HOME if present), copies them into a staging folder it owns, and sends Ctrl+U to the embedded browser so ChatGPT's file dialog opens. The user selects all files in the dialog (Ctrl+A) and confirms, then adds a message if wanted and presses Send in ChatGPT. AlgoPrep stops there (Plan section 8.1 and the "After UPLOAD" decision).

The main design concern is **staging safety**. ChatStory's `clearStaging()` deletes every regular file in a folder that can be set to anything, so setting it to HOME or PROBLEMS would delete originals. AlgoPrep stages into its own subfolder, rejects overlapping folders, and deletes only inside that subfolder (Plan section 8.2). Solutions and test cases are never uploaded.

## Desired Outcome

- With a problem selected, **Upload** stages exactly that problem's files and opens the ChatGPT file dialog. The staging folder holds only that problem's files, so Ctrl+A in the dialog selects the right set.
- The MAIN tab's **Problem Files** panel shows exactly which files Upload will attach, for example `0001_two-sum_problem.md, 0001_two-sum_notes.md, 0001_two-sum_AlgoPrepNotes.md`. Notes that do not exist are simply left out.
- If any existing file cannot be read or copied, the upload is aborted with an error. A partial set is never offered.
- The Problem panel shows the advisory `Last attached: ...` next to the selected problem, and clears it when the browser reloads or navigates.
- The Settings tab has a **Staging directory** setting (default `%LOCALAPPDATA%\AlgoPrep\upload-staging`). A root that equals, contains or is inside PROBLEMS or HOME is rejected.
- Status messages are honest: files copied is `N file(s) staged. Select them in the ChatGPT file dialog.`, and nothing claims the files were attached. After an uncertain result, nothing is retried automatically.
- `UploadManifest` and `StagingFolder` have the unit tests listed in Plan section 13, and the full build passes.

**Not in this cycle:** automatic file selection in the dialog (an optional later spike, Plan section 8.3), uploading solutions or test cases, the Ctrl+Shift+U shortcut and other shortcuts (DC5), a statement-only upload mode.

---

## Tasks

### Phase 1: UploadManifest

**Status:** Verified

- [x] Create package `com.algoprep.upload` and write a pure `UploadManifest` (no Swing): given the selected `Problem` and the HOME setting, return the ordered list of files to attach (Plan section 8.1, step 1).
- [x] Always include the statement. Include the supplied notes only if the file exists. Include the HOME notes (`<key>_AlgoPrepNotes.md`, found with `HomeNotes`) only if HOME is set, is a directory, and the file exists.
- [x] Never include the solution, test cases, or any other file.
- [x] Order: statement, supplied notes, HOME notes. Expose the file names for the summary line.
- [x] A file that exists but is not readable is reported as a problem (so the upload aborts), not silently dropped. A file that does not exist is left out.
- [x] A HOME that cannot be listed gives a clear problem, not an empty result that looks like "no notes".
- [x] Unit tests (with `@TempDir`): statement only; with supplied notes; with HOME notes; all three; HOME unset, HOME missing, HOME notes missing; similar-key HOME notes not included (`two-sum-ii` for `two-sum`); case differences in the HOME file name; solution and test-case files next to the statement never included; the three names are always distinct; an unreadable file is reported; order is stable.

**Technical Notes:**
The supplied notes path comes from the `Problem` record at scan time, so re-check that it still exists. The three suffixes (`_problem.md`, `_notes.md`, `_AlgoPrepNotes.md`) differ, so the staged names cannot collide even if HOME and PROBLEMS are the same folder.

### Phase 2: StagingFolder

**Status:** Verified

- [x] Write a pure `StagingFolder`. The configured staging directory is a **root**, and AlgoPrep stages into its own subfolder `<root>\algoprep-upload\` (Plan section 8.2).
- [x] `validateRoot(root, problemsDir, homeDir)`: reject a root that equals, contains or is inside PROBLEMS or HOME, a root that is an existing file, and a subfolder path that is an existing file. Compare absolute, normalized paths. Return a message the UI can show.
- [x] `stage(files)`: validate, create the subfolder, **clear only the regular files directly inside it** (non-recursive, leaving subdirectories and anything outside alone), then copy each file under its original name.
- [x] Check that every source is a readable regular file **before** touching the staging subfolder, so a bad source leaves the previous staging intact.
- [x] If a copy fails part-way, remove the files copied in that attempt and abort with an error, so a partial set is never left behind (Plan section 8.1, step 2).
- [x] Return the staged paths on success, so the summary and status can use them.
- [x] Unit tests: clears only its own subfolder; leaves unrelated files in the root alone; leaves subdirectories inside the subfolder alone; rejects a root equal to, containing, or inside PROBLEMS or HOME (including different case, trailing separators and `..`); accepts a root beside them; aborts and cleans up on a copy failure; a missing source is rejected before anything is deleted; a re-stage replaces the previous problem's files; staging twice gives exactly the new set; the subfolder being a file is reported.

**Technical Notes:**
Do not reuse ChatStory's `ContextFileStore`: it carries checked-file lists and snapshot support, and its `clearStaging()` is unsafe (Plan sections 3 and 8.2). One fixed subfolder, rather than a new folder per upload, keeps the manual flow easy because the file dialog remembers its last folder. Symlink containment checks are deliberately left out as unnecessary for a personal tool (Plan section 16), so the comparison is on normalized absolute paths only (see Open Question 4).

### Phase 3: UploadService

**Status:** Verified

- [x] Write a pure `UploadService` in `upload/`, in the style of `InstructionsSender`, that runs the sequence from Plan section 8.1 against a `ChatBridge` interface so it is testable with a fake bridge.
- [x] Sequence: no selected or an unavailable problem gives a message; the browser must be able to send (`AppState.isSendEnabled()`), checked **before** `reset()` because a reset forces Ready from anywhere and would hide a loading page; build the manifest; validate the staging root against PROBLEMS and HOME; call `chatBridge.reset()`; stage; call `chatBridge.clickUploadFile()`; report.
- [x] Any failure before `clickUploadFile()` aborts with a specific message and does not open the dialog.
- [x] Success message: `N file(s) staged. Select them in the ChatGPT file dialog.` It never says "attached" (Plan section 6.1).
- [x] Call a callback once the dialog has been requested, so the UI can record the advisory "last attached" problem.
- [x] No automatic retry.
- [x] Unit tests with a fake bridge: order of calls (reset, then click, only after staging succeeded); no call at all when no problem is selected, the problem is unavailable, the browser is not ready, the manifest has a problem, the root is rejected, or staging fails; the success message wording and file count; the callback fires only on success; the original files are untouched.

**Technical Notes:**
`clickUploadFile()` is fire-and-forget: it sets browser focus and presses Ctrl+U from a virtual thread after 200 ms (Plan A.8), so there is no confirmation that the dialog opened. That is why the message and indicator are advisory.

### Phase 4: Staging Directory Setting

**Status:** Verified

- [x] Add a **Staging directory** section to `SettingsPanel` with a path label, `Browse...` and a way to return to the default (Plan section 6.3 and Open Question 3). It shows the effective path: the saved one, or the default `%LOCALAPPDATA%\AlgoPrep\upload-staging`.
- [x] Persist to `SettingsStore.setStagingRoot` (the field already exists). An unset value means the default from `AppConfig`.
- [x] On Browse, validate with `StagingFolder.validateRoot` against the current PROBLEMS and HOME settings. A rejected choice shows an error dialog and does not change the setting.
- [x] Show a warning if the current effective root overlaps PROBLEMS or HOME (they can change after the staging root was chosen). Upload re-validates at the moment of use.
- [x] Pass the `AppConfig` default staging path into the panels from `Main`.

**Technical Notes:**
Reuse the `directoryControls` helper added in DC3 where it fits, extending it for the validation hook and the default button. A rejection reason mentions which of PROBLEMS or HOME it overlaps, so the fix is obvious.

### Phase 5: Problem Files Panel and Last Attached

**Status:** Verified

- [x] In `MainPanel`, replace the disabled placeholder in the **Problem Files** panel with the real **Upload** button and a summary line of the exact files it will attach (Plan section 6.1).
- [x] Keep the summary current: refresh it on selection changes, HOME changes, and the existing slow refresh timer (files can appear or disappear outside AlgoPrep). The full staging folder path is available as a tooltip.
- [x] Disable Upload, with a tooltip giving the reason, when no problem is selected, the selected problem's statement is unavailable, the manifest reports an unreadable file, or the browser cannot send (`AppState.isSendEnabled()`).
- [x] On press, run `UploadService` and send its messages through `StatusReporter`. Disable the button briefly (about 2 seconds) after a press, so a double-click does not open two dialogs.
- [x] Update the Problem panel to `Selected: 2 - Add Two Numbers   Last attached: 1 - Two Sum`. The "last attached" part is advisory, with a tooltip saying it means the files were staged and the dialog was opened, not that the upload was confirmed.
- [x] Clear the "last attached" indicator when `AppState` reports the page loading (reload or navigation), as the Plan says.
- [x] Add a short hint near the button: in the file dialog, press Ctrl+A and then Open.
- [x] Expose `triggerUpload()` on `MainPanel` for the DC5 shortcut, and leave it unwired.

**Technical Notes:**
The existing `MainPanel` already has the refresh timer, the `AppState` listener and the `StatusReporter` wiring used by Send Instructions. Follow that pattern. The panel needs the `SelectedProblemModel` (already passed in), the settings, the default staging path and the bridge.

### Phase 6: Manual Verification and Closeout

**Status:** Verified

- [x] `gradlew.bat build` is green. Record the test counts.
- [x] Log in, select a problem that has supplied notes and HOME notes, and press **Upload**. Confirm the ChatGPT file dialog opens and the folder shows exactly three files.
- [x] Press Ctrl+A and Open, add a message, and send in ChatGPT. Confirm the three files attach.
- [x] Select a problem with no notes and no HOME notes. Confirm only the statement is staged, and the dialog shows only that file with nothing left over from the previous problem.
- [x] Confirm the solution and test-case files are never staged or shown anywhere.
- [x] With HOME unset, confirm the HOME notes are simply left out. With HOME set to a missing folder, confirm the behavior is clear.
- [x] Try Upload with no problem selected, with an unavailable problem, and while ChatGPT is loading or signed out. The button should be disabled with a reason, or the message should be accurate.
- [x] Make the staging root equal to HOME, then PROBLEMS, then a folder inside each, then a folder containing each. Confirm each is rejected and no original file is ever deleted.
- [x] Change HOME or PROBLEMS to overlap the staging root after choosing it, then press Upload. Confirm the upload is refused with a message.
- [x] Remove read access or lock a source file (or point the manifest at an unreadable file) and confirm the upload aborts without a partial set.
- [x] Press Upload twice quickly and confirm only one dialog opens.
- [x] Confirm the "Last attached" indicator appears after an upload and clears after a page reload.
- [x] Confirm AlgoPrep and ChatStory keep separate staging folders when run side by side.
- [x] Complete the Completion Summary and report **Work Complete**. Only the user marks anything **Verified**.

**Technical Notes:**
The staging safety checks are the part to be most careful with. Use a throwaway PROBLEMS and HOME copy for the overlap tests, never the user's real folders.

---

## Decisions

The six open questions from planning were resolved by adopting the recommendations:

1. **"Last attached"** keeps the Plan's label, with a tooltip saying it is advisory: the files were staged and the dialog was opened, not confirmed.
2. **Upload requires a ready browser** (`AppState.isSendEnabled()`), checked before `reset()`, as for Send Instructions.
3. **"Use Default"** button on the Staging directory setting.
4. **Overlap check** compares absolute, normalized paths only. There are no symlink checks.
5. **A partly copied set** is removed and the upload is aborted.
6. **Staged files** stay until the next upload replaces them.

---

## Implementation Results

Phases 1-5 are implemented. The full build passes with 234 tests and 0 failures (43 new). Phase 6 (the manual check) was closed by the user, who approved Verified without a step-by-step record of each manual item.

- **Phase 1:** `upload/UploadManifest` returns the files in order (statement, supplied notes, HOME notes) and a `problem` message if an existing file cannot be read. A missing notes file, an unset HOME and a HOME that is not a directory just leave files out. A HOME that cannot be listed is a problem. Only the three file kinds are ever included. 13 tests.
- **Phase 2:** `upload/StagingFolder` stages into `<root>\algoprep-upload`. `stage(root, problemsDir, homeDir, sources)` validates the root itself on every call, so the safety rule cannot be skipped by a caller. It rejects a root that equals, is inside, or contains PROBLEMS or HOME (compared as absolute normalized paths, so `..` and trailing separators do not hide an overlap), a root that is a file, and a subfolder name taken by a file. It checks every source before touching the staging folder, clears only regular files directly inside its own subfolder, and removes the files copied in the attempt if a copy fails. 19 tests, including that originals in PROBLEMS and HOME are never deleted and that a case-insensitive overlap is caught on Windows.
- **Phase 3:** `upload/UploadService` runs the sequence against a `ChatBridge`: problem selected and available, manifest, staging root validity, browser ready, then `reset()`, stage, `clickUploadFile()`, and the message `N file(s) staged. Select them in the ChatGPT file dialog.` Every earlier failure stops before the dialog opens, a staging failure resets but never clicks, and nothing says "attached". 10 tests with a fake bridge.
- **Phase 4:** The Settings tab has an Upload staging directory section showing the effective path (marked "(default)" when unset), a wrapped warning when it overlaps PROBLEMS or HOME, Browse, and Use Default. A refused choice shows an error dialog and leaves the setting unchanged. The three directory sections now share one `directoryControls` helper driven by a small spec record. `SettingsStore.effectiveStagingRoot(default)` supplies the default (1 test), and `AppConfig`'s default is passed in from `Main`.
- **Phase 5:** The Problem Files panel has the Upload button, a wrapped summary of the exact files (with the staging subfolder path as a tooltip), and the Ctrl+A hint. The button is disabled with a reason when nothing is selected, the statement is unavailable, a file is unreadable, the staging root overlaps PROBLEMS or HOME, or the browser is not ready. It is disabled for 2 seconds after a press. The Problem panel shows `Last attached: ...` on its own line, cleared when the page starts loading. `MainPanel.triggerUpload()` exists for DC5 and is not wired. The titled panels are now capped at their preferred height so spare room goes below them.
- **Layout checked offscreen:** A temporary test (since deleted) painted the MAIN and Settings tabs at the right pane's width, including a staging root equal to HOME. The summary wraps, the overlap warning is readable and Upload is greyed out.
- **Not yet confirmed live:** nothing has been uploaded to ChatGPT, and the real file dialog and Ctrl+U have not been exercised.
- **Test data for Phase 6:** the DC3 folders in the session scratchpad work: `...\scratchpad\display-problems` (problem 1 has supplied notes plus a solution and test-case file that must never be staged) and `...\scratchpad\display-home` (saved notes for problem 1).

---

## Notes and Risks

- **Risk: deleting the wrong files.** Mitigation: AlgoPrep-owned subfolder, overlap rejection at choose time and again at upload time, non-recursive regular-file-only clearing, and a test for each rule. This is the highest-risk area of the whole project, so test it first.
- **Risk: Ctrl+U stops opening the dialog** if ChatGPT changes its page. It is a ChatGPT behavior ChatStory relies on and could change (Plan A.8). Mitigation: the staging folder is a fixed, known location, so the user can still attach the files by hand, and the status tells them where the files were staged.
- **Risk: Robot keystrokes hit the wrong window.** `clickUploadFile()` focuses the browser and waits 200 ms first. If this proves flaky, the fix belongs in the copied bridge and should be a separate, deliberate change (Plan bridge policy).
- **Risk: stale or double uploads.** Mitigation: the button is disabled briefly after a press, and the staging folder holds only the latest problem's files.
- Notes may contain hints, and they go to ChatGPT. That is an accepted choice. The instructions file should tell ChatGPT how to treat them (Plan sections 8.1 and 9).
- The Plan's optional automatic dialog selection (DC6) is not touched here.
- `chatStoryRef/` is still read-only reference. `ContextFileStore.java` and `MainPanel.java` there show ChatStory's stage-then-upload sequence.
- Git is managed by the user. Agents do not run git commands in this project.
- Creating this document does not authorize implementation. Work starts only on the user's explicit instruction.

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** 2026-10-06
**Phases Completed:** All (1-6)
**Work Deferred:** Two small staging-path conveniences were offered and not requested: showing the full staged path in the status message, and copying it to the clipboard on Upload. Automatic file selection in the dialog remains the optional DC6 spike.

**Accomplishments:**
- `UploadManifest`: exactly the statement, supplied notes and HOME notes, never solutions or test cases
- `StagingFolder`: AlgoPrep-owned `algoprep-upload` subfolder, overlap rejection, non-recursive clearing, all-or-nothing copy
- `UploadService`: the upload sequence, with honest messages and no automatic retry
- Staging directory setting (with Use Default and an overlap warning) and the Problem Files panel with the advisory "Last attached" indicator

**Metrics:**
- Files: 3 new main classes (`UploadManifest`, `StagingFolder`, `UploadService`), 5 changed (`MainPanel`, `SettingsPanel`, `SettingsStore`, `AppFrame`, `Main`), 4 new test classes (`UploadManifestTest`, `StagingFolderTest`, `UploadServiceTest`, `SettingsStoreStagingTest`)
- Tests passing: 234, 0 failures (43 new)

**Lessons / Notes:**
Verified by the user on 2026-10-06. During testing the user noticed that Upload stages into `<staging directory>\algoprep-upload` rather than the directory itself, and that ChatGPT's file dialog does not open there on its own. The subfolder is deliberate (it is the only place AlgoPrep deletes files), and the dialog is ChatGPT's, so it opens where Chromium last was. Both are candidates for DC5 or the DC6 spike. `StagingFolder.stage` validates the root on every call so the safety rule cannot be skipped by a caller.
