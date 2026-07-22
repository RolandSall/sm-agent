# Local development harness

Run the **real** MCP + REST server against a **MockServer** that mimics Jira with rich, prod-like
data — so you can manually drive every scrum skill end to end, with **MCP on or off**, without
touching real Jira. Two ways to run it, same dataset:

## Option 1 — In-JVM (fastest, no Docker)

```bash
./gradlew :scrum-mcp-server:runLocal
```

MockServer runs **inside the same JVM** as the app (`LocalDevRunner` → `LocalJiraFixtures.register`).
One command, dynamic dates computed at boot.

## Option 2 — Containerized (standalone MockServer via docker-compose)

```bash
./gradlew :scrum-mcp-server:bootJar                  # build the app jar the image copies in
./gradlew :scrum-mcp-server:generateMockInitializer  # write mockserver/initializer.json (dated NOW)
docker compose -f development/docker-compose.yml up --build
# stop with: docker compose -f development/docker-compose.yml down
```

A **standalone MockServer container** (`:1080`) loads `mockserver/initializer.json` — generated from
the *same* `LocalJiraFixtures` so the data is identical — and the scrum-agent container (`:8097`) is
wired to it. The initializer's dates are stamped at generation time; if the sprint stops resolving as
"active", rerun `generateMockInitializer` and restart. Both options serve `:8097` (MCP + REST) and
`:1080` (mock Jira), so everything below is identical either way.

This starts one process:

| Surface | URL | Used by |
|---|---|---|
| Mock Jira | `http://localhost:1080` | the server's `JiraClient` (profile `local`) |
| **MCP** (on) | `http://localhost:8097/mcp` | a harness with MCP enabled (Claude Code) |
| **REST** (off) | `http://localhost:8097/api` — or `./scrum <cmd>` | a harness with MCP disabled (Copilot), or you |

Stop with `Ctrl-C`. Story points, dates, and changelogs are generated **relative to today** each run,
so the active sprint always straddles "now" and burndown/flow/scope stay realistic.

## How it's wired (no Docker, nothing ships to prod)

- **`runLocal`** (build.gradle.kts) is a `JavaExec` on the **test** classpath, so it reuses the
  existing MockServer dependency and the logback/JUL isolation — MockServer never enters the
  production artifact or the Modulith scan.
- **`localdev.LocalDevRunner`** starts MockServer on :1080, loads `LocalJiraFixtures`, then boots
  the app under the **`local`** profile.
- **`localdev.LocalDevApp`** is a boot class identical to `ScrumAgentApplication` but excludes the
  Cucumber test beans from the component scan (they need `MockMvc`).
- **`application-local.yml`** points `scrum.jira.base-url` at :1080, mirrors the prod field mappings
  (`customfield_10043` / `11977` / `34061`, native timetracking), and sets `read-only: false` so the
  publish `prepare → commit` flow is drivable.
- **`LocalJiraFixtures`** models each story once and renders it into the different Jira wire shapes
  the client requests; the dataset deliberately covers every skill's edge cases.

## The two ways to drive it

### MCP ON — point a harness at the running server
- **Claude Code**: `.mcp.json` already has a `scrum-local` entry (`http://localhost:8097/mcp`).
  Ask for a tool (e.g. `board_hygiene`). *(This is the cell Copilot can't test — your org disables MCP.)*
- **Copilot**: `.vscode/mcp.json` has the same entry — usable only if your org enables MCP.

### MCP OFF — the terminal / REST path (your org's shipping path)
```bash
./scrum hygiene
./scrum wip --per-assignee 3 --team 5
./scrum release-queue "Ready for Release" "Ready to Test" "In UAT"
./scrum worklog
./scrum estimate-variance --overrun 1.25 --min-estimate 4
./scrum velocity 6
./scrum scope-change
./scrum sprint-flow --stuck-days 5 --stalled-days 3
./scrum deps VAL-101 Blocks Dependency
./scrum sprint-deps Blocks Dependency
./scrum risk --committed 50 --available 40      # forces a capacity-driven HIGH
./scrum issue VAL-101
./scrum metrics-preview
```

## What the fixture data exercises

| Skill / tool | What to look for in the output |
|---|---|
| `board_hygiene` | VAL-102 no estimate · VAL-103 no AC · VAL-104 AC in fallback field · VAL-105/106 unassigned · MXEV-201 whitespace AC · VAL-101 clean |
| `wip_status` | Alice over the per-assignee limit (4 In Progress) · unassigned bucket · `--team 5` breaches team limit |
| `release_queue` | buckets across Ready for Release / Ready to Test / In UAT, VAL + MXEV |
| `worklog_summary` | overrun & no-timetracking rows; native `timeSpentSeconds` totals per assignee |
| `estimate_variance` | VAL-110 OVERRUN · VAL-141 FINISHED_EARLY · VAL-142 NO_LOGGING · VAL-146 exact-boundary (not overrun) · `--min-estimate 4` skips VAL-145 |
| `sprint_velocity` | 6-sprint average (trims a 7th); current sprint shows 0 completed |
| `sprint_scope_change` | VAL-150 added-after-start · VAL-151 removed · VAL-152 committed-at-planning (not counted) |
| `sprint_flow` | VAL-110 stuck · VAL-116 stalled · VAL-117/118 reopened · VAL-119 stuck+reopened · VAL-120 "Shipped" not counted |
| `dependency_radar` / `sprint_dependency_radar` | VAL-101 → PLTF-88 (open, moved recently) & DATA-42 (resolved, stale); Relates + same-project links excluded |
| `sprint_delivery_risk` | VAL-101 HIGH (open blocker) · VAL-161 MEDIUM (8 SP) · VAL-102 MEDIUM (unestimated) · resolved/relates/same-project blockers not counted · `--committed 50 --available 40` → capacity HIGH |
| `feature_effort_rollup` | epics VAL-900 (Payments resilience, dev-job 21, 3 stories, 16 SP, 28h logged) & VAL-901 (KYC & tax, dev-job 13) — resolved from sprint epic-links (no arg) or by `epic=VAL-900` |
| `stories_to_test` | testable-status stories with populated `epicKey` (VAL-302→VAL-901, MXEV-303→VAL-900) via the shared `status in (…)` search |
| `get_issue` | VAL-101 (rich: labels, links, fixVersions, rendered description) · VAL-104 (AC fallback) |
| `prepare_publish_metrics` → `commit_publish_metrics` | preview + token; commit works because `read-only: false` here |

> Team-policy numbers (WIP/risk thresholds) are tool **parameters**, not fixture data — pass them on
> the command line (or let the skill supply team defaults) to see different classifications.
