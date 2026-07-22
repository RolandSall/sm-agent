---
name: cross-team-dependencies
description: >
  Returns EVERY cross-team Dependency issue on our Epics — raw, unfiltered — plus our team name.
  Dependency-type issues linked to our Epics name the team we wait on (Depend On) and the team
  waiting (Dependent/s). YOU keep the open ones where Dependent/s == ourTeam and Depend On != ourTeam.
---

# Cross-Team Dependencies Skill

Answer "what are we blocked on, which team owns it, and when will it clear?". **Read-only.** The tool
returns raw ingredients; **you filter and reason**.

On this instance a cross-team dependency is a dedicated **`Dependency`-type issue** linked to an
**Epic** (via Epic Link `customfield_12471`) — **not** a Jira issue link. Each Dependency names:

- **`Depend On`** (`customfield_27471`, single-select) — the team we **wait on** → `blockingTeam`.
- **`Dependent/s`** (`customfield_27470`, single-select) — the team **waiting** → `waitingTeam`.
- a **delivery sprint** (`customfield_14960`, a Jira Sprint field) — the target for when it clears.

The radar scopes Dependencies to the Epics of the issue/sprint and returns **every** one of them,
unfiltered (open and resolved, both directions), plus `ourTeam`. **A blocker WE face = an OPEN
Dependency where `Dependent/s == ourTeam` and `Depend On != ourTeam`** — you apply that rule (see the
three steps in [SKILL.md](SKILL.md)); the code no longer does.

## When to use
- Before/at sprint planning, to surface cross-team blockers early.
- When a story is stuck and you need to know which team it waits on and their delivery target.

## Prerequisites
- **Mode A (MCP):** tools `dependency_radar`, `sprint_dependency_radar`.
- **Mode B (REST):** run `./scrum <cmd>` against the running server — see [README](../README.md).
- **Config (Tier 1):** `scrum.jira.team-name` (RIO), `scrum.jira.dependency-issue-type` (Dependency),
  and the `depend-on` / `dependents` / `delivery-sprint` / `epic-link` field ids.

## Commands

### One issue's dependencies (via the issue's Epic)
- **MCP tool:** `dependency_radar(issueKey)`.
- **REST (when MCP is disabled):**
  ```bash
  ./scrum deps VAL-123
  # or raw curl (dynamic port from .scrum-env):
  source .scrum-env && curl "$SCRUM_API_URL/api/dependencies/VAL-123"
  ```

### The whole active sprint's dependency radar
- **MCP tool:** `sprint_dependency_radar()`.
- **REST (when MCP is disabled):**
  ```bash
  ./scrum sprint-deps
  source .scrum-env && curl "$SCRUM_API_URL/api/dependencies"
  ```

## Parameters
None. The tool takes no resolved-status parameter — it returns the raw rows and `ourTeam`, and you
apply the resolved-status / direction rule yourself (adaptable per conversation, no redeploy).

## Output
Returns a `DependencyReport` with:
- `rootIssue` — the issue key, or `"active-sprint"`
- `ourTeam` — our configured team identity (e.g. `RIO`); compare against it to decide direction
- `dependencies[]` — **every** Dependency on the scope's Epics, RAW (nothing dropped), each a
  `TeamDependency`:
  - `key`, `summary`, `status`, `statusCategory`
  - `blockingTeam` — the team we wait on (`Depend On`)
  - `waitingTeam` — the team waiting (`Dependent/s`)
  - `epicKey` — the Epic the Dependency is linked to
  - `deliverySprint` — target sprint name (best-effort, may be `null`)
  - `deliveryEnd` — target sprint end instant, the ETA (best-effort, may be `null`)

Resolved rows and reverse-direction rows (`Depend On == ourTeam`) are **included**; you filter them out
in Step 1.

## How to interpret / act
Apply Step 1 from [SKILL.md](SKILL.md): keep open Dependencies where `waitingTeam == ourTeam` and
`blockingTeam != ourTeam`. Read out the team we wait on, the Dependency status, and its delivery-sprint
ETA ("waiting on ROMA for VAL-1, status Open, ETA their RIO Sprint 5"). Those need a conversation
before the sprint stalls; feed open ones into [delivery-risk](../delivery-risk/reference.md).

## Error handling
- No Dependencies on the Epics (or the issue has no Epic) → empty `dependencies[]`; report "no
  cross-team dependencies".
- After you apply Step 1, if nothing survives → report "no open cross-team dependencies we're waiting
  on".
- `deliverySprint` / `deliveryEnd` are parsed best-effort from Jira's Sprint field and may be `null` —
  say "no delivery sprint recorded" rather than inventing one.
- **Ask the human** for the team's real resolved-status set if the defaults don't match the board.

## Related skills
[delivery-risk](../delivery-risk/reference.md) · [sprint-metrics](../sprint-metrics/reference.md) · [README](../README.md)
