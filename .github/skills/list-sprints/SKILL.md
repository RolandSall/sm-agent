---
name: list-sprints
description: Enumerate the board's sprints (id, full name, state, dates) so you can suggest which sprint to target given only the board id. Use when the human asks "which sprint", "what sprints are active", "list the sprints", or before any active-sprint tool when several sprints may be running at once and you need to confirm the right one by name.
allowed-tools: mcp__scrum__list_sprints, Bash(./scrum sprints*)
---

# List Sprints

Read-only. Enumerates the configured board's sprints straight from Jira (`/rest/agile/1.0/board/{id}/sprint`) — id, full name, state, start/end/complete dates, goal. Nothing is computed; this is the raw list a human uses to pick a sprint.

## Why this exists

A board can have several sprints **active at once** (parallel teams, PREP/PI overlaps). The other active-sprint tools resolve *the* current sprint themselves, disambiguating with the optional `JIRA_SPRINT_FILTER` substring fallback — and when that is unset the server picks by date/first. That guess can be wrong. `list_sprints` lets you avoid guessing:

**Recommended flow:** call `list_sprints` → show the human the full sprint names → let them pick → target that one.

## Invoke

- MCP: `list_sprints(state)` — `state` optional, defaults to **active**.
- REST: `./scrum sprints` (or `./scrum sprints closed`).

## Parameters

| Parameter | Team default | Notes |
|---|---|---|
| `state` | **active** | Jira sprint state: `active`, `future`, `closed`, or a comma-combination (e.g. `active,future`). Null/blank falls back to `active`. |

## Report like this

List the sprints by **full name** with their state (and dates when closing/planning matters). If more than one is `active`, say so explicitly and **ask the human which one to target** rather than assuming — that is the whole point of this tool.

## Full detail

Output fields and error handling: [reference.md](reference.md).
