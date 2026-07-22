package io.github.scrumagent.shared;

/**
 * Shared, handler-side issue-attribute filters. Every active-sprint read tool applies the same rule
 * so an agent can narrow any report to one issue type or one assignee without a new query shape.
 *
 * <p>Filters are OPTIONAL by design: a {@code null}/blank filter matches everything (so an omitted
 * filter is byte-for-byte the pre-filter behaviour). A supplied filter is a case-insensitive,
 * trim-tolerant exact match against the issue's value — {@code "story"} matches {@code "Story"} and
 * {@code " Alice "} matches {@code "Alice"}.
 */
public final class IssueFilters {

    private IssueFilters() {
    }

    /**
     * @param filter the agent-supplied filter, or {@code null}/blank for "no filter"
     * @param value the issue's value for that attribute (may be {@code null} when absent/unassigned)
     * @return {@code true} when the filter is absent, or when it case-insensitively trim-equals value
     */
    public static boolean matches(String filter, String value) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        return value != null && filter.trim().equalsIgnoreCase(value.trim());
    }
}
