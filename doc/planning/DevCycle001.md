# DevCycle 001: Skeleton and Instructions

**Status:** Verified
**Start Date:** TBD
**Target Completion:** TBD
**Focus:** Create the AlgoPrep project from the ChatStory seed files, so that it builds, shows the three-pane window, and sends the instructions file to ChatGPT.

Design source: `doc/planning/AlgoPrepPlan-Claude02.md` (the "Plan"), mainly sections 3, 4, 6.1, 10, 11 and Appendix A. Section numbers below refer to it.

---

## Goal

Stand up AlgoPrep as an independent Java 21 / Swing / JCEF project by copying and renaming the proven ChatStory browser, bridge, state and theme code from `chatStoryRef/`. Then prove the delicate part first: the embedded ChatGPT window and the bridge, using `sendRawPrompt` to send the user's instructions file. Everything later (problems, display, upload) depends on this foundation and is almost independent of the bridge, so the bridge code should be touched only once.

## Desired Outcome

- `gradlew.bat build` passes, including the four copied test classes (`BridgeMessageTest`, `ResourceLoadingTest`, `AppStateTest`, `NativeThemeModelTest`) and the new `SettingsStore` and `AppConfig` tests.
- `gradlew.bat run` opens a three-pane window: display placeholder (left), embedded ChatGPT (middle), MAIN / Problems / Settings tabs (right).
- The user logs in to ChatGPT once (AlgoPrep has its own profile) and presses **Send Instructions** on the MAIN tab. The chosen file is sent once, as plain text, with no response tracking. The state returns to Ready, and the status line reports the result honestly.
- Dark and light themes work, and the chosen theme and instructions-file path persist across restarts.
- AlgoPrep has no runtime dependency on ChatStory, and `chatStoryRef/` is untouched.

**Not in this cycle:** problem scanning, the Problems list, Markdown display, HOME/PROBLEMS/staging settings, upload, keyboard shortcuts beyond what is needed to run (all DC2-5).

---

## Tasks

### Phase 1: Project Skeleton and Seed Copy

**Status:** Verified

- [x] Create the Gradle project at the repo root from `chatStoryRef/algoprep/`: `build.gradle.kts`, `settings.gradle.kts` (`rootProject.name = "AlgoPrep"`), `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `.gitignore`.
- [x] Set group `com.algoprep`, `mainClass` = `com.algoprep.Main`, and keep the `run` task's three JVM args (Plan A.2).
- [x] Copy the "copy as-is" Java files (Plan A.1) into `src/main/java/com/algoprep/...` and rename the package: `AppState`, `UiThread`, `browser/*` (four files), `bridge/*` (eight files), `theme/*` (three files).
- [x] Copy all seven files in `src/main/resources/js/` unchanged. Leave the `chatStory...` JS function names alone (Plan A.4).
- [x] Copy the four test classes into `src/test/java/com/algoprep/` and rename the package.
- [x] Rename `BUILDING.md` and `config.example.properties` for AlgoPrep (`%APPDATA%\AlgoPrep`, profile and staging paths from Plan section 10). Copy `LICENSE` and keep the notice.
- [x] Do **not** copy: `BrowserContextMenuHandler`, `CorrectionType`, `PromptEncoder`, story stores, `ContextFileStore`, or ChatStory's `AGENTS.md`, `CLAUDE.md`, `DevelopmentProcess.md`, `DevCycleTemplate.md` (AlgoPrep has its own).
- [x] Search the new tree for leftover `chatstory` / `ChatStory` references in Java, Gradle and docs, other than the intentional `chatStory...` JS function names and the copied license notice.

**Technical Notes:**
The copied set was checked in planning to have no references to story-only classes, so the package rename alone should compile. The only ChatStory-specific coupling left is in `AppFrame`, `MainPanel`, `ConfigurationPanel`, `OutputPanel` and `Main`, which are rewritten or adapted in later phases, not copied. `gradle.properties` pins `org.gradle.java.home` to `C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot` (see Open Question 1).

**Phase 1 result:** 28 files copied under `src/` (2 root Java files, 4 browser, 8 bridge, 3 theme, 7 resources, 4 tests) plus the Gradle, `.gitignore`, `LICENSE`, `BUILDING.md` and `config.example.properties` files at the root. `com.chatstory` is now `com.algoprep` everywhere, and `version` was reset to `0.1.0`. The only remaining `chatStory` text is the intentional JS function names (`window.chatStory...` in `ChatGptBridge` and the JS files) and `[ChatStory]` console-log prefixes in `click_upload_file.js`. `gradle.properties` was kept because the pinned JDK path exists on this machine. Nothing has been compiled yet, which is Phase 2. There is no `Main` yet, so the build may need a temporary stub.

### Phase 2: Build and Dependency Check

**Status:** Verified

- [x] Confirm the JDK 21 toolchain resolves and `gradlew.bat build` runs with the copied main and test sources only (a temporary stub `Main` is acceptable if needed to compile).
- [x] Confirm `jcefmaven:146.0.10`, `gson:2.11.0` and JUnit `5.11.4` still resolve. If a version needs to change, record why in Technical Notes (Plan A.10).
- [x] Add the `org.commonmark:commonmark` and `commonmark-ext-gfm-tables` dependencies now, so DC3 does not touch the build file. Confirm they resolve and leave them unused.
- [x] Run the tests. All four copied test classes pass.

**Technical Notes:**
Record the exact versions that worked. `ResourceLoadingTest` must stay in step with the JS files copied in Phase 1.

**Phase 2 result:** `gradlew.bat clean build` succeeds with the Java 21 toolchain (pinned JDK from `gradle.properties`) and **no stub `Main` was needed**, since the copied set compiles on its own. Versions confirmed unchanged: `jcefmaven:146.0.10`, `gson:2.11.0`, JUnit `5.11.4`. Added `org.commonmark:commonmark:0.30.0` and `commonmark-ext-gfm-tables:0.30.0` (current Maven Central release); both resolve on the runtime classpath and are unused until DC3. Tests: 53 pass, 0 fail (`AppStateTest` 26, `BridgeMessageTest` 18, `ResourceLoadingTest` 7, `NativeThemeModelTest` 2). The Gradle native-access warning on startup is harmless.

### Phase 3: Configuration and Settings Store

**Status:** Verified

- [x] Write `config/AppConfig` by adapting ChatStory's version: `APP_NAME = "AlgoPrep"`; keep the `%LOCALAPPDATA%` / `%APPDATA%` logic with the `user.home` fallback, directory creation that logs but does not fail, and the optional `target.chat.url` property. Drop all story file paths. Expose the profile path, `settings.json` path, default staging root, and target URL (Plan A.7, section 10).
- [x] Write `config/SettingsStore`: Gson JSON file, one `addListener(Runnable)` list, load tolerantly, save on every change. Fields for this cycle: instructions file path, theme. Reserve the remaining fields from the Plan (PROBLEMS, HOME, staging root, last selected key) so later cycles do not change the file format, but do not expose UI for them yet.
- [x] If `settings.json` cannot be parsed, rename it to `settings.json.bad`, load defaults and report it through a status callback (Plan section 10).
- [x] Report a failed settings write through the same status callback, not only the console.
- [x] Unit tests: round trip, missing file gives defaults, malformed file is preserved as `.bad`, listener fires on change, unknown fields are tolerated.

**Technical Notes:**
Keep `SettingsStore` constructor-injectable with a `Path`, so tests use a temp directory and never touch `%APPDATA%`. The status callback can be a `Consumer<String>` wired to the status label in Phase 4.

**Phase 3 result:** Added `config/AppConfig` (profile, `settings.json`, config file and default staging root paths; a package-private constructor takes the base folders for tests; an invalid-escape `config.properties` also falls back to the default URL) and `config/SettingsStore` (constructor takes a `Path` and a `Consumer<String>` status callback; setters save and notify only when a value actually changes; an unknown theme value in the file falls back to DARK). Tests added: `AppConfigTest` (5) and `SettingsStoreTest` (8). Full build passes with 66 tests and 0 failures. Neither class is wired into anything yet, which is Phase 4. Because the store may report a bad file from its constructor, `Main` must supply a status callback that works before the frame exists (for example, buffer messages until the status label is ready).

### Phase 4: Main Entry Point and Three-Pane Frame

**Status:** Verified

- [x] Adapt `Main` from ChatStory, keeping the JCEF startup order in Plan A.3 **exactly** (in particular, register `BrowserKeyboardHandler` before `createBrowser`). Remove all story stores. Wire `AppConfig`, `AppState`, `SettingsStore`, and the theme model initialized from settings.
- [x] Write `AppFrame` using ChatStory's `AppFrame` as read-only reference: toolbar with **DevTools** button and status label, an outer `JSplitPane` (display left, inner split of browser and right tabs), weights about 0.30 / 0.45 / 0.25, no `SOUTH` panel. Window closes with `dispose()` and `System.exit(0)`.
- [x] Left pane: placeholder panel (a `JTabbedPane` with empty Problem / Notes / My Notes tabs is fine). Real content arrives in DC3.
- [x] Right pane: `JTabbedPane` with **MAIN**, **Problems** (placeholder) and **Settings**.
- [x] Settings tab (minimal, modeled on `ConfigurationPanel`): Dark / Light radio buttons that update the theme model, which writes to `SettingsStore`.
- [x] Apply the theme via `NativeThemeApplier` through `UiThread.run` when the model changes and once after the window is shown (Plan A.9).
- [x] Status line shows `AppState` changes and messages from the settings store.
- [x] Ctrl+Shift+X (reset `AppState`) and Ctrl+Shift+B (focus browser) only if cheap to wire with the mechanism in Plan A.6. The remaining shortcuts belong to DC5.

**Technical Notes:**
The local panes must stay usable while ChatGPT is loading, signed out or failed (Plan section 4), so do not make any Swing component construction depend on the browser being ready. `NativeThemeApplier` sets only basic `JEditorPane` colors, so any later Markdown styling is DC3's problem.

**Phase 4 result:** Written: `Main` (ChatStory's JCEF startup order kept exactly; story stores removed; settings, theme model and status reporter wired in), `AppFrame` (toolbar with DevTools and status label, outer split display | (browser | right tabs) at 420 / 630 px of a 1400 px window, no south panel), `ui/DisplayPanel` (three placeholder tabs), `ui/MainPanel` (stub, replaced in Phase 5), `ui/SettingsPanel` (Dark / Light radio buttons) and `ui/StatusReporter`. The Problems tab is an inline placeholder until DC2.
- **Early status messages:** `StatusReporter` buffers messages reported before the window exists (for example a bad `settings.json`, which `SettingsStore` reports from its constructor), flushes them when `AppFrame` attaches the status label, and also writes each to stderr. The status label is shared with `AppState`, so a later state change replaces the text.
- **Theme persistence:** `Main` sets the theme model from settings at startup and saves every later change back to `SettingsStore`. `AppFrame` applies the theme on change and once after the window is shown.
- **Shortcuts:** Only Ctrl+Shift+X (reset the bridge and `AppState`) and Ctrl+Shift+B (focus the browser) are registered, through both the browser handler map and a Swing key dispatcher. The rest are DC5.
- **Tests:** `StatusReporterTest` (3) added. Full build passes with 69 tests and 0 failures.
- **Not yet run:** The window has been compiled but not launched, so it is unconfirmed on screen. The first launch downloads about 100 MB of Chromium, so it is left to the Phase 6 manual check.

### Phase 5: MAIN Tab and Send Instructions

**Status:** Verified

- [x] Write `ui/MainPanel` as a titled-panel `BoxLayout`, using ChatStory's `MainPanel` as read-only reference. This cycle has the **Problem** panel (static text, "No problem selected"), the **Instructions** panel, and a **Problem Files** panel with a disabled **Upload** button and no summary yet. Upload behavior is DC4.
- [x] Instructions panel: path field, `Browse...` (file chooser; persists to `SettingsStore`), and **Send Instructions**.
- [x] Read the instructions file at **each press** of Send. The button is disabled until a readable, non-empty file is chosen, or while `AppState.isSendEnabled()` is false, with a tooltip saying why (Plan section 6.1).
- [x] On press: call `chatBridge.reset()`, then `chatBridge.sendRawPrompt(text, listener)`. Keep the reset-before-send for now (Plan section 6.1).
- [x] `ResponseListener`: `onPromptSubmitted` sets the status `Instructions message sent`. `onError` shows the distinguishing message (file unreadable, browser not ready, injection failed, send not confirmed). The other callbacks are empty.
- [x] Never retry automatically after an uncertain send. The message tells the user to check ChatGPT and press again if needed.
- [x] Keep an advisory "instructions sent" indicator. Clear it when `AppState` reports page loading.
- [x] Update button enablement when `AppState` changes, on the Swing thread via `UiThread`.

**Technical Notes:**
`sendRawPrompt` runs `Ready` to `InjectingPrompt` to `Sending`, then returns to `Ready` once the user message appears. It never reaches `WaitingForResponse` (Plan A.5), so the Send button re-enables without the 180 s wait. Status wording must never say "tutor configured" or "attached". If the copied bridge does not behave this way with `sendRawPrompt` alone, that is the finding of this phase (Plan A.10).

**Phase 5 result:** Written: `instructions/InstructionsSender` (no Swing; reads the file as UTF-8 on each send, then `reset()` and `sendRawPrompt`, with error wording that separates "not ready", "nothing was sent", "could not press Send" and "not confirmed, may or may not have been sent"), and `ui/MainPanel` (replacing the Phase 4 stub) with the Problem placeholder, the Instructions panel (read-only path field, Browse, Send Instructions, sent indicator) and a disabled Upload button for DC4. `AppFrame` and `Main` now pass the `SettingsStore` through.
- **Send guard:** `send` checks `AppState.isSendEnabled()` *before* calling `reset()`, because `reset()` forces `Ready` from anywhere and would otherwise let a send go out while the page is loading or signed out. The button is also disabled in that case, with a tooltip giving the reason (no file chosen, file missing or blank, or ChatGPT not ready).
- **Enablement refresh:** on `AppState` changes, on settings changes, and on a 2 s `javax.swing.Timer`, since the file can be created or edited outside AlgoPrep.
- **Indicator:** "Instructions sent: name at HH:mm:ss (advisory)" appears only after the bridge confirms, and is cleared when `AppState` reports `LoadingChatGPT`. Editing the file after sending does not change it, so it can be stale, as the Plan allows.
- **Status text:** goes through `StatusReporter`, so it shows in the toolbar label. Nothing says "configured" or "attached", and a failed or unconfirmed send is never retried automatically.
- **Tests:** `InstructionsSenderTest` (10) added. Full build passes with 79 tests and 0 failures.
- **Not yet confirmed live:** the real send path has not been exercised against ChatGPT, which is Phase 6. `triggerSendInstructions()` exists on `MainPanel` for the DC5 shortcut but is not wired.

### Phase 6: Manual Verification and Closeout

**Status:** Verified

- [x] `gradlew.bat build` is green. Record the test counts.
- [x] First run: Chromium downloads into `jcef-bundle/` (about 100 MB), the window opens, and the user logs in to ChatGPT in the embedded browser.
- [x] Write a short throwaway instructions file (the draft in Plan section 9 will do) and send it. Confirm exactly one message appears in ChatGPT as plain text, and the state returns to Ready.
- [x] Edit the instructions file, press Send again, and confirm the new text is sent.
- [x] Send while the page is loading or signed out and confirm the button is disabled with a tooltip, or the failure message is accurate.
- [x] Restart AlgoPrep and confirm theme and instructions path persist. Corrupt `settings.json` and confirm it is renamed to `.bad` and reported.
- [x] Resize all three panes in both themes and confirm every tab is readable.
- [x] Confirm AlgoPrep and ChatStory can run side by side with separate profiles and settings.
- [x] Complete the Completion Summary and report **Work Complete**. Only the user marks anything **Verified**.

**Technical Notes:**
A live ChatGPT page cannot be unit tested, so this phase is manual. If ChatGPT's page has changed, the likely fixes are in `chatgpt_selectors.json` (selectors), not Java (Plan A.4).

---

## Open Questions

1. **Keep ChatStory's pinned JDK path in `gradle.properties`?**
   Recommendation: Check whether `C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot` exists on this machine. If it does, keep the file. If not, drop `gradle.properties` and rely on the Java 21 toolchain.

2. **Should `chatStoryRef/` stay in the repository after this cycle?**
   Recommendation: Keep it through DC5, because the later cycles still read `AppFrame`, `MainPanel`, `ConfigurationPanel`, `OutputPanel` and `ContextFileStore` for reference. Make sure it is excluded from the Gradle build (it is outside `src/`, so it should be automatically). Decide on removal at the end of DC5.

3. **Should the minimal Settings tab (theme only) be in this cycle?**
   Recommendation: Yes. The Plan lists "theme" in DC1, and the tab skeleton avoids reworking the right-pane layout in DC2. The folder settings (PROBLEMS, HOME, staging) are added in DC2-4.

---

## Notes and Risks

- **Risk: bridge behavior on first run.** ChatStory's DC21-24 chased an intermittent hang in this code. Mitigation: copy without refactoring (Plan section 3), keep reset-before-send, and keep the JCEF startup order exactly.
- **Risk: ChatGPT page changes** can break the selectors. Mitigation: selectors live in `chatgpt_selectors.json`.
- **Risk: dependency versions** may have moved since ChatStory last built. Mitigation: confirm in Phase 2 before writing anything on top.
- `chatStoryRef/` is read-only. Make changes only in the copies.
- Git is managed by the user. Agents do not run git commands in this project.
- Creating this document does not authorize implementation. Work starts only on the user's explicit instruction.

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** 2026-10-06
**Phases Completed:** All (1-6)
**Work Deferred:** None

**Accomplishments:**
- AlgoPrep project created from the ChatStory seed files in `chatStoryRef/`, package renamed to `com.algoprep`
- `AppConfig` and `SettingsStore` (with `.bad` preservation and reported write failures)
- Three-pane `AppFrame`, theme persistence, minimal Settings tab, Ctrl+Shift+X and Ctrl+Shift+B shortcuts
- MAIN tab with instructions picker and Send Instructions via `sendRawPrompt` (`InstructionsSender`)
- Project `README.md`

**Metrics:**
- Files modified: not counted (new project)
- Tests passing: 79, 0 failures

**Lessons / Notes:**
Verified by the user on 2026-10-06 after the manual check against live ChatGPT. Decisions: kept the pinned JDK path in `gradle.properties`; included the theme-only Settings tab; `chatStoryRef/` stays until the end of DC5. `StatusReporter` messages share the status label with the browser state, so a state change replaces the text (also written to stderr). `send` checks readiness before `reset()` so a loading page is not hidden.
