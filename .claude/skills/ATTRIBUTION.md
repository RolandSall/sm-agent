# Attribution

Some development skills here are adapted from third-party sources. All are MIT-licensed, and all
were **adapted rather than copied verbatim** — retargeted to Java 21 / Gradle / Spring Boot /
Cucumber and to this repo's specific failure modes.

| Skill | Source | Author | License |
|---|---|---|---|
| [test-driven-development](test-driven-development/SKILL.md) | [obra/superpowers](https://github.com/obra/superpowers) | Jesse Vincent | MIT |
| [verification-before-completion](verification-before-completion/SKILL.md) | [obra/superpowers](https://github.com/obra/superpowers) | Jesse Vincent | MIT |
| [systematic-debugging](systematic-debugging/SKILL.md) | [obra/superpowers](https://github.com/obra/superpowers) | Jesse Vincent | MIT |
| [java-code-review](java-code-review/SKILL.md) | [alirezarezvani/claude-skills](https://github.com/alirezarezvani/claude-skills) — `engineering-team/skills/code-reviewer/languages/java.md` | Alireza Rezvani | MIT |

Written from scratch for this repo, with no third-party source: `scrum-architecture`,
`add-capability`, `jira-gateway`, and all ten capability skills.

## What was borrowed

Mostly **structure**, which is where the value is. The strongest community skills encode
*anticipated failure modes* rather than prose — three techniques worth keeping as this set grows:

- **The Iron Law** — one unambiguous rule at the top, stated so that "violating the letter is
  violating the spirit".
- **The rationalization table** — name the excuses the reader will actually reach for, and answer
  each. This is what closes loopholes that prose leaves open.
- **The evidence gate** — a claim requires the command output that proves it, not confidence.
  "Tests pass" requires pasted output.

Rules were kept only where they survive contact with Java. The upstream TDD skill's TypeScript
examples were replaced with this repo's Gherkin/Gradle cycle; the upstream Java review rules were
extended with Spring and repo-specific checks (no `JsonNode` past the module boundary, no hardcoded
`customfield_*`, no logic in the presentation layer).

## Sources evaluated and rejected

Recorded so the evaluation isn't repeated:

- **`anthropics/skills`** — 17 skills, none covering engineering practice. Nothing applicable.
- **`ComposioHQ/awesome-claude-skills`** — 832 of 864 files are byte-identical vendor templates
  differing only by a product name, all promoting the maintainer's commercial MCP endpoint. Zero
  coverage of SOLID/DDD/CQRS/Java. Unmaintained.
- **`davila7/claude-code-templates`** — its TDD skill is byte-identical to `obra/superpowers`' with
  no attribution, and its architecture skill contains a vendor placement. Contaminated.
- **`wshobson/agents`** — its CQRS skill is Python/FastAPI. Across 175 skill files: 1 Java code
  block, 152 Python.
- **`VoltAgent/*`, `travisvn/*`, `hesreallyhim/*`** — link indexes hosting no skill files. Useful for
  browsing, nothing to vendor.

Nothing in the ecosystem covers **Spring Modulith, Java-idiom CQRS/mediator, or Java Cucumber
acceptance testing** — hence `scrum-architecture`, `add-capability`, and `jira-gateway` being
written from scratch.
