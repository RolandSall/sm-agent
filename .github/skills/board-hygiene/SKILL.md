---
name: board-hygiene
description: Check the active Jira sprint for data-quality gaps — issues missing a story-point estimate, acceptance criteria, or an assignee. Use when asked "is the board clean/ready", before standup, or at sprint planning to confirm every committed story is groomed.
allowed-tools: mcp__scrum__board_hygiene, Bash(./scrum hygiene)
---

# Board Hygiene

Read-only. Reports the per-field readiness **ingredients** for a human to judge; never writes to Jira.

## Invoke

- **MCP (preferred):** `board_hygiene(issueType, assignee)` — both optional.
- **REST fallback** (when MCP is disabled by org policy): `./scrum hygiene [--type Story] [--assignee "Jane Dev"]`

| Parameter | Default | Notes |
|---|---|---|
| `issueType` (REST: `--type`) | all types | Only examine issues of this type, e.g. `Story` (default: all types). |
| `assignee` (REST: `--assignee`) | all assignees | Only examine issues assigned to this person (default: all assignees). |

The active sprint is resolved from configured project/board — never pass a sprint.

## What comes back

`issues[]` carries **every** active-sprint issue (clean ones included), each with three raw per-field booleans: `missingEstimate`, `missingAcceptanceCriteria`, `missingAssignee`. These three are the ingredients — apply **your team's real definition-of-ready** over them. A team that does not require acceptance criteria simply ignores that boolean.

The tool also returns `totalIssues` and three **deterministic per-field counts**: `missingEstimateCount`, `missingAcceptanceCriteriaCount`, `missingAssigneeCount`. Each is a plain tally over one field. There is **no** combined "issues with gaps" number, on purpose — combining the three fields is the definition-of-ready judgement, and that is yours.

## Applying a definition-of-ready (this is the reasoning step)

The team's definition-of-ready is a **subset** of `{estimate, acceptance criteria, assignee}` — most teams require estimate and assignee, some also require acceptance criteria, a few require only one. **Flag an issue as not-ready only on the fields the team actually requires**, and **ask the human which fields those are** before you decide. Then, per issue, treat it as not-ready if any *required* boolean is true, and count/report over that subset. Do not treat all three as mandatory by default — that is a policy choice you have not been given.

## Report like this

Summarize **by gap type**, not issue-by-issue, so the team fixes in one pass:

> "3 issues need attention: PROJ-1 and PROJ-2 have no estimate; PROJ-4 has no acceptance criteria; PROJ-2 is also unassigned."

No issue tripping the team's definition-of-ready means the board is clean — report that as success, not an error.

## Human-in-the-loop

**Ask the human for the team's definition-of-ready field set** before deciding what's a gap — do estimate, acceptance criteria and assignee all need to be present, or only some? Apply their answer over the per-field booleans and counts. Only when unattended, fall back to treating all three fields as required — and say so explicitly rather than presenting it as the team's rule.

## Full detail

Output fields, error handling, and config prerequisites: [reference.md](reference.md).

Related: [wip-limits](../wip-limits/SKILL.md) · [sprint-flow](../sprint-flow/SKILL.md)
