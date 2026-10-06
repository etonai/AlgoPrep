# Project Template

This repository is a lightweight starting point for new projects.

It is intended to provide a small amount of durable project structure without adding placeholder files that need to be cleaned up later.

## Purpose

This template is meant to give new projects:

- a standard project context entry point in `AGENTS.md`
- compatibility with workflows that use `CLAUDE.md` as a project context file
- a lightweight planning workflow in `doc/planning/DevelopmentProcess.md`
- a default `DevCycleTemplate.md` that projects may keep or adjust
- a clean foundation that can be adapted without unnecessary template residue

## Agent Context Standard

`AGENTS.md` is intended to be the standard first-read document for agents with little or no project context.

When starting work in a project created from this template, users should direct agents to read `AGENTS.md` first.

This is intended to be the standard across all projects created from this template.

## Important

When you create a real project from this template, you should replace this `README.md` with a project-specific README.

This is the one document in the template that is expected to be fully overwritten for the new project.

## Getting Started

After creating a new project from this template:

1. Overwrite `README.md` with the actual project description.
2. Tell agents with no context to read `AGENTS.md` before starting work.
3. Update `AGENTS.md` with project-specific context as needed.
4. Review `DevCycleTemplate.md` and keep it as-is or adjust it for the new project.
5. If your workflow uses `CLAUDE.md`, keep it aligned with `AGENTS.md` or adapt as needed for Claude Code.
6. Continue using `doc/planning/DevelopmentProcess.md` as the basis for planning and execution.

## Project Owner Responsibility

The template includes a `DevCycleTemplate.md` file for new projects to use as a starting point.

The project owner may keep that template as-is or adjust it to better fit the project's planning style.

`doc/planning/DevelopmentProcess.md` describes how DevCycle documents are used, while `DevCycleTemplate.md` provides the default starting structure.
