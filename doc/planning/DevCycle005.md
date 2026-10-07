# DevCycle 005: Ad Hoc Testing and Fixes

**Status:** Work Complete
**Start Date:** 2026-10-06
**Target Completion:** 2026-10-07 (closed by the user)
**Focus:** Use AlgoPrep for real and record everything the user wants changed, make the specific changes the user asks for (a Reset button, an Upload button on the Problems tab, and display font-size buttons), and test in between.

---

## Goal

DevCycles 1-4 delivered every core capability in `doc/planning/AlgoPrepPlan-Claude02.md`: the three-pane window with embedded ChatGPT, instructions sending, problem scanning and selection, the Markdown display, and upload staging. This cycle is the user's time to live with it. The user tries it on real problems, and any issue, annoyance or idea is written down in the **Findings Log** below.

The cycle has five phases: **Testing**, then **Fixes** (two specific changes the user asked for), then **More Testing**, then a **Font Size** change the user asked for after that testing, then **More Testing** again. It is deliberately not a quick cycle. It has no deadline, and the user decides when it closes. Apart from the changes in Phases 2 and 4, nothing is built. Everything else the user notices is logged and becomes input for the next planning step.

## Desired Outcome

- The user has used AlgoPrep on real problems, ideally in several sessions, including a practice session with a real instructions file written with ChatGPT.
- A stuck "send" can be recovered from with a **Reset** button next to **Send Instructions**, so the application is never left unable to do anything.
- The **Problems** tab has an **Upload** button at the bottom that behaves exactly like the one on the MAIN tab.
- The display window (the left pane) has **+** and **-** buttons that make its text larger and smaller.
- Every other change the user wants is in the Findings Log, in the user's own words, with enough detail to act on later.
- The user decides what the next cycle contains (most likely a "Polish" cycle built from the findings, plus anything from the carried-in list that the user still wants).

**Not in this cycle:** features or refactoring beyond the changes in Phases 2 and 4. If another finding is a clear bug that blocks testing, the user may ask for it to be fixed on the spot. That is the one other exception, and it is recorded in the log.

---

## How This Cycle Works

1. In the testing phases the user tests whatever they like, in whatever order. There is no required script.
2. The user tells the assistant about each finding, in any form (a sentence, a screenshot description, an error message). The assistant adds it to the Findings Log without rewording it away. The assistant may add reproduction details it observed, marked as such.
3. The assistant changes code only for the items in Phases 2 and 4, or for a blocking bug the user explicitly asks to have fixed.
4. Findings stay in the log whether or not they are acted on. The log is the record of the cycle.
5. When the user is done, they triage the log (Fix, Defer or Drop), and the assistant records the outcome in the Completion Summary.

---

## Tasks

### Phase 1: Testing

**Status:** Work Complete

- [x] Use AlgoPrep for real problem-solving sessions, across more than one sitting.
- [x] Write the first real instructions file with ChatGPT, and send it with **Send Instructions** (Plan section 9).
- [x] Record every finding in the Findings Log as it comes up.

**Technical Notes:**
The areas below are suggestions of where to look, not a checklist to complete. Skip any of it.
- **Setup and settings:** first-run experience, changing PROBLEMS, HOME and the staging directory, how clear the warnings are, whether settings survive restarts.
- **Problems tab:** filter behavior, refresh, selecting by double-click and Enter, a real, large problem collection.
- **Display:** how real problem files and your own notes render in both themes, long code lines, tables, reload when files are edited outside AlgoPrep.
- **Instructions and ChatGPT:** how well the instructions file steers ChatGPT, behavior when ChatGPT is slow or signed out, how a failed or unconfirmed send is reported.
- **Upload:** the staged folder and the file dialog, switching between problems, problems without notes, anything surprising about what gets attached.
- **Window and workflow:** pane sizes, tab order, how often you reach for the mouse, anything you wish had a shortcut.
- **Status line and messages:** whether messages are clear, last long enough, and never say more than was observed.

### Phase 2: Fixes

**Status:** Work Complete

Two changes the user has asked for (Findings Log #1 and #2). Nothing else is built in this phase.

**2a. Reset button next to Send Instructions**

- [x] Add a **Reset** button beside **Send Instructions** in the Instructions panel on the MAIN tab.
- [x] Reset is **always enabled**. It must work exactly when the app is stuck, which is when every other button is disabled because `AppState` is not Ready.
- [x] Pressing it calls `chatBridge.reset()`, the same recovery that Ctrl+Shift+X already performs. That abandons any request still waiting, and forces `AppState` back to Ready, so Send Instructions and Upload become usable again.
- [x] Show an honest status message, for example `Reset. ChatGPT is set to Ready. If a message was being sent, check ChatGPT to see whether it arrived.` It must not claim the earlier send did or did not happen, because that is not known.
- [x] Refresh the buttons immediately after the reset, so Send Instructions and Upload enable without waiting for the 2-second timer.
- [x] Give it a tooltip that says what it does and that it does not undo anything already sent.
- [x] Add a short test that a reset returns a stuck state to Ready and re-enables Send Instructions (if it can be done without a window, as `MarkdownViewTest` does).
- [x] Mention the Reset button next to the Ctrl+Shift+X shortcut in `README.md`.

**2b. Upload button on the Problems tab**

- [x] Add an **Upload** button at the bottom of the **Problems** tab, below the list and the message line.
- [x] It has the **exact same functionality** as the Upload button on the MAIN tab: the same files, the same checks (selected problem, readable files, staging root clear of PROBLEMS and HOME, browser ready), the same reset-before-use, the same staging, the same status messages, the same short cooldown, and the same tooltip reasons when it is disabled.
- [x] Share one implementation rather than copying it. Move the upload action and the "why is Upload disabled" decision out of `MainPanel` into one place both buttons use, so they cannot drift apart. The decision part should be pure and unit tested, as the rest of the upload logic is.
- [x] Pressing either button updates the same state: the advisory `Last attached` on the MAIN tab, and the cooldown, which disables both buttons.
- [x] Keep the existing Upload button and summary on the MAIN tab unchanged.
- [x] Expose it in a way that the Ctrl+Shift+U shortcut can use later, with one trigger for both.

**Technical Notes:**
- **Reset:** `ChatGptBridge.reset()` clears the active request and calls `AppState.reset()`, which forces Ready from any state (Plan A.5). `Main`/`AppFrame` already bind Ctrl+Shift+X to it. The button adds a visible way to do the same thing. Send Instructions and Upload are disabled whenever `AppState.isSendEnabled()` is false, which is why a stuck state leaves the user unable to do anything.
- **Upload on the Problems tab:** the button always uploads the **selected** problem (the one on the MAIN tab's Problem panel), not the row that is merely highlighted in the list. See Open Question 2.
- **Likely code shape:** a small shared upload controller in `ui/` that owns the cooldown flag, the enabled state and reason, and the call to `UploadService`, with `MainPanel` and `ProblemsPanel` each binding a button to it. The pure "reason" decision can move to the `upload/` package.

### Phase 3: More Testing

**Status:** Verified

- [x] Re-test the two Phase 2 changes: get the app stuck on an unconfirmed send and recover with Reset; upload from both the MAIN tab and the Problems tab and confirm identical behavior.
- [x] Carry on using AlgoPrep for real sessions, and record every finding in the Findings Log.
- [x] Triage the full log (Fix, Defer or Drop) when the user decides testing is done.

**Technical Notes:**
Same suggested areas as Phase 1. Anything accepted at triage becomes input for the next cycle, not work in this one, unless the user says otherwise. Marked Verified by the user. The result was one further request, the font-size buttons in Phase 4 (Findings Log #3).

### Phase 4: Display Font Size

**Status:** Work Complete

One change the user has asked for (Findings Log #3). Nothing else is built in this phase.

- [x] Add a **+** button and a **-** button to the display window (the left pane with the Problem, Notes and My Notes tabs). **+** makes the text larger and **-** makes it smaller.
- [x] The size applies to all three tabs together, so switching tabs never changes the size. Messages such as `No problem selected.` scale with the rest.
- [x] Each press changes the size by one fixed step. Headings, code and table text scale in proportion to the body text, so the layout keeps its shape.
- [x] Limit the range, and disable **+** at the largest size and **-** at the smallest, so a button never does nothing silently.
- [x] Give each button a tooltip (`Larger text` and `Smaller text`) that also shows the current size.
- [x] Keep the reading position when the size changes, so pressing **+** in the middle of a long problem does not jump back to the top.
- [x] Remember the size across restarts (Open Question 1), and restore it at startup. A missing or invalid saved value means the default size.
- [x] Works in both themes, and the theme switch keeps the chosen size.
- [x] Unit tests: the size model (default, step up and down, limits, listeners fire only on a real change), the generated styles at several sizes (every font size scales, none drops below a readable minimum, the default produces exactly today's styles), and saving and loading the setting (round trip, invalid and missing values).
- [x] Add a test that the view re-renders at the new size and keeps its text, as `MarkdownViewTest` does without a window.
- [x] Mention the buttons in `README.md`.

**Technical Notes:**
- **Where the buttons go:** a small bar across the top of the display pane, right-aligned above the tabs, so the buttons stay visible whichever tab is open. `DisplayPanel` is currently a `JTabbedPane`, so it will need to wrap the tabs in a panel. `AppFrame` only needs to keep treating it as one component.
- **How the size is applied:** `MarkdownStyles.rules(theme)` has fixed point sizes (body 12pt, headings 18, 16, 14 and 12pt, code 11pt). Add a size percentage input and multiply each size by it, so `100%` gives exactly today's rules. Keep the percentage in a small pure model in `display/`, shared by the three `MarkdownView`s, which already rebuild their style sheet and re-render on a theme change and can do the same on a size change.
- **Reading position:** a restyle currently scrolls back to the top. For a size change, record the scroll position as a fraction of the content height and restore it after the re-render.
- **Persistence:** add one field to `SettingsStore` for the size. It is optional in the file, so existing `settings.json` files still load.
- **Scope:** only the display window. The right-hand panels and the ChatGPT pane are not affected.


### Phase 5: More Testing

**Status:** Work Complete

- [ ] Try the **+** and **-** buttons on real problem files and your own notes: both themes, long code lines, tables, a long problem scrolled halfway down, pressing the buttons several times quickly, and restarting to see that the size is remembered.
- [ ] Carry on using AlgoPrep for real sessions, and record every finding in the Findings Log.
- [ ] Triage the full log (Fix, Defer or Drop) when the user decides testing is done.

**Technical Notes:**
Phase 5 was closed when the user declared the cycle complete on 2026-10-07. The checklist items above were not individually recorded, so they are left unticked.

Same suggested areas as Phase 1, plus the text size: whether the step and the range feel right, whether the buttons are easy to find, whether the size should apply to anything else, and whether a keyboard or mouse-wheel zoom is missed (Open Question 4 of Phase 4 left that out). Anything accepted at triage becomes input for the next cycle, not work in this one, unless the user says otherwise.

---

## Findings Log

Add one row per finding, newest at the bottom. Keep the user's wording. The Source column says who raised it. Status values are **New**, **Fix**, **Defer**, **Drop**, or **Fixed**.

| # | Date | Area | Finding (what happened or what you would like changed) | Source | Priority | Status |
|---|------|------|--------------------------------------------------------|--------|----------|--------|
| 1 | 2026-10-06 | Send Instructions | "I want there to be a reset button next to send instructions. The application does not confirm the send and that makes the application hang, unable to do anything else. The reset button should fix that." Observation by the assistant: Send Instructions and Upload are disabled whenever `AppState` is not Ready, and a send that never gets confirmed leaves it in a non-Ready state until it recovers. The cause of the missing confirmation has not been investigated. | User | Phase 2 (2a) | Fixed |
| 2 | 2026-10-06 | Problems tab | "I want an upload button in the Problems tab, at the bottom, that has the exact functionality of the upload button in the main tab." | User | Phase 2 (2b) | Fixed |
| 3 | 2026-10-06 | Display window | "add a + and - buttons in the display window that increases and decreases the font size." | User | Phase 4 | Fixed (not yet tried by the user) |

---

## Carried-In Items

These came up during DC1-4 and were not done. They are candidates for the next cycle, not commitments. The user decides which to keep during triage.

- **Status line replaces messages.** The status label is shared with the browser state, so a state change can overwrite a message such as a bad `settings.json` (it is also written to the console).
- **Shortcuts not wired.** Only Ctrl+Shift+B and Ctrl+Shift+X work. `MainPanel.triggerSendInstructions()` and `triggerUpload()` exist for Ctrl+Shift+I and Ctrl+Shift+U, and M and P are planned too (Plan section 12).
- **"Instructions sent" can go stale.** Editing the instructions file after sending does not change the indicator.
- **Markdown view cosmetic flaw.** An inline code span can sit tight against the preceding word.
- **Warning color.** The orange warning text in Settings is repainted in the muted theme color by `NativeThemeApplier`.
- **Automatic file selection in ChatGPT's dialog.** The optional spike in Plan section 8.3.
- **Housekeeping.** Move the verified DevCycle documents (001-004) to `doc/planning/completed/` and update the README and plan links. Decide whether to keep `chatStoryRef/`. Apply the suggested `.gitignore` additions (the whole `.idea/` folder, `hs_err_pid*.log` and `*.log`, `.claude/settings.local.json`).
- **Deferred ideas from the Plan (section 15).** Using test cases or the solution, editing My Notes in AlgoPrep, saving a ChatGPT reply as a note, a New Chat or Reload button, multiple instruction variants, a statement-only upload mode, a plain-text source view, remembered window layout, slimming down the copied bridge.

---

## Decisions

The four open questions from planning were resolved by adopting the recommendations:

1. **Cause of the unconfirmed send:** not investigated as part of the Reset button. The button treats the symptom. When it happens again, note what the status line says and whether the message actually appeared in ChatGPT, and log it as a finding.
2. **Upload on the Problems tab uses the selected problem**, exactly like the MAIN tab. To make that visible, `Selected: ...` is shown beside the button. This is an addition to what was asked, so say if it should go.
3. **No file summary** on the Problems tab. The tooltip gives the reason when Upload is disabled.
4. **Reset does not clear the "Instructions sent" indicator**, because that indicator only appears after a confirmed send.

---

## Phase 2 Results

Both fixes are implemented. The full build passes with 256 tests and 0 failures (22 new). Neither has been tried against live ChatGPT yet, which is Phase 3.

- **2a, Reset:** `MainPanel` has a **Reset** button beside **Send Instructions**. It is always enabled. It calls `chatBridge.reset()` (the same recovery as Ctrl+Shift+X), reports `Reset. ChatGPT is set to Ready. If a message was being sent, check ChatGPT to see whether it arrived.` after the reset so the "Ready" state label does not replace it, and refreshes the buttons at once. The tooltip says it does not undo anything already sent. `README.md` now describes it, and I also brought its stale status table up to date and added a short section on uploading.
- **2b, Upload on the Problems tab:** One shared implementation now drives both buttons.
  - `upload/UploadAvailability` is a pure decision (can Upload be pressed, why not, and the exact file summary). 9 tests.
  - `ui/UploadController` owns the cooldown, the advisory "last attached" name, the call to `UploadService`, and the enabled state and tooltip of every bound button. Both buttons are attached with `bind`, so they cannot drift apart. It re-checks on app state, settings and selection changes and on a 2-second timer. It has one `trigger()` for the future Ctrl+Shift+U shortcut. 9 tests with two stand-in buttons, covering identical enabled state and tooltip, one upload for either press, the cooldown disabling both, the shared "last attached", and the overlap and browser-not-ready cases.
  - `MainPanel` no longer contains upload logic. It shows the controller's summary and "last attached", and its Upload button is bound to the controller. `ProblemsPanel` has an **Upload** button at the bottom, below the message line, with `Selected: ...` beside it. `AppFrame` creates the controller and passes it to both.
- **Reset test:** `MainPanelResetTest` (4 tests) builds the real MAIN panel with a fake bridge whose `reset()` forces Ready like the real one. It checks that a send stuck in `Sending` disables Send Instructions, that Reset stays enabled and recovers it, that Send Instructions is usable again immediately, and that the message does not claim whether the send arrived.
- **Layout checked offscreen:** A temporary test (since deleted) painted both tabs at the right pane's width. The three buttons fit on one row, and the Problems tab shows the list, the count, and `Upload` with `Selected: 1 - Two Sum` at the bottom.
- **One behavior to know about:** Reset forces Ready from any state, including while the page is still loading, exactly as Ctrl+Shift+X always has. Pressing it while ChatGPT is loading makes Send Instructions available before the page is ready.

---

## Phase 4 Decisions and Results

The five open questions about Phase 4 were resolved by adopting the recommendations:

1. **The size is remembered** across restarts, in one optional field in `settings.json`. A missing or invalid saved value means the normal size.
2. **Steps of 10%, from 60% to 250%,** with 100% as the normal size.
3. **No size label or reset control.** The tooltips show the current size and the buttons disable at the limits.
4. **No keyboard or mouse-wheel zoom** in this phase. Log it as a finding if it is wanted.
5. **The reading position is kept** approximately when the size changes.

The change is implemented. The full build passes with 290 tests and 0 failures (34 new). It has not been tried by the user yet, which is Phase 5.

- **`display/FontScaleModel`** (pure): the size as a percentage with `increase()`, `decrease()`, `canIncrease()`, `canDecrease()`, listeners that fire only on a real change, and `restore(Integer)`, which accepts only a value on a step within the range and otherwise uses the default. 10 tests.
- **`MarkdownStyles.rules(theme, percent)`**: every font size is scaled and rounded, with a 6pt floor, and the colors are untouched. `rules(theme)` is the same as 100%, which gives exactly the original sizes (body 12, headings 18, 16, 14 and 12, code 11). 6 new tests, including that no size shrinks as the percentage grows, headings keep their order, and only the sizes differ between 100% and 200%.
- **`MarkdownView`:** takes the shared model and re-renders on a change. A theme switch keeps the chosen size. It keeps the reading position by remembering the text offset at the top of the view and scrolling back to it after the new layout exists, retrying for up to about a second because the first attempt can land short while the document is still being laid out. If another size change arrives while that is in progress it keeps the earlier target, so two quick presses do not lose the place. 4 tests with a real laid-out frame, covering larger and smaller text taking more and less room, the text being kept, and the position being kept for both a double increase and a decrease.
- **`DisplayPanel`:** is now a panel with a bar of **-** and **+** buttons above the three tabs, right-aligned and not focusable. All three tabs share one size model. Each button is disabled at its limit, and its tooltip reads `Larger text (now 110%)`, `Smaller text (now 110%)`, `Largest size (now 250%)` or `Smallest size (now 60%)`. 9 tests.
- **Persistence:** `SettingsStore` has an optional `fontScalePercent`, so existing `settings.json` files still load. `Main` restores the size at startup and saves every change. 5 tests.
- **Bug found by the tests:** the first version lost the reading position when the button was pressed twice quickly, because the second press read the scroll position before the first had finished restoring it. That is fixed, and the double press is covered by a test.
- **Look checked offscreen:** a temporary test (since deleted) painted the display pane at 70%, 100% and 140%. The buttons sit at the top right above the tabs, and the text, code block and table all scale together.
- **README:** the status table mentions the buttons.

---

## Notes and Risks

- Findings are most useful when they are logged as they happen. A short note at the time beats a clearer memory later.
- Risk: the cycle never ends. Mitigation: the user decides when it closes. There is no scheduled end.
- Risk: the Findings Log grows into an unplanned backlog. Mitigation: triage into Fix, Defer and Drop before anything is built.
- **Risk: Reset hides a real problem.** It makes the stuck state recoverable, but it does not explain it. Mitigation: log each occurrence (Open Question 1), and keep the message honest about not knowing whether the send arrived.
- **Risk: Reset during a real send.** Pressing it while a message is genuinely in flight abandons the wait for confirmation, so the user should check ChatGPT before sending again. The tooltip and message say so.
- **Risk: two Upload buttons drifting apart.** Mitigation: one shared implementation and a unit-tested decision function.
- The bridge and selectors depend on ChatGPT's current page. If sending or Ctrl+U stops working, that is a finding, and the likely fix is in `chatgpt_selectors.json` (Plan A.4).
- Git is managed by the user. Agents do not run git commands in this project.
- `chatStoryRef/` is still read-only reference.
- Creating or editing this document does not authorize implementation. Phase 2 starts only on the user's explicit instruction.

---

## Completion Summary

**Completion Date:** 2026-10-07
**Phases Completed:** All (1-5). Phases 1, 2, 4 and 5 are Work Complete, and Phase 3 is Verified. The cycle is Work Complete, pending the user's Verified.
**Work Deferred:** The carried-in items above were not triaged one by one and stay open for a future cycle (shortcuts, the status line replacing messages, the stale "Instructions sent" indicator, the Markdown inline-code spacing, the Settings warning color, automatic file selection in the dialog, housekeeping, and the Plan's deferred ideas). The cause of the unconfirmed send (Finding 1) was never investigated.

**Accomplishments:**
- Used AlgoPrep for real sessions, and logged three findings, all acted on
- **Reset** button beside Send Instructions (Finding 1)
- **Upload** button on the Problems tab, sharing one implementation with the MAIN tab (Finding 2)
- **+** and **-** text-size buttons for the display window, remembered between runs (Finding 3)

**Metrics:**
- Findings logged: 3
- Triage result: 3 fixed / 0 deferred / 0 dropped (the carried-in items were deferred, not triaged)
- Tests passing: 290 at the end of Phase 4 (the suite is now 337 after DevCycle 6)

**Lessons / Notes:**
The font-size buttons (Finding 3) are marked "Fixed (not yet tried by the user)" in the log, and nothing later contradicts that. DevCycle 6 (studied tracking) was planned and finished while this cycle was still open. DC5 has not been moved to `doc/planning/completed/`, and the user has not marked it Verified.
