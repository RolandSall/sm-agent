package io.github.scrumagent.jira.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Timestamp parsing + day math for Jira's wire format. Pure functions moved verbatim from the
 * former JiraClient god class so the parsing behaviour (and thus every derived metric) is unchanged.
 */
final class JiraTime {

    private static final Logger log = LoggerFactory.getLogger(JiraTime.class);

    /** Jira's changelog timestamps: {@code 2026-07-12T09:30:00.000+0000} (offset without a colon). */
    private static final DateTimeFormatter JIRA_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ROOT);

    private JiraTime() {
    }

    static Instant parseInstant(String text) {
        OffsetDateTime odt = parseOffset(text);
        return odt == null ? null : odt.toInstant();
    }

    /** Lenient parse: ISO-8601 (agile dates, often {@code ...Z}) then Jira's colon-less offset. */
    static OffsetDateTime parseOffset(String text) {
        if (text == null || text.isBlank() || "null".equals(text)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (RuntimeException ignored) {
            try {
                return OffsetDateTime.parse(text, JIRA_DATETIME);
            } catch (RuntimeException e) {
                log.debug("Unparseable Jira timestamp: {}", text);
                return null;
            }
        }
    }

    /** Whole days between two instants (a before b). */
    static long daysBetween(Instant a, Instant b) {
        return Duration.between(a, b).toDays();
    }
}
