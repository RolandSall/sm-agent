package io.github.scrumagent.jira;

import java.time.Instant;

/**
 * Curated Agile sprint (from {@code /rest/agile/1.0}).
 *
 * @param state one of {@code future}, {@code active}, {@code closed}
 * @param completeDate when the sprint was actually completed, or {@code null} if not closed
 */
public record JiraSprint(
        long id,
        String name,
        String state,
        Instant startDate,
        Instant endDate,
        Instant completeDate,
        String goal) {
}
