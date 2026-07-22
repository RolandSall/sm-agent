# Scrum Master Agent

An AI agent that takes over the mechanical parts of the Scrum Master job — board hygiene,
dependency tracking, metrics, risk detection, capacity planning, ceremony prep — so the human
SM walks into every meeting with a perfect brief.

**Guiding principle: the agent prepares, humans facilitate.** SOS attendance, retro
facilitation, and dependency negotiation stay human. The agent's job is that the human never
has to assemble the data themselves.

**Environment:** Jira (boards, sprints, RISK issues) · Confluence (PI pages) · Microsoft Teams
(channels, capacity sheets, ceremonies) · SAFe-style cadence (PIs of ~2–3 months, each a group
of sprints S1, S2, S3…; weekly Scrum-of-Scrums across teams).

---

## 1. Responsibility inventory

### A. Dependency management (cross-team)
- Track dependencies with other teams via Jira issue links (`blocks` / `is blocked by`)
- Detect when a dependency is **delivered** and notify the waiting team
- Chase dependencies that are running late

### B. Board & process hygiene
- Every dev has a ticket assigned in the active sprint
- Tickets are assigned to the right person / in the right status
- Work is being logged (time tracking present and plausible)

### C. Risk & delivery forecasting
- Near sprint end: flag committed features that won't make it — raise early
- Weekly SOS: report progress; draft **RISK** issues in Jira when delivery is threatened

### D. Metrics
- Sprint velocity, burndown, cycle time, WIP, spillover rate, planned-vs-delivered %,
  logged-hours vs capacity
- **Rule: metrics are computed in code/JQL, never by the LLM.** The LLM only interprets
  ("velocity dropped 20%, correlates with 2 devs at half capacity").

### E. Capacity planning (per PI)
- Build the capacity sheet (devs × sprints), post to the Teams channel, chase devs to fill it
- Read it back and compute PI capacity in story points
  (`total dev-days × focus factor × SP-per-dev-day from velocity history`)
- Capacity data feeds C — risk assessment knows who is available when

### F. Ceremony operations
- Schedule sprint planning, dailies, demos, retros on the Teams calendar (once per PI)
- Prepare the demo presentation template pre-filled with what was actually delivered
- Prepare the retro doc pre-filled with sprint stats

## 2. Feasibility map

| Feature | Cadence | Automation level |
|---|---|---|
| Board hygiene report | Daily | 🟢 Fully agent-ownable, day one |
| Metrics & velocity | Per sprint | 🟢 Fully ownable (deterministic math + LLM narrative) |
| Dependency radar | Daily/weekly | 🟢 Detection auto; 🟡 chasing messages human-approved at first |
| Sprint risk assessment | 2×/week | 🟡 Agent drafts RISK issue, human approves |
| SOS briefing pack | Weekly | 🟢 Generation auto; attending stays human |
| Capacity sheet | Per PI | 🟡 Co-produced (agent generates/parses, devs fill, human uploads in phase 1) |
| Ceremony scheduling | Per PI | 🟡 Low value to automate; fine manual until phase 2 |
| Demo template + retro prep | Per sprint | 🟢 High value, easy |
| Weekly self-audit | Weekly | 🟢 Compare agent flags vs what actually happened — builds the trust case for autonomy |

**Killer feature:** the E×D×C connection. Cross-referencing the capacity sheet against
remaining estimates *daily* is something no human SM has time for:
*"PROJ-234 has 13 points remaining, Sara owns it but has 40% capacity in S3 due to leave — at risk."*

## 3. Architecture — three layers

```
┌─────────────────────────────────────────────┐
│ 1. HARNESS — the agentic loop               │  who runs think → act → observe
│    Phase 1: rented (Copilot / Claude Code)  │
│    Phase 2: owned (Spring AI / Embabel /    │
│             LangChain) + scheduler/webhooks │
├─────────────────────────────────────────────┤
│ 2. INSTRUCTIONS — the brain config          │  .md playbooks, team context,
│    (identical text in both phases)          │  format examples with gotchas
├─────────────────────────────────────────────┤
│ 3. TOOLS — hands into the world             │  Atlassian MCP (Jira+Confluence),
│    (harness-agnostic, reused across phases) │  Teams webhook, Graph API, scripts
└─────────────────────────────────────────────┘
```

Layers 2 and 3 are permanent assets; only layer 1 is swapped between phases.
In phase 1 the .md files ride as injected context under the vendor's system prompt;
in phase 2 the same text becomes the literal system prompt.

### Phase 1 — rented harness (prove the playbooks)
- Playbooks run interactively in Copilot / Claude Code by the SM
- Jira/Confluence in via **Atlassian Remote MCP server**
- Teams out via **Workflows (Power Automate) webhook** — send-only, no token, URL kept secret
  (the old O365 Incoming Webhook connectors are retired; use the Workflows replacement)
- All writes (tickets, chase messages) drafted by the agent, approved by a human
- Goal: refine prompts, discover edge cases, collect wins to justify phase 2

### Phase 2 — owned harness (autonomous service)
- Small JVM service (Spring AI or Embabel preferred over LangChain — we're a Spring shop;
  "LangChain" in manager-speak means "a real framework with our own API keys")
- `@Scheduled` runs (daily hygiene 8am), Jira webhook listeners (react to transitions)
- Approval flow: agent drafts → posts to Teams → one-click approve → then writes to Jira
- **Entra ID app registration** (one-time, admin consent) unlocks Microsoft Graph:
  file upload to channel (SharePoint under the hood), live Excel readback via the
  workbook API, and any Teams MCP server (official: Work IQ Teams MCP, in preview)
- Evals: replay recorded sprint data to regression-test risk/hygiene judgments

## 4. Capacity sheet design

The workflow today: PI sprint dates live on a Confluence page → SM builds an Excel
(devs × S1/S2/S3, values in person-days) → uploads to the Teams channel → devs fill it →
SM derives PI capacity (e.g. ~120 SP).

Agent pipeline:
1. **Read** PI sprint dates from Confluence (Atlassian MCP)
2. **Generate** the .xlsx via script (openpyxl) by filling the team's own template —
   pre-fill public holidays in the sprint ranges
3. **Upload** — phase 1: human drags into Teams (10 s); phase 2: Graph API
   (or interim: extend the Workflows flow with a SharePoint "Create file" action — tokenless)
4. **Chase** — daily nudge listing devs with empty cells, via webhook
5. **Read back & compute** — phase 1: parse the downloaded sheet by script;
   phase 2: Graph Excel workbook API reads the live sheet in place

### Don't standardize the sheets — standardize the data

```
Team A's xlsx ──┐                                   ┌── sprint-risk playbook
Team B's xlsx ──┤→ (parse) → CANONICAL CAPACITY DATA ├── SOS brief
Team C's xlsx ──┘   { dev, sprint, days_available,  └── velocity/forecast
                      absences, focus_factor }
```

- Every downstream consumer reads only the canonical form (`data/capacity-<PI>.md` / JSON —
  the machine-readable twin, refreshed after each readback). The Excel is a human interface.
- Per-team format knowledge lives in a `capacity-format.md`: an **annotated markdown example**
  of that team's sheet (layout rules + gotchas like "empty cell = not filled yet, chase;
  0 = absent, don't chase"). LLMs can't read .xlsx directly — the markdown twin is what the
  model reads; the template .xlsx is what generation scripts fill.
- The LLM is the adapter engine; each team's example .md is the adapter. Onboarding team B =
  they paste their example sheet with their gotchas, same playbooks work.
- Multi-team payoff: once all sheets parse to one schema, the agent can produce the
  cross-team PI capacity view nobody has today.

## 5. Teams integration tiers

| Tier | Capability | Auth |
|---|---|---|
| Workflows webhook | Post messages/Adaptive Cards to a channel | None (secret URL) |
| Extended Workflows flow | + create files in the channel's SharePoint | None (runs as SM's account) |
| Microsoft Graph API | File upload, live Excel readback, calendar events | Entra app registration + admin consent |
| Teams MCP server | Two-way: read/send/reply in chats & channels | Same Entra/Graph credentials |

Get a token only when two-way or file automation is actually needed (phase 2).

## 6. Planned repository structure

```
scrum-agent/
├── README.md                        # this document
├── instructions/
│   └── team-context.md              # team members, sprint cadence, PI dates, DoD/DoR,
│                                    # WIP rules, Jira project keys, focus factor
├── playbooks/
│   ├── daily-hygiene.md             # board checks → Teams webhook
│   ├── dependency-radar.md          # link scan → delivered/late report
│   ├── sprint-risk.md               # capacity × remaining work → risk flags, draft RISK issue
│   ├── sos-brief.md                 # weekly one-pager for the Scrum-of-Scrums
│   ├── sprint-metrics.md            # velocity etc. (script computes, LLM narrates)
│   ├── capacity-sheet.md            # generate / chase / parse / compute (see §4)
│   ├── demo-retro-prep.md           # templates pre-filled from delivered tickets
│   └── self-audit.md                # weekly: agent flags vs reality
├── templates/
│   ├── capacity-template.xlsx       # real file, filled by scripts
│   └── capacity-format.md           # annotated markdown twin, read by the LLM
├── schemas/
│   └── capacity.md                  # canonical capacity data schema (one page)
├── data/                            # machine-readable twins (capacity-PI3.md, metrics history)
└── scripts/                         # deterministic pieces: xlsx generator/parser, JQL metrics
```

## 7. Roadmap

1. **Now** — write `team-context.md` + the two zero-risk playbooks (`daily-hygiene`,
   `sprint-metrics`); wire Atlassian MCP + Teams Workflows webhook; demo to manager
2. **This PI** — capacity-sheet loop with human upload/download; dependency radar; SOS brief
3. **Next PI** — request Entra app registration; automate file upload + live readback;
   sprint-risk with drafted RISK issues
4. **Phase 2** — Spring AI / Embabel service: scheduler, Jira webhooks, Teams approval flow,
   evals against recorded sprints; same playbooks, same MCP tools

## 8. Open questions

- Scope: this team only, or a product for all SMs in the SOS? (Canonical schema is written
  down from day one either way — adding a team must be an adapter, not a rewrite)
- Which Jira fields encode cross-team dependencies here — issue links, or dedicated
  dependency tickets? (Determines the dependency-radar queries)
- Where does the SP-per-dev-day ratio come from initially, before we have velocity history?
- IT lead time for the Entra app registration — start the conversation early
- Framework bake-off for phase 2: Spring AI vs Embabel (goal-oriented planning) vs LangChain4j
