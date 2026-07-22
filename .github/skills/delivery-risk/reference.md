---
name: delivery-risk
description: >
  Raw delivery-risk ingredients — the sprint's stories, the cross-team Dependency issues on their Epics,
  and the team's capacity. The agent joins them by Epic, keeps the open blockers, and judges the level.
---

# Delivery Risk Skill

Assess whether the active sprint is likely to deliver. **Read-only.** The tool **fetches raw facts**;
**you do the reasoning** — it computes no level. See the [SKILL.md](SKILL.md) for the full 3-step
reasoning (filter open blockers we face → join to stories by Epic → judge). This file covers invocation,
parameters, and output.

## When to use
- Mid-sprint health check, or when someone asks "are we going to make it?".
- Reuses the same dependency model as [cross-team-dependencies](../cross-team-dependencies/reference.md).

## Prerequisites
- **Mode A (MCP):** tool `sprint_delivery_risk`.
- **Mode B (REST):** run `./scrum <cmd>` against the running server — see [README](../README.md).
- **Config:** Tier-1 Jira connection (incl. `team-name`, the Dependency fields).
- **Capacity input:** phase 1 has no capacity data source, so you supply it (see Parameters). Omit it
  and the tool reports `capacityKnown: false` rather than guessing.

## Commands
### Fetch the active-sprint risk ingredients
- **MCP tool:** `sprint_delivery_risk(capacityCommittedPoints, capacityAvailablePoints, issueType, assignee)` — all optional.
- **REST (when MCP is disabled):**
  ```bash
  ./scrum risk --committed 40 --available 35 [--type Story] [--assignee "Jane Dev"]
  # or raw curl (dynamic port from .scrum-env):
  source .scrum-env && curl "$SCRUM_API_URL/api/risk?committed=40&available=35"
  ```

## Parameters
| Parameter (MCP / REST query) | Team default | Notes |
|---|---|---|
| `capacityCommittedPoints` / `committed` | — | points the team has committed this sprint. **Ask the human** (phase 2 reads the Teams sheet). |
| `capacityAvailablePoints` / `available` | — | points the team can actually do this sprint. **Ask the human.** |
| `issueType` / `issueType` | all types | only include stories of this type, e.g. `Story` (default: all types). |
| `assignee` / `assignee` | all assignees | only include stories assigned to this person (default: all assignees). |

No thresholds and no resolved-status parameter — the tool returns raw data; the filtering (which
Dependency counts as open) and the level are your judgement. Nothing here is server config.

## Output
Returns a `RiskReport`:
- `sprint`
- `stories[]` — `{ key, summary, epicKey, storyPoints, unestimated }`
- `dependencies[]` — every cross-team `Dependency` on those Epics, RAW/unfiltered:
  `{ key, summary, status, statusCategory, blockingTeam, waitingTeam, epicKey, deliverySprint, deliveryEnd }`
- `remainingCapacityPoints`, `capacityKnown`

**No `level` or per-story blocker count is returned.** The tool only fetches; the agent filters,
joins by `epicKey`, and judges (see SKILL.md). The resolved-status set that decides "open" is applied
in your reasoning — the instance's terminal Dependency statuses are `Closed`, `Solved` (`Cancel`,
`Released`, `Resolved` also count as done); `Open`/`Acknowledged` are open. Confirm with the human.

## How to interpret / act
State the overall picture and the **drivers** per HIGH story: blocked cross-team (by whom, due when —
the blocking dependency's `blockingTeam` + `deliverySprint`), big, or over capacity. Recommend what to
de-scope or unblock. If `capacityKnown` is false, say so and ask for the capacity numbers.

**Ask the human**, when available, for: the capacity numbers (always), the team's resolved Dependency
statuses, and whether a given open blocker or heavy estimate is actually a problem this sprint.

## Error handling
- No active-sprint issues → empty `stories[]` (and no dependencies).
- Missing capacity → `capacityKnown: false`, `remainingCapacityPoints: null` (not an error — ask for it).

## Related skills
[cross-team-dependencies](../cross-team-dependencies/reference.md) · [sprint-metrics](../sprint-metrics/reference.md) · [README](../README.md)
