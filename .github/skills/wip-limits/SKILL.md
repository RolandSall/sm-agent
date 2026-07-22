---
name: wip-limits
description: Report how many Jira stories are in progress at once, per assignee and team-wide, against the team's WIP limits. Use when asked who has too much in flight, whether WIP limits are breached, or why flow feels stuck with lots started and little finished.
allowed-tools: mcp__scrum__wip_status, Bash(./scrum wip*)
---

# WIP Limits

Read-only. The tool **counts in-progress work**; **you judge it against the limit**. In-progress is detected via Jira's built-in status **category**, so it works regardless of the team's exact status names. The tool returns exact counts and echoes the limits you supplied — it computes **no** over-limit flag.

The tool returns:

- `perAssignee[]` — `{ assignee, inProgress, limit }` per person (unassigned bucketed as "Unassigned"); `inProgress` is the exact count, `limit` is the per-assignee number you passed, echoed back (may be `null`).
- `teamInProgress` — exact team-wide sum; `teamLimit` — the team number you passed, echoed back (may be `null`).

## Invoke

- **MCP (preferred):** `wip_status(wipLimitPerAssignee, wipLimitTeam, issueType, assignee)` — all optional.
- **REST fallback:** `./scrum wip --per-assignee 3 --team 15 [--type Story] [--assignee "Jane Dev"]`

WIP limits are **team policy passed at call time**, not server config. Use the team defaults unless the human gives different numbers:

| Parameter | Team default | Meaning |
|---|---|---|
| `wipLimitPerAssignee` | **3** | max issues one person should have in progress |
| `wipLimitTeam` | **15** | max in-progress across the team |
| `issueType` (REST: `--type`) | all types | Only count issues of this type, e.g. `Story` (default: all types) |
| `assignee` (REST: `--assignee`) | all assignees | Only count issues assigned to this person (default: all assignees) |

Omit a limit to report counts without any number to judge against.

## Judge over-limit yourself — counts are facts, the verdict is yours

The tool deliberately returns **no** over-limit flag. You decide:

- **Per assignee** — someone is over **only** when `inProgress > limit` **and a `limit` was supplied** (non-`null`). Name **who** exceeds the supplied number.
- **Team** — the team is over **only** when `teamInProgress > teamLimit` **and `teamLimit` was supplied**.
- **When no limit was supplied** (`limit`/`teamLimit` is `null`), **withhold any flag** — report the bare count and say no limit was set. Never invent a threshold to have something to compare against.

Frame every flag as **"over the number you supplied"**, not "objectively overloaded" — the verdict is only as meaningful as the limit passed. Lead with the counts; a reader can re-judge against a different limit without re-querying. **If you don't know the team's real limit, ask for it** (see below) rather than judging against a guess.

## Human-in-the-loop

WIP/capacity limits are **team policy, not a server setting** — **ask the human** for the team's real per-assignee and team limits before flagging anyone as over. Use the `3` / `15` team defaults only when unattended; when a limit is genuinely unknown, omit it and report bare counts rather than inventing a threshold.

## Report like this

Call out **who** is over the per-assignee limit and whether the team total breaches its limit. Recommend finishing in-flight work before pulling new items. High WIP often explains a stalled burndown — cross-check [sprint-metrics](../sprint-metrics/SKILL.md) and [sprint-flow](../sprint-flow/SKILL.md).

## Full detail

Output fields and error handling: [reference.md](reference.md).
