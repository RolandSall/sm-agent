---
name: sprint-metrics
description: Sprint velocity (Jira's own numbers, committed vs completed, plus average) and scope-change (points added/removed mid-sprint). Use when asked about velocity, sprint trend, whether scope crept, or for sprint-review and retro numbers. Burndown is deliberately left to Jira's native chart.
allowed-tools: mcp__scrum__sprint_velocity, mcp__scrum__sprint_scope_change, Bash(./scrum velocity*), Bash(./scrum scope-change)
---

# Sprint Metrics

Read-only. **Velocity is Jira's own** — pulled from its velocity chart, not recomputed, so it matches what the board shows. **Scope-change** is computed from the sprint changelog.

## Invoke

**Velocity** — committed vs completed per sprint, plus the average:
- MCP: `sprint_velocity(velocitySprints)` — optional, defaults to the last **6**.
- REST: `./scrum velocity 6`

**Scope change** — the raw scope ingredients for the active sprint (no verdict):
- MCP: `sprint_scope_change` — no arguments.
- REST: `./scrum scope-change`

It returns `sprint`, `committedAtStart`, `addedPoints`, `removedPoints`, and the `added[]` / `removed[]`
item lists (each `{ key, storyPoints }`). It deliberately does **not** collapse these into a signed
net-percent — added and removed stay separate, because one number hides churn.

### Read the scope change (you do this, not the tool)

Compare the three totals against each other and against the original commitment:

- **Grew** — `addedPoints` clearly exceeds `removedPoints`: the plan took on net-new work.
- **Shrank** — `removedPoints` clearly exceeds `addedPoints`: the sprint was de-scoped.
- **Churned** — both `addedPoints` and `removedPoints` are large even if they roughly cancel: a lot of
  the plan moved (a near-zero net would have masked this — that's why there's no net-% field). High
  churn relative to `committedAtStart` (e.g. added+removed a big fraction of the commitment) is worth
  flagging even when the sprint's size barely changed.

Name the specific stories from `added[]`/`removed[]` when you report, and size the movement against
`committedAtStart`. What counts as "a lot" is a team judgment — **confirm with the human** whether the
observed movement is normal for this team before calling it creep or instability.

**`removedPoints` is a lower bound** (the Agile members endpoint may not return every removed issue),
so removals — and any "shrank/churned" read that leans on them — may understate the real movement.

## Burndown — do not build one

There is no burndown tool, by design. Open the sprint's **Burndown Chart** in Jira instead. A hand-rolled version would drift from Jira's official numbers (working days, mid-sprint scope changes, sub-tasks), and two disagreeing burndowns is worse than one. If asked for a burndown, point at Jira's chart rather than computing something.

## Report like this

Summarize the **trend**, not raw numbers: is velocity stable or falling, and did scope grow mid-sprint? Low completed velocity often pairs with high WIP — cross-check [wip-limits](../wip-limits/SKILL.md).

Caveat to carry: issues *removed* from a sprint are best-effort (the Agile members endpoint may not return them), so treat removed-scope as a **lower bound**.

To publish a summary durably, use [publish-metrics](../publish-metrics/SKILL.md) — a separate, gated, human-in-the-loop step.

## Full detail

Output fields and error handling: [reference.md](reference.md).
