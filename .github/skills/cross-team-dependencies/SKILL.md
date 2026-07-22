---
name: cross-team-dependencies
description: Returns EVERY cross-team Dependency issue on our sprint's (or one issue's) Epics — raw, unfiltered — plus our team name. YOU keep the open ones we're waiting on and reason about severity. Use when asked what we're blocked on, which team owns a blocker, what external dependencies threaten the sprint, or to prep sprint planning.
allowed-tools: mcp__scrum__dependency_radar, mcp__scrum__sprint_dependency_radar, Bash(./scrum deps*), Bash(./scrum sprint-deps*)
---

# Cross-Team Dependencies

Read-only. The tool **fetches the raw facts**; **you do the filtering and reasoning**. On this
instance a cross-team dependency is **not** a Jira issue link. It is a dedicated **`Dependency`-type
issue** linked to an **Epic** (via Epic Link), carrying two team fields:

- **`Depend On`** — the team we **wait on** (the blocker). Surfaced as `blockingTeam`.
- **`Dependent/s`** — the team **waiting** (the consumer). Surfaced as `waitingTeam`.

The radar scopes to an issue's (or the whole sprint's) Epics and returns **every** Dependency on
them — open and resolved, both directions — plus `ourTeam` (our configured identity, e.g. `RIO`). It
deliberately does **not** filter, count, or classify. That is your job.

What you get:

- `rootIssue` — the issue key, or `"active-sprint"` for the whole-sprint variant
- `ourTeam` — our team identity; compare against it to decide direction
- `dependencies[]` — every Dependency on the scope's Epics, RAW (unfiltered):
  `{ key, summary, status, statusCategory, blockingTeam, waitingTeam, epicKey, deliverySprint, deliveryEnd }`

## Step 1 — keep the open blockers WE face (from `dependencies[]`)

Keep a dependency only if **all** hold (this is the error-prone bit — apply it exactly, using
`ourTeam` from the report, e.g. `RIO`):

- `waitingTeam == ourTeam` — **we are the one waiting** (`Dependent/s`), and
- `blockingTeam != ourTeam` — someone else is the blocker (`Depend On`), and
- it is **open**: `status` is **not** in `{Closed, Solved, Cancel, Released, Resolved}` (case-insensitive;
  `Open`/`Acknowledged` are open on this instance).

Discard the rest: resolved ones, and reverse-direction ones where `Depend On == ourTeam` (those are
teams waiting on *us* — our obligations, not our blockers). Confirm the resolved-status set with the
human if the team's Dependency workflow uses different terminal statuses.

## Step 2 — report the kept blockers

Lead with the **team we wait on** and where the Dependency stands, plus its target delivery sprint:

> "We're waiting on **ROMA** (DEP-501, status Open) for Epic VAL-1 — ETA their `RIO Sprint 5`."

Those are the conversations to have before the sprint stalls. Feed open ones into
[delivery-risk](../delivery-risk/SKILL.md), where an open cross-team blocker on a story's Epic pushes
that story toward HIGH.

## Invoke

**One issue's dependencies** (uses the issue's Epic):
- MCP: `dependency_radar(issueKey)`
- REST: `./scrum deps VAL-123`

**The whole active sprint:**
- MCP: `sprint_dependency_radar()`
- REST: `./scrum sprint-deps`

No parameters: the tool returns the raw rows and `ourTeam`; the resolved-status set and direction rule
are applied by you in Step 1, so they can be adapted per conversation without a redeploy.

## Human-in-the-loop

**Ask the human** for the team's real resolved-status vocabulary if you are unsure — a wrong set turns
a solved dependency into a false blocker (or hides a live one). Fall back to the default set
(`Closed, Solved, Cancel, Released, Resolved`) only when unattended.

## Full detail

Output fields and error handling: [reference.md](reference.md).
