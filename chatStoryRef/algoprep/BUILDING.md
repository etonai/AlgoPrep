# Building and Running ChatStory

## Prerequisites

- **Java 21 JDK** (Temurin/OpenJDK recommended)
  - Confirmed working: Eclipse Temurin 21.0.7
  - Download: https://adoptium.net/
- **Internet access on first run** — JCEF Chromium binaries (~100 MB) are downloaded automatically

No other tools need to be pre-installed. The Gradle wrapper (`gradlew.bat`) downloads Gradle automatically on first use.

---

## JCEF Distribution

**Artifact:** `me.friwi:jcefmaven:146.0.10`
**Chromium version:** 146.0.7680.179
**Source:** https://github.com/jcefmaven/jcefmaven

This library downloads and extracts Chromium native binaries on first run. The binaries are stored in the `jcef-bundle/` directory at the project root. This directory is in `.gitignore` and must not be committed.

**jpackage compatibility:** To produce a self-contained Windows installer in a future DevCycle, replace the auto-download approach with the bundled-natives artifact:
```
me.friwi:jcef-natives-windows-amd64:jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179
```
See the [Hydraulic deployment guide](https://hydraulic.dev/blog/13-deploying-apps-with-jcef.html) for packaging details.

---

## Running the Application

```bat
gradlew.bat run
```

**On first run only:** JCEF will download and extract Chromium native binaries (~100 MB). This takes 1–3 minutes and prints progress to the console. Subsequent launches start immediately.

## Running Tests

```bat
gradlew.bat test
```

Test reports: `build/reports/tests/test/index.html`

---

## File Locations

| Purpose | Path |
|---------|------|
| JCEF native binaries | `jcef-bundle/` (project root, gitignored) |
| Browser profile / cookies | `%LOCALAPPDATA%\ChatStory\profile` |
| Optional config file | `%APPDATA%\ChatStory\config.properties` |

The profile directory stores your ChatGPT session cookies so you remain logged in between application restarts. It is created automatically on first launch.

---

## Config File (Optional)

Create `%APPDATA%\ChatStory\config.properties` to set a target chat URL:

```properties
target.chat.url=https://chatgpt.com
```

Do not commit real chat or project URLs. See `config.example.properties` for a template.

---

## Logging In

On first launch, ChatGPT will show its login page inside the embedded browser. Log in normally using your preferred method (Google SSO, email/password, etc.). After logging in once, the session persists across restarts via the profile directory.

To verify session persistence: log in, close the application, relaunch — you should land directly on ChatGPT without being prompted to log in again.

---

## DevTools

Click the **DevTools** button in the toolbar to open Chromium DevTools for the current page. This is used during development to inspect the live ChatGPT DOM and validate CSS selectors.

---

## Known Issues

- JVM warnings about "restricted method" from JCEF's native loader are expected and harmless on Java 21+.
- The Chromium registry error at startup (`Failed opening key Software\Chromium`) is expected and harmless in embedded contexts.
- If `gradle.properties` pins `org.gradle.java.home` to JDK 21, ensure that path is valid on your machine.

---

## Troubleshooting

**Application fails to start with UnsatisfiedLinkError or native library error:**
- Ensure `jcef-bundle/` was fully extracted (check that it contains `.dll` files on Windows)
- Delete `jcef-bundle/` and rerun `gradlew.bat run` to re-download

**ChatGPT does not load or shows a blank page:**
- Check internet connectivity
- Open DevTools and check the Console tab for errors

**Session does not persist after restart:**
- Confirm `%LOCALAPPDATA%\ChatStory\profile` directory exists and contains files after logging in
- Ensure the application was closed cleanly (window close button, not task-killed)
