# Architecture

How `scrum-mcp-server` is built and why. For *what* it should become, read
[DESIGN.md](DESIGN.md) — the responsibility inventory, roadmap, and the Teams/capacity plan. For
*how to run it*, read the root [README](../README.md). This document is the middle layer: the shape
of the code as it stands today.

---

## 1. The one-sentence version

A Spring Boot modular monolith that turns Jira into **scrum capabilities** — computed in code, never
estimated by a model — and exposes each capability over **two transports** (MCP and REST) that share
a single computation core. An LLM harness reasons over the results; it does not produce them.

That last point is the load-bearing constraint. From DESIGN §1D:

> **Rule: metrics are computed in code/JQL, never by the LLM.** The LLM only interprets.

Everything below exists to keep that rule structurally true rather than merely intended.

---

## 2. Four layers

```
┌──────────────────────────────────────────────────────────────┐
│ SKILLS      .github/skills/ (capability) .claude/skills/ (dev)│  when to call what,
│             (auto-discovered by the harness)                 │  how to report it
├──────────────────────────────────────────────────────────────┤
│ PRESENTATION  presentation/mcp/*Tools    presentation/rest/  │  two transports,
│               @Tool methods              ScrumApiController  │  zero logic
├──────────────────────────────────────────────────────────────┤
│ CAPABILITY    hygiene/ flow/ risk/ metrics/ worklog/         │  Query → Handler
│               dependencies/ capacity/       (mediator bus)   │  the actual computation
├──────────────────────────────────────────────────────────────┤
│ GATEWAY       jira/JiraGateway → jira/internal/JiraClient    │  the only door to Jira
└──────────────────────────────────────────────────────────────┘
```

The skills layer is not decoration. A capability the harness cannot discover is a capability that
does not get used — see §7.

---

## 3. Module boundaries are enforced, not documented

This is a **Spring Modulith** application. Each capability package declares what it may reach:

```java
// flow/package-info.java
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"jira", "shared"})
package io.github.scrumagent.flow;
```

`ModularityTests` fails the build on any violation — cycles, cross-module reaches into an
`internal` package, or a dependency not in `allowedDependencies`:

```java
ApplicationModules.of(ScrumAgentApplication.class).verify();
```

Consequences worth knowing before you add code:

- **Capability modules never talk to each other.** `risk` cannot call `dependencies`. If a capability
  needs another's data, it goes through `jira` or the data is passed in as a parameter. The one
  sanctioned exception is `risk → capacity`: `capacity` is a leaf **gateway seam** (a `CapacityGateway`
  interface with no phase-1 implementation), so `risk` may depend on it the way any module depends on
  a gateway. In phase 1 capacity still arrives as tool params; `CapacityGateway` is the phase-2 plug
  point, at which risk reads capacity through the seam instead of the params.
- **`internal` packages are private.** Handlers live in `<module>/internal/`. Only the Query, Report,
  and Gateway types are public API.
- **`jira` and `shared` are the only shared dependencies.** Everything else is a leaf.

| Module | Owns | Depends on |
|---|---|---|
| `jira` | `JiraGateway`, `JiraClient`, all Jira DTOs | `shared` |
| `hygiene` | estimate / AC / assignee gaps | `jira`, `shared` |
| `flow` | WIP, release queue, sprint flow | `jira`, `shared` |
| `worklog` | logged hours, estimate variance | `jira`, `shared` |
| `metrics` | velocity, scope change, publication | `jira`, `shared` |
| `dependencies` | cross-team dependency radar | `jira`, `shared` |
| `risk` | delivery-risk scoring | `jira`, `capacity`, `shared` |
| `capacity` | capacity gateway (stub — see §9) | `shared` |
| `shared` | governance flags, `ReadTool`/`WriteTool`, enums | — |

---

## 4. One request, end to end

Both transports dispatch the **same query object** through a CQRS mediator bus
(`io.github.springmediator:spring-mediator-starter`). Take `sprint_flow`:

```
MCP client                          REST caller (./scrum sprint-flow)
    │                                          │
    ▼                                          ▼
FlowTools.sprintFlow(...)          ScrumApiController.sprintFlow(...)
    │                                          │
    └──────────────┬───────────────────────────┘
                   ▼
       mediator.query(new SprintFlowQuery(stuckDays, stalledDays))
                   ▼
       SprintFlowHandler  (@QueryHandler, in flow/internal/)
                   ▼
       JiraGateway.<changelog-derived sprint flow>
                   ▼
       JiraClient → RestClient → Jira REST / Agile API
                   ▼
              SprintFlowReport  ── serialized identically to both callers
```

The presentation layer holds **no logic** — a `@Tool` method is one line:

```java
@Tool(name = "sprint_flow", description = "[read-only] Daily-standup flow view: ...")
public SprintFlowReport sprintFlow(
        @ToolParam(required = false, description = "...") Integer stuckDays,
        @ToolParam(required = false, description = "...") Integer stalledDays) {
    return mediator.query(new SprintFlowQuery(stuckDays, stalledDays));
}
```

**Why this matters:** MCP and REST cannot drift. There is no second implementation to keep in sync,
so the REST fallback is genuinely equivalent rather than approximately equivalent — which is what
makes it a safe answer to "our org disabled MCP".

Defaults live in the **handler**, not the transport:

```java
private static final int DEFAULT_STUCK_DAYS = 5;
...
int stuckDays = query.getStuckDays() != null ? query.getStuckDays() : DEFAULT_STUCK_DAYS;
```

So a `null` from either transport resolves identically. When you change a default, change it in one
place — and update the skill's parameter table, which is the only other place the number appears.

---

## 5. Governance: writes are gated twice

The server is read-only by default (`scrum.governance.read-only=true`). There is exactly one write
capability, `commit_publish_metrics`, and it is blocked at **two independent layers**:

**Layer 1 — structural registration.** Every `*Tools` bean implements a marker interface:

```java
public interface ReadTool {}   // always registered
public interface WriteTool {}  // registered ONLY when read-only=false
```

`McpToolConfig` registers `WriteTool` beans only in write mode. In the default profile the tool is
not in `tools/list` at all — the model cannot call what it cannot see.

**Layer 2 — handler re-check.** The command handler re-reads the flag and refuses. This closes the
REST and CLI paths, which never went through MCP registration in the first place.

On top of that, the write is **not exposed over REST at all**, and it requires a token from a prior
`prepare_publish_metrics` call that expires in ~15 minutes. The flow is prepare → *human reads the
rendered content* → commit with the exact token.

This is the template for every future write. A capability that notifies Teams or files a RISK issue
should follow the same shape: a safe `prepare` that returns rendered content plus a token, and a
gated `commit`. Do not add a one-shot write.

---

## 6. Configuration is tiered on purpose

**Tier 1 — instance identity and secrets. Lives in `application.yml` / `.env`.** Things the agent
must never guess:

| Setting | Env var | Notes |
|---|---|---|
| `scrum.jira.base-url` | `JIRA_BASE_URL` | |
| `scrum.jira.token` | `JIRA_TOKEN` | PAT, Bearer auth. Never committed. |
| `scrum.jira.project-keys` | `JIRA_PROJECT_KEYS` | **Plural, comma-separated** (`VAL,MXEV`). Anything outside this set counts as another team — this is what makes cross-team dependency detection work. |
| `scrum.jira.board-id` | `JIRA_BOARD_ID` | rapidView id; resolved from the project key if unset |
| `scrum.jira.sprint-name-filter` | `JIRA_SPRINT_FILTER` | Picks the team's sprint when a board has several active |
| `scrum.jira.fields.*` | — | Per-instance custom-field ids (story points, acceptance criteria) |

**Tier 2 — team policy. Passed as tool parameters at call time, never stored server-side.** WIP
limits, release status names, risk thresholds, dependency link types, capacity numbers.

The reason for the split: Tier 2 values are the ones a Scrum Master legitimately changes per sprint
or per conversation, and baking them into config would mean a redeploy to answer "what if we said
the limit was 2?". The team-standard values live in each skill's parameter table instead, so the
agent supplies them with a documented default and can be overridden mid-conversation.

**Logged hours has a deliberate fallback.** `scrum.jira.fields.logged-hours` is optional. Left unset
— the common case, since most instances have no such custom field — the server reads Jira's native
worklog / timetracking instead.

---

## 7. The skills layer

**Agent Skills** are a cross-vendor standard — GitHub Copilot and Claude Code both discover them and
both implement progressive disclosure (only `name` + `description` sit in context; the body loads
when the description matches; bundled files load when the body links to them).

They are split by **audience**, because the two sets have different readers and lifecycles:

```
.github/skills/<name>/          # CAPABILITY — how a Scrum Master's harness uses the server
├── SKILL.md                    #   invocation, params, how to report, caveats
└── reference.md                #   loaded on demand — full field-by-field detail

.claude/skills/<name>/          # DEVELOPMENT — how to write and maintain the server
└── SKILL.md
```

**Why `.github/skills/` for the capability set:** it has the broadest Copilot support — VS Code,
JetBrains, Visual Studio, Eclipse, the CLI, and the cloud agent. `.claude/skills/` is read by VS Code
and JetBrains but **not** Visual Studio or Eclipse. Since the Scrum Master is on Copilot and the
developer is on Claude Code, each set sits where its audience will actually find it.

Copilot Chat **on github.com supports no skills at all**; `.github/copilot-instructions.md` is the
always-on summary covering that surface — the tool table, team-standard parameters, and the
interpretation rules.

Each `SKILL.md` frontmatter carries:

- **`description`** — the trigger. Contains the phrases a Scrum Master actually says ("what's stuck",
  "are we going to make it", "is the board clean"). Max 1024 chars, and truncation strips matching
  keywords, so they are front-loaded.
- **`allowed-tools`** — pre-approves that skill's own MCP tools, so read-only capabilities run
  without a permission prompt. **`commit_publish_metrics` is excluded everywhere on purpose** — the
  one write always prompts.

Skills also carry the interpretation rules that keep the model honest about data quality: that
all-zero logged hours means the team isn't logging rather than that nobody worked; that
`capacityKnown: false` means the capacity factor was not evaluated; that `movedSinceLastCheck` is a
fixed 7-day look-back, not "since you last looked". Those distinctions are where a plausible-sounding
wrong answer would otherwise come from.

Catalogs: [capability](../.github/skills/README.md) · [development](../.claude/skills/README.md).

---

## 8. Testing

**Cucumber acceptance tests, one feature file per capability**, against a MockServer-stubbed Jira —
`src/test/resources/features/*.feature`. The stack is feature → steps → `ScrumApiDriver` → the real
Spring context → stubbed Jira HTTP. Assertions live in `*State` classes (`@ScenarioScope`), never in
step definitions. The conventions are themselves a skill:
`scrum-mcp-server/.claude/skills/cucumber-acceptance-test/`.

Tests drive the **REST transport**, which — because both transports dispatch the same query — covers
the MCP path's logic too. What it does *not* cover is MCP registration itself, including the
`WriteTool` gating in `McpToolConfig`. Worth knowing when you touch that seam.

`ModularityTests` verifies the module graph. It is fast and it will catch an architectural mistake
before review does.

```bash
./gradlew :scrum-mcp-server:build     # compile + acceptance tests + modularity
```

Two build quirks documented in `build.gradle.kts`, both deliberate: the shaded MockServer artifact
avoids a `json-schema-validator` clash with the MCP SDK, and Logback is excluded from the *test*
classpath only because the shaded jar bundles a competing JUL provider. Production logging is
untouched.

---

## 9. Adding a capability

The path is mechanical. Following it keeps modularity, both transports, and the docs in step:

1. **Query + Report** in `<module>/` — `XQuery implements IQuery<XReport>`, nullable Tier-2 params.
2. **Handler** in `<module>/internal/` — `@QueryHandler(XQuery.class)`, defaults as constants.
3. **Gateway method** on `JiraGateway` if new Jira data is needed; implement in `JiraClient`.
4. **MCP tool** — one line in the module's `*Tools` bean. Implement `ReadTool` (or `WriteTool`, and
   then follow the prepare/commit pattern in §5).
5. **REST endpoint** — one line in `ScrumApiController`, plus a `./scrum` subcommand.
6. **Feature file + steps** — `src/test/resources/features/<name>.feature`.
7. **Skill** — `.github/skills/<name>/SKILL.md` and `reference.md`. Add a row to the skills catalog.

Skip step 7 and the capability exists but is invisible to the agent. That was the state of all ten
skills before they were migrated.

---

## 10. Transports and deployment

| Path | Transport | Port | Launched by |
|---|---|---|---|
| MCP | stdio (`docker run -i`) | none | the harness, per session, via `.mcp.json` |
| MCP | streamable HTTP `/mcp` | 8097 | `bootRun` / `docker.sh run` |
| REST | HTTP `/api` | dynamic → `.scrum-env` | `docker compose up` (via `setup.sh`) |

`setup.sh` builds the jar on the **host** and packages it into a ~250 MB JRE image. Building Java
inside Docker is avoided deliberately — it is unreliable on locked-down/proxied corporate networks.

`.mcp.json` is generated by `setup.sh` and git-ignored. It uses `--env-file .env`, which passes the
whole Tier-1 set; a hand-edited version that lists env vars individually will silently omit
`JIRA_PROJECT_KEYS` / `JIRA_BOARD_ID` and fail to resolve config at boot.

**Before corporate use:** `/api` and `/mcp` are unauthenticated. Put them behind ingress with TLS,
pass the PAT via `--env-file` or a vault, and leave `read-only=true` until writes are deliberately
wanted.

---

## 11. Known gaps

Honest list of what is stubbed, approximate, or unverified — so nobody reports these as findings:

- **`capacity` is a gateway with no source.** `delivery-risk` therefore takes capacity as a
  parameter with no default and reports `capacityKnown: false` when omitted. The Teams capacity-sheet
  pipeline (DESIGN §4) is what fills this in.
- **Scope-change "removed" is a lower bound.** The Agile members endpoint may not return issues
  removed from a sprint.
- **`movedSinceLastCheck` is a fixed 7-day window.** There is no persisted last-check state.
- **Reopen detection is keyword-based** on status names (closed/done/resolved/released/cancel/
  solved/approved). A custom terminal status outside that list means a reopen is missed.
- **Velocity depends on the unofficial greenhopper endpoint.** `scripts/check-jira.sh` probes whether
  it works on a given instance; there is an Agile-API fallback path.
- **Worklog data quality is the binding constraint on two capabilities.** `logged-hours` and
  `estimate-variance` are only as good as the team's logging habit. Verify against the real instance
  before drawing conclusions from either.
- **No burndown, on purpose.** Jira's native chart is authoritative; a second, disagreeing burndown
  would be worse than none.
