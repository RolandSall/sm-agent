package io.github.scrumagent.jira;

/**
 * Curated flow snapshot for one active-sprint issue, derived from the changelog. Drives the
 * daily-standup "what is stuck / aging / stalled" view. {@code statusCategory} is Jira's built-in
 * bucket ({@code To Do} / {@code In Progress} / {@code Done}) so flow rules never need configured
 * status names. Day counts are boxed so "unknown" (no changelog signal) stays distinct from zero.
 *
 * @param ageInStatusDays whole days since the LATEST status change, or {@code null} if the issue has
 *        never changed status (so we cannot tell how long it has sat where it is)
 * @param daysSinceUpdated whole days since {@code fields.updated}, or {@code null} if absent
 * @param reopenCount status changes made after the issue first reached a done-like status — a proxy
 *        for churn / reopens
 */
public record SprintIssueFlow(
        String key,
        String summary,
        String type,
        String assignee,
        String status,
        String statusCategory,
        Long ageInStatusDays,
        Long daysSinceUpdated,
        int reopenCount) {
}
