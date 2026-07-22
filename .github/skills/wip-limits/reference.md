---
name: wip-limits
description: >
  Report how many stories are in progress at once — per assignee and for the whole team —
  against the team's WIP limits, to catch too much work started in parallel.
---

# WIP Limits Skill

Detect "too many stories in progress at the same time". **Read-only.** In-progress is detected via
Jira's built-in status category (`In Progress`), so it works regardless of the team's exact status names.

## When to use
- During standup, to spot people juggling too many tickets.
- When flow feels stuck and you suspect low finishing / high starting.

## Prerequisites
- **Mode A (MCP):** tool `wip_status`.
- **Mode B (REST):** run `./scrum <cmd>` against the running server — see [README](../README.md).
- **Config:** Tier-1 Jira connection. WIP limits are **not** stored server-side — you pass them.

## Commands
### Check WIP against limits
- **MCP tool:** `wip_status(wipLimitPerAssignee, wipLimitTeam, issueType, assignee)` — all optional.
- **REST (when MCP is disabled):**
  ```bash
  ./scrum wip --per-assignee 3 --team 15   # limits 3 / 15
  ./scrum wip                              # counts only, nothing flagged
  ./scrum wip --per-assignee 3 --team 15 --type Story --assignee "Jane Dev"
  # or raw curl (dynamic port from .scrum-env):
  source .scrum-env && curl "$SCRUM_API_URL/api/wip?perAssignee=3&team=15"
  ```

## Parameters
WIP limits are **team policy supplied at call time** (not config). Use the team-standard values
unless the human gives you different numbers:

| Parameter | Team default | Notes |
|---|---|---|
| `wipLimitPerAssignee` | **3** | max issues one person should have in progress |
| `wipLimitTeam` | **15** | max in-progress across the team |
| `issueType` (REST: `issueType`, wrapper `--type`) | all types | Only count issues of this type, e.g. `Story` (default: all types) |
| `assignee` (REST: `assignee`, wrapper `--assignee`) | all assignees | Only count issues assigned to this person (default: all assignees) |

Omit a limit to report counts without flagging it.

## Output
Returns a `WipReport` — raw counts only, **no** over-limit verdict:
- `teamInProgress` — exact total in-progress issues; `teamLimit` — the team limit you passed, echoed back (may be `null`).
- `perAssignee[]` — `{ assignee, inProgress, limit }` per person (unassigned bucketed as "Unassigned"). `inProgress` is the exact count; `limit` is the per-assignee limit you passed, echoed back (may be `null`).

The counts are facts; the tool computes no `overLimit` / `teamOverLimit` flag. **You** decide over/under: someone is over only when `inProgress > limit` and a `limit` was supplied; the team is over only when `teamInProgress > teamLimit` and `teamLimit` was supplied. When a limit is `null`, withhold any flag and report the bare count. Treat any verdict as "over the number you supplied", not "objectively overloaded".

## How to interpret / act
Compare the counts to the limits **yourself** and call out **who** is over the per-assignee limit and
whether the **team** total breaches its limit. Suggest finishing in-flight work before pulling new
items. High WIP often explains a stalled [sprint-metrics](../sprint-metrics/reference.md) burndown.

**Ask the human** for the team's real per-assignee and team WIP limits before flagging anyone — these
are team policy, not a server setting. Use the `3` / `15` defaults only when unattended; when a limit
is genuinely unknown, omit it and report bare counts rather than inventing a threshold.

## Error handling
- No in-progress work → `teamInProgress: 0`, empty `perAssignee` — report "nothing in progress".

## Related skills
[board-hygiene](../board-hygiene/reference.md) · [release-queue](../release-queue/reference.md) · [sprint-metrics](../sprint-metrics/reference.md) · [README](../README.md)
