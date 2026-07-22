---
name: release-queue
description: Count Jira issues parked in the team's ready-to-test / ready-for-release statuses, bucketed by status. Use when asked how much is waiting for the PM to test, what's ready for release, or to track a growing test backlog mid-sprint.
allowed-tools: mcp__scrum__release_queue, Bash(./scrum release-queue*)
---

# Release Queue

Read-only. Jira has **no native category** for a "ready to test" bucket, so you must tell the tool which status names count.

## Invoke

- **MCP (preferred):** `release_queue(releaseStatuses)` — the list of status names.
- **REST fallback:** `./scrum release-queue "Ready for Release" "Ready to Test" "In UAT"`

| Parameter | Team default | Notes |
|---|---|---|
| `releaseStatuses` | `["Ready for Release", "Ready to Test", "In UAT"]` | Team-specific — supply the exact names your board uses |

A status name that doesn't exist in Jira simply matches nothing; it is not an error. If counts look suspiciously low, verify the status names against the board first.

## Report like this

If `count > 0`, tell the PM which issues are waiting and in which status. `count: 0` → "nothing waiting to test".

## Full detail

Output fields and error handling: [reference.md](reference.md).
