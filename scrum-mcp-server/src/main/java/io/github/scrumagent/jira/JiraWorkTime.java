package io.github.scrumagent.jira;

/**
 * Curated time-tracking rollup for one issue, in hours. Logged hours come from the configured
 * custom field (some instances track work in a custom field, not the native worklog); estimates
 * come from Jira's {@code timetracking} block.
 *
 * @param status the issue's status name ({@code fields.status.name}), or {@code null}
 * @param statusCategory Jira's built-in bucket ({@code To Do} / {@code In Progress} / {@code Done}) —
 *        instance-independent, so estimate-variance flags never need configured status names
 * @param loggedHours from the configured logged-hours custom field, or {@code null} if absent
 * @param originalEstimateHours from {@code timetracking.originalEstimateSeconds}, or {@code null}
 * @param remainingEstimateHours from {@code timetracking.remainingEstimateSeconds}, or {@code null}
 */
public record JiraWorkTime(
        String key,
        String type,
        String assignee,
        String status,
        String statusCategory,
        Double loggedHours,
        Double originalEstimateHours,
        Double remainingEstimateHours) {
}
