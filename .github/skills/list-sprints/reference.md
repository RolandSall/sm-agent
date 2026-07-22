---
name: list-sprints
description: >
  Enumerate the board's sprints (id, full name, state, dates) so the agent can suggest which sprint
  to target given only the board id — read-only.
---

# List Sprints Skill

Read-only enumeration of the configured board's sprints, surfaced unchanged from Jira's Agile API
(`/rest/agile/1.0/board/{boardId}/sprint?state=<state>`). No computation.

## When to use

- The human asks "which sprint", "what sprints are active", "list/show the sprints".
- A board has **several active sprints at once** and you must confirm the right one by full name
  before running an active-sprint capability (velocity, scope-change, sprint-flow, wip, risk …).
- You want to suggest a sprint by name instead of relying on the `JIRA_SPRINT_FILTER` fallback.

## Prerequisites
- **Mode A (MCP):** tool `list_sprints`.
- **Mode B (REST):** run `./scrum sprints [state]` against the running server — see [README](../README.md).
- **Config:** Tier-1 Jira connection; `scrum.jira.board-id` (the board whose sprints are listed).

## Commands

### List sprints in a state
- **MCP tool:** `list_sprints(state)` — `state` optional; defaults to `active`.
- **REST (when MCP is disabled):**
  ```bash
  ./scrum sprints            # active (default)
  ./scrum sprints closed     # closed sprints
  ./scrum sprints active,future
  # or raw curl (dynamic port from .scrum-env):
  source .scrum-env && curl "$SCRUM_API_URL/api/sprints?state=active"
  ```

## Parameters
| Parameter | Team default | Notes |
|---|---|---|
| `state` | **active** | `active`, `future`, `closed`, or a comma-combination. Null/blank → `active`. |

## Output
`SprintListReport`: `sprints[]`, each a Jira sprint record:
- `id` — the sprint id (pass this to sprint-scoped calls).
- `name` — the **full** sprint name (what you show the human to disambiguate).
- `state` — `active` / `future` / `closed`.
- `startDate`, `endDate` — planned window (may be null for future sprints).
- `completeDate` — when it was actually closed (null unless `closed`).
- `goal` — the sprint goal, if set (may be null).

## How to interpret / act
- List sprints by **full name** and state. When more than one is `active`, say so and **ask which one
  to target** — do not silently pick. That disambiguation is this tool's reason to exist; the
  active-sprint tools otherwise fall back to the optional `JIRA_SPRINT_FILTER` substring, and when
  that is unset the server picks by date/first, which can be the wrong sprint.
- Recommended flow: `list_sprints` → show names → human picks → run the sprint capability.

## Error handling
- No sprints in the requested state (e.g. a brand-new board, or no closed sprints yet) → empty
  `sprints[]`. Report "no sprints found in state X", not an error.
- Only very old boards page beyond the fetch limit; if that ever happens the server logs a
  truncation warning.

## Related skills
[sprint-metrics](../sprint-metrics/reference.md) · [sprint-flow](../sprint-flow/reference.md) · [wip-limits](../wip-limits/reference.md) · [delivery-risk](../delivery-risk/reference.md) · [README](../README.md)
