---
name: estimate-variance
description: >
  Return the raw logged-vs-estimate ingredients for every estimated active-sprint issue — no verdict.
  The caller flags blown estimates (overrun), suspiciously-early finishes, or zero logging.
---

# Estimate Variance Skill

Return the raw logged-vs-estimate **ingredients** for every estimated issue; the caller (you) turns them
into **flags a Scrum Master would act on**. Where [logged-hours](../logged-hours/reference.md) reports
totals, this reports the per-issue ratios so you can spot the *outliers*. **Read-only.**
Logged time is native worklog (see [logged-hours](../logged-hours/reference.md)); estimates come from Jira's
time-tracking block.

## When to use
- Mid/late sprint, or before a retro, to spot estimates that were badly off.
- When someone asks "is anything blowing its estimate, or did something finish suspiciously fast?"
- As a data point for grooming/estimation coaching (feed patterns back into planning).

## Prerequisites
- **Mode A (MCP):** tool `estimate_variance`.
- **Mode B (REST):** run `./scrum <cmd>` against the running server — see [README](../README.md).
- **Config:** Tier-1 Jira connection. No custom field required — uses native worklog.
- **Data caveat:** only meaningful if the team logs work. If worklog is empty, most issues will surface as `NO_LOGGING` — that's a logging-habit
  signal, not necessarily an estimation problem.

## Commands
### Fetch estimate-variance ingredients for the active sprint
- **MCP tool:** `estimate_variance` — optional arg below.
- **REST (when MCP is disabled):**
  ```bash
  ./scrum estimate-variance                 # no floor
  ./scrum estimate-variance --min-estimate 4
  source .scrum-env && curl "$SCRUM_API_URL/api/estimate-variance?minEstimateHours=4"
  ```

## Parameters (Tier 2 — optional, agent-supplied)
| Param | MCP / REST name | Default | Meaning |
| --- | --- | --- | --- |
| Min estimate (h) | `minEstimateHours` | `0` | **Fact-filter:** ignore issues estimated below this many hours (skip trivial tickets). |

Issues with **no** original estimate are always skipped (nothing to compare). There are no overrun /
early-finish factors on the tool — those are **judgement lines you apply**, not tool inputs (see below).

## Output
Returns an `EstimateVarianceReport` — one entry for **every** estimated issue (on-track ones included), raw ingredients only, no verdict:
- `issues[]` — `{ key, assignee, loggedHours, originalEstimateHours, loggedDays, originalEstimateDays, usageRatio, statusCategory }`:
  - `loggedDays` / `originalEstimateDays` — derived working-days from the authoritative hours via `WorkingDays.fromHours` (the `*Hours` fields remain the source of truth).
  - `usageRatio` — raw ingredient: `loggedHours / originalEstimateHours` (0 when the estimate is 0).
  - `statusCategory` — raw ingredient: Jira's built-in bucket (`In Progress`, `Done`, `To Do`). Passed through raw; do not re-derive.

Apply the team's line against `usageRatio` / `statusCategory` yourself. A sensible default (adapt to the team; ask the human before flagging):
- **OVERRUN** — `usageRatio > ~1.25` (estimate blown; may need re-planning).
- **FINISHED_EARLY** — `statusCategory == "Done"` and `usageRatio < ~0.5` (over-estimated, or work not logged).
- **NO_LOGGING** — `statusCategory == "In Progress"` and `usageRatio == 0` (untracked work or not started).
- **On track** — nothing tripped; still returned, with its ratio.

The `~1.25` / `~0.5` lines are team policy — ask the human what an acceptable overrun looks like before flagging; fall back to those defaults only unattended.

## How to interpret / act
Summarize by flag, not issue-by-issue:
> "2 issues are overrunning (PROJ-101 logged 20h on an 8h estimate), 1 finished on a fraction of its estimate,
> and 3 in-progress issues have nothing logged — worth a quick check at standup that work is being tracked."
`OVERRUN` on a still-open issue is an early risk signal — cross-check [delivery-risk](../delivery-risk/reference.md).
Patterns of over/under estimation are retro material — feed into [sprint-metrics](../sprint-metrics/reference.md).

## Error handling
- Empty/absent worklog → issues surface as `NO_LOGGING` or `FINISHED_EARLY`; report it as a **logging-habit**
  finding, not a tool error.
- No active sprint resolved → empty report (same as the other active-sprint tools).

## Related skills
[logged-hours](../logged-hours/reference.md) · [sprint-flow](../sprint-flow/reference.md) · [delivery-risk](../delivery-risk/reference.md) · [README](../README.md)
