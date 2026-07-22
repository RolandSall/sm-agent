---
name: logged-hours
description: >
  Per-assignee logged hours vs original estimate vs remaining (with team totals) for the active
  sprint, from native worklog (or a custom field if configured) — to spot under/over-logging.
---

# Logged Hours Skill

Summarize logged work across the active sprint. **Read-only.** Logged hours come from Jira's
**native time-tracking** (`timetracking.timeSpentSeconds` / individual `worklog` entries) by default;
if a specific instance stores work in a custom field instead, set `scrum.jira.fields.logged-hours`
and that field is used instead. Estimates come from Jira's time-tracking block either way.

> **Know which source your instance uses.** Most have **no logged-hours custom field**, so
> `logged-hours` is left unset and the native worklog fallback applies. Either way the numbers are only
> as good as the team's logging habit: if people don't log work, `logged` reads 0 even when work happened.
> Pair this with [estimate-variance](../estimate-variance/reference.md), which turns these numbers into flags.

## When to use
- Mid/late sprint, to confirm worklogs are being kept up to date.
- To compare effort spent against original estimates.

## Prerequisites
- **Mode A (MCP):** tool `worklog_summary`.
- **Mode B (REST):** run `./scrum <cmd>` against the running server — see [README](../README.md).
- **Config:** Tier-1 Jira connection. `scrum.jira.fields.logged-hours` is **optional** — leave it unset
  to use native worklog (the default for this instance), or point it at a custom-field id if the
  instance tracks work there.

## Commands
### Summarize logged work for the active sprint
- **MCP tool:** `worklog_summary` — all arguments optional (Tier-2 team policy):
  - `issueType` — only roll up issues of this type (default: all types).
  - `assignee` — only roll up issues for this person (default: all assignees).
  - `hoursPerDay` — working-hours-per-day divisor for the derived `*Days` figures (default: the
    configured working-hours-per-day, normally 8).
- **REST (when MCP is disabled):**
  ```bash
  ./scrum worklog [--type Story] [--assignee "Alice"] [--hours-per-day 6]
  # or raw curl (dynamic port from .scrum-env):
  source .scrum-env && curl "$SCRUM_API_URL/api/worklog?hoursPerDay=6"
  ```

## Output
Returns a `WorkloadReport` (hours are authoritative; counts are exact):
- `perAssignee[]` — `{ assignee, logged, originalEstimate, remaining, loggedDays, originalEstimateDays,
  remainingDays, issueCount, worklogCount }` (unassigned bucketed as "Unassigned"). The `*Days` are the
  same hours re-expressed as working-days via `hoursPerDay`; the hours are never altered.
  - `issueCount` — issues assigned to that person this sprint (exact count).
  - `worklogCount` — of those, how many carry any logged time (`logged > 0`) — exact count. A negative
    or missing worklog is clamped to 0 and does **not** count.
- `totals` — the summed-hour fields across the sprint.

## How to interpret / act
Report hours logged per assignee and the sprint total. Crucially, **`logged 0` is ambiguous** — use the
counts to disambiguate rather than reporting "0 hours worked":
- `issueCount > 0` **and** `worklogCount == 0` → work assigned, **nothing logged** (the *never-logged*
  signal — a tracking gap, not proof no work happened).
- `worklogCount` below `issueCount` → partial logging; some issues tracked, some not.
- `worklogCount == issueCount` with low hours → genuinely low *tracked* effort.

Flag anyone conspicuously low (may just be a missing worklog — see the counts) and anyone whose logged
hours far exceed their estimate (possible under-estimation — feed that back into
[sprint-metrics](../sprint-metrics/reference.md)). The "conspicuously low" line is **team policy** — a
human-in-the-loop should confirm the threshold and whether a low count is expected before flagging a person.

## Error handling
- An issue with no logged time contributes **0** hours and does not increment `worklogCount` (no error).
- A negative logged value is clamped to 0 (bad data) and likewise does not count toward `worklogCount`.
- All-zero output (`worklogCount 0` team-wide) means either the team isn't logging work, or (if you set
  `logged-hours`) the custom field id is wrong. On this instance, all-zero most likely means work simply
  isn't being logged.

## Related skills
[estimate-variance](../estimate-variance/reference.md) · [sprint-metrics](../sprint-metrics/reference.md) · [board-hygiene](../board-hygiene/reference.md) · [README](../README.md)
