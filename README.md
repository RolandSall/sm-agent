# Scrum Agent — MCP server

A Scrum-Master assistant exposed as an **MCP tool server** over your on-prem Jira. It computes the
things Jira *can't* hand you (cross-team delivery risk, dependency radar, WIP-vs-your-limits, board
hygiene, logged-work rollups, per-feature effort) and surfaces Jira's own numbers where it already
has them (velocity). An LLM harness — GitHub Copilot or Claude Code — reasons over the results.

**Metrics are computed in code, never estimated by the model.** Same inputs always give the same
numbers; the model only interprets them.

> Design rationale: [`docs/DESIGN.md`](docs/DESIGN.md) · Architecture:
> [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) · Capability catalog:
> [`.github/skills/README.md`](.github/skills/README.md)

---

## Prerequisites

| Need | Why | Notes |
|---|---|---|
| **JDK 21** | The jar is built on the host, not in Docker | Any distribution — Temurin, Zulu, Corretto, Microsoft, or your OS package manager |
| **Docker** + Compose v2 | Packaging and running the image | Only for the Docker path; skip it if you run locally |
| `curl` | The `./scrum` REST wrapper | Present on most systems |
| `jq` *(optional)* | Pretty-prints `./scrum` output | Works without it, just unformatted |
| **A Jira personal access token** | All API calls use Bearer auth | Jira Server/Data Center |

Java is built on the host deliberately: in-container Gradle downloads are slow or blocked entirely on
many corporate Docker networks, so the image only ever packages a ready-made jar.

If Gradle can't find your JDK 21, point it at one explicitly — either set `JAVA_HOME`, or add the
path to `gradle.properties`:

```properties
org.gradle.java.installations.paths=/path/to/your/jdk-21
```

---

## Option A — Docker (recommended)

```bash
git clone <this repo> && cd scrum-agent

./setup.sh          # first run creates .env from the template, then stops
#                   → fill in .env (see Configuration below)
./setup.sh          # builds the jar + image, wires MCP, starts the REST container
```

`setup.sh` does three things:

1. builds the jar with host Gradle and packages it into a slim JRE image (~250 MB),
2. writes **`.mcp.json`** so a harness can launch the image over **stdio** — no port needed,
3. starts a **REST container on a free port**, recording it in `.scrum-env` for the `./scrum` wrapper.

Day-to-day — all from the **repo root**, where `docker-compose.yml` lives:

```bash
docker compose ps               # is it up, and on which host port?
docker compose logs -f scrum    # follow the logs
docker compose down             # stop it
./setup.sh                      # rebuild after code changes
```

### Running compose directly

`setup.sh` wraps this, but you can drive it yourself:

```bash
cd /path/to/scrum-agent         # must be the repo root — compose reads ./docker-compose.yml and ./.env

docker compose up -d            # start (fails immediately if .env is missing)
docker compose logs scrum | grep "Started ScrumAgentApplication"

# the host port is allocated dynamically; ask Docker which one, then record it for ./scrum
PORT="$(docker compose port scrum 8097 | sed 's/.*://')"
echo "SCRUM_API_URL=http://localhost:${PORT}" > .scrum-env

./scrum hygiene
```

`.env` must exist first — `docker compose` refuses to start without it (`env file … not found`).
Copy `.env.example` to `.env` and fill it in, or let `./setup.sh` do it on its first run.

Shipping the image to another machine (air-gapped or via a registry) is covered in
[`scripts/README.md`](scripts/README.md) — including `docker.sh install-skills`, which installs the
capability skills and the Scrum Master agent onto a user's machine when they don't have this repo.

---

## Option B — run locally, no Docker

```bash
export JIRA_BASE_URL=https://jira.example.com
export JIRA_TOKEN=your-personal-access-token
export JIRA_PROJECT_KEYS=PROJ,PLTF
export JIRA_BOARD_ID=1234
export JIRA_SPRINT_FILTER=            # optional

./gradlew :scrum-mcp-server:bootRun   # serves /mcp and /api on port 8097
```

Then either point a harness at `http://localhost:8097/mcp`, or use the wrapper:

```bash
./scrum hygiene
./scrum help
```

Build and test:

```bash
./gradlew :scrum-mcp-server:build     # compile + acceptance tests + module-boundary checks
```

---

## Configuration

Two tiers, deliberately separated.

### Tier 1 — instance identity and secrets

**`.env`** (git-ignored; copy from `.env.example`). Consumed by Docker via `--env-file`, or exported
into your shell when running locally.

| Variable | Required | What it is |
|---|---|---|
| `JIRA_BASE_URL` | **yes** | Jira base URL, no trailing slash — e.g. `https://jira.example.com` |
| `JIRA_TOKEN` | **yes** | Personal access token, sent as `Authorization: Bearer`. Never commit it. |
| `JIRA_PROJECT_KEYS` | **yes** | Comma-separated project keys your team owns, e.g. `PROJ,PLTF`. Everything outside this set counts as *another team* — this is what makes cross-team dependency and risk detection work. |
| `JIRA_BOARD_ID` | recommended | The Agile board (rapidView) id. Needed for velocity and sprint resolution; falls back to resolving from the project key if unset. |
| `JIRA_SPRINT_FILTER` | optional | Substring picking your team's sprint when a board has several active at once (PI overlaps, parallel teams). Leave blank if only one is ever active. |

Don't know your board id or field ids? `./scripts/check-jira.sh` is read-only and reports them.

### Tier 1 (continued) — per-instance field mappings

**`scrum-mcp-server/src/main/resources/application.yml`.** Custom-field ids differ on every Jira
instance, so **these must be checked before first real use** — the committed values will not match
your instance:

```yaml
scrum:
  governance:
    read-only: true                  # the one write tool isn't even registered while true
  jira:
    base-url: ${JIRA_BASE_URL}
    token: ${JIRA_TOKEN}
    project-keys: ${JIRA_PROJECT_KEYS}
    board-id: ${JIRA_BOARD_ID:}
    sprint-name-filter: ${JIRA_SPRINT_FILTER:}
    feature-issue-type: Epic         # the issue type that represents a "feature"
    fields:
      acceptance-criteria:           # a LIST — first match wins, since instances differ
        - customfield_XXXXX
        - customfield_YYYYY
      story-points: customfield_XXXXX
      epic-link: customfield_XXXXX   # how a story points at its parent Epic
      dev-job: customfield_XXXXX     # the Epic's own planned-effort snapshot
      # logged-hours: customfield_XXXXX
```

| Setting | Notes |
|---|---|
| `feature-issue-type` | `Epic` on most instances; change if yours has a real `Feature` type |
| `acceptance-criteria` | A list of candidate ids, first match wins |
| `story-points` | Required for hygiene, risk, and effort roll-ups |
| `epic-link`, `dev-job` | Needed only for the per-feature effort roll-up |
| `logged-hours` | **Leave unset on most instances** — the server then reads Jira's native worklog / timetracking, which is usually where logged time lives |

### Tier 2 — team policy, *not* config

WIP limits, release status names, risk thresholds, dependency link types, capacity numbers — these
are **passed as tool parameters at call time**, never stored server-side. They change per sprint or
per conversation, and storing them would mean a redeploy to answer "what if the limit were 2?".
Team-standard values live in each capability's skill file.

### Profiles

| Profile | Where | Purpose |
|---|---|---|
| *(default)* | `src/main/resources/application.yml` | Real Jira. Read-only. Port 8097. This is the one you deploy. |
| `local` | `src/test/resources/application-local.yml` | A development-only harness against a mock Jira. Sets `read-only: false`. See [`development/README.md`](development/README.md). |

---

## Using it from a harness

### MCP (primary)

Restart your harness in this folder. It reads `.mcp.json`, launches the image over stdio, and the
tools appear. In Claude Code, `/mcp` lists them; in VS Code, pick the **Scrum Master** agent from the
agents dropdown. Then just ask — *"prep standup"*, *"are we going to make it?"*, *"is the board
clean?"*

### REST via `./scrum` (fallback — when MCP is disabled by org policy)

A thin curl wrapper over the same REST API. No hardcoded port: it reads `.scrum-env` under Docker,
else defaults to `8097`.

```bash
./scrum hygiene
./scrum sprint-flow --stuck-days 5
./scrum wip --per-assignee 3 --team 15
./scrum release-queue "Ready for Release" "In UAT"
./scrum risk --committed 40 --available 35
./scrum help                       # full list
```

`SCRUM_API_URL` overrides the host. `SCRUM_PRINT=1 ./scrum <cmd>` prints the request instead of
sending it.

Both paths dispatch the **same query objects** through the same computation core, so results are
identical — only the transport differs.

### What the harness reads

| Path | For | Loaded by |
|---|---|---|
| `.github/skills/` | Per-capability guidance — parameters, output fields, how to report | Copilot and Claude Code, automatically, on demand |
| `.github/agents/scrum-master.agent.md` | A dedicated Scrum Master mode with the tools pre-scoped | VS Code Copilot agents dropdown |
| `.github/copilot-instructions.md` | Always-on summary for surfaces without skill support (Copilot Chat on github.com) | Copilot |
| `.claude/skills/` | **Developer** skills — how to build and maintain this server | Claude Code |

---

## Safety

- **Read-only by default** (`scrum.governance.read-only=true`). The single write tool
  (`commit_publish_metrics`) isn't registered at all while true, and the handler re-checks the flag
  so REST and CLI callers are gated identically.
- Publishing is **prepare → human reviews the rendered content → commit with a token** that expires
  in ~15 minutes. Never a one-shot write.
- Your **PAT lives in `.env`**, which is git-ignored. `.mcp.json` and `.scrum-env` are generated
  locally and git-ignored too.
- `/api` and `/mcp` are **unauthenticated**. Before any real deployment, put them behind TLS and
  authentication — see the hardening notes in [`scripts/README.md`](scripts/README.md).

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Server won't start | `.env` missing, or `JIRA_PROJECT_KEYS` unset — it's required and **plural** |
| 401 / 403 from Jira | PAT expired or lacks permission |
| Empty story points / acceptance criteria | Custom-field ids in `application.yml` don't match your instance — run `./scripts/check-jira.sh` |
| Wrong sprint's data | A board with several active sprints — set `JIRA_SPRINT_FILTER` |
| Velocity is empty | The velocity endpoint is unofficial and may not exist on your instance |
| All logged hours are 0 | Usually means the team isn't logging work, not that the tool is broken |
| Tools appear but the agent has no guidance | Skills weren't installed — `./scripts/docker.sh install-skills` |
