---
name: sprint-flow
description: >
  Daily-standup flow view of the active sprint — every ticket returned RAW (statusCategory,
  ageInStatusDays, daysSinceUpdated, reopenCount) from the changelog. The agent applies the
  stuck / stalled / reopened lines; the tool buckets nothing.
---

# Sprint Flow Skill

The **daily-standup view**: surface the tickets that need a conversation *today*. Derived from each
issue's **changelog**, so it sees movement (or the lack of it), not just current state. **Read-only.**

This maps directly to the Scrum Master's morning routine — *"which stories stalled since yesterday, and
what bounced back?"* — and to the two flow metrics most useful in the Daily Scrum: **Work Item Age** and
work-in-progress that's aging.

The tool is a **fetch, not a verdict**: it returns every active-sprint issue with its raw ingredients
and applies no thresholds and no buckets. Classifying stuck / stalled / reopened is the agent's job,
against lines the team owns.

## When to use
- Every day before standup — the primary use.
- When someone asks "what's stuck / what's not moving / did anything get reopened?"
- Alongside [wip-limits](../wip-limits/reference.md) (how much is in progress) — this says how *long* it's been there.

## Prerequisites
- **Mode A (MCP):** tool `sprint_flow`.
- **Mode B (REST):** run `./scrum <cmd>` against the running server — see [README](../README.md).
- **Config:** Tier-1 Jira connection. Uses the Agile changelog API — no custom field required.

## Commands
### Fetch the active sprint's flow ingredients
- **MCP tool:** `sprint_flow(issueType, assignee)` — both optional (scoping only).
- **REST (when MCP is disabled):**
  ```bash
  ./scrum sprint-flow                          # every active-sprint issue, raw
  ./scrum sprint-flow --type Story --assignee "Jane Dev"   # optional issue-type / assignee scoping
  source .scrum-env && curl "$SCRUM_API_URL/api/sprint-flow?issueType=Story"
  ```

## Parameters (Tier 2 — optional, agent-supplied)
| Param | MCP / REST name | Default | Meaning |
| --- | --- | --- | --- |
| Issue type | `issueType` (wrapper `--type`) | all types | Only include issues of this type, e.g. `Story` (default: all types). |
| Assignee | `assignee` (wrapper `--assignee`) | all assignees | Only include issues assigned to this person (default: all assignees). |

The stuck / stalled / reopen **thresholds are no longer tool parameters** — they are lines the agent
applies to the raw numbers below. There is nothing to pass and nothing echoed back.

## Output
Returns a `SprintFlowReport` with a single flat `issues[]` — every active-sprint issue (after any
type/assignee scoping), **no buckets**. Each `FlowIssue`:

`{ key, summary, assignee, statusCategory, status, ageInStatusDays, daysSinceUpdated, reopenCount }`

- `statusCategory` — Jira's built-in bucket (`To Do` / `In Progress` / `Done`), curated in Java and
  passed through verbatim. Use it as given; do **not** re-derive it from the status name.
- `ageInStatusDays` — whole days since the latest status change, or `null` when the issue has never
  changed status (age **unknown**).
- `daysSinceUpdated` — whole days since `fields.updated`, or `null` if absent.
- `reopenCount` — status changes made after the issue first reached a done-like status.

## How to classify (the agent applies these; the tool does not)
Ask the human where the lines sit, then:
- **STUCK** — `statusCategory == "In Progress"` and `ageInStatusDays >= stuck line` (null age = unknown, not stuck).
- **STALLED** — `statusCategory != "Done"` and `daysSinceUpdated >= stalled line` (null = unknown, not stalled).
- **REOPENED** — `reopenCount >= reopen line`.

Sensible starting lines when unattended: stuck ≥ 5 days, stalled ≥ 3 days, reopen ≥ 1 — but confirm
with the team; whether one reopen is a problem or normal churn is a judgement call.

## How to interpret / act
Lead standup with the three groups:
> "2 tickets are stuck (PROJ-88 has been In Review 9 days), 1 hasn't moved in 4 days, and PROJ-40 was
> reopened once — let's find out what's blocking those."
`stuck` + a cross-team link → check [cross-team-dependencies](../cross-team-dependencies/reference.md).
Repeated `reopened` → a quality/definition-of-done conversation for the retro.

## Tuning to this instance
"Done-like" (for `reopenCount`) is detected by status-name keywords: closed / done / resolved / released /
cancel / solved / approved. This instance's real done statuses (Released, Approved, Cancel, Solved, …)
are covered, but if a custom terminal status isn't caught, a reopen may be missed — verify against real data
and add any missing terminal status names.

## Error handling
- No active sprint resolved → empty `issues[]`.
- An issue with no status transitions in its changelog → `ageInStatusDays` is `null` (age unknown) —
  not an error; treat it as "cannot tell how long it has sat", never as stuck.

## Related skills
[wip-limits](../wip-limits/reference.md) · [board-hygiene](../board-hygiene/reference.md) · [estimate-variance](../estimate-variance/reference.md) · [README](../README.md)
