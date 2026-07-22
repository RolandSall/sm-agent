---
name: board-hygiene
description: >
  Flag active-sprint issues that are missing a story-point estimate, acceptance criteria,
  or an assignee, so the board is ready before standup or sprint planning.
---

# Board Hygiene Skill

Check the active sprint for data-quality gaps. This is a **read-only** capability — it never
changes anything in Jira; it reports what a human should fix.

## When to use
- Before daily standup, to catch un-groomed tickets.
- At the start of sprint planning, to confirm every committed story is estimated and has acceptance criteria.
- Any time someone asks "is the board clean / ready?"

## Prerequisites
- **Mode A (MCP):** the `scrum` MCP server is connected (tool shows as `board_hygiene`).
- **Mode B (REST):** run `./scrum <cmd>` against the running server (used when the harness's MCP
  feature is disabled but the terminal is allowed). See [README](../README.md) for how to run it.
- **Config:** `JIRA_BASE_URL`, `JIRA_TOKEN`, `JIRA_PROJECT_KEYS` set; acceptance-criteria and
  story-points custom-field IDs configured in `application.yml`.

## Commands
### Check hygiene of the active sprint
- **MCP tool:** `board_hygiene(issueType, assignee)` — both optional.
- **REST (when MCP is disabled):**
  ```bash
  ./scrum hygiene                                        # wrapper (curl + dynamic port + jq)
  ./scrum hygiene --type Story --assignee "Jane Dev"     # optional issue-type / assignee filters
  source .scrum-env && curl "$SCRUM_API_URL/api/hygiene" # or raw curl on the Docker-assigned port
  ```
The active sprint is resolved from the configured project/board — you do not pass a sprint. Both forms
hit the same server; `$SCRUM_API_URL` comes from `.scrum-env`, so no port is ever hardcoded.

## Parameters (optional)
| Parameter (MCP / REST query) | Default | Notes |
|---|---|---|
| `issueType` / `issueType` (wrapper `--type`) | all types | only examine issues of this type, e.g. `Story` (default: all types). |
| `assignee` / `assignee` (wrapper `--assignee`) | all assignees | only examine issues assigned to this person (default: all assignees). |

## Output
Returns a `BoardHygieneReport`:
- `totalIssues` — issues examined in the active sprint.
- `missingEstimateCount` — how many examined issues have no story-points value.
- `missingAcceptanceCriteriaCount` — how many examined issues have no acceptance criteria.
- `missingAssigneeCount` — how many examined issues are unassigned.
- `issues[]` — one entry per **every** active-sprint issue (clean ones included, so the caller applies its own definition-of-ready), each with:
  - `key`, `summary`, `assignee`
  - `missingEstimate` — no story-points value.
  - `missingAcceptanceCriteria` — acceptance-criteria field empty.
  - `missingAssignee` — unassigned.

The three per-field booleans are the raw ingredients and the three counts are their deterministic per-field tallies. There is **no** OR-combined "issues with gaps" number — combining fields is the team's definition-of-ready, so the tool never bakes one in. The team's definition-of-ready (a subset of `{estimate, acceptance criteria, assignee}`) decides which fields count; ask the human which.

## How to interpret / act
Summarize by **gap type**, not issue-by-issue, so the team can fix in one pass:
> "3 issues need attention: PROJ-1 and PROJ-2 have no estimate; PROJ-4 has no acceptance criteria; PROJ-2 is also unassigned."
Then nudge owners (see [cross-team-dependencies](../cross-team-dependencies/reference.md) for cross-team follow-ups, or DM an assignee in phase 2).

**Ask the human for the team's definition-of-ready field set** before deciding what's a gap; only when unattended, fall back to treating all three fields as required and say so.

## Error handling
- An empty sprint or an all-green board returns zero on every per-field count (`missingEstimateCount`, `missingAcceptanceCriteriaCount`, `missingAssigneeCount`) — report "board is clean", not an error.
- A 401/403 from Jira means the PAT is missing/expired — check `JIRA_TOKEN`.

## Related skills
[wip-limits](../wip-limits/reference.md) · [sprint-metrics](../sprint-metrics/reference.md) · [README](../README.md)
