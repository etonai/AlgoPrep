# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Getting Started

Read `AGENTS.md`, then `doc/planning/DevelopmentProcess.md`. These are the authoritative sources for how work in this project is organized and executed.

## Development Process

Work is organized into **DevCycles** — short focused sprints with a clear goal, a set of tasks, and an end-of-cycle review.

### Planning files

- `doc/planning/` — active DevCycle documents
- `doc/planning/completed/` — finished DevCycle documents
- `doc/planning/DevelopmentProcess.md` — authoritative workflow reference

### DevCycle lifecycle

1. Create a new DevCycle document in `doc/planning/`.
2. Define the goal and break it into concrete tasks.
3. Work through the tasks, updating the document as work progresses.
4. When planned work is done, record notes and move the document to `doc/planning/completed/`.

### Status values

| Status | Meaning |
|---|---|
| Planning | Defined but not started |
| In Progress | Actively underway |
| Work Complete | Implementation done, pending user verification |
| Verified | Confirmed correct — **user permission required** |

Claude may not set any DevCycle or phase to `Verified` without explicit user approval. Stop at `Work Complete` and report that verification is pending.

### Naming

DevCycle files: `DevCycle001.md`, `DevCycle002.md`, etc.
Commit message shorthand: `DC-1`, `DC-01`, `DC-001`.

## Git Rules

Claude Code must not run `git add` or `git commit` under any circumstances. The user manages all staging and committing. Do not offer to commit, do not stage files, and do not run any git command that modifies the index or commit history.
