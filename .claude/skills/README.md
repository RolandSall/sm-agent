# Development Skills

How to **write and maintain** this server. For how a Scrum Master *uses* it against Jira, see the
capability skills in [`.github/skills/`](../../.github/skills/README.md) — different audience,
different lifecycle.

These live in `.claude/skills/` because the developer audience is on Claude Code. (Copilot reads
this path too, in VS Code and JetBrains — but the capability skills sit in `.github/skills/` because
that path additionally covers Visual Studio, Eclipse, and the cloud agent, which the Scrum Master
may be on.)

Repo-wide invariants and commands are in [`CLAUDE.md`](../../CLAUDE.md), loaded every session.

## Project skills

| Skill | Use it when | Auto-triggers on |
| --- | --- | --- |
| [scrum-architecture](scrum-architecture/SKILL.md) | Deciding where code goes — module table, request flow, the write gate, what's stubbed | `scrum-mcp-server/src/**` |
| [add-capability](add-capability/SKILL.md) | Adding a tool, metric, report, or check — the 7-step recipe | asked to add a capability |
| [jira-gateway](jira-gateway/SKILL.md) | Jira API calls, JQL, custom fields, changelog parsing, test stubs | `**/jira/**`, `src/test/**` |
| `cucumber-acceptance-test` | Writing acceptance tests — lives at `scrum-mcp-server/.claude/skills/`, discovered when working in that subtree | server subtree |

## General engineering practice

Not specific to this project, adapted to Java 21 / Gradle / Cucumber. Provenance and licences:
[ATTRIBUTION.md](ATTRIBUTION.md).

| Skill | Use it when |
| --- | --- |
| [test-driven-development](test-driven-development/SKILL.md) | Implementing any feature or bugfix — test first, watch it fail |
| [verification-before-completion](verification-before-completion/SKILL.md) | About to claim something is done, fixed, or passing |
| [systematic-debugging](systematic-debugging/SKILL.md) | Any bug, test failure, wrong metric, or unexpected Jira response |
| [java-code-review](java-code-review/SKILL.md) | Reviewing Java — concrete exception/resource/concurrency/Spring rules |

## Anatomy

Project skills are `SKILL.md` + optional `reference.md` (progressive disclosure — the reference
loads only when linked to). Frontmatter that matters:

- **`description`** decides whether the skill triggers. Lead with the action, pack in the phrases
  someone would actually use. Max 1024 chars; front-load keywords.
- **`paths`** limits automatic activation to matching files, keeping unrelated work uncluttered.

Adding a capability to the server? Follow [add-capability](add-capability/SKILL.md) — all seven
steps. The skill file is step 7 and is not optional: a capability without one is invisible to the
agent that's supposed to use it.

## Docs

[`docs/ARCHITECTURE.md`](../../docs/ARCHITECTURE.md) — layers, modules, request flow, known gaps.
[`docs/DESIGN.md`](../../docs/DESIGN.md) — product vision, responsibility inventory, roadmap.
