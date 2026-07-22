---
name: java-code-review
description: Concrete Java and Spring Boot review rules — exception handling, resource management, concurrency, JPA/SQL injection, performance, and modern Java idioms. Use when reviewing Java changes, before committing, or when asked to check code quality in this repo.
paths: "**/*.java"
---

# Java / Spring code review

Rules adapted from [alirezarezvani/claude-skills](https://github.com/alirezarezvani/claude-skills) (MIT, © Alireza Rezvani), plus Spring and repo-specific additions. See [ATTRIBUTION.md](../ATTRIBUTION.md).

These are **greppable, concrete** checks — not general advice. For this repo's architectural rules, see [CLAUDE.md](../../../CLAUDE.md) and [scrum-architecture](../scrum-architecture/SKILL.md); those take priority over anything here.

## Exception handling

- Empty `catch` blocks — `catch (Exception e) {}` swallows the failure silently
- `e.printStackTrace()` as the only handling — use the logger
- Checked exception caught and neither re-thrown with context nor logged
- `throw new RuntimeException(e)` with no message — loses the context that would explain it
- `InterruptedException` caught without `Thread.currentThread().interrupt()` — breaks cooperative cancellation
- Catching `NullPointerException` — fix the cause instead

## Resource management

- `InputStream`, `OutputStream`, `Connection`, `ResultSet`, `PreparedStatement` not in try-with-resources
- Manual `finally { resource.close(); }` — replace with try-with-resources
- `HttpURLConnection` not disconnected

## Concurrency

- `ExecutorService.submit()` return value ignored — exceptions vanish
- `Thread.sleep()` used for synchronization — use `CountDownLatch`, `CompletableFuture`, or an await
- `CompletableFuture` chain with no `.exceptionally()` / `.handle()` terminal handler
- `synchronized` on a non-final field — the lock object can be swapped
- `HashMap` shared across threads — use `ConcurrentHashMap`

## Security

- JPQL/HQL or native SQL built by string concatenation — require named parameters or `CriteriaBuilder`
- **JQL built by concatenating user input** — the analog here; see the repo-specific section
- `@RequestMapping` with no explicit HTTP method on a state-changing endpoint
- User-controlled input into `Runtime.exec()` / `ProcessBuilder` without validation
- `ObjectInputStream.readObject()` on untrusted data
- Hardcoded credentials or JDBC URLs — environment or vault only

## Performance

- `String` concatenation in a loop — `StringBuilder`
- N+1 JPA/Hibernate queries — `JOIN FETCH` or `@BatchSize`
- `new ObjectMapper()` / `new Gson()` per request — share a singleton
- Fully iterating a `ResultSet` when only the first row is needed

## Modern Java (21 here)

- Raw types in new code — always parameterize
- `==` on `String` or boxed types — use `.equals()`
- Missing `@Override`
- `@SuppressWarnings` without a justification comment
- Prefer records for pure data carriers; `Optional<T>` over returning `null`; `instanceof` pattern matching over explicit casts; switch *expressions* where a value is returned; `.toList()` over `.collect(Collectors.toList())`

## Spring-specific

- Field injection (`@Autowired` on a field) — use constructor injection; this repo does throughout
- `@Transactional` on a private or self-invoked method — the proxy won't apply it
- `@Value` for anything that belongs in a typed `@ConfigurationProperties` record
- Broad `@ComponentScan` widening the context beyond the module
- Catching and swallowing `RestClientException` — a Jira outage must surface, not read as empty data

## Repo-specific — the ones that matter most here

- **Hardcoded `customfield_NNNNN`** anywhere outside `JiraProperties`. Custom-field ids are per-instance config.
- **`JsonNode` in a capability module.** Wire format stops at `JiraClient`.
- **`get(...)` on a `JsonNode`** where `path(...)` is meant — `get` returns null and NPEs; Jira omits absent fields routinely.
- **Logic in `*Tools` or `ScrumApiController`.** These are one-line dispatchers.
- **A magic threshold in a handler with no comment** explaining where the number came from.
- **A Tier-2 policy value that migrated into `application.yml`.**
- **`Instant.parse` on a Jira changelog timestamp** — the offset has no colon; use the `JIRA_DATETIME` formatter.
- **Assuming one active sprint** — always resolve via `activeSprint()`.
- **A capability module importing another capability module** — `ModularityTests` will fail, but catch it in review first.

## Reviewing output, not just code

This server feeds numbers to a Scrum Master who acts on them. Two questions no compiler asks:

1. **Can this report a confidently wrong number?** Silent fallbacks and defaulted-to-zero paths are worse than an error here.
2. **Does absent data read as a real zero?** Empty worklog must not present as "nobody worked". If a field can be legitimately absent, the report and its skill must say what absence means.
