# Scripts

Everything you need to (1) verify the server fits **your** Jira, (2) smoke-test it, and (3) move the
image to your corporate server.

## Prereqs
- `curl`, and `jq` for pretty-printed output (optional — scripts work without it)
- Docker Desktop (for the image scripts)
- A `.env` with `JIRA_BASE_URL`, `JIRA_TOKEN` (a personal access token), `JIRA_PROJECT_KEYS` (plural, comma-separated)

---

## 1. Verify Jira fit — `check-jira.sh`

Run this **first**, on a machine that can reach your Jira. It checks every assumption the server makes
and prints exactly what to put in `application.yml`.

```bash
./scripts/check-jira.sh                 # or: ./scripts/check-jira.sh PROJ-123  (a sample issue)
```

It reports:
| Section | Answers |
|---|---|
| 1. Custom fields | the real IDs for **story points / acceptance criteria / logged hours** on your instance |
| 2. Board | your Agile **board id** (velocity/sprints) |
| 3. Statuses | your status **names + categories** (for `release_queue` and WIP) |
| 4. Link types | your issue-link names (for `dependency_radar`, e.g. Blocks / Dependency) |
| 5. Active sprint | that `openSprints()` returns issues (hygiene/wip/worklog/risk need this) |
| 6. **Velocity** | whether the **unofficial greenhopper** endpoint works — **with a fallback** to the official Agile API if not |
| 7. **Logged work** | whether your logged hours are a **custom field** (what we read) or Jira's **native worklog** (needs a code change) |
| 8. Raw shapes | the JSON the metric/dependency code parses, so we can confirm field paths |

**If any check fails or returns something unexpected, paste the output back** and I'll adjust the config
or the code (e.g. switch velocity to the Agile API, or read native worklog).

---

## 2. Smoke-test our capabilities — `smoke.sh`

After the config matches your Jira, start the server and hit every read tool against real data:

```bash
./setup.sh                       # or ./scripts/docker.sh run
./scripts/smoke.sh PROJ-123      # runs hygiene, wip, worklog, velocity, release-queue, risk, deps, issue
```
Eyeball the numbers vs. Jira. Empty story-points/AC/logged-hours ⇒ custom-field IDs are wrong (§1).

---

## 3. Ship the image — `docker.sh`

Build **once** where the network works (deps can't be fetched inside Docker on locked-down networks),
then move the image. `docker.sh` wraps the whole lifecycle:

```bash
./scripts/docker.sh build                       # host jar -> slim image (~250 MB)

# --- move to corporate: pick ONE ---
# A) no registry (air-gapped):
./scripts/docker.sh save                        # -> scrum-mcp-server.tar.gz
scp scrum-mcp-server.tar.gz user@corp:/tmp/
ssh corp './scripts/docker.sh load'             # (with the repo, or: gunzip -c ... | docker load)

# B) corporate registry (preferred):
./scripts/docker.sh push registry.mycorp.com/scrum-mcp-server:1.0
#   ...then on the server:
./scripts/docker.sh pull registry.mycorp.com/scrum-mcp-server:1.0

# --- run it ---
./scripts/docker.sh run                         # HTTP server: /mcp + /api on PORT (default 8097)
PORT=9000 ./scripts/docker.sh run               # different port
./scripts/docker.sh stdio                       # one stdio MCP session (what .mcp.json uses)
./scripts/docker.sh logs                        # follow logs
./scripts/docker.sh stop
```

### Install the skills and the agent on the user's machine

The image carries the capability skills **and** the Scrum Master agent as a payload, so shipping it
moves **one** artifact. But a harness discovers both on the filesystem of the machine **it** runs
on — the user's laptop — not on the server. Anyone who got the image without cloning the repo must
extract them, or they get the MCP tools with no guidance on parameters, defaults, or how to report
results, and no dedicated mode to use them from.

The argument is a **base directory**; skills and agents go into `skills/` and `agents/` beneath it,
because a harness scans those separately.

```bash
./scripts/docker.sh install-skills              # -> ~/.copilot/{skills,agents}   (personal scope)
./scripts/docker.sh install-skills ~/repo/.github   # -> ~/repo/.github/{skills,agents}
```

Personal scope is usually right — it follows the user into whatever project they open. Restart the
harness afterwards, then pick **Scrum Master** from the agents dropdown.

| Harness | Reads |
|---|---|
| Copilot (VS Code, JetBrains, Visual Studio, Eclipse, CLI, cloud agent) | `~/.copilot/skills`, `<repo>/.github/skills` |
| Copilot custom agents (VS Code) | `~/.copilot/agents`, `<repo>/.github/agents` |
| Claude Code | `~/.claude/skills`, `<repo>/.claude/skills` |
| **Copilot Chat on github.com** | **neither** — needs `.github/copilot-instructions.md` committed to the repo |

Cloning the repo instead? Nothing to install — `.github/skills/` and `.github/agents/` are already
on disk.

### Corporate hardening (before real use)
- **TLS + auth**: `/api` and `/mcp` are unauthenticated. Put them behind your ingress/reverse proxy
  with HTTPS, and restrict `/api` (or add a Spring Security filter).
- **Secrets**: pass the PAT via `--env-file`/vault, never bake it into the image.
- **Network**: the server must reach Jira (and the greenhopper endpoint) from inside corporate.
- **Read-only stays on** (`scrum.governance.read-only=true`) until you deliberately enable writes.
