---
name: stories-to-test
description: List the dev-complete stories waiting for the Product Owner to accept/test, each with its acceptance criteria and parent epic. Use when asked "what's ready for me to test", "what's waiting for acceptance", "what has dev finished", or to prep a PO acceptance session.
allowed-tools: mcp__scrum__stories_to_test, Bash(./scrum stories-to-test*)
---

# Stories to Test (PO acceptance queue)

Read-only. **PO-facing.** Lists the stories currently sitting in the team's "ready to test" statuses
across the configured projects — **not** limited to the active sprint, because a PO tests dev-complete
work whenever it is ready. Each story comes back with the ingredients a PO needs to start testing:
acceptance criteria and the parent epic.

## Invoke

- **MCP (preferred):** `stories_to_test(statuses, issueType, assignee)` — all optional; omit `statuses` for the default.
- **REST fallback:** `./scrum stories-to-test "Ready for Acceptance" "Under Validation" [--type Story] [--assignee "Jane Dev"]`

| Parameter | Team default | Notes |
|---|---|---|
| `statuses` | `["Ready for Acceptance"]` | The PO "ready to test" status(es). Exact spelling; both projects use "Ready for Acceptance". |
| `issueType` (REST: `--type`) | all types | Only include issues of this type, e.g. `Story` (default: all types). |
| `assignee` (REST: `--assignee`) | all assignees | Only include issues assigned to this person (default: all assignees). |

**HITL — the "to test" statuses are the team's call.** The default is `Ready for Acceptance`. VAL also
uses `Under Validation` / `Under Testing`. If the PO cares about those, pass them explicitly. A status
that doesn't exist in Jira simply matches nothing (not an error).

## Report like this

If the list is non-empty, tell the PO which stories are waiting, and for each its AC and epic so they
can start. Empty list → "nothing waiting for acceptance right now".

## Phase-2 note

This read tool is the foundation a future "post the acceptance queue to the PO's channel" piece will
sit on — keep it a clean read.

## Full detail

Output fields and error handling: [reference.md](reference.md).
