---
name: stories-to-test
description: >
  List dev-complete stories waiting for the Product Owner to accept/test, across the configured
  projects (not sprint-limited), each with its acceptance criteria and parent epic.
---

# Stories to Test Skill

Report the stories parked in the team's acceptance-test statuses so the PO knows what to test.
**Read-only. PO-facing.**

## When to use
- The PO asks "what's ready for me to test / accept?"
- To prep an acceptance / UAT session.
- To track a growing acceptance backlog mid-sprint.

## Prerequisites
- **Mode A (MCP):** tool `stories_to_test`.
- **Mode B (REST):** `./scrum stories-to-test [status ...]` against the running server — see [README](../README.md).
- **Config:** Tier-1 Jira connection; the Epic-Link and acceptance-criteria custom-field ids are
  configured per instance in `application.yml`.

## Commands
- **MCP tool:** `stories_to_test(statuses, issueType, assignee)` — the list of status names plus optional filters; omit for the default.
- **REST (when MCP is disabled):**
  ```bash
  ./scrum stories-to-test "Ready for Acceptance"
  ./scrum stories-to-test "Ready for Acceptance" --type Story --assignee "Jane Dev"
  # or raw curl (dynamic port; -G --data-urlencode handles the spaces):
  source .scrum-env && curl -G "$SCRUM_API_URL/api/stories-to-test" \
    --data-urlencode "status=Ready for Acceptance" --data-urlencode "status=Under Validation"
  ```
  With no `status`, the server applies the default `["Ready for Acceptance"]`.

## Parameters
| Parameter | Team default | Notes |
|---|---|---|
| `statuses` (REST: `status`, repeatable) | `["Ready for Acceptance"]` | The PO "ready to test" status names. Exact spelling. VAL also uses `Under Validation` / `Under Testing`. |
| `issueType` (REST: `issueType`, wrapper `--type`) | all types | Only include issues of this type, e.g. `Story` (default: all types). |
| `assignee` (REST: `assignee`, wrapper `--assignee`) | all assignees | Only include issues assigned to this person (default: all assignees). |

The default is applied in-code when the parameter is null/empty — it is **not** server config, so the
team can override per call without a redeploy.

## Output
Returns a `StoriesToTestReport`:
- `stories[]` — one `StoryToTest` per waiting story:
  - `key` — the story key.
  - `summary` — the story summary.
  - `assignee` — who did the work (display name), or `null` if unassigned.
  - `acceptanceCriteria` — the AC to test against, or `null` if none captured. **`null` means no AC
    was recorded, not that the story has none in someone's head** — flag it, don't assume it's testable.
  - `epicKey` — the parent Epic ("feature") key, or `null` if the story isn't under an epic.
  - `status` — the current status.

## How to interpret / act
List the waiting stories to the PO with each story's AC and epic. If `acceptanceCriteria` is `null`,
call it out — the PO can't objectively accept a story with no written criteria. Scope spans **all**
configured projects and is **not** sprint-limited by design.

## Error handling
- Empty list → nothing is waiting for acceptance.
- A status name that doesn't exist in Jira simply matches nothing (no error) — if counts look wrong,
  verify the exact status spelling against the board.

## Phase 2
A scheduled job will use this same read to auto-notify the PO's channel; in phase 1 the agent relays it.

## Related skills
[release-queue](../release-queue/reference.md) · [board-hygiene](../board-hygiene/reference.md) · [README](../README.md)
