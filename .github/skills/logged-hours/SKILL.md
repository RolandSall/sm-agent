---
name: logged-hours
description: Per-assignee logged hours vs original estimate vs remaining, with team totals, for the active Jira sprint. Use when asked how much time was logged, whether worklogs are up to date, or to compare effort spent against estimates mid-sprint.
allowed-tools: mcp__scrum__worklog_summary, Bash(./scrum worklog)
---

# Logged Hours

Read-only. All values in **hours**. Logged time comes from Jira's **native worklog** (`timetracking.timeSpentSeconds`) unless `scrum.jira.fields.logged-hours` points at a custom field.

## Invoke

- **MCP (preferred):** `worklog_summary` — no arguments.
- **REST fallback:** `./scrum worklog`

## What the tool returns

Per assignee: `logged`, `originalEstimate`, `remaining` (summed hours, plus the same as `*Days`), and two exact counts the tool computes for you:

- `issueCount` — issues assigned to that person this sprint.
- `worklogCount` — of those, how many carry **any** logged time (`logged > 0`).

Plus team `totals`. The tool computes no verdict — you apply the reading below.

## Read the numbers honestly — "not logging" is NOT "0h worked"

**Where `logged` comes from depends on the instance.** With no `logged-hours` custom field configured — the common case — it reads Jira's native worklog. Either way the numbers are only as good as the team's logging habit: if people don't log work, `logged` reads **0 even when work happened**.

Summed hours alone can't tell an idle assignee from an un-logging one — both show `logged 0`. **The counts break the tie:**

- **`issueCount > 0` and `worklogCount == 0`** → work is assigned but **nothing is logged**. This is the *never-logged* signal — almost certainly a tracking gap on this instance, **not** proof no work happened. Say "not logging", never "0h worked".
- **`worklogCount` well below `issueCount`** → partial logging; some issues tracked, others not. Conspicuously low, worth a nudge.
- **`worklogCount == issueCount`** with low hours → genuinely low *tracked* effort; this is a real reading you can lean on.

A **whole-team** all-zero (`worklogCount 0` everywhere) means either the team isn't logging (most likely here), or a configured `logged-hours` custom-field id is wrong.

**Human-in-the-loop:** where you'd call logging "conspicuously low" or flag someone, the low/expected line is a **team policy** — confirm the threshold and whether that person's low count is expected (part-time, on leave, spike work) before naming anyone.

## Report like this

Hours per assignee plus the sprint total. Separate **untracked** (issues present, `worklogCount 0`) from **genuinely low** (fully logged, few hours) — don't collapse them into "0 hours". Flag anyone conspicuously low and anyone whose logged hours far exceed their estimate. For flags turned **actionable**, use [estimate-variance](../estimate-variance/SKILL.md) — this skill gives totals, that one gives outliers.

## Full detail

Output fields and error handling: [reference.md](reference.md).
