# AlgoPrep

AlgoPrep is a personal Windows desktop tool for practicing coding-interview problems (LeetCode style). You solve each problem yourself, in your own editor, and use ChatGPT only as a hint-giver. AlgoPrep removes the friction around that: choosing a problem, showing it, sending ChatGPT your instructions, and attaching the right files.

ChatGPT runs in an embedded browser (JCEF), and AlgoPrep drives the real ChatGPT web UI. It is not an API client. It does not run code, edit solutions or grade anything.

It is built on code reused from the author's ChatStory project (Java 21, Swing, JCEF, Gson, Gradle).

## Status

DevCycles 1-4 are Verified: every core capability in the plan is in place. DevCycle 5 is ad hoc testing, with two small fixes (a Reset button and an Upload button on the Problems tab).

| What it does |
|---|
| Three-pane window: your problem and notes on the left, ChatGPT in the middle, controls on the right |
| **Problems** tab: filter, refresh and select a problem from your PROBLEMS directory |
| Left pane: the selected problem's statement, supplied notes and your own saved notes, as rendered Markdown, with **+** and **-** buttons to change the text size (remembered between runs) |
| **Send Instructions** sends your instructions file to ChatGPT. **Reset** recovers if a send gets stuck |
| **Upload** stages the selected problem's files and opens ChatGPT's file dialog, from the MAIN tab or the Problems tab |
| **Studied** records today's date for the selected problem, and the Problems list shows `(STUDIED 2026-10-07)` on it. **Clear Studied Tag** removes it |
| An optional **study list** (a CSV such as `Grind75.csv`) adds a tab listing those problems in the file's order, with difficulty and time. Set it in Settings |
| Dark and light themes, and all settings, remembered in `settings.json` |

Still to come: more keyboard shortcuts and polish. The full design is in `doc/planning/AlgoPrepPlan-Claude02.md`.

## Requirements

- Windows
- Java 21 JDK (Eclipse Temurin 21 recommended, https://adoptium.net/)
- Internet access on the first run, to download about 100 MB of Chromium binaries
- A ChatGPT account

No other tools are needed. The Gradle wrapper downloads Gradle on first use.

`gradle.properties` pins `org.gradle.java.home` to `C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot`. If your JDK 21 is somewhere else, change that line or delete the file.

## Running

```bat
gradlew.bat run
```

On the first run, JCEF downloads and unpacks Chromium into `jcef-bundle/` at the project root. This takes one to three minutes and needs internet access. Later launches start immediately.

### First-time setup

1. When the window opens, log in to ChatGPT in the middle pane. AlgoPrep uses its own browser profile, so you log in once and the session persists.
2. Write an instructions file (a plain UTF-8 text or Markdown file telling ChatGPT how to help). A starting draft is in section 9 of the plan.
3. On the **MAIN** tab, press **Browse...** under Instructions and choose that file.
4. Press **Send Instructions** once ChatGPT shows as Ready. The file is read each time you press the button, so edits take effect on the next send.

Sending puts the file's text into ChatGPT as a normal message and does not wait for a reply. If a send cannot be confirmed, AlgoPrep tells you to check ChatGPT and does not retry on its own, since a retry could duplicate the message. Starting a new conversation is up to you.

### Keyboard shortcuts

These work with focus in the browser or in the window.

| Shortcut | Action |
|---|---|
| Ctrl+Shift+B | Focus the browser |
| Ctrl+Shift+X | Reset the app state to Ready (recovery if it gets stuck) |

The **Reset** button next to **Send Instructions** on the MAIN tab does the same thing as Ctrl+Shift+X. Use it if a send is never confirmed and the app seems stuck: it forces the state back to Ready so Send Instructions and Upload work again. It does not undo anything that was already sent, so check ChatGPT to see whether the message arrived before sending again.

More shortcuts are planned.

### Uploading a problem's files

Select a problem on the **Problems** tab (double-click or Enter), then press **Upload**. There is an Upload button on the MAIN tab and one at the bottom of the Problems tab. They are the same, and both upload the **selected** problem (shown beside the button on the Problems tab), not just the row you have highlighted.

AlgoPrep copies the statement, the supplied notes and your saved notes (the ones that exist) into `<staging directory>\algoprep-upload` and opens ChatGPT's file dialog. In the dialog, press Ctrl+A, then Open, and add a message in ChatGPT if you want one. AlgoPrep never uploads solutions or test cases.

### Tracking what you have studied

Select a problem and press **Studied** after you have successfully studied it. There is a Studied button (with **Clear Studied Tag**) on the MAIN tab next to Upload, and one at the bottom of the Problems tab. They are the same, and both act on the **selected** problem. Studied records today's date, and pressing it again later replaces the date. Afterwards a dialog asks "Copy Studied prompt to clipboard?" (Yes is the default). Yes copies `Studied leetcode problem #1` (with the problem's number) to the clipboard so you can paste it into ChatGPT, and No copies nothing. The date is recorded either way. Only the last date is kept. The Problems list then shows `(STUDIED 2026-10-07)` on that row, and a large light gray `STUDIED 2026-10-07` banner appears at the top of the **Problem** tab in the display window while that problem is selected. **Clear Studied Tag** removes the date if you pressed Studied by mistake, after asking "Are you sure?" (the default answer is No). Whenever you open the Problems tab or the study list tab, the selected problem (if there is one) is highlighted and scrolled into view.

The dates are kept in `AlgoPrep_studied.csv` in your **HOME** directory, next to your notes, so the buttons need HOME to be set. It is a plain file with one `key,date` row per problem, and you can edit it by hand. **Refresh**, or switching to the Problems tab, re-reads it. If it cannot be read at all it is renamed to `AlgoPrep_studied.csv.bad` and AlgoPrep starts with no dates. This is the only file AlgoPrep writes in HOME. Your notes are never changed.

### Study lists

In **Settings**, the optional **Study List File** setting takes a CSV file. When it is set, a tab titled with the file's name (`Grind75.csv` gives `Grind75`) appears after **Problems**. Clear the setting and the tab goes away. The tab works like the Problems tab: filter, Refresh, double-click or Enter to select, and the same Upload, Studied and Clear Studied Tag buttons. Rows keep the file's order and show the same `(selected)` and `(STUDIED date)` tags. AlgoPrep only reads the file, it never changes it.

The file has one problem per line, `key, difficulty, time`. The difficulty and time are free text and optional. Blank lines and lines starting with `#` are ignored. A first line naming the columns (`problem,difficulty,time`) is skipped, so a file saved from a spreadsheet works as is:

```
problem,difficulty,time
0001_two-sum, Easy, 20 minutes
15_not-found, Tough, 15 minutes
99_no-difficulty, , 2 minutes
105_no-time, Medium,
```

A key is matched to a problem in the PROBLEMS directory by its number and name, so `99_no-difficulty` finds `0099_no-difficulty` and you do not have to pad the numbers. Rows show as:

```
1 - Two Sum, Easy, 20 minutes
15_not-found, Tough, 15 minutes - NOT FOUND
99 - No Difficulty, 2 minutes
105 - No Time, Medium
```

A problem with no files in the PROBLEMS directory is shown as NOT FOUND and cannot be selected. The line under the list says how many problems are in it, how many are studied, and how many were not found. **Refresh**, or switching to the tab, re-reads the file. If the file cannot be read, the tab still appears and says why.

### DevTools

The **DevTools** button opens Chromium DevTools for the current page. This is used to inspect ChatGPT's page when its selectors need updating. They live in `src/main/resources/js/chatgpt_selectors.json`.

## Tests

```bat
gradlew.bat test
```

Reports are written to `build/reports/tests/test/index.html`. A live ChatGPT page cannot be unit tested, so sending is checked by hand.

## Where things are stored

| Item | Location |
|---|---|
| Chromium binaries | `jcef-bundle/` (project root, git-ignored) |
| Browser profile and ChatGPT login | `%LOCALAPPDATA%\AlgoPrep\profile` |
| Settings (theme, instructions file) | `%APPDATA%\AlgoPrep\settings.json` |
| Studied dates | `AlgoPrep_studied.csv` in your HOME directory |
| Upload staging (used from DC4) | `%LOCALAPPDATA%\AlgoPrep\upload-staging` |
| Optional config | `%APPDATA%\AlgoPrep\config.properties` |

If `settings.json` cannot be read, it is renamed to `settings.json.bad` and defaults are used. To open a specific ChatGPT conversation on startup, set `target.chat.url` in `config.properties` (see `config.example.properties`). Do not commit real conversation URLs.

AlgoPrep and ChatStory keep separate profiles and settings, so they can run side by side.

`BUILDING.md` has more detail on the build, JCEF and troubleshooting.

## Project layout

```
src/main/java/com/algoprep/
  Main, AppFrame, AppState, UiThread
  browser/       embedded ChatGPT (copied from ChatStory)
  bridge/        talks to ChatGPT's page (copied from ChatStory)
  config/        AppConfig, SettingsStore
  instructions/  InstructionsSender
  theme/         dark and light themes
  ui/            MainPanel, SettingsPanel, DisplayPanel, StatusReporter
src/main/resources/js/   scripts and selectors injected into ChatGPT
doc/planning/            plan and DevCycle documents
chatStoryRef/            read-only snapshot of ChatStory files used as reference
```

`chatStoryRef/` is not part of the build. Do not edit it.

## Development process

Work is organized into DevCycles. Start with `AGENTS.md`, then `doc/planning/DevelopmentProcess.md`. The active cycle is `doc/planning/DevCycle001.md`, and the overall design is `doc/planning/AlgoPrepPlan-Claude02.md`.

## License

MIT. See `LICENSE`.
