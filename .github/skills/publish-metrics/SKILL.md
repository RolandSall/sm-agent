---
name: publish-metrics
description: Publish a sprint-metrics summary via the gated prepare → human review → commit flow. Use only when the human explicitly asks to publish or post the metrics summary somewhere durable. Never a first step — read sprint-metrics first.
allowed-tools: mcp__scrum__prepare_publish_metrics, Bash(./scrum metrics-preview)
---

# Publish Metrics — human-in-the-loop

The **only write capability** in this server, and the template every future write follows. Preparing is safe; committing writes and is gated.

`commit_publish_metrics` is **deliberately not in `allowed-tools`** above. The commit must go through an explicit permission prompt every time — do not work around that.

## The flow — all three steps, in order

**1. Prepare (read-only, safe)**
- MCP: `prepare_publish_metrics` — no arguments.
- REST: `./scrum metrics-preview`

Returns `{ token, targetSpace, targetTitle, renderedContent, expiresAt }`. **Nothing is written.**

**2. Human review (required, not optional)**

Show `renderedContent` to the human and get an **explicit** go-ahead. Do not infer approval from earlier conversation, from the fact that they asked to publish, or from a general "yes go ahead" that predates seeing the content. They must approve *this rendered content*.

**3. Commit (write, gated)**
- MCP: `commit_publish_metrics(token)` — pass the exact token from step 1.
- Not exposed over REST at all. MCP write tool only.

## When commit is refused

`commit_publish_metrics` is registered **only** when `scrum.governance.read-only=false`, and the handler re-checks the flag. In the default read-only profile the tool does not exist.

If the commit is refused, that is **by design, not a bug**. Tell the human the server is in read-only mode and that enabling writes is their decision — do not retry, and do not go looking for another path to write the same content.

Tokens expire after ~15 minutes; a stale token is rejected with "call prepare first".

## Full detail

[reference.md](reference.md) · upstream numbers: [sprint-metrics](../sprint-metrics/SKILL.md)
