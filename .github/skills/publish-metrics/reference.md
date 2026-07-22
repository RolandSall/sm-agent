---
name: publish-metrics
description: >
  Stage the sprint-metrics summary for human review (safe), then publish it with the returned token
  (write, gated) — the prepare → review → commit human-in-the-loop flow. Never publish in one shot.
---

# Publish Metrics Skill (human-in-the-loop)

Publishing sprint metrics is a **two-step, human-in-the-loop** flow. Preparing is always safe;
committing writes and is gated. This is the template every "write to an external system" capability
follows.

## When to use
- After reviewing [sprint-metrics](../sprint-metrics/reference.md), when the human wants the summary published
  (e.g. to a Confluence page — the real publisher lands in phase 2; today it's logged).

## Prerequisites
- **Mode A (MCP):** tools `prepare_publish_metrics` (read) and `commit_publish_metrics` (write).
- **Commit availability:** `commit_publish_metrics` exists **only** when
  `scrum.governance.read-only=false`. In the default read-only profile it isn't registered at all,
  and the command handler re-checks the flag — so a REST caller is refused too.

## Flow

### Step 1 — prepare (read-only, safe)
- **MCP tool:** `prepare_publish_metrics` — no arguments.
- **REST (when MCP is disabled):** `./scrum metrics-preview` (or `source .scrum-env && curl -X POST "$SCRUM_API_URL/api/metrics-preview"`)
- Returns a `MetricsPublicationPreview`: `{ token, targetSpace, targetTitle, renderedContent, expiresAt }`.
  **Nothing is written.**

### Step 2 — human review (required)
Show `renderedContent` to the human and get an **explicit** go-ahead. Do not proceed on your own.

### Step 3 — commit (write, gated)
- **MCP tool:** `commit_publish_metrics(token)` — pass the exact `token` from step 1.
- Publishes the previewed content and returns a confirmation string.
- The commit is **not** exposed over REST — only via the MCP write tool, after approval, and
  only when `scrum.governance.read-only=false`. This keeps a human in the loop for the actual write.

## How to interpret / act
Present the preview → capture explicit approval → commit with the exact token → confirm success. If
the commit is refused, the server is in read-only mode: tell the human to enable write mode
(`scrum.governance.read-only=false`) rather than retrying.

## Error handling
- Committing without a matching/live token → rejected ("call prepare first, or it expired"). Tokens
  expire after ~15 minutes.
- Commit in read-only mode → refused with an error; this is by design, not a bug.

## Related skills
[sprint-metrics](../sprint-metrics/reference.md) · [README](../README.md)
