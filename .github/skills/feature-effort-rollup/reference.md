---
name: feature-effort-rollup
description: >
  Per-feature (Epic) effort ingredients — Σ story points, Σ estimate hours, Σ logged hours, story
  count, and the epic's own dev-job snapshot — computed live from child stories. Ingredients only; the
  agent assesses which features are off.
---

# Feature Effort Roll-up Skill

For each feature (Epic), roll its child stories up into effort ingredients so you can spot features
whose logged effort is drifting from the estimate. **Read-only. Ingredients only — no verdict.**

## When to use
- "Which features are over/under on effort?" / "Is any epic off by a lot?"
- Comparing spent-vs-estimated across the features in a sprint or PI.

## Prerequisites
- **Mode A (MCP):** tool `feature_effort_rollup`.
- **Mode B (REST):** `./scrum feature-effort [EPIC ...]` against the running server — see [README](../README.md).
- **Config:** Tier-1 Jira connection; the Epic-Link, dev-job and story-points custom-field ids, plus
  `feature-issue-type`, are configured per instance in `application.yml`.

## Commands
- **MCP tool:** `feature_effort_rollup(epicKeys)` — omit `epicKeys` to derive scope from the active sprint.
- **REST (when MCP is disabled):**
  ```bash
  ./scrum feature-effort PROJ-100 PROJ-215
  ./scrum feature-effort                 # active-sprint epics
  # raw curl:
  source .scrum-env && curl -G "$SCRUM_API_URL/api/feature-effort" \
    --data-urlencode "epic=PROJ-100" --data-urlencode "epic=PROJ-215"
  ```

## Parameters
| Parameter | Default | Notes |
|---|---|---|
| `epicKeys` (REST: `epic`, repeatable) | active-sprint epics | Epics to roll up. Omit → the DISTINCT non-null Epic-Links of the active sprint's issues. |

## How the numbers are built
- **Children** come from JQL `"Epic Link" = <epicKey>`.
- **Sums are live** over the children. `null` values (no estimate, nobody logged) count as 0.
- **Hours = seconds / 3600.** Estimate and logged hours **prefer Jira's aggregate fields**
  (`aggregatetimeoriginalestimate` / `aggregatetimespent`) so a story's **sub-task time is included**,
  falling back to the story's own `timetracking` when the aggregate is absent.
- **`devJobPoints`** is read from the epic's own "Planned Dev Job" field — a synced snapshot, echoed as
  a **reference** only. It drifts from the live child point sum; do not treat it as truth.

## Output
Returns a `FeatureEffortReport`:
- `features[]` — one `FeatureEffort` per epic in scope:
  - `epicKey`, `epicSummary`.
  - `devJobPoints` — the epic's own planned dev-job points, or `null`. **Reference only.**
  - `storyCount` — number of child stories.
  - `sumStoryPoints` — Σ child story points.
  - `sumEstimateHours` — Σ child original-estimate hours.
  - `sumLoggedHours` — Σ child logged hours.
  - `sumEstimateDays` / `sumLoggedDays` — derived working-days from the authoritative `sumEstimateHours` / `sumLoggedHours` via `WorkingDays.fromHours` (the `*Hours` fields remain the source of truth).
  - `stories[]` — each `StoryEffort`: `{ key, status, points, estimateHours, loggedHours }` (any may be `null`).

## How to interpret / act
The signal is **hours**: compare `sumEstimateHours` vs `sumLoggedHours` per feature, and hours-per-point
(`sumLoggedHours / sumStoryPoints`) **across** features to find the outlier. Points (incl. `devJobPoints`)
are drift-prone and weak for effort. There is **no baked threshold** — call out which features look off
and why. If the team doesn't log time, `sumLoggedHours` is ~0 everywhere: say the hours signal is
uninformative and fall back to points/estimate.

## Error handling
- No epics in scope (empty active sprint, or none have an Epic-Link) → `features: []`.
- An epic key with no children → that feature has `storyCount: 0` and zero sums (still listed).
- `feature = Epic` is a config default; on an instance with a real "Feature" type, change
  `scrum.jira.feature-issue-type`.

## Related skills
[estimate-variance](../estimate-variance/reference.md) · [logged-hours](../logged-hours/reference.md) · [README](../README.md)
