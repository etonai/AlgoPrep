# AlgoPrep

AlgoPrep is a personal Windows desktop tool for practicing coding-interview problems (LeetCode style). You solve each problem yourself, in your own editor, and use ChatGPT only as a hint-giver. AlgoPrep removes the friction around that: choosing a problem, showing it, sending ChatGPT your instructions, and attaching the right files.

ChatGPT runs in an embedded browser (JCEF), and AlgoPrep drives the real ChatGPT web UI. It is not an API client. It does not run code, edit solutions or grade anything.

It is built on code reused from the author's ChatStory project (Java 21, Swing, JCEF, Gson, Gradle).

## Status

Early development. DevCycle 001 is in progress: the first five phases are implemented and the manual check against live ChatGPT (Phase 6) is still to do, so nothing is marked Verified.

| Works now | Planned |
|---|---|
| Three-pane window with embedded ChatGPT | Problem list with filter and refresh (DC2) |
| Dark and light themes, remembered between runs | Markdown display of problem, notes and your notes (DC3) |
| MAIN tab: choose an instructions file and **Send Instructions** | Upload of the selected problem's files (DC4) |
| Settings saved in `settings.json` | Remaining keyboard shortcuts and polish (DC5) |

The Problems tab, the left display pane, the Problem panel and the Upload button are placeholders for now. The full design is in `doc/planning/AlgoPrepPlan-Claude02.md`.

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

More shortcuts are planned for DC5.

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
