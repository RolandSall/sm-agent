---
name: jira-gateway
description: Conventions for the jira module of scrum-mcp-server — adding a JiraGateway method, parsing Jira Server/DC wire format in JiraClient, curated records, changelog handling, and stubbing Jira in acceptance tests with JiraStubs/JiraJson. Use when touching Jira API calls, JQL, custom fields, or Jira test stubs.
paths: scrum-mcp-server/src/**/jira/**,scrum-mcp-server/src/test/**
---

# The jira module

`JiraClient` is **the single place that knows Jira's wire format**. Everything it returns is already curated into records in `io.github.scrumagent.jira`, so no raw payload ever crosses the module boundary. Keep it that way.

Target: **Jira Server/DC**, Bearer PAT auth, `/rest/api/2` + `/rest/agile/1.0`.

## Adding a gateway method

**1. Declare on `JiraGateway`** with a Javadoc line saying what it returns in domain terms, not HTTP terms:

```java
/** Active-sprint issues with changelog-derived flow signals (age-in-status, staleness, reopens). */
List<SprintIssueFlow> activeSprintFlow();
```

**2. Implement in `JiraClient`.** Use the injected `RestClient` (base URL, Bearer header, and Accept are already configured in the constructor — never build a new client):

```java
JsonNode json = rest.get()
        .uri(uri -> uri.path("/rest/api/2/issue/{key}")
                .queryParam("expand", "renderedFields")
                .build(key))
        .retrieve()
        .body(JsonNode.class);
return toIssue(json, true);
```

**3. Return a curated record.** `JsonNode` must not escape the method. If the shape is new, add a record to `io.github.scrumagent.jira`.

## Parsing rules

- **Absence is normal, not an error.** Jira omits absent fields entirely. Use `path(...)` (never `get(...)`, which NPEs) and `.asText(null)`. An issue with no estimate, no assignee, or no changelog entries is valid data — return null/0 and let the capability handler decide what it means.
- **Custom fields are per-instance and configured, never hardcoded.** Story points and acceptance criteria come from `JiraProperties`. Acceptance criteria is a **list** of candidate ids — first match wins — because instances differ. Never inline a `customfield_NNNNN` literal.
- **Timestamps:** Jira's changelog format is `2026-07-12T09:30:00.000+0000` — offset **without** a colon, so it needs the explicit `JIRA_DATETIME` formatter, not `Instant.parse`.
- **Request only the fields you read.** Extend `BASE_SEARCH_FIELDS` rather than fetching everything; search responses get large fast.

## Two Jira-specific traps

**A board can have several active sprints** (PREP sprints, parallel teams, PI overlaps). `activeSprint()` narrows by the configured name substring, *then* by "today falls in the date range", then falls back to the first candidate. Any new sprint-scoped call must go through `activeSprint()` — never assume `sprints("active")` returns exactly one.

**The greenhopper velocity endpoint is unofficial** and may 404 on a given instance. `scripts/check-jira.sh` probes it. Treat any greenhopper call as failure-prone and keep the Agile-API fallback path working.

## Stubbing Jira in acceptance tests

Two helpers, both public because step definitions live in sub-packages:

- **`JiraJson`** — builds minimal, *valid* Jira wire-format JSON. Only populate the fields `JiraClient` actually reads; omit the rest, exactly as Jira does. Values are assembled as maps/lists and serialized with Jackson, so output is always well-formed. **Do not hand-write JSON string literals in a step definition.**
- **`JiraStubs`** — registers MockServer expectations matched the same way `JiraClient` issues them: path plus the *discriminating* query params, with values as full-match regexes so the two search variants and two expand variants stay distinct.

```java
JiraStubs.activeSprintSearch(JiraJson.searchResponse(issue1, issue2));
```

Note `activeSprintSearch` also stubs the sprint-resolution call (a wide-date "RIO" sprint, id 100) — because the client resolves the sprint *before* querying issues. If you add a call that resolves the sprint differently, the stub needs the same two-step treatment or the test will fail in a confusing way.

## Verifying against a real instance

Stubs prove the parsing, not the JQL or the field paths. Before trusting a new call:

```bash
./scripts/check-jira.sh   # custom-field ids, board id, statuses, link types, endpoint support
```

`check-jira.sh` is read-only and covers every assumption the server makes — run it when onboarding a new Jira instance.
