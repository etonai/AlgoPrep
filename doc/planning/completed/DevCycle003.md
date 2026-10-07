# DevCycle 003: Display

**Status:** Verified
**Start Date:** TBD
**Target Completion:** TBD
**Focus:** Show the selected problem's statement, supplied notes and personal notes as rendered, read-only Markdown in the left pane.

Design source: `doc/planning/AlgoPrepPlan-Claude02.md` (the "Plan"), mainly sections 6.3, 7, 7.1, 10, 11 and 13. Section numbers below refer to it. This cycle builds on DevCycle 002 (problem scanning and the selection model), which is Verified.

---

## Goal

Make the left pane useful. When a problem is selected, the **Problem**, **Notes** and **My Notes** tabs show the matching files as rendered Markdown, so the user can read the statement and their notes beside ChatGPT. This cycle also adds the **HOME directory** setting, where the user's own `*_AlgoPrepNotes.md` files live. Rendering is Swing's `JEditorPane`, not a second browser (Plan section 2), so the cycle includes checking that real problem files (fenced code, tables, long lines) look acceptable in both themes. The display is read-only, and the solution and test cases are never displayed or opened (Plan section 7).

## Desired Outcome

- Selecting a problem shows its statement in the **Problem** tab, its supplied notes in **Notes**, and the user's saved notes in **My Notes**.
- Missing content shows the plan's messages: `No supplied notes for this problem.`, `No saved notes.`, or `Select a HOME directory in Settings.` when HOME is unset.
- Edits made outside AlgoPrep show up without a file watcher: the files are re-read when a problem is opened and whenever the Notes or My Notes tab is selected.
- Markdown renders with headings, lists, emphasis, fenced and inline code in a monospaced font, and GFM tables. It is readable in both themes, and switching theme restyles the open content.
- Raw HTML in a Markdown file is shown as text, not interpreted. Remote images are never loaded, and links are not clickable (Plan section 7.1).
- The Settings tab has a **HOME directory** setting with `Browse...` and a warning when the saved path is missing.
- The new pure logic has unit tests, and the full build passes.

**Not in this cycle:** editing or creating notes, a plain-text source view, clickable links, upload and staging (DC4), shortcuts (DC5).

---

## Tasks

### Phase 1: Document Loading Logic

**Status:** Verified

- [x] Write `notes/HomeNotes`: given the HOME directory and a base key, find `<key>_AlgoPrepNotes.md` by listing the flat directory and matching the full name case-insensitively, keeping the real file name. Read only. No writing (Plan section 11).
- [x] Write a pure `display/DocumentLoader` that, for the current selection and HOME setting, returns a result for each of the three tabs. Each result is either content to render or a short message.
- [x] Messages (Plan section 7): Notes missing gives `No supplied notes for this problem.`; My Notes missing gives `No saved notes.`; HOME unset gives `Select a HOME directory in Settings.`.
- [x] Other cases: nothing selected gives `No problem selected.`; a selected problem whose statement is gone gives a message to refresh the list (Problem and Notes tabs), while My Notes still loads by key; HOME set but not found gives `HOME directory not found: <path>`; a file that cannot be read gives `Could not read <name>: <reason>`.
- [x] Read files as UTF-8, replacing malformed bytes rather than failing, and drop a leading byte-order mark.
- [x] Apply a size cap (see Open Question 1) with the message `<name> is too large to display (<size>).`.
- [x] Never open, list or mention solution or test-case files. The loader only touches the statement, the supplied notes and the HOME notes file.
- [x] Unit tests (with `@TempDir`): all three present; notes missing; HOME unset, HOME not found, HOME notes missing; full-key matching (HOME notes for `two-sum-ii` are not shown for `two-sum`); case differences in the HOME file name; nothing selected; unavailable problem; an unreadable file; invalid UTF-8; BOM; oversized file; and that a solution file sitting next to the statement is never read.

**Technical Notes:**
The loader takes plain values (`Optional<Problem>`, the unavailable flag, the selected key, the HOME string) so it has no Swing and no dependency on the settings class. The supplied notes path comes from the `Problem` record found at scan time, so a notes file deleted since the scan is treated as missing, not as an error. `HomeNotes` lists the directory instead of relying on the file system's case rules, to match `ProblemScanner` (see Open Question 2 for the package).

### Phase 2: Markdown Conversion and Styles

**Status:** Verified

- [x] Write a pure `display/MarkdownConverter` using `commonmark` and `commonmark-ext-gfm-tables` (the dependencies were added in DC1). Configure `HtmlRenderer` with `escapeHtml(true)` and `sanitizeUrls(true)` (Plan section 7.1).
- [x] Replace images with text such as `[image: alt text]`, so `JEditorPane` never fetches a URL (see Open Question 3).
- [x] Add table attributes (for example `border="1"` and `cellpadding="4"`) through an attribute provider, because Swing's HTML 3.2 renderer draws no borders from CSS alone.
- [x] Write a pure `display/MarkdownStyles` that builds the CSS for a theme: body font and colors, headings, `code` and `pre` in a monospaced font with preserved whitespace, tables, blockquotes, lists and link color. The dark and light colors match the field and text colors `NativeThemeApplier` already uses, so the pane does not look mismatched against the rest of the window (see Open Question 4).
- [x] Unit tests: raw HTML (`<script>`, `<b>`, `<img>`) comes out escaped; `![alt](http://x/y.png)` produces no `<img>`; `[x](javascript:alert(1))` is not emitted as a live link; a GFM table gets the border attributes; a fenced block becomes `<pre><code>`; headings, lists and emphasis convert; empty input gives empty output; and the dark and light CSS differ and both define `pre`.

**Technical Notes:**
Converting and styling are kept apart from the Swing view so everything security-relevant is testable. The exact CSS properties Swing honors are limited (CSS 1 subset), so the stylesheet is checked by eye in Phase 3 rather than by tests.

### Phase 3: MarkdownView

**Status:** Verified

- [x] Write `ui/MarkdownView`: a read-only `JEditorPane` with an `HTMLEditorKit`, a per-theme `StyleSheet` built from `MarkdownStyles`, inside a `JScrollPane`, modeled on ChatStory's `OutputPanel` (Plan section 7.1).
- [x] `show(markdown)` renders and resets the scroll position to the top. `showMessage(text)` shows a plain, muted message (not Markdown).
- [x] No `HyperlinkListener`, so links are not clickable. Text stays selectable so the user can copy from it.
- [x] Observe `NativeThemeModel`: when the theme changes, rebuild the kit and stylesheet and re-render the current content.
- [x] Make sure `NativeThemeApplier`, which sets the background and foreground of every `JEditorPane` and calls `updateComponentTreeUI`, does not undo the styling (see Notes and Risks).
- [x] Check long lines in code blocks and wide tables: the pane should scroll sideways rather than clip.
- [x] Try the real-world sample files (see Phase 6) in both themes. If rendering proves inadequate, record the specific failure. The fallback in the Plan is a plain-text view of the source, not a second browser.

**Technical Notes:**
Keep the Swing class thin: it takes already converted HTML and a theme. A fresh `StyleSheet` is built per theme and `HTMLEditorKit.getStyleSheet()` is never modified, because that style sheet is shared across the JVM.

### Phase 4: DisplayPanel and Reload Rules

**Status:** Verified

- [x] Replace the placeholder `DisplayPanel` with three fixed tabs (Problem, Notes, My Notes), each backed by a `MarkdownView`, always visible (Plan section 7).
- [x] Observe `SelectedProblemModel`: when the selection changes, load all three tabs. Also update when the selected problem is refreshed or becomes unavailable.
- [x] Reload the Notes and My Notes files whenever their tab is selected, so outside edits appear without a file watcher. Also reload on selecting the Problem tab, since it is cheap and avoids a stale statement.
- [x] Pass the HOME setting to the loader from the settings store, and update My Notes when HOME changes.
- [x] Show the messages from Phase 1 in the matching tab.
- [x] Keep it independent of the browser: the pane must work while ChatGPT is loading, signed out or failed (Plan section 4).
- [x] Update `AppFrame` and `Main` to pass in the selection model, settings and theme model.

**Technical Notes:**
Reading happens on the Swing thread. These are small local files, and a file watcher and background loading are deliberately left out (see Open Question 5). The tab-selection reload uses a `ChangeListener` on the tabbed pane. Because the user's own notes are written outside the app, an edit-then-switch-tab reload is the main way they will see changes.

### Phase 5: HOME Setting

**Status:** Verified

- [x] Add a **HOME directory** section to `SettingsPanel` with a path label and `Browse...`, matching the PROBLEMS section (Plan section 6.3).
- [x] Share the directory-chooser code between PROBLEMS and HOME instead of copying it.
- [x] Persist to `SettingsStore.setHomeDir` (the field already exists). Show a warning, and keep the path visible, when the saved path no longer exists.
- [x] Changing HOME updates the My Notes tab immediately.
- [x] Do not scan or create anything in HOME. It is only read when a problem is selected.

**Technical Notes:**
Unlike PROBLEMS, there is no catalog or scan for HOME, so label updates come from the settings listener. HOME can be changed independently of the problems (Plan section 5.1), and it does not clear the selection.

### Phase 6: Manual Verification and Closeout

**Status:** Verified

- [x] `gradlew.bat build` is green. Record the test counts.
- [x] Prepare sample content outside the repository: problem statements with headings, lists, bold and italic text, inline code, fenced code (including very long lines), a GFM table, a blockquote, a link, an image, raw HTML (`<script>`, `<b>`), non-ASCII text and a very large file; plus supplied notes for some problems and `*_AlgoPrepNotes.md` files in a HOME directory for some.
- [x] Select problems and confirm the three tabs show the right files, with the plan's messages for missing notes and an unset HOME.
- [x] Confirm raw HTML shows as text, the image shows as a placeholder with nothing fetched, links are not clickable, and tables, code and long lines are readable.
- [x] Edit a notes file in another editor, switch to its tab, and confirm the new text appears. Do the same for the statement.
- [x] Delete a statement, press Refresh, and confirm Problem and Notes show the unavailable message while My Notes still loads by key.
- [x] Change HOME in Settings and confirm My Notes updates. Point it at a missing folder and confirm the warning.
- [x] Switch between Dark and Light with content open, and confirm text, code blocks, tables and the background are readable in both.
- [x] Confirm the solution and test-case files never appear in any tab.
- [x] With ChatGPT loading or signed out, confirm the display works normally.
- [x] Complete the Completion Summary and report **Work Complete**. Only the user marks anything **Verified**.

**Technical Notes:**
The Plan asks for real problem files to be checked in both themes during this cycle (Plan section 7.1). Use a few of the user's own problem files if available, since they are the real test of the Swing renderer.

---

## Decisions

The five open questions from planning were resolved by adopting the recommendations:

1. **Size cap:** displayed files over 1 MiB show `<name> is too large to display (<size>).` and are not read.
2. **Packages:** `HomeNotes` is in `notes/`. `DocumentLoader`, `MarkdownConverter` and `MarkdownStyles` are in a new Swing-free `display/` package.
3. **Images:** shown as a text placeholder, `[image: alt]`, and never loaded.
4. **Theme colors:** duplicated in `MarkdownStyles` with a comment pointing at `NativeThemeApplier`, which is left untouched.
5. **File reads:** on the Swing thread, with no watcher.

---

## Implementation Results

Phases 1-5 are implemented. The full build passes with 191 tests and 0 failures (56 new). Phase 6 (the manual check) was closed by the user, who approved Verified without a step-by-step record of each manual item.

- **Phase 1:** `notes/HomeNotes` lists the flat HOME directory and matches `<key>_AlgoPrepNotes.md` by the full key, case-insensitively (8 tests). `display/DocumentLoader` has one method per tab (`problem`, `notes`, `myNotes`) taking plain values: the current problem if its statement is found, and the selected key (so "key but no problem" means unavailable). It reads UTF-8 with malformed bytes replaced, drops a BOM, reports empty and oversized files, treats a notes file deleted since the scan as missing, and loads My Notes by key even when the statement is gone. It opens only the statement, the supplied notes and the HOME notes, and a test confirms a solution and test-case file next to the statement are never read (20 tests).
- **Phase 2:** `display/MarkdownConverter` uses commonmark with the GFM tables extension: `escapeHtml(true)`, `sanitizeUrls(true)`, images replaced by a text placeholder through a custom node renderer, and `border`, `cellpadding` and `cellspacing` added to tables (15 tests). `display/MarkdownStyles` builds per-theme CSS rules and the page background (5 tests).
- **Phase 3:** `ui/MarkdownView` is a read-only `JEditorPane` inside a scroll pane, with a fresh `StyleSheet` per theme (the shared default is never modified), no hyperlink listener, and selectable text. Showing identical content again leaves the scroll position alone. On a theme change it restyles through `invokeLater`, so it runs after `NativeThemeApplier` has finished. `MarkdownViewTest` (8) exercises it without a window.
- **Phase 4:** `ui/DisplayPanel` has the three fixed tabs. It reloads all three when the selection changes, reloads the tab the user selects, and reloads My Notes when the HOME setting changes. `AppFrame` passes in the selection model, settings and theme model.
- **Phase 5:** `SettingsPanel` has a HOME directory section. The PROBLEMS and HOME sections share one `directoryControls` helper (path label, not-found warning, Browse). PROBLEMS labels follow the catalog and HOME labels follow the settings.
- **Look checked offscreen:** A temporary test painted the view in both themes (since deleted). Headings, inline and fenced code, a bordered table, nested lists, a blockquote, a link, the image placeholder and escaped raw HTML all rendered readably, and a long code line produced a horizontal scroll bar instead of clipping. One small flaw: an inline code span can sit tight against the preceding word. It is cosmetic.
- **Not yet confirmed in the real window:** the display pane has not been run inside AlgoPrep itself.
- **Test data for Phase 6:** generated outside the repository in the session scratchpad: `...\scratchpad\display-problems` (a full-featured statement with supplied notes, a solution and test-case file that must never appear, a problem without notes, a 1.5 MB file, an empty file, and a problem whose only HOME notes belong to a similar key) and `...\scratchpad\display-home` (saved notes).

---

## Notes and Risks

- **Risk: `NativeThemeApplier` and `JEditorPane`.** The applier sets the background and foreground of every `JEditorPane` and calls `updateComponentTreeUI`. This could clash with the HTML body colors or reset the editor kit. Mitigation: match the palette (Question 4), verify in both themes during Phase 3, and re-apply the kit and re-render after a theme change.
- **Risk: Swing's HTML 3.2 renderer.** Tables, code blocks and long lines may look rough. Mitigation: table attributes, a tested stylesheet, an early look at real files, and the plain-text fallback from the Plan if it is not good enough.
- **Risk: untrusted file content.** Problem and notes files can contain anything. Mitigation: escaped raw HTML, sanitized URLs, no image loading, no clickable links, and tests for each.
- **Risk: stale content.** There is no file watcher. Mitigation: reload on problem selection and on tab selection.
- Solutions and test cases are never read. The loader only opens the three files above.
- `chatStoryRef/` is still read-only reference. `OutputPanel.java` there shows the `JEditorPane` and `StyleSheet` approach.
- Git is managed by the user. Agents do not run git commands in this project.
- Creating this document does not authorize implementation. Work starts only on the user's explicit instruction.

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** 2026-10-06
**Phases Completed:** All (1-6)
**Work Deferred:** None. Clickable links, a plain-text source view and editing notes remain out of scope.

**Accomplishments:**
- `HomeNotes` and the Swing-free `display/` package: `DocumentLoader`, `MarkdownConverter` (escaped HTML, sanitized URLs, image placeholders, table borders) and `MarkdownStyles`
- `MarkdownView`: read-only, theme-aware `JEditorPane` rendering
- `DisplayPanel`: Problem, Notes and My Notes tabs, reloaded on selection and tab change
- HOME directory setting, sharing the directory-chooser code with PROBLEMS

**Metrics:**
- Files: 5 new main classes (`HomeNotes`, `DocumentLoader`, `MarkdownConverter`, `MarkdownStyles`, `MarkdownView`), 3 changed (`DisplayPanel` rewritten, `SettingsPanel`, `AppFrame`), 5 new test classes
- Tests passing: 191, 0 failures (56 new)

**Lessons / Notes:**
Verified by the user on 2026-10-06. Painting the view offscreen in a throwaway test was a quick way to check the Swing renderer in both themes without a window. The theme restyle is deferred with `invokeLater` so it runs after `NativeThemeApplier`. Known cosmetic flaw: an inline code span can sit tight against the preceding word. Files are read on the Swing thread with a 1 MiB cap and no file watcher.
