---
name: estimate-variance
description: Returns the raw logged-vs-estimate ingredients (usageRatio, statusCategory, hours) for every estimated active-sprint issue — no verdict. YOU flag OVERRUN (blown estimate), FINISHED_EARLY, or NO_LOGGING against your team's line. Use when asked if anything is blowing its estimate, finished suspiciously fast, or as retro/grooming input on estimation quality.
allowed-tools: mcp__scrum__estimate_variance, Bash(./scrum estimate-variance*)
---

# Estimate Variance

Read-only. The tool **fetches the raw facts**; **you do the reasoning**. Where [logged-hours](../logged-hours/SKILL.md) reports totals, this returns, for **every** estimated issue (on-track ones too), the deterministic **ingredients** and computes no verdict:

- `issues[]` — `{ key, assignee, loggedHours, originalEstimateHours, loggedDays, originalEstimateDays, usageRatio, statusCategory }`
  - `usageRatio` — `loggedHours ÷ originalEstimateHours` (0 when the estimate is 0), computed in code.
  - `statusCategory` — Jira's built-in bucket (`In Progress`, `Done`, `To Do`), passed through raw. Do **not** re-derive it.

The tool deliberately does **not** flag, bucket, or filter (beyond the `minEstimateHours` fact-gate) — that judgement is yours.

## Invoke

- **MCP (preferred):** `estimate_variance(minEstimateHours)` — optional.
- **REST fallback:** `./scrum estimate-variance --min-estimate 4`

| Param | Default | Meaning |
|---|---|---|
| `minEstimateHours` | `0` | **Fact-filter:** ignore issues estimated below this many hours |

`minEstimateHours` is a legitimate fact-gate (variance below the floor is noise). Issues with **no** original estimate are always skipped — nothing to compare.

## Flag each issue against your team's line

Read `usageRatio` and `statusCategory` and classify. A sensible default line (**adapt to the team**):

- **OVERRUN** — `usageRatio > ~1.25` (logged more than ~25% past the estimate). On a still-open issue this is an early risk signal; cross-check [delivery-risk](../delivery-risk/SKILL.md).
- **FINISHED_EARLY** — `statusCategory == "Done"` **and** `usageRatio < ~0.5`. Over-estimated, **or** work simply not logged.
- **NO_LOGGING** — `statusCategory == "In Progress"` **and** `usageRatio == 0`. Untracked work, or not actually started.
- **On track** — nothing tripped. Still returned, with its ratio, so you can show the healthy ones too.

These lines are guidelines, not a formula. Take the raw ratio and decide.

## Human-in-the-loop

The `~1.25` / `~0.5` lines are **team policy, not facts** — before treating any issue as a problem, **ask the human** (when available) what an acceptable overrun/early-finish looks like for this team; a research spike legitimately blows its estimate. Fall back to the `~1.25` / `~0.5` defaults only unattended. The report echoes the ratios so a person can re-judge without re-querying.

## Interpret before reporting

This team's worklog may be sparse. If most issues come back `NO_LOGGING`, that is a **logging-habit finding, not an estimation problem** — say so explicitly rather than implying the estimates are bad.

Summarize by flag, not issue-by-issue:

> "2 issues are overrunning (PROJ-101 logged 20h on an 8h estimate), 1 finished on a fraction of its estimate, and 3 in-progress issues have nothing logged — worth checking at standup that work is being tracked."

## Full detail

Output fields and error handling: [reference.md](reference.md).
