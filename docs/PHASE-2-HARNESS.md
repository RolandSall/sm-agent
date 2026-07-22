# Phase 2 — owning the harness

A sketch, not a plan of record. Phase 2 is when the agentic loop moves in-house (Spring AI
`ChatClient`) instead of being rented from Claude Code / Copilot. Nothing here is built yet.

The question this answers: **what happens to the skills when we write the LLM loop ourselves?**

Short version — they become the system prompt. From [DESIGN.md](DESIGN.md) §3:

> In phase 1 the .md files ride as injected context under the vendor's system prompt; in phase 2 the
> same text becomes the literal system prompt.

---

## What survives the swap

```
              PHASE 1                          PHASE 2
   ┌────────────────────────┐      ┌────────────────────────────┐
   │ Copilot / Claude Code  │ ───▶ │ Spring AI ChatClient       │  ◀── ONLY this changes
   ├────────────────────────┤      ├────────────────────────────┤
   │ .github/skills/*.md    │ ═══▶ │ system prompt (same text)  │
   ├────────────────────────┤      ├────────────────────────────┤
   │ MCP protocol           │  ✗   │ (gone — same JVM)          │
   ├────────────────────────┤      ├────────────────────────────┤
   │ @Tool beans            │ ═══▶ │ @Tool beans (unchanged)    │
   │ mediator → handlers    │ ═══▶ │ mediator → handlers        │
   │ JiraClient             │ ═══▶ │ JiraClient                 │
   └────────────────────────┘      └────────────────────────────┘
```

Layers 2 and 3 are permanent assets. Only layer 1 is rented.

## The MCP layer simply disappears

In phase 2 the agent runs **in the same JVM** as the tools, so there is no protocol to speak.
`ChatClient.Builder.defaultTools(Object...)` accepts a `ToolCallbackProvider` — which is exactly what
`McpToolConfig` already produces:

```java
@Bean
ToolCallbackProvider scrumTools(List<ReadTool> readTools, List<WriteTool> writeTools) { … }
```

So the existing bean plugs straight in:

```java
@Bean
ChatClient scrumAgent(ChatClient.Builder builder,
                      @Value("classpath:prompts/router.md") Resource systemPrompt,
                      ToolCallbackProvider scrumTools) {
    return builder
        .defaultSystem(systemPrompt)     // a markdown file IS the system prompt
        .defaultTools(scrumTools)        // the same beans MCP was exposing
        .build();
}
```

**Everything below the presentation layer is reused untouched** — the `ReadTool`/`WriteTool`
governance gate, the mediator, every handler, `JiraClient`. That reuse is the payoff for keeping the
presentation layer logic-free (invariant 2 in [CLAUDE.md](../CLAUDE.md)).

> API signatures verified against current Spring AI docs. This repo pins `1.1.8` — re-check
> `defaultSystem(Resource)` and `defaultTools(...)` against that version before building on them.

Phase 2 also needs a **model starter** (`spring-ai-starter-model-*`) and an API key. Today there is
deliberately none: an MCP server exposes tools, it does not call an LLM.

---

## The one real problem: progressive disclosure

Phase 1 gets this free from the harness. Phase 2 does not.

```
   PHASE 1 — the harness does it for you
   ┌──────────────────────────────────────────────────┐
   │ always in context:  10 descriptions   ~2.5 KB    │
   │ on match:           1 SKILL.md body              │
   │ on link:            that skill's reference.md    │
   └──────────────────────────────────────────────────┘

   PHASE 2 — naive approach
   ┌──────────────────────────────────────────────────┐
   │ defaultSystem(everything)             ~30 KB     │
   │   • paid on every single turn                    │
   │   • attention split across 10 unrelated topics   │
   │   • the model reads risk-scoring rules while     │
   │     answering a board-hygiene question           │
   └──────────────────────────────────────────────────┘
```

Do not concatenate the skills into the system prompt.

### Rebuild it with a router + a loader tool

```
   system prompt = router.md  (~3 KB, always on)
        │  "which capability answers this?"
        ▼
   model calls  load_skill("sprint-flow")     ← a tool, like any other
        │
        ▼
   SKILL.md body returned into the conversation
        │
        ▼
   model calls  sprint_flow(stuckDays=5)      ← the real capability
```

`load_skill` is an ordinary `@Tool` that reads from the skills directory. This reproduces the
harness's three-level disclosure with about thirty lines of code, and keeps the *same markdown
files* as the source of truth — no second copy to drift.

### The router already exists

**`.github/copilot-instructions.md` is the router.** It was written for Copilot Chat on github.com,
which supports no skills — the identical constraint phase 2 hits. It already carries the tool table,
the team-standard defaults, and the interpretation rules that stop confidently-wrong answers.

```
   .github/copilot-instructions.md
        ├─▶ today:    always-on doc for github.com Copilot Chat
        └─▶ phase 2:  defaultSystem(Resource) — the router
```

Keep it in sync with the skills and it does double duty.

---

## Notes for when this gets built

- **Prompt caching.** The router is stable across turns, so it caches well. Put the volatile part
  (the user's question, fetched data) after it, never before.
- **Governance still applies.** `scrum.governance.read-only` gates the handlers, not the transport,
  so an in-JVM agent is gated identically. The publish flow must stay prepare → human → commit; an
  autonomous loop makes the human-approval step *more* important, not less.
- **Evals.** DESIGN §3 calls for replaying recorded sprint data to regression-test risk and hygiene
  judgments. The acceptance-test fixtures (`JiraJson`/`JiraStubs`) are the obvious seed corpus.
- **Metrics stay in code.** Invariant 1 does not relax because we own the loop. The model interprets;
  it never computes.

## Open questions

- Framework: Spring AI vs Embabel (goal-oriented planning) — DESIGN §8 leaves this open.
- Does `load_skill` return the whole `SKILL.md`, or should `reference.md` be a separate call? The
  harness treats them as two levels; probably worth mirroring.
- Where does the loop run — `@Scheduled` job, Jira webhook, or Teams approval callback? Each implies
  a different conversation lifetime and a different caching story.
