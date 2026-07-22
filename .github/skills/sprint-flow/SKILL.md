---
name: sprint-flow
description: Daily-standup flow view of the active Jira sprint from the changelog. The tool returns every active-sprint ticket RAW — statusCategory, ageInStatusDays, daysSinceUpdated, reopenCount — as one flat list; YOU apply the stuck / stalled / reopened lines. Use when asked what's stuck, what's not moving, what got reopened, or to prep standup.
allowed-tools: mcp__scrum__sprint_flow, Bash(./scrum sprint-flow*)
---

# Sprint Flow

Read-only. The morning-standup view — surfaces tickets needing a conversation *today*. Derived from
each issue's changelog, so it sees movement, not just current state. The tool **fetches the raw
ingredients**; **you do the classifying**. It returns one flat list and computes no verdict.

## Invoke

- **MCP (preferred):** `sprint_flow(issueType, assignee)` — both optional.
- **REST fallback:** `./scrum sprint-flow [--type Story] [--assignee "Jane Dev"]`

| Param | Default | Meaning |
|---|---|---|
| `issueType` (REST: `--type`) | all types | Only include issues of this type, e.g. `Story` (default: all types) |
| `assignee` (REST: `--assignee`) | all assignees | Only include issues assigned to this person (default: all assignees) |

## What comes back

A single `issues[]` — **every** active-sprint issue (after any type/assignee scoping), each with its
raw flow ingredients and **no bucketing**:

`{ key, summary, assignee, statusCategory, status, ageInStatusDays, daysSinceUpdated, reopenCount }`

- `statusCategory` — Jira's own bucket (`To Do` / `In Progress` / `Done`), passed through verbatim.
  **Use it as given — never re-derive it from the status name.**
- `ageInStatusDays` — whole days in the current status, or **`null`** if the issue has never changed
  status (age **unknown**, *not* "stuck").
- `daysSinceUpdated` — whole days since last update, or `null` if absent.
- `reopenCount` — status changes after the issue first reached a done-like status (raw churn proxy).

## Step 1 — apply the three lines (this is your job, not the tool's)

Ask the human where the lines sit (see below), then classify each issue:

- **STUCK** — `statusCategory == "In Progress"` **and** `ageInStatusDays >= stuck line`. A `null`
  age is unknown, so it is **not** stuck.
- **STALLED** — `statusCategory != "Done"` **and** `daysSinceUpdated >= stalled line`. A `null`
  days-since-updated is unknown, so it is **not** stalled.
- **REOPENED** — `reopenCount >= reopen line`.

The lines overlap on purpose — one issue can be both stuck and reopened. Everything else is healthy;
don't invent a bucket for it.

## Step 2 — report like this

Lead standup with the three groups (an issue can appear in more than one):

> "2 tickets are stuck — PROJ-88 has been In Review 9 days; 1 hasn't moved in 4 days; PROJ-40 was
> reopened once."

Quote the raw ingredient and the line you applied, so the number is debatable.

- `stuck` + a cross-team link → check [cross-team-dependencies](../cross-team-dependencies/SKILL.md).
- Repeated `reopened` → retro material, a definition-of-done conversation.

## Human-in-the-loop

The lines are **team policy, not tool defaults** — there are no thresholds baked into the tool
anymore. **Ask the human** where they sit before flagging: a common starting point is stuck ≥ 5
days, stalled ≥ 3 days, reopen ≥ 1, but whether one reopen is "a problem" or normal churn is the
team's call. Only fall back to those sensible defaults when unattended, and say so.

## Caveat worth stating

"Done-like" detection (behind `reopenCount`) is keyword-based
(closed/done/resolved/released/cancel/solved/approved). A custom terminal status outside that list
means a reopen can be missed. See [reference.md](reference.md) for how to verify against real data.

## Full detail

Output fields and error handling: [reference.md](reference.md).
