# Scrum Agent Skills

Each skill documents one scrum capability: when to use it, how to invoke it (two ways), what it
returns field-by-field, how to interpret the result, and what can go wrong. Every capability is
reachable two ways and every skill describes **both**.

These are **Agent Skills** — a cross-vendor standard. **GitHub Copilot** (VS Code, JetBrains, Visual
Studio, Eclipse, the CLI, and the cloud agent) and **Claude Code** both discover and load them
automatically. Nobody has to point a harness at this folder.

Discovery requires the `<name>/SKILL.md` layout **exactly**; a flat `<name>.md` in some other
directory is invisible, which is what these files used to be.

**Exception — Copilot Chat on github.com supports no skills at all.** The repo's
`.github/copilot-instructions.md` is the always-on summary covering that surface. If you change a
tool name or a team-standard default here, change it there too.

> This folder is **self-contained and portable**: it ships inside the server image and is installed
> onto a user's machine with `./scripts/docker.sh install-skills`. Keep every link inside this
> folder — anything pointing up and out of it dangles once installed. Repo-only docs (the root
> README, the developer skills in `.claude/`) are referred to by name here, never linked.

## Layout — two files per skill

```
.github/skills/<name>/
├── SKILL.md      # loaded automatically; lean — invocation, params, how to report
└── reference.md  # loaded on demand when SKILL.md links to it — full field-by-field detail
```

This split is **progressive disclosure**. Only `SKILL.md` frontmatter descriptions sit in context at
all times; the body loads when the skill triggers, and `reference.md` only when actually needed. Keep
`SKILL.md` short — put anything exhaustive in `reference.md`.

Two frontmatter fields carry the weight:
- **`description`** decides whether the skill triggers at all. Lead with the action and pack in the
  phrases a human would actually use ("what's stuck", "are we going to make it"). It is truncated
  when many skills are in scope, and truncation strips the matching keywords — so front-load them.
- **`allowed-tools`** pre-approves this skill's own MCP tools for the invoking turn, so read-only
  capabilities run without a permission prompt. It is turn-scoped and clears on the next user
  message. `commit_publish_metrics` is deliberately excluded everywhere — the one write must always
  prompt.

## Two ways to invoke a capability

- **Primary — MCP tool.** When the harness's MCP feature is enabled, its MCP client speaks the MCP
  protocol to this server (stdio locally, or streamable-HTTP when deployed), discovers the tools via
  `tools/list`, and calls them via `tools/call`. Just name the tool (e.g. `board_hygiene`).
- **Fallback — REST via `./scrum`.** When the harness's MCP feature is **disabled** by org policy but
  its terminal tool is allowed, the harness runs `./scrum <cmd>` (a thin curl wrapper) against this
  server's `/api` on the already-running process — no per-command JVM boot.

Both hit the **same mediator queries**, so results are identical — only the transport differs. Prefer
the MCP tool when available; fall back to `./scrum <cmd>` when it is not.

## Setup / prerequisites

**Config (Tier 1 — the only server config):**
```bash
export JIRA_BASE_URL=https://jira.mycompany.com
export JIRA_TOKEN=<your personal access token>   # never commit this
export JIRA_PROJECT_KEYS=PROJ,PLTF               # one or more; all count as "ours"
export JIRA_BOARD_ID=<id>                         # the team's rapidView/board id
export JIRA_SPRINT_FILTER=<substring>             # picks the team's sprint when several are active
```
Instance-specific custom-field IDs and the board id live in
`scrum-mcp-server/src/main/resources/application.yml`. Team policy (WIP limits, thresholds, statuses,
link types, capacity) is **not** config — it's passed to each tool at call time; the per-skill
"Parameters" tables give the team-standard values.

**Run it:** the easiest path is the repo-root `./setup.sh` (Docker) — see the repo's root README. It
builds the image, wires MCP over stdio, and starts the REST container on a **free port** recorded in
`.scrum-env`. For local dev without Docker:
```bash
./gradlew :scrum-mcp-server:bootRun            # serves /mcp (MCP) and /api (REST) on port 8097
```
The `./scrum` wrapper resolves the REST port automatically (`.scrum-env` under Docker, else 8097), so
skills never hardcode a port. Put auth in front of `/api` when the server is deployed.

**Convenience wrapper (`./scrum`, repo root):** a thin curl wrapper — **no JVM** — that talks to the
running server, URL-encodes params, pretty-prints JSON (via `jq` if present), and reports errors, so
neither you nor the harness hand-crafts curl:
```bash
./scrum hygiene
./scrum wip --per-assignee 3 --team 15
./scrum release-queue "Ready for Release" "In UAT"
./scrum risk --committed 40 --available 35
./scrum deps PROJ-9 Blocks Dependency
```
`SCRUM_API_URL` overrides the host; otherwise `./scrum` reads `.scrum-env` (the Docker-allocated port)
and falls back to `http://localhost:8097` for local dev. `SCRUM_PRINT=1 ./scrum <cmd>` prints the
request instead of sending it. Run `./scrum help` for the full list.

**Read-only by default:** every read capability is always available; the only write
(`commit_publish_metrics`) exists **only** when `scrum.governance.read-only=false`, is exposed **only**
as an MCP tool (never REST), and the handler re-checks the flag.

## Who these are for

These are the **capability skills** — how a Scrum Master's harness uses the server against Jira, one per capability. They live in `.github/skills/` because that path has the **broadest Copilot support**: VS Code, JetBrains, Visual Studio, Eclipse, the Copilot CLI, and the cloud agent all read it, and so does Claude Code's `--add-dir`.

The **development** skills — how to write and maintain the server itself — live separately in the repo under `.claude/skills/`. Different audience, different lifecycle; they are not needed to *use* the server, and they are deliberately **not** shipped with it.

### Skill index

| Skill | MCP tool(s) | REST endpoint (`GET` unless noted, fallback) |
| --- | --- | --- |
| [board-hygiene](board-hygiene/SKILL.md) | `board_hygiene` | `/api/hygiene` |
| [sprint-flow](sprint-flow/SKILL.md) | `sprint_flow` | `/api/sprint-flow?issueType=&assignee=` |
| [wip-limits](wip-limits/SKILL.md) | `wip_status` | `/api/wip?perAssignee=&team=` |
| [release-queue](release-queue/SKILL.md) | `release_queue` | `/api/release-queue?status=…&status=…` |
| [stories-to-test](stories-to-test/SKILL.md) | `stories_to_test` | `/api/stories-to-test?status=…&status=…` |
| [feature-effort-rollup](feature-effort-rollup/SKILL.md) | `feature_effort_rollup` | `/api/feature-effort?epic=…&epic=…` |
| [logged-hours](logged-hours/SKILL.md) | `worklog_summary` | `/api/worklog` |
| [estimate-variance](estimate-variance/SKILL.md) | `estimate_variance` | `/api/estimate-variance?minEstimateHours=` |
| [list-sprints](list-sprints/SKILL.md) | `list_sprints` | `/api/sprints?state=` |
| [sprint-metrics](sprint-metrics/SKILL.md) | `sprint_velocity`, `sprint_scope_change` (burndown → Jira's chart) | `/api/velocity?sprints=`, `/api/scope-change` |
| [publish-metrics](publish-metrics/SKILL.md) | `prepare_publish_metrics` → `commit_publish_metrics` | `POST /api/metrics-preview` (prepare only; commit is MCP-only) |
| [cross-team-dependencies](cross-team-dependencies/SKILL.md) | `dependency_radar`, `sprint_dependency_radar` | `/api/dependencies/{key}?linkType=…&lookBackDays=`, `/api/dependencies?linkType=…&lookBackDays=` |
| [delivery-risk](delivery-risk/SKILL.md) | `sprint_delivery_risk` | `/api/risk?committed=&available=` |

(Also: `GET /api/issue/{key}` for a single issue.)

## Anatomy of a skill

**`SKILL.md`** — frontmatter (`name`, `description`, `allowed-tools`) · what it is and that it is
read-only · **Invoke** (MCP tool + `./scrum` fallback) · **Parameters** with team defaults · **Report
like this** (how to summarize for a human) · any caveat the agent must state out loud · a link to
`reference.md`.

**`reference.md`** — the long form: **When to use** · **Prerequisites** · **Commands** with full curl
examples · **Parameters** · **Output** field-by-field · **How to interpret / act** · **Error
handling** · **Related skills**.

When adding a capability, write both. If you only write one, write `SKILL.md` — a capability with no
`SKILL.md` is one the agent will never find.

## Conventions

- Everything except the publish **commit** is read-only. Publishing is human-in-the-loop: prepare
  (safe) → human review → commit via the MCP write tool (write mode only).
- Prefer the team-standard parameter values in each skill; **ask the human** when a value is team- or
  sprint-specific — most importantly the **delivery-risk capacity points**, which have no default.
- Metrics and risk are computed in code/JQL, never estimated by the model — the same inputs always
  give the same numbers.
