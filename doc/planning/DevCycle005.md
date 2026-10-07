# DevCycle 005: Ad Hoc Testing

**Status:** In Progress
**Start Date:** 2026-10-06
**Target Completion:** Open-ended. This cycle closes when the user says testing is done.
**Focus:** Use AlgoPrep for real, and record everything the user wants changed. No feature work happens in this cycle.

---

## Goal

DevCycles 1-4 delivered every core capability in `doc/planning/AlgoPrepPlan-Claude02.md`: the three-pane window with embedded ChatGPT, instructions sending, problem scanning and selection, the Markdown display, and upload staging. This cycle is the user's time to live with it. The user tries it on real problems, and any issue, annoyance or idea is written down in the **Findings Log** below.

This is deliberately not a quick cycle. It is not scheduled, has no deadline and has no list of changes to build. The output is the Findings Log, which becomes the input for the next planning step.

## Desired Outcome

- The user has used AlgoPrep on real problems, ideally in several sessions, including a practice session with a real instructions file written with ChatGPT.
- Every change the user wants is in the Findings Log, in the user's own words, with enough detail to act on later.
- Each finding is triaged by the user: fix, defer, or drop.
- The user decides what the next cycle contains (most likely a "Polish" cycle built from the accepted findings, plus anything from the carried-in list that the user still wants).

**Not in this cycle:** implementing changes, adding features, or refactoring. If a finding is a clear bug that blocks testing, the user may ask for it to be fixed on the spot. That is the one exception, and it is recorded in the log.

---

## How This Cycle Works

1. The user tests whatever they like, in whatever order. There is no required script.
2. The user tells the assistant about each finding, in any form (a sentence, a screenshot description, an error message). The assistant adds it to the Findings Log without rewording it away. The assistant may add reproduction details it observed, marked as such.
3. The assistant does not change code, other than a fix the user explicitly requests for a blocking bug.
4. Findings stay in the log whether or not they are acted on. The log is the record of the cycle.
5. When the user is done, they triage the log, and the assistant records the outcome in the Completion Summary.

---

## Tasks

### Phase 1: Ad Hoc Testing

**Status:** In Progress

- [ ] Use AlgoPrep for real problem-solving sessions, across more than one sitting.
- [ ] Write the first real instructions file with ChatGPT, and send it with **Send Instructions** (Plan section 9).
- [ ] Record every finding in the Findings Log as it comes up.

**Technical Notes:**
The areas below are suggestions of where to look, not a checklist to complete. Skip any of it.
- **Setup and settings:** first-run experience, changing PROBLEMS, HOME and the staging directory, how clear the warnings are, whether settings survive restarts.
- **Problems tab:** filter behavior, refresh, selecting by double-click and Enter, a real, large problem collection.
- **Display:** how real problem files and your own notes render in both themes, long code lines, tables, reload when files are edited outside AlgoPrep.
- **Instructions and ChatGPT:** how well the instructions file steers ChatGPT, behavior when ChatGPT is slow or signed out, how a failed send is reported.
- **Upload:** the staged folder and the file dialog, switching between problems, problems without notes, anything surprising about what gets attached.
- **Window and workflow:** pane sizes, tab order, how often you reach for the mouse, anything you wish had a shortcut.
- **Status line and messages:** whether messages are clear, last long enough, and never say more than was observed.

### Phase 2: Triage

**Status:** Planning

- [ ] The user marks each finding in the log as **Fix**, **Defer** or **Drop**, and adds a priority to the ones to fix.
- [ ] The user decides which carried-in items below are still wanted.
- [ ] The assistant records the outcome in the Completion Summary and proposes how to group the accepted work into the next DevCycle (no implementation until asked).

**Technical Notes:**
Triage happens after testing, not during it, so findings are not dismissed too early.

---

## Findings Log

Add one row per finding, newest at the bottom. Keep the user's wording. The Source column says who raised it. Status values are **New**, **Fix**, **Defer**, **Drop**, or **Fixed** (only for a blocking bug fixed during this cycle).

| # | Date | Area | Finding (what happened or what you would like changed) | Source | Priority | Status |
|---|------|------|--------------------------------------------------------|--------|----------|--------|
| 1 | 2026-10-06 | Upload | When Upload is pressed, the files are staged in `<staging directory>\algoprep-upload`, not in the directory the user set, and ChatGPT's file dialog does not open there on its own. The subfolder is deliberate (the only place AlgoPrep deletes files), and the dialog belongs to ChatGPT's page. Two small ideas were offered and not requested: show the full staged path in the status message, and copy it to the clipboard on Upload so it can be pasted into the dialog. Automatic selection in the dialog is the optional spike in Plan section 8.3. | User | | New |

---

## Carried-In Items

These came up during DC1-4 and were not done. They are candidates for the next cycle, not commitments. The user decides which to keep during triage.

- **Status line replaces messages.** The status label is shared with the browser state, so a state change can overwrite a message such as a bad `settings.json` (it is also written to the console).
- **Shortcuts not wired.** Only Ctrl+Shift+B and Ctrl+Shift+X work. `MainPanel.triggerSendInstructions()` and `triggerUpload()` exist for Ctrl+Shift+I and Ctrl+Shift+U, and M and P are planned too (Plan section 12).
- **"Instructions sent" can go stale.** Editing the instructions file after sending does not change the indicator.
- **Markdown view cosmetic flaw.** An inline code span can sit tight against the preceding word.
- **Warning color.** The orange warning text in Settings is repainted in the muted theme color by `NativeThemeApplier`.
- **Automatic file selection in ChatGPT's dialog.** The optional spike in Plan section 8.3.
- **Housekeeping.** Move the verified DevCycle documents (001-004) to `doc/planning/completed/` and update the README and plan links. Decide whether to keep `chatStoryRef/` (agreed to revisit at the end of the polish work). Apply the suggested `.gitignore` additions (the whole `.idea/` folder, `hs_err_pid*.log` and `*.log`, `.claude/settings.local.json`).
- **Deferred ideas from the Plan (section 15).** Using test cases or the solution, editing My Notes in AlgoPrep, saving a ChatGPT reply as a note, a New Chat or Reload button, multiple instruction variants, a statement-only upload mode, a plain-text source view, remembered window layout, slimming down the copied bridge.

---

## Open Questions

*Remove this section if there are no open questions.*

1. **Should small fixes be allowed during this cycle?**
   Recommendation: Only for a bug that blocks testing, and only when the user asks. Everything else is logged and handled after triage, so the log stays a clean record of what testing found.

---

## Notes and Risks

- Findings are most useful when they are logged as they happen. A short note at the time beats a clearer memory later.
- Risk: the cycle never ends. Mitigation: the user decides when it closes. There is no scheduled end.
- Risk: the Findings Log grows into an unplanned backlog. Mitigation: triage into Fix, Defer and Drop before anything is built.
- The bridge and selectors depend on ChatGPT's current page. If sending or Ctrl+U stops working, that is a finding, and the likely fix is in `chatgpt_selectors.json` (Plan A.4).
- Git is managed by the user. Agents do not run git commands in this project.
- `chatStoryRef/` is still read-only reference.

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** [YYYY-MM-DD]
**Phases Completed:** [List or "All"]
**Work Deferred:** [What was not done and why, or "None"]

**Accomplishments:**
- [What was learned or decided]

**Metrics:**
- Findings logged: [N]
- Triage result: [N fix / N defer / N drop]

**Lessons / Notes:**
[Anything worth remembering for future cycles.]
