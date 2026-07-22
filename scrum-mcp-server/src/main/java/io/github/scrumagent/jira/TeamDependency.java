package io.github.scrumagent.jira;

import java.time.Instant;

/**
 * A cross-team dependency as this instance models it: a {@code Dependency}-type issue linked to an
 * Epic (via Epic Link), naming the team we wait on and the team waiting. Curated ingredients only —
 * no {@code open} flag and no verdict; a handler decides "open" from a resolved-status set and
 * "a blocker we face" from the configured team name.
 *
 * @param key the Dependency issue's key
 * @param summary its summary
 * @param status current status name
 * @param statusCategory the status's category name
 * @param blockingTeam the "Depend On" option value — the team WE wait on (the blocker)
 * @param waitingTeam the "Dependent/s" option value — the team WAITING (the consumer)
 * @param epicKey the Epic this Dependency is linked to (Epic Link), or {@code null}
 * @param deliverySprint the delivery sprint's name, best-effort parsed, or {@code null}
 * @param deliveryEnd the delivery sprint's end instant, best-effort parsed, or {@code null}
 */
public record TeamDependency(
        String key,
        String summary,
        String status,
        String statusCategory,
        String blockingTeam,
        String waitingTeam,
        String epicKey,
        String deliverySprint,
        Instant deliveryEnd) {
}
