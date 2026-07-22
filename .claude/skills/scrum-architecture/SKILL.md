---
name: scrum-architecture
description: Detailed architecture reference for scrum-mcp-server — the module dependency table, how a request flows from both transports through the mediator to Jira, how the read-only write gate is built, and what is stubbed or approximate. Use when writing or reviewing Java here, or deciding where new code belongs.
paths: scrum-mcp-server/src/**
---

# scrum-mcp-server — architecture reference

**The seven invariants live in [CLAUDE.md](../../../CLAUDE.md) and are always in context.** This file
is the detail behind them: where code goes, how the pieces connect, and what is approximate.

Recipes: [add-capability](../add-capability/SKILL.md) · [jira-gateway](../jira-gateway/SKILL.md) ·
`cucumber-acceptance-test` (under `scrum-mcp-server/.claude/skills/`).
Narrative version with diagrams: [docs/ARCHITECTURE.md](../../../docs/ARCHITECTURE.md).

## Where code goes

```
presentation/mcp/*Tools          @Tool methods            ← one line each
presentation/rest/ScrumApiController   @GetMapping        ← one line each
<module>/XQuery, XReport         public API of the module
<module>/internal/XHandler       @QueryHandler — the computation
jira/JiraGateway                 interface capability modules depend on
jira/internal/JiraClient         the only place that knows Jira's wire format
shared/                          governance flags, ReadTool/WriteTool, enums
```

## Module dependency table

Every capability module declares `allowedDependencies = {"jira", "shared"}` in its
`package-info.java`. `ModularityTests` fails the build on any violation — cycles, reaches into
another module's `internal/`, or an undeclared dependency.

| Module | Owns | May depend on |
|---|---|---|
| `jira` | `JiraGateway`, `JiraClient`, Jira DTOs | `shared` |
| `hygiene` | estimate / AC / assignee gaps | `jira`, `shared` |
| `flow` | WIP, release queue, sprint flow | `jira`, `shared` |
| `worklog` | logged hours, estimate variance | `jira`, `shared` |
| `metrics` | velocity, scope change, publication | `jira`, `shared` |
| `dependencies` | cross-team dependency radar | `jira`, `shared` |
| `risk` | delivery-risk scoring | `jira`, `shared` |
| `capacity` | capacity gateway (stubbed — see below) | `shared` |
| `shared` | governance, markers, enums | — |

**Practical consequence:** if `risk` needs dependency data it goes through `jira` or takes it as a
parameter — it does **not** import from `dependencies`. This is why `delivery-risk` accepts capacity
as an argument rather than reaching into `capacity`.

## One request, both transports

```
FlowTools.sprintFlow(...)        ScrumApiController.sprintFlow(...)
            └──────────┬──────────────────┘
                       ▼
      mediator.query(new SprintFlowQuery(stuckDays, stalledDays))
                       ▼
      SprintFlowHandler   (@QueryHandler, in flow/internal/)
                       ▼
      JiraGateway → JiraClient → RestClient → Jira
                       ▼
                SprintFlowReport
```

There is no second implementation to keep in sync, which is what makes the REST fallback genuinely
equivalent for orgs that disable MCP rather than approximately equivalent.

## How the write gate is built

Two independent layers; both must stay intact.

**Registration.** Every `*Tools` bean implements `ReadTool` or `WriteTool`. `McpToolConfig` injects
both lists and registers them — there is no hard-coded bean list, so a new capability's tools are
picked up just by implementing the marker. `WriteTool` beans are conditional on
`scrum.governance.read-only=false`, so in the default profile they are absent from `tools/list`
entirely. The model cannot call what it cannot see.

**Handler re-check.** The command handler re-reads the flag and refuses. This covers REST and CLI
callers, who never passed through MCP registration.

The `publish-metrics` flow is the template for any future write: safe `prepare` returning rendered
content plus a ~15-minute token → human reads it → gated `commit` taking that exact token.

## What is stubbed or approximate

Don't report these as bugs; they're known.

- **`capacity` is a gateway with no data source.** `delivery-risk` therefore takes capacity as a
  parameter with no default and honestly reports `capacityKnown: false` when omitted.
- **Scope-change "removed" is a lower bound** — the Agile members endpoint may not return issues
  removed from a sprint.
- **`movedSinceLastCheck` is a fixed 7-day window** — no persisted last-check state.
- **Reopen detection is keyword-based** on status names (closed/done/resolved/released/cancel/
  solved/approved). A custom terminal status outside that list means a reopen is missed.
- **Velocity depends on the unofficial greenhopper endpoint**; there is an Agile-API fallback.
- **No burndown, deliberately.** Jira's native chart is authoritative; a second, disagreeing
  burndown would be worse than none.

## Testing seams

Acceptance tests drive the **REST transport**. Because both transports dispatch the same query, that
covers the MCP path's logic too — but **not** MCP registration itself, including the `WriteTool`
gating in `McpToolConfig`. If you touch that seam, verify it by hand.

Tests stub Jira at the HTTP boundary, so they prove parsing, not JQL correctness or field paths.
