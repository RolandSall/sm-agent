---
name: feature-effort-rollup
description: Roll up each feature (Epic) into effort ingredients — Σ story points, Σ estimate hours, Σ logged hours, story count, plus the epic's own dev-job number — so you can spot features where logged effort is drifting from the estimate. Use when asked "which features are over/under on effort", "is any epic off by a lot", "how much has been spent vs estimated per feature".
allowed-tools: mcp__scrum__feature_effort_rollup, Bash(./scrum feature-effort*)
---

# Feature Effort Roll-up (per Epic)

Read-only. For each feature (an **Epic** on this instance), sums its child stories into effort
**ingredients** computed **live** from the children. It returns **numbers only — no verdict**. YOU
decide which features are "off by a lot".

## Invoke

- **MCP (preferred):** `feature_effort_rollup(epicKeys)` — omit `epicKeys` to scope from the active sprint.
- **REST fallback:** `./scrum feature-effort PROJ-100 PROJ-215` (or `./scrum feature-effort` for active-sprint scope).

| Parameter | Default | Notes |
|---|---|---|
| `epicKeys` (REST: `epic`, repeatable) | active-sprint epics | Which epics to roll up. Omit → the DISTINCT epics of the active sprint's issues. |

## How to assess (this is the whole point)

Per feature you get `{epicKey, epicSummary, devJobPoints, sumStoryPoints, sumEstimateHours, sumLoggedHours, sumEstimateDays, sumLoggedDays, storyCount, stories[]}`. `sumEstimateDays`/`sumLoggedDays` are derived working-days from the authoritative `sumEstimateHours`/`sumLoggedHours` via `WorkingDays.fromHours` (the hours are the source of truth).

- **The meaningful "off by a lot" signal is HOURS:** compare `sumEstimateHours` vs `sumLoggedHours`,
  and look at hours-per-point (`sumLoggedHours / sumStoryPoints`) **across** features — an outlier
  feature is the flag.
- **Points are a weak signal here.** The epic's own `devJobPoints` ("Planned Dev Job") is a synced
  snapshot that **drifts** from the live child sum, so `devJobPoints ≈ sumStoryPoints` and neither
  tells you much about effort. Treat `devJobPoints` as a **reference only**; trust the live sums.
- **No baked threshold.** There is deliberately no "over budget" flag — you judge over the numbers,
  and say which features look off and why.

## Caveats

- **`feature = Epic` is a config default** (`scrum.jira.feature-issue-type`), swappable to a real
  "Feature" issue type per instance.
- **Zero logged hours means nobody logged time, not that no work happened** — if the team doesn't log
  work, `sumLoggedHours` is near zero everywhere and the hours signal is uninformative; lean on the
  points/estimate columns and say so.
- **HITL:** if scope matters, ask the human which epics to include rather than defaulting to the sprint.

## Full detail

Field-by-field output, the aggregate-vs-timetracking rule, and error handling: [reference.md](reference.md).
