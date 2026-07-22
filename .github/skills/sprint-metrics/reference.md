---
name: sprint-metrics
description: >
  Velocity (Jira's own numbers) and scope-change for the sprint — read-only. Burndown is left to
  Jira's native chart rather than recomputed.
---

# Sprint Metrics Skill

Two read-only metrics. **Velocity is Jira's own** (pulled from its velocity chart, not recomputed, so
it matches the board). **Scope-change** is computed from the sprint's changelog. For **burndown**, use
Jira's built-in chart — we deliberately don't reimplement it (a hand-rolled version wouldn't match
Jira's, and two different burndowns is worse than one).

## Prerequisites
- **Mode A (MCP):** tools `sprint_velocity`, `sprint_scope_change`.
- **Mode B (REST):** run `./scrum <cmd>` against the running server — see [README](../README.md).
- **Config:** Tier-1 Jira connection; `scrum.jira.board-id` (or it's resolved from the project key).

## Commands

### Velocity — Jira's committed vs completed per sprint, plus the average
- **MCP tool:** `sprint_velocity(velocitySprints)` — optional; defaults to averaging the last 6.
- **REST (when MCP is disabled):**
  ```bash
  ./scrum velocity 6
  # or raw curl (dynamic port from .scrum-env):
  source .scrum-env && curl "$SCRUM_API_URL/api/velocity?sprints=6"
  ```
- **Source:** Jira's greenhopper velocity chart (the same numbers as the board's Velocity report).
- **Output** `VelocityReport`: `sprints[] { sprint, committed, completed }`, `averageVelocity`.

### Scope change — points added/removed since sprint start (active sprint)
- **MCP tool:** `sprint_scope_change` — no arguments.
- **REST (when MCP is disabled):** `./scrum scope-change` (or `source .scrum-env && curl "$SCRUM_API_URL/api/scope-change"`)
- **Output** `ScopeChangeReport`: `sprint`, `committedAtStart`, `addedPoints`, `removedPoints`, `added[]`, `removed[]` (each item `{ key, storyPoints }`). Raw ingredients only — there is **no** signed net-percent; compare added vs removed vs the commitment yourself (grew / shrank / churned).

### Burndown — use Jira directly
No tool. Open the sprint's **Burndown Chart** in Jira. Reimplementing it here would drift from Jira's
official numbers (working days, mid-sprint scope changes, sub-tasks), so we don't.

## Parameters
| Parameter | Team default | Notes |
|---|---|---|
| `velocitySprints` | **6** | how many recent sprints to average for velocity |

## How to interpret / act
Summarize the **trend**, not raw numbers: is velocity stable or falling, and did scope move mid-sprint?
For scope, compare `addedPoints` vs `removedPoints` vs `committedAtStart` and call it **grew** (added
dominates), **shrank** (removed dominates), or **churned** (both large even if they roughly cancel — a
signed net would have hidden this). Size the movement against `committedAtStart`; what counts as "a lot"
is a team judgment, so confirm with the human before calling it creep. Low completed velocity often
pairs with high [wip-limits](../wip-limits/reference.md). To publish a summary durably, use
[publish-metrics](../publish-metrics/reference.md) — a separate, gated, human-in-the-loop step.

## Error handling
- No velocity data (new board) → `averageVelocity: 0`, empty `sprints`.
- No active sprint → scope returns empty with a null sprint; report "no active sprint".
- Note: issues *removed* from a sprint are best-effort (the Agile members endpoint may not return
  them); treat removed-scope as a lower bound.

## Related skills
[wip-limits](../wip-limits/reference.md) · [logged-hours](../logged-hours/reference.md) · [publish-metrics](../publish-metrics/reference.md) · [README](../README.md)
