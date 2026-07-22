---
name: release-queue
description: >
  Count issues sitting in the team's release / ready-to-test statuses (bucketed by status),
  so the PM knows how much is waiting to be tested or released.
---

# Release Queue Skill

Report how many issues are parked in the "ready to test / ready for release" columns. **Read-only.**
Jira has no native category for a "ready to test" bucket, so you must tell the tool which status
names count.

## When to use
- To tell the PM "N items are ready for you to test".
- To track a growing test/release backlog during the sprint.

## Prerequisites
- **Mode A (MCP):** tool `release_queue`.
- **Mode B (REST):** run `./scrum <cmd>` against the running server — see [README](../README.md).
- **Config:** Tier-1 Jira connection.

## Commands
### Count the release queue
- **MCP tool:** `release_queue(releaseStatuses)` — the list of status names.
- **REST (when MCP is disabled):**
  ```bash
  ./scrum release-queue "Ready for Release" "Ready to Test" "In UAT"
  # or raw curl (dynamic port; -G --data-urlencode handles the spaces):
  source .scrum-env && curl -G "$SCRUM_API_URL/api/release-queue" \
    --data-urlencode "status=Ready for Release" --data-urlencode "status=Ready to Test"
  ```
Pass each status as a quoted argument; the wrapper URL-encodes them.

## Parameters
| Parameter | Team default (edit to your workflow) | Notes |
|---|---|---|
| `releaseStatuses` | `["Ready for Release", "Ready to Test", "In UAT"]` | your instance's ready-to-test / release status names |

These are team-specific — supply the exact status names your board uses.

## Output
Returns a `ReleaseQueueReport`:
- `count` — total issues in those statuses.
- `byStatus` — `{ "Ready to Test": 2, ... }` count per status.
- `items[]` — `{ key, summary, status, assignee }`.

## How to interpret / act
If `count > 0`, tell the PM which issues are waiting and in which status. In phase 2 a scheduled job
uses this to auto-notify the PM; in phase 1 you (the agent) relay it.

## Error handling
- Empty queue → `count: 0` — report "nothing waiting to test".
- A status name that doesn't exist in Jira simply matches nothing (no error).

## Related skills
[wip-limits](../wip-limits/reference.md) · [board-hygiene](../board-hygiene/reference.md) · [README](../README.md)
