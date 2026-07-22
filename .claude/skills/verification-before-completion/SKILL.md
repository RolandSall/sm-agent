---
name: verification-before-completion
description: Run the verification command and read its output before claiming anything is done, fixed, passing, or working. Use before committing, opening a PR, reporting a task complete, or expressing satisfaction with a change.
---

# Verification Before Completion

Adapted from [obra/superpowers](https://github.com/obra/superpowers) (MIT, © Jesse Vincent). See [ATTRIBUTION.md](../ATTRIBUTION.md).

## Core principle

**Evidence before claims, always.** Claiming work is complete without verifying it isn't efficiency — it's a false report, and it costs more than the verification would have.

## The Iron Law

```
NO COMPLETION CLAIMS WITHOUT FRESH VERIFICATION EVIDENCE
```

If you haven't run the command in this message, you cannot say it passes.

## The gate

```
BEFORE claiming any status:
  1. IDENTIFY  which command proves this claim
  2. RUN       it fully and freshly
  3. READ      the full output — exit code, failure count
  4. VERIFY    does the output actually confirm the claim?
                 no  → state the real status, with the evidence
                 yes → state the claim, with the evidence
  5. THEN      make the claim
```

## What each claim actually requires

| Claim | Requires | Not sufficient |
|---|---|---|
| "Tests pass" | `./gradlew :scrum-mcp-server:build` → BUILD SUCCESSFUL | An earlier run; "should pass" |
| "It compiles" | A build that reached compilation | The IDE showing no red |
| "Module boundaries are fine" | `ModularityTests` green | "I only added one import" |
| "The bug is fixed" | The original symptom re-tested and gone | The code changed |
| "The MCP tool works" | Called it through an MCP client | The REST endpoint working |
| "It works against real Jira" | Ran `./scrum <cmd>` against a live instance | Acceptance tests green — they stub Jira |
| "The skill is discoverable" | `.claude/skills/<name>/SKILL.md` exists and frontmatter parses | Having written a markdown file somewhere |
| "The subagent did it" | Read the diff yourself | The agent reporting success |

## Two traps specific to this repo

**Green acceptance tests do not mean it works against Jira.** They stub Jira at the HTTP boundary, so they prove your parsing, not that your JQL matches anything, that a custom-field id is right, or that the endpoint exists on this instance. "Tests pass" and "works against Jira" are separate claims needing separate evidence.

**REST passing does not mean MCP passes.** Both dispatch the same query, so the *logic* is shared — but MCP registration, `@ToolParam` wiring, and the `WriteTool` gate are not exercised by the acceptance suite at all.

## Rationalizations

| Excuse | Reality |
|---|---|
| "Should work now" | Run it. |
| "I'm confident" | Confidence isn't evidence. |
| "It compiled, so it works" | Compiling is not behaving. |
| "The IDE shows no errors" | The IDE isn't the build. |
| "Just this once" | No exceptions. |
| "The subagent said it succeeded" | Verify independently. Read the diff. |
| "I'm tired" | Exhaustion isn't an exemption. |
| "Partial check is enough" | Partial proves the part you checked. |

## Red flags — stop

- "should", "probably", "seems to", "looks right"
- Satisfaction expressed before verification — "Perfect!", "Done!", "All set!"
- About to commit or push without a build
- Reporting a subagent's result you haven't checked
- Any wording implying success where no command was run

## Reporting honestly

State what you ran and what you saw. When something is unverified, say so plainly rather than hedging into implied success:

> "`./gradlew :scrum-mcp-server:build` → BUILD SUCCESSFUL, 14 scenarios passed. I have **not** run this against real Jira, so the JQL is unverified."

That is a more useful report than "done" — and if it later breaks, nobody was misled about what was checked.
