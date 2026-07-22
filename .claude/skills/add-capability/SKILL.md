---
name: add-capability
description: End-to-end recipe for adding a new scrum capability to scrum-mcp-server — Query, Handler, gateway method, MCP tool, REST endpoint, ./scrum subcommand, acceptance feature, and the agent-facing skill. Use when adding a new tool, metric, report, or check to this server.
argument-hint: "[capability name and what it should compute]"
---

# Adding a capability

Seven steps. Follow them in order — each layer depends on the one before, and skipping the last two leaves a capability the agent can neither test nor find.

Read [scrum-architecture](../scrum-architecture/SKILL.md) first if you haven't; the invariants there constrain every step below.

Worked example throughout: a `sprint_flow` style capability in the `flow` module.

---

## 1. Query + Report — `<module>/`

Public API of the module. The Query carries **nullable** Tier-2 params.

```java
package io.github.scrumagent.flow;

import io.github.springmediator.mediator.core.IQuery;

/**
 * Mediator query: <what it answers>. Thresholds are agent-supplied (Tier 2) and optional —
 * a {@code null} falls back to the handler's default.
 */
public class SprintFlowQuery implements IQuery<SprintFlowReport> {

    private final Integer stuckDays;

    public SprintFlowQuery(Integer stuckDays) {
        this.stuckDays = stuckDays;
    }

    public Integer getStuckDays() {
        return stuckDays;
    }
}
```

The Report is a record (nested records for row types). Return **only what a Scrum Master would act on** — omit clean/on-track rows, as `board_hygiene` and `estimate_variance` do. A report that lists everything makes the model summarize noise.

## 2. Handler — `<module>/internal/`

Package-private-ish by convention; lives in `internal/` so no other module can reach it.

```java
package io.github.scrumagent.flow.internal;

import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;

@QueryHandler(SprintFlowQuery.class)
public class SprintFlowHandler implements IQueryHandler<SprintFlowQuery, SprintFlowReport> {

    /** Default: a card in progress for 5+ days without a status move is worth a standup mention. */
    private static final int DEFAULT_STUCK_DAYS = 5;

    private final JiraGateway jira;

    public SprintFlowHandler(JiraGateway jira) {
        this.jira = jira;
    }

    @Override
    public SprintFlowReport execute(SprintFlowQuery query) {
        int stuckDays = query.getStuckDays() != null ? query.getStuckDays() : DEFAULT_STUCK_DAYS;
        ...
    }
}
```

Defaults are **constants with a comment explaining the number** — that comment is the only record of why the threshold is what it is.

## 3. Gateway method — only if new Jira data is needed

Add to `JiraGateway`, implement in `JiraClient`. See [jira-gateway](../jira-gateway/SKILL.md) for the wire-format conventions. If an existing gateway method already returns what you need, use it — do not add a near-duplicate.

## 4. MCP tool — `presentation/mcp/<Module>Tools.java`

One line. The bean implements `ReadTool` (or `WriteTool`, and then see the governance section of [scrum-architecture](../scrum-architecture/SKILL.md)).

```java
@Tool(name = "sprint_flow", description = "[read-only] Daily-standup flow view: which "
        + "active-sprint tickets are STUCK ... The key signals for the daily scrum.")
public SprintFlowReport sprintFlow(
        @ToolParam(required = false, description = "Days in the current status before an in-progress ticket is STUCK (default 5)") Integer stuckDays) {
    return mediator.query(new SprintFlowQuery(stuckDays));
}
```

Tool naming is `snake_case`. The `description` is what an MCP client sees when deciding to call it — state what it returns and that it is read-only. **Put the default value in the `@ToolParam` description**; a caller reading `tools/list` has no other way to know it.

No new bean is needed for a new tool in an existing module. `McpToolConfig` picks up `ReadTool`/`WriteTool` beans automatically — there is no registration list to edit.

## 5. REST endpoint — `presentation/rest/ScrumApiController.java`

One line, same query, `@RequestParam(required = false)` for every Tier-2 param.

```java
@GetMapping("/sprint-flow")
SprintFlowReport sprintFlow(@RequestParam(required = false) Integer stuckDays) {
    return mediator.query(new SprintFlowQuery(stuckDays));
}
```

Query-param names may be shorter than the MCP param names (`committed` vs `capacityCommittedPoints`) — that's fine, but both must appear in the skill's parameter table.

## 6. `./scrum` subcommand — the `scrum` wrapper at repo root

Add to both the `usage()` heredoc and the `case` block:

```bash
  sprint-flow)
    a=()
    while [ "$#" -gt 0 ]; do
      case "$1" in
        --stuck-days) a+=(--data-urlencode "stuckDays=$2"); shift 2 ;;
        *) usage ;;
      esac
    done
    get /api/sprint-flow "${a[@]}" ;;
```

Flags are `--kebab-case`; they map to the REST camelCase param. Use `--data-urlencode` for everything so values with spaces (status names) survive.

## 7. Acceptance test + skill — **do not skip these**

**Test:** a feature file at `src/test/resources/features/<name>.feature`, plus `steps/<name>/` with `<Name>Steps` and `<Name>State`. Stub Jira via `JiraStubs`/`JiraJson`, drive it through `ScrumApiDriver`. Conventions: the `cucumber-acceptance-test` skill under `scrum-mcp-server/.claude/skills/`.

Tests drive the REST transport, which covers the MCP path's logic too since both share the query — but **not** MCP registration or `WriteTool` gating. Verify those by hand if you touched that seam.

**Skill:** `.github/skills/<name>/SKILL.md` + `reference.md`, and a row in `.github/skills/README.md`. Without it the capability works but the agent never discovers it. Every one of the ten existing skills was invisible for exactly this reason until they were migrated.

The `SKILL.md` must carry any **data-quality caveat** the model would otherwise state wrongly — e.g. that zero logged hours means nobody logged, not that nobody worked.

---

## Verify

```bash
./gradlew :scrum-mcp-server:build     # compile + acceptance + ModularityTests
```

Then exercise it for real against a running server:

```bash
./gradlew :scrum-mcp-server:bootRun
./scrum <your-subcommand>
```

A green test suite against stubbed Jira is not evidence the JQL or field paths are right. Run it against real Jira before calling it done.

---

## Common rationalizations

Steps 6 and 7 are the ones that get skipped, and skipping them is how all ten existing capabilities ended up undiscoverable. The excuses are predictable:

| Excuse | Reality |
|---|---|
| "I'll add the skill file later" | Later doesn't come. Ten capabilities sat invisible to the agent for exactly this reason. The skill is how the capability gets used at all. |
| "The acceptance test can wait" | Every other capability has one. The next person copies the pattern from whatever they read first — leave a gap and it propagates. |
| "It's read-only, so it's low-risk" | Read-only means it can't corrupt Jira. It can still report a wrong number to a Scrum Master who acts on it. |
| "The MCP tool is enough, REST is a fallback nobody uses" | REST is the *only* path for orgs that disable MCP. Half a capability is not a capability. |
| "The default is obvious, no comment needed" | Six months on, nobody knows whether `5` was measured, argued, or guessed. Write down which. |
| "I'll put the threshold in application.yml so it's configurable" | That's a Tier-2 value. Config means a redeploy to answer "what if it were 2?". Pass it as a parameter. |
| "Tests pass, so it works" | Tests stub Jira. They prove parsing, not that your JQL matches anything real. |
| "This capability is too small to need all seven steps" | Small capabilities are exactly where shortcuts compound — they're the ones nobody revisits. |

## Red flags — stop and finish the steps

- About to say "done" with no `SKILL.md` written
- No feature file for the new capability
- Only one transport wired
- A magic number in a handler with no comment
- A Tier-2 threshold that ended up in `application.yml`
- Claiming it works without having run it against real Jira

## Completion checklist

- [ ] Query + Report, Tier-2 params nullable
- [ ] Handler in `internal/`, defaults as commented constants
- [ ] Gateway method (or reused an existing one)
- [ ] MCP tool, with defaults stated in `@ToolParam` descriptions
- [ ] REST endpoint
- [ ] `./scrum` subcommand, in both `usage()` and the `case` block
- [ ] Feature file + steps + state
- [ ] `SKILL.md` + `reference.md` + row in the skills catalog
- [ ] `./gradlew :scrum-mcp-server:build` green — output read, not assumed
- [ ] Exercised against real Jira

Can't tick them all? The capability isn't done.
