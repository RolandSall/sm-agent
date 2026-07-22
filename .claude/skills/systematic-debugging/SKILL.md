---
name: systematic-debugging
description: Find the root cause before proposing any fix. Use when hitting a bug, test failure, wrong metric, unexpected Jira response, or build failure in this repo — especially when a quick fix looks obvious or a previous fix didn't work.
---

# Systematic Debugging

Adapted from [obra/superpowers](https://github.com/obra/superpowers) (MIT, © Jesse Vincent). See [ATTRIBUTION.md](../ATTRIBUTION.md).

## Core principle

**Find the root cause before attempting a fix.** A fix aimed at a symptom either fails or hides the real defect until it resurfaces somewhere worse.

## The Iron Law

```
NO FIXES WITHOUT ROOT CAUSE INVESTIGATION FIRST
```

Use this *especially* under time pressure — that's when guessing feels justified and costs the most.

## Phase 1 — investigate

**Read the error completely.** Full stack trace, line numbers, exit codes. Java stack traces bury the real cause several `Caused by:` levels down — read to the bottom.

**Reproduce it consistently.** Exact steps. Every time, or intermittently? Not reproducible means gather more data, not guess harder.

**Check what changed.** `git diff`, recent commits, a changed custom-field id, a Jira instance change, a new `application.yml` value.

## Phase 2 — locate the failing layer

This system has a long chain, and a wrong number can enter at any point:

```
harness → MCP tool / REST endpoint → mediator → handler → JiraGateway → JiraClient → Jira
```

**Don't guess which layer.** Instrument the boundaries once and let the evidence tell you:

| Layer | Question | How to check |
|---|---|---|
| Config | Are the Tier-1 values actually loaded? | `JIRA_PROJECT_KEYS` set? Board id? Is `.env` present? |
| Jira | Does Jira return what you think? | `./scripts/check-jira.sh`, or curl the raw endpoint |
| `JiraClient` | Is parsing right? | `DEBUG` logging on `io.github.scrumagent`; check field paths |
| Handler | Is the computation right? | Acceptance test with a known fixture |
| Transport | Do MCP and REST agree? | Same query both ways — a difference means logic leaked into presentation |

**A very common root cause here:** the data is genuinely absent in Jira. Zero logged hours, a null estimate, an empty changelog, no active sprint. That is **not a bug** — the capability is correctly reporting sparse data. Confirm against real Jira before "fixing" anything.

**Second most common:** the wrong sprint. A board can have several active sprints, so a plausible-but-wrong result often means `activeSprint()` picked a different one than you assumed.

## Phase 3 — trace to the source

When a bad value appears deep in a handler, trace *backwards*: where did it originate, what passed it in, and keep going up until you reach the source. Fix it there, not where it surfaced.

Fixing at the symptom in this codebase usually looks like special-casing a null in a handler that should never have received one — which then hides the real parsing bug in `JiraClient` from every other capability.

## Phase 4 — fix, with a test

Per [test-driven-development](../test-driven-development/SKILL.md): write the failing test that reproduces the bug **first**. It proves the fix and prevents the regression. Never fix a bug without one.

## Rationalizations

| Excuse | Reality |
|---|---|
| "I'll just try this one thing" | That's guessing. Guesses that happen to work hide the real cause. |
| "It's probably the custom-field id" | Probably isn't evidence. Check it. |
| "Adding a null check will make it go away" | Going away and being fixed are different things. |
| "It's flaky, re-run it" | Flaky tests have root causes too — often shared state or a stub not reset. |
| "No time to investigate" | Systematic is faster than three failed guesses. |
| "The test is wrong" | Sometimes true. Establish it, don't assume it. |

## Red flags — stop

- Changing code before you can explain the failure
- Second fix attempt on the same bug
- "Should be fixed now" with nothing re-run
- Adding a null guard without knowing where the null came from
- Concluding "Jira is weird" without checking the raw response

## Before you call it fixed

- [ ] Can explain the root cause in one sentence
- [ ] A test reproduces the original symptom and now passes
- [ ] Confirmed it isn't simply sparse Jira data behaving correctly
- [ ] Fixed at the source, not the surface
- [ ] `./gradlew :scrum-mcp-server:build` green — output read

See [verification-before-completion](../verification-before-completion/SKILL.md) before reporting.
