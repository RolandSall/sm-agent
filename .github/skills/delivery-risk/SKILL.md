---
name: delivery-risk
description: Returns the active sprint's stories, the cross-team Dependency issues on their Epics, and the team's capacity — raw. YOU join them by Epic, keep the open blockers, and judge each story's LOW/MEDIUM/HIGH. Use when asked whether the sprint will make it, what's at risk, what to de-scope, or for a mid-sprint health check.
allowed-tools: mcp__scrum__sprint_delivery_risk, Bash(./scrum risk*)
---

# Delivery Risk

Read-only. The tool **fetches the raw facts**; **you do the reasoning**. It returns three lists/values
and computes no verdict:

- `stories[]` — `{ key, summary, epicKey, storyPoints, unestimated }`
- `dependencies[]` — every cross-team `Dependency` on those stories' Epics, RAW (unfiltered):
  `{ key, summary, status, statusCategory, blockingTeam, waitingTeam, epicKey, deliverySprint, deliveryEnd }`
- `remainingCapacityPoints`, `capacityKnown`

The tool deliberately does **not** filter, count, or classify — those are your job.

## Step 1 — find the open blockers WE face (from `dependencies[]`)

Keep a dependency only if **all** hold (this is the error-prone bit — apply it exactly):
- `waitingTeam == "RIO"` — **RIO is the one waiting** (`Dependent/s`), and
- `blockingTeam != "RIO"` — someone else is the blocker (`Depend On`), and
- it is **open**: `status` is **not** in `{Closed, Solved, Cancel, Released, Resolved}` (case-insensitive;
  `Open`/`Acknowledged` are open on this instance).

Discard the rest: resolved ones, and reverse-direction ones where `Depend On == RIO` (those are teams
waiting on *us* — our obligations, not our sprint risk). Confirm the resolved-status set with the human
if the team's Dependency workflow differs.

## Step 2 — join to stories by Epic

A story is **blocked** if a kept dependency has the same `epicKey` as the story. Stories under the same
Epic share its blockers. Count the blockers per story (usually 0 or a small number).

## Step 3 — judge the level (a sensible default line; adapt to the team)

- **HIGH** — the story is blocked (≥1 open cross-team blocker), OR the team is over-committed
  (`remainingCapacityPoints < 0`).
- **MEDIUM** — a heavy story (`storyPoints` ≳ 8) or `unestimated`, OR it no longer fits the remaining
  headroom (`remainingCapacityPoints < storyPoints`).
- **LOW** — small, estimated, unblocked, fits capacity.
- Take the **worst** factor and **name the driver** (blocked by <team> / big / over-capacity), and cite
  the blocking dependency (`key`, `blockingTeam`, `deliverySprint` ETA).

Guidelines, not a formula — if the team says a given blocker is expected or a heavy story is fine,
reflect that. **If `capacityKnown` is false, don't invoke the capacity factor** — say it wasn't evaluated.

## Invoke

- **MCP (preferred):** `sprint_delivery_risk(capacityCommittedPoints, capacityAvailablePoints, issueType, assignee)` — all optional.
- **REST fallback:** `./scrum risk --committed 40 --available 35 [--type Story] [--assignee "Jane Dev"]`.

| Parameter | Default | Notes |
|---|---|---|
| `capacityCommittedPoints` | — | **Ask the human.** No default. |
| `capacityAvailablePoints` | — | **Ask the human.** No default. |
| `issueType` | all types | Only include stories of this type, e.g. `Story` (default: all types). |
| `assignee` | all assignees | Only include stories assigned to this person (default: all assignees). |

## Capacity: ask, don't invent

There is no capacity data source yet (that lands with the Teams capacity sheet). The two capacity numbers have **no defaults on purpose**. Ask the human for them. If you call without them, the tool reports `capacityKnown: false` and `remainingCapacityPoints: null` — **say the capacity factor was not evaluated**; never substitute a guess.

## Human-in-the-loop

Ask the human, when available, for: the capacity numbers (always); the team's resolved Dependency
statuses (does its workflow use different terminal statuses? — it changes which blockers count as open);
and whether a given open blocker or heavy estimate is actually a problem this sprint.

## Report like this

Overall picture, then the **drivers** per HIGH story — blocked cross-team (by whom, due when), big, or
over capacity. Recommend concretely what to de-scope or unblock.

## Full detail

Output fields and error handling: [reference.md](reference.md) · dependency model: [cross-team-dependencies](../cross-team-dependencies/SKILL.md)
