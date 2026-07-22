# scrum-agent

A Scrum-Master assistant exposed as an **MCP tool server** over on-prem Jira. Spring Boot modular
monolith (Java 21, Gradle). Computes scrum capabilities in code and exposes each over **two
transports** — MCP and REST — sharing one CQRS core. An LLM harness reasons over the results.

## Commands

```bash
./gradlew :scrum-mcp-server:build      # compile + acceptance tests + ModularityTests
./gradlew :scrum-mcp-server:bootRun    # local server: /mcp and /api on 8097
./scrum <cmd>                          # curl wrapper over the REST API; ./scrum help
./setup.sh                             # Docker: build image, wire MCP, start REST container
```

## Working agreement

- **Never commit or branch on your own.** Do not run `git commit`, `git push`, `git checkout -b`,
  or `git branch` unless the user explicitly asks for *that specific git action* in the current turn.
  A past "yes, commit" (or an earlier branch) is not standing authorization for later work, and do
  not create a branch as a side effect of preparing to commit. Just edit files in place on the
  current branch. Finish the change, report it, and **wait** — the user decides when to branch,
  commit, and push. This holds even when the suite is green and the work is "done".

## Invariants

These are non-negotiable. Breaking one is a design error, not a style preference.

1. **Metrics are computed in code, never by a model.** Never add a capability that asks an LLM to
   produce a number. Same inputs must always give the same numbers.
2. **The presentation layer holds no logic.** A `@Tool` method and a `@GetMapping` method are each
   one line: build the query, dispatch it. An `if` there belongs in the handler.
3. **Every capability is reachable both ways** — MCP tool *and* REST endpoint *and* a `./scrum`
   subcommand, all dispatching the same query object. One transport alone is incomplete.
4. **Defaults live in the handler as constants**, with a comment explaining the number. Tier-2
   params are nullable all the way down so both transports resolve identically.
5. **Capability modules never import each other.** Enforced by `@ApplicationModule(allowedDependencies
   = {"jira", "shared"})`; verified by `ModularityTests`. Handlers live in `<module>/internal/`.
   The one sanctioned exception is `risk → capacity`: `capacity` is a leaf gateway seam (no
   implementation in phase 1), so `risk` declares `allowedDependencies = {"jira", "capacity",
   "shared"}`. In phase 1 capacity is supplied by tool params; `CapacityGateway` is the phase-2
   plug point. This is a dependency on a gateway seam, not a capability-to-capability import.
6. **No raw Jira payload crosses a module boundary.** `JiraClient` owns the wire format and returns
   curated records. A `JsonNode` must never appear in a capability module.
7. **Writes stay gated.** Read-only by default. The one write follows prepare → human review →
   commit-with-token, blocked at both the registration seam (`ReadTool`/`WriteTool`) and inside the
   handler. Never add a one-shot write; never expose a commit over REST.

## Config is tiered — don't collapse it

- **Tier 1** (`application.yml` / `.env`) — instance identity and secrets: base URL, PAT,
  `JIRA_PROJECT_KEYS` (**plural**; it's what separates "our" issues from other teams'), board id,
  sprint name filter, custom-field ids.
- **Tier 2** (tool parameters at call time, never stored) — team policy: WIP limits, release status
  names, risk thresholds, link types, capacity numbers.

Tier-2 values change per sprint or per conversation; storing them would mean a redeploy to answer
"what if the limit were 2?". **Do not promote a Tier-2 value into `application.yml`.**

## Skills

Skills are split by audience, because they have different readers and different lifecycles:

- **[`.claude/skills/`](.claude/skills/README.md)** — **development** skills: how to write and
  maintain this server. This is what you want when working in this repo.
- **[`.github/skills/`](.github/skills/README.md)** — **capability** skills: how a Scrum Master's
  harness uses the server against Jira. Versioned with the tool surface, and on the path with the
  broadest GitHub Copilot support, since that's the harness they use.

Adding a capability? Follow `add-capability` — all seven steps, including the acceptance test and
the skill file. A capability with no `SKILL.md` is invisible to the agent.

## Gotchas

- **A board can have several active sprints** (PREP, parallel teams, PI overlaps). Always resolve
  through `activeSprint()`; never assume `sprints("active")` returns one.
- **The greenhopper velocity endpoint is unofficial** and may 404. Keep the Agile-API fallback.
- **Worklog data is sparse on this instance.** Zero logged hours usually means nobody logged, not
  that nobody worked. Two capabilities depend on this — say which you mean when reporting.
- **Acceptance tests stub Jira**, so green tests prove parsing, not JQL or field paths. Verify new
  Jira calls against a real instance (`scripts/check-jira.sh`).
- **`.env` is required and git-ignored.** MCP will not start without it.

## Docs

[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — layers, modules, request flow, known gaps.
[`docs/DESIGN.md`](docs/DESIGN.md) — product vision, responsibility inventory, roadmap.
[`docs/PHASE-2-HARNESS.md`](docs/PHASE-2-HARNESS.md) — sketch: skills → system prompt when the
agentic loop moves in-house (Spring AI). Not built yet.
