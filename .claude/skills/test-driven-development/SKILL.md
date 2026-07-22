---
name: test-driven-development
description: Write the failing test first, watch it fail, then write minimal code to pass. Use when implementing any feature, bugfix, or behavior change in this repo, before writing implementation code. Covers the JUnit and Cucumber acceptance-test cycle here.
---

# Test-Driven Development

Adapted for Java/Gradle/Cucumber from [obra/superpowers](https://github.com/obra/superpowers) (MIT, © Jesse Vincent). See [ATTRIBUTION.md](../ATTRIBUTION.md).

## Core principle

**If you didn't watch the test fail, you don't know if it tests the right thing.**

## The Iron Law

```
NO PRODUCTION CODE WITHOUT A FAILING TEST FIRST
```

Wrote code before the test? Delete it and start over from the test. Not "keep it as reference", not "adapt it while writing tests" — you will adapt it, and that is testing after. Delete means delete.

**Exceptions** (ask first): throwaway spikes, generated code, config files.

## Red → Green → Refactor

### RED — write the failing test

In this repo that usually means a **Gherkin scenario first**, because acceptance tests are the primary safety net:

```gherkin
Scenario: An in-progress issue untouched for longer than the threshold is STUCK
  Given the active sprint contains issue "VAL-88" in status "In Review" since 9 days ago
  When the agent requests the sprint flow view with a stuck threshold of 5 days
  Then the system reports "VAL-88" as stuck
```

One behavior. A name that describes the behavior, not the mechanism. If the name needs "and", split it.

Per the `cucumber-acceptance-test` skill: write the `.feature` file first and get it approved **before** any Java.

### Verify RED — watch it fail. Mandatory.

```bash
./gradlew :scrum-mcp-server:test --tests '*RunCucumberTest*'
```

Confirm three things:
- it **fails**, rather than erroring
- the failure message is the one you expected
- it fails because the behavior is missing — not because of a typo, a missing stub, or a wiring error

**Test passed immediately?** You're testing behavior that already exists. Fix the test.
**Test errored?** Fix the error and re-run until it fails *correctly*.

This step is where stubbed-Jira tests bite: a missing `JiraStubs` registration produces a confusing error, not a clean failure. Get to a clean red before writing any handler code.

### GREEN — minimal code

Simplest thing that passes. No extra parameters "while I'm here", no speculative generality. If the handler needs one threshold, give it one threshold.

### Verify GREEN

```bash
./gradlew :scrum-mcp-server:build     # the new test, all other tests, and ModularityTests
```

Test still failing? Fix the code, not the test. Other tests broke? Fix now, not later.

### REFACTOR

Only once green. Remove duplication, improve names, extract helpers. Don't add behavior.

## Why order matters

Tests written after code **pass immediately**, and passing immediately proves nothing. They might test the wrong thing, test the implementation rather than the behavior, or miss the edge case you forgot — and you never saw the test catch anything.

Tests-after answer *"what does this do?"*. Tests-first answer *"what should this do?"*.

## Common rationalizations

| Excuse | Reality |
|---|---|
| "I'll write the test after" | Tests passing immediately prove nothing. |
| "Too simple to test" | Simple code breaks. The test takes a minute. |
| "Already manually tested it with `./scrum`" | Ad-hoc ≠ systematic. No record, can't re-run, forgotten under pressure. |
| "Deleting hours of work is wasteful" | Sunk cost. Keeping code you can't trust is the actual waste. |
| "Keep it as reference and write tests first" | You'll adapt it. That's testing after. |
| "The Cucumber setup is heavy for this" | Every capability has one. Copy the nearest `steps/` package. |
| "It's read-only, low risk" | It can still report a wrong number to someone who acts on it. |
| "Test is hard to write" | Listen to that. Hard to test = hard to use. Usually the design. |
| "TDD will slow me down" | Slower than debugging a wrong metric in front of a Scrum Master? |

## Red flags — stop and start over

- Code written before the test
- A test that passed the first time you ran it
- Can't explain *why* the test failed
- "I'll add tests later"
- "This one is different because…"

## Checklist

- [ ] Feature file / test written first and approved
- [ ] Watched it fail
- [ ] It failed for the *expected reason*, not a wiring error
- [ ] Minimal code to pass
- [ ] `./gradlew :scrum-mcp-server:build` green — output read, not assumed
- [ ] Edge cases covered: empty sprint, missing estimate, no active sprint, absent changelog

Missing a box means you skipped TDD.

## Bug fixes

Never fix a bug without a test. Write the failing test that reproduces it first — that test proves the fix *and* prevents the regression. See [systematic-debugging](../systematic-debugging/SKILL.md) for finding the root cause before you get there.
